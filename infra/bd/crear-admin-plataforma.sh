#!/bin/sh
# Nombre: crear-admin-plataforma.sh
# Descripción: Crea el primer administrador de la plataforma (RF-01, RP-15): corre
#              crear-admin-plataforma.sql con psql dentro del contenedor de la base. Para Linux, macOS,
#              Git Bash en Windows y la EC2.
# Fecha de creación: 13/10/2026
# Actualización: 13/10/2026
# Autor: Equipo InspectIA
# Versión: 1.0
#
# Uso, con la base encendida y después de arrancar el backend una vez (Flyway crea las tablas):
#   sh infra/bd/crear-admin-plataforma.sh
#
# Datos: INSPECTIA_ADMIN_NOMBRE, INSPECTIA_ADMIN_CORREO e INSPECTIA_ADMIN_CONTRASENA. docker compose los
# toma del entorno o, si no están, de infra/.env; en la EC2 vendrán de AWS SSM. Los valores no pasan por
# la línea de comandos: con «-e NOMBRE», sin valor, docker compose copia la variable al contenedor.

set -eu

infra=$(cd "$(dirname "$0")/.." && pwd)
compose="$infra/docker-compose.yml"

if ! docker compose -f "$compose" ps --status running --services | grep -qx postgres; then
    echo "La base no está encendida. Enciéndela con: docker compose -f infra/docker-compose.yml up -d" >&2
    exit 1
fi

docker compose -f "$compose" exec -T \
    -e INSPECTIA_ADMIN_NOMBRE -e INSPECTIA_ADMIN_CORREO -e INSPECTIA_ADMIN_CONTRASENA \
    -e PGCLIENTENCODING=UTF8 \
    postgres sh -c 'exec psql -X -q -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
    < "$infra/bd/crear-admin-plataforma.sql"
