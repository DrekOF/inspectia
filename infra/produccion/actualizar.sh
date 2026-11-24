#!/usr/bin/env bash
# Nombre: actualizar.sh
# Descripción: Levanta o actualiza InspectIA en la instancia (D-16): lee de SSM la clave de los tokens y la
#              contraseña de la base con el rol de la instancia y las deja solo en memoria (/run/inspectia/secretos,
#              permisos 400), inicia sesión en ECR, baja las imágenes de la etiqueta pedida y levanta el compose,
#              esperando a que los tres servicios estén sanos. Lo corren el despliegue de la CI (por SSM, con la
#              etiqueta del commit) y el servicio inspectia de systemd al encender la instancia (sin etiqueta: usa la
#              última desplegada).
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
#
# Uso, como root en la instancia:
#   /opt/inspectia/actualizar.sh <etiqueta>   # despliega esa etiqueta (el commit), como hace la CI
#   /opt/inspectia/actualizar.sh              # vuelve a levantar la última, como al encender
# Volver a la versión anterior: sudo /opt/inspectia/actualizar.sh "$(sed -n 's/^ETIQUETA_ANTERIOR=//p' /opt/inspectia/.env)"
# (si la nueva cambió la base con una migración, la anterior no arranca: ver infra/README.md, «Volver atrás»).

set -euo pipefail

# shellcheck source=infra/produccion/comun.sh
source "$(dirname "$0")/comun.sh"

# Usuario del backend dentro de su imagen (backend/Dockerfile): sus secretos son suyos y de nadie más.
UID_BACKEND=10001
SECRETOS=/run/inspectia/secretos

registrar actualizar

# Una sola actualización a la vez: al encender, la del servicio de systemd y la de la CI pueden coincidir.
install -d -m 700 /run/inspectia
exec 9> /run/inspectia/actualizar.lock
flock -w 1200 9 || fallar 'Otra actualización sigue corriendo desde hace 20 minutos.'

# La etiqueta se lee después de tomar el turno: si la CI desplegó otra mientras tanto, se usa esa.
etiqueta=${1:-$(valor_en_env ETIQUETA)}
[ -n "$etiqueta" ] || fallar 'Falta la etiqueta: todavía no hay un despliegue. Córrelo con la del commit.'
[[ $etiqueta =~ ^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$ ]] || fallar "La etiqueta «$etiqueta» no es válida."

paso "InspectIA $etiqueta"
cuenta=$(cuenta_aws)
registro="$cuenta.dkr.ecr.$AWS_REGION.amazonaws.com"

# Lee un parámetro SecureString de SSM, revisa su forma y lo deja en un archivo de /run/inspectia/secretos que
# solo lee el backend (y root, como el arranque de PostgreSQL). El valor nunca se muestra.
#   $1: nombre del archivo (el de la variable de entorno que lo recibe)
#   $2: nombre del parámetro en SSM
#   $3: expresión regular que debe cumplir
#   $4: qué debe ser, para el mensaje de error
escribir_secreto() {
    local valor temporal
    valor=$(aws ssm get-parameter --name "$2" --with-decryption --query Parameter.Value --output text) \
        || fallar "No se pudo leer $2 de SSM. ¿Existe y el rol inspectia-ec2 puede leerlo?"
    [[ $valor =~ $3 ]] || fallar "El parámetro $2 no es válido: debe ser $4 (ver infra/README.md)."
    # Si Docker creó una carpeta con ese nombre (un contenedor que arrancó sin el secreto), se quita.
    [ ! -d "$SECRETOS/$1" ] || rm -rf "${SECRETOS:?}/$1"
    temporal=$(mktemp "$SECRETOS/.secreto.XXXXXX")
    printf '%s' "$valor" > "$temporal"
    chown "$UID_BACKEND:$UID_BACKEND" "$temporal"
    chmod 400 "$temporal"
    mv -f "$temporal" "$SECRETOS/$1"
}

paso 'Secretos de SSM, en memoria'
umask 077
install -d -m 700 "$SECRETOS"
escribir_secreto INSPECTIA_JWT_CLAVE /inspectia/jwt-clave '^[A-Za-z0-9+/]{43,}={0,2}$' \
    '32 bytes o más en Base64, como la que da «openssl rand -base64 32»'
escribir_secreto POSTGRES_PASSWORD /inspectia/postgres-contrasena '^[A-Za-z0-9._~+-]{20,}$' \
    'de 20 caracteres o más, solo letras sin tildes, números y . _ ~ + -'
umask 022

paso 'Etiqueta y cuenta en /opt/inspectia/.env (sin secretos)'
anterior=$(valor_en_env ETIQUETA)
if [ "$anterior" = "$etiqueta" ]; then
    anterior=$(valor_en_env ETIQUETA_ANTERIOR)
fi
cat > "$DIR_INSPECTIA/.env.nuevo" <<FIN
# Lo escribe actualizar.sh en cada despliegue. Sin secretos: esos están en $SECRETOS, en memoria.
AWS_CUENTA=$cuenta
ETIQUETA=$etiqueta
ETIQUETA_ANTERIOR=$anterior
FIN
mv -f "$DIR_INSPECTIA/.env.nuevo" "$DIR_INSPECTIA/.env"

paso "Sesión en ECR ($registro)"
aws ecr get-login-password | docker login --username AWS --password-stdin "$registro" > /dev/null

paso 'Imágenes y contenedores'
# --pull missing: baja las imágenes de la etiqueta si no están (al encender ya están y no baja nada).
# --wait: espera a que los tres estén sanos; el servicio de IA puede tardar unos minutos si baja el modelo de S3.
if ! compose up -d --pull missing --remove-orphans --wait --wait-timeout 900; then
    paso 'No arrancó. Estado y últimos registros:'
    compose ps -a || true
    compose logs --no-color --tail 80 backend ia-servicio || true
    fallar "InspectIA $etiqueta no quedó sano. La versión anterior era «$anterior»."
fi

paso 'Limpieza de imágenes viejas (las de más de 3 días que nadie usa)'
docker image prune -af --filter 'until=72h' > /dev/null || true

compose ps
paso "InspectIA $etiqueta quedó arriba y sano."
