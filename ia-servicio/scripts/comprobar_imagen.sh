#!/usr/bin/env bash
# Nombre: comprobar_imagen.sh
# Descripción: Arranca una imagen ya construida del servicio de IA y comprueba que carga TensorFlow, OpenCV, Pillow y
#              la API; con la ruta del zip del modelo, corre además la prueba de paridad (test_paridad.py) dentro de
#              la imagen. En la CI se corre en el runner arm64 después de construirla, antes de subirla a ECR (D-16).
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
#
# Uso (desde cualquier carpeta; la imagen debe estar en el Docker local, por ejemplo con «load: true»):
#   bash ia-servicio/scripts/comprobar_imagen.sh <imagen> [<ruta del zip del SavedModel de EfficientNet-B0>]
#
# Sin el zip solo comprueba las importaciones. Con el zip monta pruebas/ y pytest.ini (solo lectura) y el zip, instala
# pytest y httpx con las versiones de requirements-dev.txt en /tmp del contenedor (sin tocar la imagen) y corre
# test_paridad.py: falla si una prueba falla o se salta.
set -eu

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
  echo "Uso: $0 <imagen> [<zip del modelo>]" >&2
  exit 2
fi
IMAGEN=$1
ZIP=${2:-}

# En Git Bash de Windows, Docker necesita rutas como C:/...: se piden con «pwd -W» y no se convierten.
export MSYS_NO_PATHCONV=1
absoluta() {
  (cd "$1" && (pwd -W 2>/dev/null || pwd))
}
SERVICIO=$(absoluta "$(dirname "$0")/..")

echo "== Importaciones en $IMAGEN"
docker run --rm --entrypoint python "$IMAGEN" -c '
import platform
import cv2
import PIL
import tensorflow as tf
import inspectia_ia.api
print("arquitectura", platform.machine(), "| tensorflow", tf.__version__, "| opencv", cv2.__version__,
      "| pillow", PIL.__version__)
'

if [ -z "$ZIP" ]; then
  echo "== Sin el zip del modelo: no se corre la prueba de paridad"
  exit 0
fi
if [ ! -f "$ZIP" ]; then
  echo "No existe el zip del modelo: $ZIP" >&2
  exit 1
fi
CARPETA_ZIP=$(absoluta "$(dirname "$ZIP")")
NOMBRE_ZIP=$(basename "$ZIP")
version() {
  grep -E "^$1==" "$SERVICIO/requirements-dev.txt" | tr -d '\r' || true
}
PYTEST=$(version pytest)
HTTPX=$(version httpx)
if [ -z "$PYTEST" ] || [ -z "$HTTPX" ]; then
  echo "pytest y httpx deben tener versión en requirements-dev.txt" >&2
  exit 1
fi

echo "== Prueba de paridad en $IMAGEN con $NOMBRE_ZIP"
SALIDA=$(mktemp)
# PYTHONUSERBASE en /tmp: el usuario de la imagen no puede escribir en /app, y con --user pip no reinstala lo que la
# imagen ya trae.
set +e
docker run --rm --entrypoint sh \
  -v "$SERVICIO/pruebas:/app/pruebas:ro" \
  -v "$SERVICIO/pytest.ini:/app/pytest.ini:ro" \
  -v "$CARPETA_ZIP:/modelo:ro" \
  -e "INSPECTIA_MODELO_PRUEBA=/modelo/$NOMBRE_ZIP" \
  -e PYTHONUSERBASE=/tmp/dev \
  -e "PYTEST=$PYTEST" -e "HTTPX=$HTTPX" \
  "$IMAGEN" -c 'python -m pip install --quiet --user --no-warn-script-location "$PYTEST" "$HTTPX" &&
    python -m pytest -p no:cacheprovider -rs pruebas/test_paridad.py' 2>&1 | tee "$SALIDA"
CODIGO=${PIPESTATUS[0]}
set -e
if [ "$CODIGO" -ne 0 ]; then
  rm -f "$SALIDA"
  echo "La prueba de paridad falló dentro de la imagen" >&2
  exit "$CODIGO"
fi
if grep -q 'skipped' "$SALIDA"; then
  rm -f "$SALIDA"
  echo "La prueba de paridad se saltó dentro de la imagen: no encontró el modelo" >&2
  exit 1
fi
rm -f "$SALIDA"
echo "== La imagen $IMAGEN carga sus dependencias y clasifica como inferencia.py"
