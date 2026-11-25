#!/usr/bin/env bash
# Nombre: admin-plataforma.sh
# Descripción: El administrador de la plataforma en producción (RF-01, D-30, RP-15): crea el primero o restablece
#              su contraseña con los mismos scripts SQL de infra/bd (que el despliegue deja en /opt/inspectia/bd),
#              contra la base del compose de producción. Pide los datos por teclado y la contraseña sin mostrarla,
#              así que no quedan en ningún archivo, en el historial ni en Session Manager.
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
#
# Uso, en la instancia por Session Manager, con InspectIA arriba (el backend ya creó las tablas):
#   sudo /opt/inspectia/admin-plataforma.sh crear
#   sudo /opt/inspectia/admin-plataforma.sh restablecer
# Las reglas de los datos y lo que hace cada uno están en infra/bd/*.sql y en infra/README.md.

set -euo pipefail

# shellcheck source=infra/produccion/comun.sh
source "$(dirname "$0")/comun.sh"

[ $# -eq 1 ] && { [ "$1" = crear ] || [ "$1" = restablecer ]; } \
    || { echo 'Uso: admin-plataforma.sh crear | restablecer' >&2; exit 2; }
[ -t 0 ] || fallar 'Pide los datos por teclado: córrelo en una sesión de Session Manager.'
compose ps --status running --services | grep -x postgres > /dev/null || fallar 'La base no está encendida.'

# Pide la contraseña dos veces, sin mostrarla, y la deja en la variable «contrasena».
pedir_contrasena() {
    local repetida
    read -r -s -p "$1" contrasena
    echo
    read -r -s -p 'Repítela: ' repetida
    echo
    [ "$contrasena" = "$repetida" ] || fallar 'Las dos contraseñas no coinciden: no se cambió nada.'
}

# Corre un script de bd/ con psql dentro del contenedor. Con «-e NOMBRE», sin valor, docker compose copia la
# variable al contenedor: los datos no pasan por la línea de comandos.
correr_sql() {
    local sql=$1
    shift
    compose exec -T "$@" -e PGCLIENTENCODING=UTF8 postgres \
        sh -c 'exec psql -X -q -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < "$DIR_INSPECTIA/bd/$sql"
}

if [ "$1" = crear ]; then
    read -r -p 'Nombre del administrador de la plataforma: ' INSPECTIA_ADMIN_NOMBRE
    read -r -p 'Correo: ' INSPECTIA_ADMIN_CORREO
    pedir_contrasena 'Contraseña inicial: '
    INSPECTIA_ADMIN_CONTRASENA=$contrasena
    export INSPECTIA_ADMIN_NOMBRE INSPECTIA_ADMIN_CORREO INSPECTIA_ADMIN_CONTRASENA
    correr_sql crear-admin-plataforma.sql \
        -e INSPECTIA_ADMIN_NOMBRE -e INSPECTIA_ADMIN_CORREO -e INSPECTIA_ADMIN_CONTRASENA
else
    read -r -p 'Correo del administrador de la plataforma: ' INSPECTIA_ADMIN_CORREO
    pedir_contrasena 'Contraseña nueva: '
    # Otra variable que la de crear, como en infra/bd/restablecer-admin-plataforma.sh.
    INSPECTIA_ADMIN_CONTRASENA_NUEVA=$contrasena
    export INSPECTIA_ADMIN_CORREO INSPECTIA_ADMIN_CONTRASENA_NUEVA
    correr_sql restablecer-admin-plataforma.sql -e INSPECTIA_ADMIN_CORREO -e INSPECTIA_ADMIN_CONTRASENA_NUEVA
fi
echo 'Listo.'
