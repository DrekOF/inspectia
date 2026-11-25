# shellcheck shell=bash
# Nombre: comun.sh
# Descripción: Funciones que comparten los scripts de producción de /opt/inspectia (D-16): el docker compose del
#              despliegue, la cuenta de AWS de la instancia (por IMDSv2, sin claves), los valores de .env y el
#              registro de cada script en /var/log/inspectia, que el agente de CloudWatch envía a /inspectia/servidor.
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
#
# Uso: no se corre solo; lo cargan actualizar.sh, respaldar.sh, restaurar-respaldo.sh y admin-plataforma.sh con
#   source "$(dirname "$0")/comun.sh"

# Carpeta del despliegue: la de los scripts (en la instancia, /opt/inspectia).
DIR_INSPECTIA=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
export DIR_INSPECTIA
export AWS_REGION=us-east-1
export AWS_DEFAULT_REGION=us-east-1

# docker compose sobre el compose de producción, desde cualquier carpeta.
compose() {
    docker compose --project-directory "$DIR_INSPECTIA" -f "$DIR_INSPECTIA/docker-compose.yml" "$@"
}

# Un valor de /opt/inspectia/.env (lo escribe actualizar.sh); vacío si no está.
valor_en_env() {
    [ -f "$DIR_INSPECTIA/.env" ] || return 0
    sed -n "s/^$1=//p" "$DIR_INSPECTIA/.env" | tail -n 1
}

# El ID de la cuenta de AWS de la instancia, del documento de identidad de IMDSv2.
cuenta_aws() {
    local token documento cuenta
    token=$(curl -fsS --retry 3 -X PUT 'http://169.254.169.254/latest/api/token' \
        -H 'X-aws-ec2-metadata-token-ttl-seconds: 300')
    documento=$(curl -fsS --retry 3 -H "X-aws-ec2-metadata-token: $token" \
        'http://169.254.169.254/latest/dynamic/instance-identity/document')
    cuenta=$(sed -n 's/.*"accountId"[[:space:]]*:[[:space:]]*"\([0-9]\{12\}\)".*/\1/p' <<< "$documento")
    if [ -z "$cuenta" ]; then
        echo 'No se pudo leer la cuenta de AWS de la instancia (IMDSv2).' >&2
        return 1
    fi
    echo "$cuenta"
}

# Copia la salida del script (también los errores) a /var/log/inspectia/<nombre>.log, sin dejar de mostrarla.
registrar() {
    install -d -m 755 /var/log/inspectia
    exec > >(tee -a "/var/log/inspectia/$1.log") 2>&1
}

# Título de un paso, con la hora en UTC.
paso() {
    printf '\n== %s · %s\n' "$(date -u '+%Y-%m-%d %H:%M:%S UTC')" "$*"
}

# Termina con un mensaje de error.
fallar() {
    echo "ERROR: $*" >&2
    exit 1
}
