#!/bin/bash
# Nombre: datos-de-usuario.sh
# Descripción: Script de arranque (datos de usuario) de la instancia inspectia-servidor (D-16): Amazon Linux 2023
#              arm64 en una t4g.medium. Instala Docker con su plugin compose y el agente de CloudWatch, crea 2 GB de
#              swap, las carpetas del despliegue y los servicios de systemd que levantan InspectIA al encender la
#              instancia (EventBridge Scheduler la apaga de noche) y sacan la copia diaria de la base a S3. No trae
#              InspectIA: el primer despliegue de la CI deja en /opt/inspectia el compose y sus scripts.
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
#
# Uso: se pega completo en «Datos de usuario» al lanzar la instancia (ver infra/README.md). Corre una sola vez, como
# root, en el primer arranque; su salida queda en /var/log/cloud-init-output.log. Se puede volver a correr sin daño:
#   sudo bash datos-de-usuario.sh

set -euo pipefail

# Versión del plugin compose de Docker (Amazon Linux 2023 no lo trae en sus repositorios). Se baja del release
# oficial y se comprueba con su SHA-256 antes de instalarlo.
COMPOSE_VERSION=v5.6.0
SWAP_MB=2048

echo '== Paquetes: Docker, el agente de CloudWatch y logrotate'
dnf install -y docker amazon-cloudwatch-agent logrotate
# La CLI de AWS ya viene en Amazon Linux 2023; si no, se instala.
command -v aws > /dev/null || dnf install -y awscli-2

echo "== Plugin compose de Docker $COMPOSE_VERSION"
arquitectura=$(uname -m)
url="https://github.com/docker/compose/releases/download/$COMPOSE_VERSION/docker-compose-linux-$arquitectura"
temporal=$(mktemp -d)
curl -fsSL --retry 5 -o "$temporal/docker-compose" "$url"
curl -fsSL --retry 5 -o "$temporal/docker-compose.sha256" "$url.sha256"
echo "$(cut -d ' ' -f 1 "$temporal/docker-compose.sha256")  $temporal/docker-compose" | sha256sum -c -
install -D -m 755 "$temporal/docker-compose" /usr/libexec/docker/cli-plugins/docker-compose
rm -rf "$temporal"

echo '== Docker'
mkdir -p /etc/docker
# Los contenedores de InspectIA mandan sus registros a CloudWatch (awslogs, en el compose); cualquier otro, a un
# archivo que rota, para no llenar el disco.
cat > /etc/docker/daemon.json <<'JSON_DOCKER'
{
  "log-driver": "json-file",
  "log-opts": { "max-size": "10m", "max-file": "3" }
}
JSON_DOCKER
systemctl enable docker
systemctl restart docker
docker compose version

echo "== Swap de $SWAP_MB MB"
# 4 GB de memoria para PostgreSQL, el backend y TensorFlow: la swap evita que un pico mate un contenedor.
if ! swapon --show=NAME --noheadings | grep -x /swapfile > /dev/null; then
    [ -f /swapfile ] || dd if=/dev/zero of=/swapfile bs=1M count="$SWAP_MB" status=none
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
fi
grep -q '^/swapfile ' /etc/fstab || echo '/swapfile none swap defaults 0 0' >> /etc/fstab
# Solo usa la swap cuando de verdad falta memoria.
echo 'vm.swappiness=10' > /etc/sysctl.d/90-inspectia.conf
sysctl -q -p /etc/sysctl.d/90-inspectia.conf

echo '== Carpetas'
# /opt/inspectia: el compose y los scripts, que deja cada despliegue. Los secretos no van aquí: actualizar.sh los
# escribe en /run/inspectia, que está en memoria y se borra al apagar.
install -d -m 755 /opt/inspectia /opt/inspectia/bd
install -d -m 755 /var/log/inspectia
install -d -m 700 /var/lib/inspectia /var/lib/inspectia/respaldos
cat > /etc/logrotate.d/inspectia <<'LOGROTATE'
/var/log/inspectia/*.log {
    weekly
    rotate 4
    compress
    missingok
    notifempty
    copytruncate
}
LOGROTATE

echo '== Agente de CloudWatch'
# Memoria, disco y swap cada minuto (las alarmas de infra/README.md), y los registros del servidor. Los de los
# contenedores no pasan por aquí: Docker los manda a /inspectia/contenedores.
cat > /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json <<'JSON_CLOUDWATCH'
{
  "agent": {
    "metrics_collection_interval": 60,
    "run_as_user": "root",
    "omit_hostname": true
  },
  "metrics": {
    "namespace": "CWAgent",
    "append_dimensions": {
      "InstanceId": "${aws:InstanceId}"
    },
    "metrics_collected": {
      "mem": {
        "measurement": ["mem_used_percent"]
      },
      "swap": {
        "measurement": ["swap_used_percent"]
      },
      "disk": {
        "measurement": ["used_percent"],
        "resources": ["/"],
        "drop_device": true
      }
    }
  },
  "logs": {
    "logs_collected": {
      "files": {
        "collect_list": [
          {
            "file_path": "/var/log/inspectia/actualizar.log",
            "log_group_name": "/inspectia/servidor",
            "log_stream_name": "{instance_id}/actualizar",
            "retention_in_days": 30
          },
          {
            "file_path": "/var/log/inspectia/respaldo.log",
            "log_group_name": "/inspectia/servidor",
            "log_stream_name": "{instance_id}/respaldo",
            "retention_in_days": 30
          },
          {
            "file_path": "/var/log/cloud-init-output.log",
            "log_group_name": "/inspectia/servidor",
            "log_stream_name": "{instance_id}/arranque",
            "retention_in_days": 30
          }
        ]
      }
    }
  }
}
JSON_CLOUDWATCH
/opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -s \
    -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json

echo '== Servicios de systemd'
# Levanta InspectIA al encender la instancia: lee los secretos de SSM, que se borraron al apagar, y arranca los
# contenedores con la última etiqueta desplegada. Antes del primer despliegue no hay /opt/inspectia/.env y no hace
# nada. Al apagar, detiene los contenedores con orden, antes que Docker.
cat > /etc/systemd/system/inspectia.service <<'UNIDAD_INSPECTIA'
[Unit]
Description=InspectIA (docker compose de /opt/inspectia)
Requires=docker.service
After=docker.service network-online.target
Wants=network-online.target
PartOf=docker.service
ConditionPathExists=/opt/inspectia/.env

[Service]
Type=oneshot
RemainAfterExit=yes
ExecStart=/opt/inspectia/actualizar.sh
# Si falla, como cuando la red todavía no responde, lo vuelve a intentar.
Restart=on-failure
RestartSec=30
ExecStop=/usr/bin/docker compose --project-directory /opt/inspectia -f /opt/inspectia/docker-compose.yml stop
TimeoutStartSec=1200
TimeoutStopSec=120

[Install]
WantedBy=multi-user.target
UNIDAD_INSPECTIA

# La copia diaria de la base a s3://inspectia-archivos-<cuenta>/respaldos/.
cat > /etc/systemd/system/inspectia-respaldo.service <<'UNIDAD_RESPALDO'
[Unit]
Description=Copia de la base de InspectIA a S3
Requires=docker.service
After=inspectia.service
ConditionPathExists=/opt/inspectia/respaldar.sh

[Service]
Type=oneshot
ExecStart=/opt/inspectia/respaldar.sh
TimeoutStartSec=1800
UNIDAD_RESPALDO

# Cada día a las 12:00 de Colombia, cuando la instancia está encendida. Con Persistent, si a esa hora estaba
# apagada, la copia se saca al encender (después de que InspectIA arranca).
cat > /etc/systemd/system/inspectia-respaldo.timer <<'TEMPORIZADOR_RESPALDO'
[Unit]
Description=Copia diaria de la base de InspectIA a S3

[Timer]
OnCalendar=*-*-* 12:00:00 America/Bogota
Persistent=true
RandomizedDelaySec=300

[Install]
WantedBy=timers.target
TEMPORIZADOR_RESPALDO

systemctl daemon-reload
systemctl enable inspectia.service
systemctl enable --now inspectia-respaldo.timer

echo '== Listo. El primer despliegue llega desde GitHub Actions (job «despliegue») o a mano por SSM.'
