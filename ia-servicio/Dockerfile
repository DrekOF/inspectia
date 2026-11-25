# Nombre: Dockerfile
# Descripción: Imagen del servicio de IA de InspectIA (FastAPI, OpenCV y TensorFlow 2.21 en CPU), para amd64 y arm64
#              (la t4g.medium es arm64, D-16). El modelo no va en la imagen: se baja de S3 al arrancar, se comprueba
#              su SHA-256 y queda en el volumen /var/lib/inspectia/modelos (D-40).
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
#
# Construir: docker buildx build --platform linux/arm64 -t inspectia/ia-servicio ia-servicio
# Python 3.12, el mismo de las pruebas locales y de la CI y el que usó Julián para entrenar. Debian (slim) y no Alpine:
# TensorFlow y OpenCV no publican ruedas para musl.
FROM python:3.12.16-slim-trixie

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    TF_CPP_MIN_LOG_LEVEL=2 \
    INSPECTIA_MODELOS_CACHE=/var/lib/inspectia/modelos

WORKDIR /app

COPY requirements.txt .
RUN pip install -r requirements.txt

# Usuario sin privilegios, dueño solo de la caché del modelo.
RUN useradd --system --uid 10001 --home-dir /app --shell /usr/sbin/nologin inspectia \
    && mkdir -p /var/lib/inspectia/modelos \
    && chown inspectia /var/lib/inspectia/modelos

COPY modelos.json .
COPY inspectia_ia/ inspectia_ia/

USER inspectia
VOLUME ["/var/lib/inspectia/modelos"]
EXPOSE 8000

# 503 mientras el modelo carga: el arranque (bajar, comprobar y cargar) toma unos segundos.
HEALTHCHECK --interval=30s --timeout=5s --start-period=120s --retries=3 \
    CMD ["python", "-c", "import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://127.0.0.1:8000/salud', timeout=4).status == 200 else 1)"]

CMD ["uvicorn", "inspectia_ia.principal:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "1"]
