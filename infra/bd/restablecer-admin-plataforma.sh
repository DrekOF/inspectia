#!/bin/sh
# Nombre: restablecer-admin-plataforma.sh
# Descripción: Restablece la contraseña del administrador de la plataforma (D-30, RF-18): pide la
#              contraseña nueva por teclado y corre restablecer-admin-plataforma.sql con psql dentro del
#              contenedor de la base. Para Linux, macOS, Git Bash en Windows y la EC2.
# Fecha de creación: 19/10/2026
# Actualización: 19/10/2026
# Autor: Equipo InspectIA
# Versión: 1.0
#
# Uso, con la base encendida:
#   sh infra/bd/restablecer-admin-plataforma.sh
#
# Datos:
#   - El correo sale de INSPECTIA_ADMIN_CORREO, del entorno o de infra/.env. Si no está en ninguno, lo pide
#     por teclado y se ve al escribirlo: así se puede correr en el servidor sin crear archivos.
#   - La contraseña nueva siempre se pide por teclado, dos veces y sin mostrarla: no queda en ningún archivo
#     ni en el historial de la consola. Viaja en INSPECTIA_ADMIN_CONTRASENA_NUEVA, una variable que nunca
#     está en infra/.env: si docker compose no encuentra una variable en el entorno, la busca allí, y con
#     INSPECTIA_ADMIN_CONTRASENA tomaría la contraseña del script de creación.
# Los valores no pasan por la línea de comandos: con «-e NOMBRE», sin valor, docker compose copia la
# variable al contenedor.

set -eu

infra=$(cd "$(dirname "$0")/.." && pwd)
compose="$infra/docker-compose.yml"

if ! docker compose -f "$compose" ps --status running --services | grep -qx postgres; then
    echo "La base no está encendida. Enciéndela con: docker compose -f infra/docker-compose.yml up -d" >&2
    exit 1
fi

# El valor de una variable en infra/.env, sin las comillas que lo rodeen; vacío si no está.
valor_en_env() {
    [ -f "$infra/.env" ] || return 0
    sed -n "s/^$1=//p" "$infra/.env" | tail -n 1 | tr -d '\r' \
        | sed -e "s/^'\(.*\)'\$/\1/" -e 's/^"\(.*\)"$/\1/'
}

INSPECTIA_ADMIN_CORREO=${INSPECTIA_ADMIN_CORREO:-$(valor_en_env INSPECTIA_ADMIN_CORREO)}
if [ -z "$INSPECTIA_ADMIN_CORREO" ]; then
    printf 'Correo del administrador de la plataforma: '
    IFS= read -r INSPECTIA_ADMIN_CORREO || true
fi

# En la consola, lo que se escribe no se muestra; si el script se interrumpe, se vuelve a mostrar.
if [ -t 0 ]; then
    trap 'stty echo' EXIT
    trap 'stty echo; exit 130' INT TERM
fi

# Lee una línea tal cual, con sus espacios, sin mostrarla en pantalla.
leer_sin_mostrar() {
    printf '%s' "$1"
    if [ -t 0 ]; then
        stty -echo
    fi
    IFS= read -r linea || linea=''
    if [ -t 0 ]; then
        stty echo
    fi
    printf '\n'
}

leer_sin_mostrar 'Contraseña nueva: '
INSPECTIA_ADMIN_CONTRASENA_NUEVA=$linea
leer_sin_mostrar 'Repítela: '
if [ "$INSPECTIA_ADMIN_CONTRASENA_NUEVA" != "$linea" ]; then
    echo "Las dos contraseñas no coinciden: no se cambió nada." >&2
    exit 1
fi
export INSPECTIA_ADMIN_CORREO INSPECTIA_ADMIN_CONTRASENA_NUEVA

docker compose -f "$compose" exec -T \
    -e INSPECTIA_ADMIN_CORREO -e INSPECTIA_ADMIN_CONTRASENA_NUEVA \
    -e PGCLIENTENCODING=UTF8 \
    postgres sh -c 'exec psql -X -q -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
    < "$infra/bd/restablecer-admin-plataforma.sql"
