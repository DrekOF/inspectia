#!/usr/bin/env bash
# Nombre: restaurar-respaldo.sh
# Descripción: Restaura una copia de la base de InspectIA desde s3://inspectia-archivos-<cuenta>/respaldos/ (D-16,
#              RP-15). Con --prueba la restaura en una base aparte (inspectia_prueba), compara cuántas filas tiene
#              cada tabla con la base en uso y la borra: así se prueba que la copia sirve sin tocar producción. Sin
#              --prueba reemplaza la base en uso: antes saca una copia de lo que hay, detiene el backend, restaura y
#              lo vuelve a levantar.
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
#
# Uso, en la instancia por Session Manager:
#   sudo /opt/inspectia/restaurar-respaldo.sh --prueba ultimo                       # prueba la última copia
#   sudo /opt/inspectia/restaurar-respaldo.sh --prueba inspectia-2026-11-26T17-00-00Z.dump
#   sudo /opt/inspectia/restaurar-respaldo.sh ultimo                                # reemplaza la base (pide confirmar)
# La lista de copias: aws s3 ls s3://inspectia-archivos-<cuenta>/respaldos/

set -euo pipefail

# shellcheck source=infra/produccion/comun.sh
source "$(dirname "$0")/comun.sh"

CARPETA=/var/lib/inspectia/respaldos
BASE=inspectia
BASE_PRUEBA=inspectia_prueba

# Filas de cada tabla del esquema public, una por línea («tabla filas»), en orden.
CONTEO="SELECT table_name || ' ' || (xpath('/row/c/text()', query_to_xml(format('SELECT count(*) AS c FROM public.%I', table_name), false, true, '')))[1]::text
FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE' ORDER BY table_name"

uso() {
    echo 'Uso: restaurar-respaldo.sh [--prueba] <ultimo | inspectia-AAAA-MM-DDTHH-MM-SSZ.dump>' >&2
    exit 2
}

prueba=false
if [ "${1:-}" = '--prueba' ]; then
    prueba=true
    shift
fi
[ $# -eq 1 ] || uso
copia=$1

# Un comando de PostgreSQL dentro del contenedor, como el superusuario de la imagen.
pg() {
    compose exec -T postgres "$@"
}

# Filas por tabla de una base.
contar() {
    pg psql -X -q -At -U inspectia -d "$1" -c "$CONTEO"
}

cuenta=$(cuenta_aws)
origen="s3://inspectia-archivos-$cuenta/respaldos"

paso 'Copia que se restaura'
if [ "$copia" = 'ultimo' ]; then
    # Los nombres llevan la fecha en UTC: el último en orden alfabético es el más reciente.
    copia=$(aws s3 ls "$origen/" | awk '{print $4}' | grep -E '^inspectia-.*\.dump$' | sort | tail -n 1 || true)
    [ -n "$copia" ] || fallar "No hay copias en $origen/."
fi
[[ $copia =~ ^inspectia-[0-9TZ-]+\.dump$ ]] || fallar "«$copia» no es el nombre de una copia."
install -d -m 700 "$CARPETA"
archivo="$CARPETA/restaurar-$copia"
aws s3 cp --only-show-errors "$origen/$copia" "$archivo"
echo "$origen/$copia ($(du -h "$archivo" | cut -f 1))"

if $prueba; then
    paso "Prueba: restaurar en la base $BASE_PRUEBA, aparte de la de producción"
    pg dropdb -U inspectia --if-exists --force "$BASE_PRUEBA"
    pg createdb -U inspectia -O inspectia "$BASE_PRUEBA"
    pg pg_restore -U inspectia -d "$BASE_PRUEBA" --no-owner --exit-on-error < "$archivo"

    paso 'Filas por tabla: copia restaurada y base en uso'
    resultado=0
    diff <(contar "$BASE_PRUEBA") <(contar "$BASE") || resultado=$?
    contar "$BASE_PRUEBA" | sed 's/^/   /'
    pg dropdb -U inspectia --force "$BASE_PRUEBA"
    rm -f "$archivo"
    if [ "$resultado" -eq 0 ]; then
        paso "La copia $copia se restauró y tiene las mismas filas que la base en uso."
    else
        paso "La copia $copia se restauró, pero sus filas difieren de la base en uso (arriba, < la copia y > la base)."
        echo 'Es normal si alguien usó InspectIA después de la copia: saca una nueva y prueba otra vez.'
        echo '  sudo /opt/inspectia/respaldar.sh && sudo /opt/inspectia/restaurar-respaldo.sh --prueba ultimo'
        exit 1
    fi
    exit 0
fi

paso "Reemplazar la base de producción por $copia"
[ -t 0 ] || fallar 'Reemplazar la base pide confirmar en la consola: corre el script en una sesión de Session Manager.'
read -r -p 'Se borra la base en uso (antes se saca una copia). Escribe RESTAURAR para seguir: ' respuesta
[ "$respuesta" = 'RESTAURAR' ] || fallar 'No se cambió nada.'

paso 'Copia de lo que hay ahora, por si hay que volver'
"$DIR_INSPECTIA/respaldar.sh"

paso 'Restaurar'
compose stop backend
pg dropdb -U inspectia --force "$BASE"
pg createdb -U inspectia -O inspectia "$BASE"
pg pg_restore -U inspectia -d "$BASE" --no-owner --exit-on-error < "$archivo"
rm -f "$archivo"
compose up -d --wait --wait-timeout 600 backend
paso "Base restaurada desde $copia y backend arriba."
