#!/usr/bin/env bash
# Nombre: probar.sh
# Descripción: Pruebas de la infraestructura del despliegue (D-16, RP-16), sin red y sin construir imágenes: la
#              sintaxis de los scripts (bash -n y, si está, ShellCheck), los JSON de AWS, el docker-compose.yml de
#              producción armado con valores de ejemplo, la CloudFront Function de las rutas de React, las
#              políticas IAM y el script de arranque de la instancia. Lo corren la CI (job «infra») y el equipo.
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
#
# Uso, desde la raíz del repositorio (Linux, macOS o Git Bash; necesita Node y docker compose, sin red):
#   bash infra/probar.sh

set -euo pipefail

cd "$(dirname "$0")/.."

echo '== Sintaxis de los scripts (bash -n)'
mapfile -t scripts < <(find infra -name '*.sh' -type f | sort)
for script in "${scripts[@]}"; do
    bash -n "$script"
    echo "   $script"
done
# El de entrada del backend corre con sh en la imagen: se revisa con sh.
sh -n backend/docker/entrada.sh
echo '   backend/docker/entrada.sh'

echo '== ShellCheck (los scripts del despliegue)'
if command -v shellcheck > /dev/null; then
    mapfile -t despliegue < <(find infra/aws infra/produccion -name '*.sh' -type f | sort)
    shellcheck --severity=warning infra/probar.sh "${despliegue[@]}"
    shellcheck --severity=warning --shell=sh backend/docker/entrada.sh
    echo '   sin advertencias'
else
    echo '   ShellCheck no está instalado: se salta aquí (en la CI sí corre).'
fi

echo '== JSON de AWS'
mapfile -t jsons < <(find infra/aws -name '*.json' -type f | sort)
node -e '
  const fs = require("node:fs");
  for (const archivo of process.argv.slice(1)) {
    JSON.parse(fs.readFileSync(archivo, "utf8"));
    console.log("   " + archivo);
  }
' "${jsons[@]}"

echo '== Pruebas'
node --test \
    infra/aws/cloudfront/rutas-react.test.mjs \
    infra/aws/iam/politicas.test.mjs \
    infra/aws/datos-de-usuario.test.mjs \
    infra/produccion/pruebas/compose.test.mjs
