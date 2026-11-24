#!/bin/sh
# Nombre: entrada.sh
# Descripción: Entrada de la imagen del backend (D-16). Convierte cada archivo de /run/secrets en la variable de
#              entorno de su nombre (en producción, POSTGRES_PASSWORD e INSPECTIA_JWT_CLAVE, que el compose monta
#              desde SSM) y arranca el servidor con las opciones de JAVA_OPTS. Las variables quedan solo en el proceso:
#              no en la configuración del contenedor ni en el disco.
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0

set -eu

if [ -d /run/secrets ]; then
    for archivo in /run/secrets/*; do
        [ -f "$archivo" ] || continue
        nombre=${archivo##*/}
        # Solo nombres de variable válidos, como POSTGRES_PASSWORD.
        case "$nombre" in
            '' | [0-9]* | *[!A-Za-z0-9_]*) continue ;;
        esac
        valor=$(cat "$archivo")
        export "$nombre=$valor"
    done
fi

# JAVA_OPTS va sin comillas a propósito: son varias opciones, separadas por espacios.
# shellcheck disable=SC2086
exec java ${JAVA_OPTS:-} -jar /aplicacion/aplicacion.jar "$@"
