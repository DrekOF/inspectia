# Nombre: conftest.py
# Descripción: Datos compartidos por las pruebas del generador y regla de marcar cada prueba con su caso.
# Fecha de creación: 13/10/2026
# Actualización: 13/10/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Datos compartidos por las pruebas del generador.

Cada prueba debe llevar ``@pytest.mark.caso("...")`` con la ficha o el requisito que cubre, para que
la lista de casos de prueba (RP-13) pueda armarse desde las pruebas.
"""

import copy
from pathlib import Path

import pytest

from inspectia_generador.carga import cargar_codigos, cargar_entidades

GENERADOR = Path(__file__).resolve().parents[1]
RAIZ = GENERADOR.parent
EJEMPLOS = Path(__file__).resolve().parent / 'ejemplos'
RUTA_MIGRACION = 'backend/src/generated/resources/db/migration/V1__esquema_inicial.sql'

_ENTIDADES = cargar_entidades(RAIZ / 'specs' / 'entidades')
_CODIGOS = cargar_codigos(RAIZ / 'specs')


def pytest_collection_modifyitems(config, items):
    """Rechaza las pruebas que no dicen qué caso cubren."""
    sin_caso = [item.nodeid for item in items if item.get_closest_marker('caso') is None]
    if sin_caso:
        raise pytest.UsageError('Pruebas sin @pytest.mark.caso (RP-13): ' + ', '.join(sin_caso))


@pytest.fixture
def entidades():
    """Copia de las entidades de specs/entidades, que cada prueba puede modificar."""
    return copy.deepcopy(_ENTIDADES)


@pytest.fixture
def codigos():
    """Códigos de fichas, decisiones, pendientes y requisitos que existen en specs/."""
    return set(_CODIGOS)


def atributo(entidad, nombre):
    """Devuelve el atributo ``nombre`` de una entidad (dict del JSON)."""
    return next(a for a in entidad['atributos'] if a['nombre'] == nombre)
