#!/usr/bin/env bash
# Nombre: actualizar-instancia.sh
# Descripción: Paso del job «despliegue» de la CI (D-16, RP-19) que actualiza la instancia inspectia-servidor por
#              AWS Systems Manager, sin llaves SSH: revisa que las dos imágenes del commit estén en ECR, enciende la
#              instancia si EventBridge Scheduler la apagó, empaqueta infra/produccion y los SQL de infra/bd, se los
#              lleva dentro del comando (en base64) a /opt/inspectia y corre actualizar.sh con la etiqueta del commit.
#              Espera el resultado: si el comando falla, este script falla y la CI abre el issue.
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
#
# Uso (en la CI, con las credenciales del rol inspectia-github; necesita la CLI de AWS, jq, tar y base64):
#   bash infra/aws/actualizar-instancia.sh <id de la instancia> <etiqueta: el SHA del commit>

set -euo pipefail

[ $# -eq 2 ] || { echo 'Uso: actualizar-instancia.sh <instancia> <etiqueta>' >&2; exit 2; }
instancia=$1
etiqueta=$2
[[ $instancia =~ ^i-[0-9a-f]{8,17}$ ]] || { echo "::error::«$instancia» no es el ID de una instancia (i-…)." >&2; exit 1; }
[[ $etiqueta =~ ^[0-9a-f]{40}$ ]] || { echo "::error::La etiqueta debe ser el SHA del commit." >&2; exit 1; }
export AWS_REGION=${AWS_REGION:-us-east-1}
raiz=$(cd "$(dirname "$0")/../.." && pwd)

echo '== Imágenes del commit en ECR'
for repositorio in inspectia/backend inspectia/ia-servicio; do
    if ! aws ecr describe-images --repository-name "$repositorio" --image-ids "imageTag=$etiqueta" > /dev/null; then
        echo "::error::Falta $repositorio:$etiqueta en ECR. ¿El job «imagenes» la subió? (sin Dockerfile no se construye)" >&2
        exit 1
    fi
    echo "   $repositorio:$etiqueta"
done

echo '== Paquete para /opt/inspectia'
paquete=$(mktemp -d)
parametros=$(mktemp)
trap 'rm -rf "$paquete" "$parametros"' EXIT
cp "$raiz"/infra/produccion/docker-compose.yml "$raiz"/infra/produccion/*.sh "$paquete/"
mkdir "$paquete/bd"
cp "$raiz"/infra/bd/*.sql "$paquete/bd/"
chmod 755 "$paquete"/*.sh
contenido=$(tar -C "$paquete" --owner=0 --group=0 -czf - . | base64 -w 0)
echo "   $(find "$paquete" -type f | wc -l) archivos, ${#contenido} bytes en base64"
# Los parámetros de un comando de SSM tienen un límite de tamaño: el paquete pesa unos pocos KB.
if [ "${#contenido}" -gt 60000 ]; then
    echo '::error::El paquete pasa de 60 KB: es demasiado para mandarlo dentro del comando de SSM.' >&2
    exit 1
fi

echo '== Estado de la instancia'
estado() {
    aws ec2 describe-instances --instance-ids "$instancia" --query 'Reservations[0].Instances[0].State.Name' --output text
}
case "$(estado)" in
    running) echo '   encendida' ;;
    pending) echo '   encendiéndose'; aws ec2 wait instance-running --instance-ids "$instancia" ;;
    stopping | stopped)
        # Apagada por EventBridge Scheduler: se enciende para que la página nueva y el servidor nuevo vayan juntos.
        # El horario la vuelve a apagar a su hora.
        echo '   apagada: se enciende'
        aws ec2 wait instance-stopped --instance-ids "$instancia"
        aws ec2 start-instances --instance-ids "$instancia" > /dev/null
        aws ec2 wait instance-running --instance-ids "$instancia"
        ;;
    *) echo "::error::La instancia está en «$(estado)»." >&2; exit 1 ;;
esac

echo '== Agente de SSM'
for _ in $(seq 1 60); do
    ping=$(aws ssm describe-instance-information --filters "Key=InstanceIds,Values=$instancia" \
        --query 'InstanceInformationList[0].PingStatus' --output text)
    [ "$ping" = 'Online' ] && break
    sleep 10
done
[ "$ping" = 'Online' ] || { echo '::error::El agente de SSM de la instancia no responde.' >&2; exit 1; }
echo '   en línea'

echo '== Comando de SSM'
# Comandos de AWS-RunShellScript: dejan el paquete en /opt/inspectia (su .env y los volúmenes de Docker no se
# tocan) y corren actualizar.sh. jq (@sh) pone el paquete y la etiqueta entre comillas para el shell.
jq -n --arg paquete "$contenido" --arg etiqueta "$etiqueta" '{
  commands: [
    "set -eu",
    "nuevo=$(mktemp -d)",
    ("echo " + ($paquete | @sh) + " | base64 -d | tar -xz --no-same-owner -C \"$nuevo\""),
    "install -d -m 755 /opt/inspectia /opt/inspectia/bd",
    "cp -a \"$nuevo\"/. /opt/inspectia/",
    "rm -rf \"$nuevo\"",
    ("bash /opt/inspectia/actualizar.sh " + ($etiqueta | @sh))
  ],
  executionTimeout: ["2100"]
}' > "$parametros"
comando=$(aws ssm send-command \
    --instance-ids "$instancia" \
    --document-name AWS-RunShellScript \
    --comment "InspectIA ${etiqueta:0:12}" \
    --parameters "file://$parametros" \
    --timeout-seconds 600 \
    --cloud-watch-output-config 'CloudWatchOutputEnabled=true,CloudWatchLogGroupName=/inspectia/despliegue' \
    --query Command.CommandId --output text)
echo "   $comando"

# Espera hasta 35 minutos, lo que dura el comando: bajar la imagen de TensorFlow la primera vez tarda y, si la
# versión nueva no queda sana, actualizar.sh espera hasta 15 minutos por ella y hasta 10 por la anterior.
resultado=Pending
for _ in $(seq 1 210); do
    sleep 10
    resultado=$(aws ssm get-command-invocation --command-id "$comando" --instance-id "$instancia" \
        --query Status --output text 2> /dev/null || echo Pending)
    case "$resultado" in
        Pending | InProgress | Delayed) ;;
        *) break ;;
    esac
done

echo '== Salida en la instancia (completa en CloudWatch Logs, /inspectia/despliegue)'
aws ssm get-command-invocation --command-id "$comando" --instance-id "$instancia" \
    --query StandardOutputContent --output text || true
errores=$(aws ssm get-command-invocation --command-id "$comando" --instance-id "$instancia" \
    --query StandardErrorContent --output text || true)
if [ -n "$errores" ] && [ "$errores" != 'None' ]; then
    echo '-- errores --'
    echo "$errores"
fi

if [ "$resultado" != 'Success' ]; then
    echo "::error::La actualización de la instancia terminó en «$resultado»." >&2
    exit 1
fi
echo "== La instancia quedó con InspectIA ${etiqueta:0:12}."
