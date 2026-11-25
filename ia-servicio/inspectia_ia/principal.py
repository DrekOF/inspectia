# Nombre: principal.py
# Descripción: Punto de entrada del servicio de IA para Uvicorn: arma la aplicación con la configuración de las
#              variables de entorno.
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Punto de entrada: ``uvicorn inspectia_ia.principal:app --host 0.0.0.0 --port 8000``."""

import logging

from inspectia_ia.api import crear_app

logging.basicConfig(level=logging.INFO, format='%(asctime)s %(levelname)s %(name)s: %(message)s')

app = crear_app()
