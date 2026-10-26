# Nombre: conftest.py
# Descripción: Datos compartidos por las pruebas del generador y regla de marcar cada prueba con su caso.
# Fecha de creación: 13/10/2026
# Actualización: 26/10/2026
# Autor: Equipo InspectIA
# Versión: 1.1
"""Datos compartidos por las pruebas del generador.

Cada prueba debe llevar ``@pytest.mark.caso("...")`` con la ficha o el requisito que cubre, para que
la lista de casos de prueba (RP-13) pueda armarse desde las pruebas.
"""

import copy
import json
import re
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


def compacto(texto):
    """El texto con cada grupo de espacios y saltos de línea en un solo espacio: así no importa dónde se parte
    una línea."""
    return ' '.join(texto.split())


def cadena_yup(esquemas, funcion, campo):
    """Las validaciones de Yup de un campo, una por línea, en una función de un archivo de esquemas generado.

    Args:
        funcion: la función que arma el esquema, como ``esquemaParaCrear``.
        campo: el campo, como ``nit``, o ``administrador.correo`` dentro del grupo de ``crear_con``.
    """
    inicio = esquemas.index(f'export function {funcion}(t) {{')
    cuerpo = esquemas[inicio:esquemas.index('\n}\n', inicio)]
    *grupos, nombre = campo.split('.')
    for grupo in grupos:
        cuerpo = cuerpo[cuerpo.index(f' {grupo}: object({{'):]
    validaciones = []
    for linea in cuerpo[cuerpo.index(f' {nombre}: '):].split('\n')[1:]:
        if not linea.strip().startswith('.'):
            break
        validaciones.append(linea.strip().removesuffix(','))
    return validaciones


def casos_java(prueba, metodo):
    """Los casos de un ``@MethodSource`` de una prueba de Java generada, en orden.

    Lee las líneas ``Arguments.of("campo", valor, "mensaje")`` del método ``static Stream<Arguments> metodo()``.

    Returns:
        list: ``(campo, valor, mensaje)``; el valor, como se escribe en Java, y el mensaje, ``None`` si el caso no
        lo lleva.
    """
    inicio = prueba.index(f'static Stream<Arguments> {metodo}() {{')
    cuerpo = prueba[inicio:prueba.index('\n    }', inicio)]
    casos = []
    # Cada caso termina en «),», y el último, en «));», que cierra también Stream.of.
    for linea in re.findall(r'Arguments\.of\((.*)\)(?:,|\);)$', cuerpo, re.M):
        con_mensaje = re.fullmatch(r'"([^"]+)", (.+?), ("(?:[^"\\]|\\.)*")', linea)
        if con_mensaje:
            casos.append((con_mensaje[1], con_mensaje[2], json.loads(con_mensaje[3])))
        else:
            campo, valor = re.fullmatch(r'"([^"]+)", (.+)', linea).groups()
            casos.append((campo, valor, None))
    return casos
