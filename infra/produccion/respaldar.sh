#!/usr/bin/env bash
# Nombre: respaldar.sh
# Descripción: Copia diaria de la base de InspectIA a S3 (D-16, RP-15): saca un pg_dump comprimido (formato custom
#              de PostgreSQL) del contenedor de la base, comprueba que pg_restore lo puede leer y lo sube a
#              s3://inspectia-archivos-<cuenta>/respaldos/inspectia-<fecha en UTC>.dump. En la instancia queda solo
#              la última copia. Si algo falla, avisa al tema de SNS inspectia-alertas (el correo del equipo) y, aunque no
#              pueda avisar, deja la falla en su registro.
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
#
# Uso: lo corre cada día el temporizador inspectia-respaldo de systemd (ver infra/aws/datos-de-usuario.sh). A mano,
# en la instancia:
#   sudo systemctl start inspectia-respaldo.service     # o: sudo /opt/inspectia/respaldar.sh
# Su registro queda en /var/log/inspectia/respaldo.log y en CloudWatch (/inspectia/servidor).
# Restaurar una copia: restaurar-respaldo.sh.

set -euo pipefail

# shellcheck source=infra/produccion/comun.sh
source "$(dirname "$0")/comun.sh"

CARPETA=/var/lib/inspectia/respaldos

registrar respaldo

install -d -m 700 /run/inspectia
exec 8> /run/inspectia/respaldo.lock
flock -n 8 || { echo 'Ya hay una copia en curso: esta no se saca.'; exit 0; }

# Si la copia falla, el equipo se entera por correo (SNS inspectia-alertas). El aviso se arma antes de leer la cuenta:
# si falla ahí (IMDS no responde), no hay a qué tema de SNS avisar, pero la falla queda en respaldo.log, que el agente
# de CloudWatch envía a /inspectia/servidor.
cuenta=''
destino='s3://inspectia-archivos-<cuenta>/respaldos'
avisar_si_falla() {
    local codigo=$?
    [ "$codigo" -ne 0 ] || return 0
    echo "ERROR: falló la copia de la base a $destino (código $codigo) el $(date -u '+%Y-%m-%d %H:%M UTC')." >&2
    if [ -z "$cuenta" ]; then
        echo 'No se avisó por SNS: no se pudo leer la cuenta de AWS de la instancia. La falla queda en este registro.' >&2
        return 0
    fi
    aws sns publish --topic-arn "arn:aws:sns:$AWS_REGION:$cuenta:inspectia-alertas" \
        --subject 'InspectIA: falló la copia diaria de la base' \
        --message "La copia de la base a $destino falló (código $codigo) en $(hostname) el $(date -u '+%Y-%m-%d %H:%M UTC'). Su registro está en CloudWatch Logs, /inspectia/servidor, flujo <instancia>/respaldo." \
        > /dev/null || echo 'Tampoco se pudo avisar por SNS.' >&2
}
trap avisar_si_falla EXIT

paso 'Copia de la base a S3'
cuenta=$(cuenta_aws) || fallar 'No se pudo leer la cuenta de AWS de la instancia (IMDSv2).'
destino="s3://inspectia-archivos-$cuenta/respaldos"

install -d -m 700 "$CARPETA"
nombre="inspectia-$(date -u '+%Y-%m-%dT%H-%M-%SZ').dump"
parcial="$CARPETA/$nombre.parcial"

# Dentro del contenedor, psql y pg_dump entran como el superusuario de la imagen, sin contraseña (socket local).
compose exec -T postgres sh -c 'exec pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom' > "$parcial"
compose exec -T postgres pg_restore --list < "$parcial" > /dev/null \
    || fallar 'pg_restore no puede leer la copia recién sacada.'
mv -f "$parcial" "$CARPETA/$nombre"
echo "Copia local: $CARPETA/$nombre ($(du -h "$CARPETA/$nombre" | cut -f 1))"

aws s3 cp --only-show-errors "$CARPETA/$nombre" "$destino/$nombre"
echo "Subida a $destino/$nombre"

# En el disco de la instancia queda solo la última; las demás están en S3.
find "$CARPETA" -maxdepth 1 -type f -name 'inspectia-*.dump*' ! -name "$nombre" -delete
paso 'Copia terminada.'
