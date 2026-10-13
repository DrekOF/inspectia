# Nombre: test_validacion.py
# Descripción: Pruebas de la validación de specs/entidades/*.json.
# Fecha de creación: 13/10/2026
# Actualización: 13/10/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Pruebas de la validación de los JSON de entidades contra su formato y contra specs/."""

import pytest

from conftest import atributo
from inspectia_generador.validacion import validar


@pytest.mark.caso('RP-23 · los JSON de specs/entidades pasan la validación')
def test_entidades_reales_son_validas(entidades, codigos):
    assert validar(entidades, codigos) == []


@pytest.mark.caso('RP-23 · la validación detecta una ficha que no existe en specs/')
def test_detecta_ficha_inexistente(entidades, codigos):
    atributo(entidades['lote'], 'categoria')['fichas'] = ['RF-99']

    errores = validar(entidades, codigos)

    assert any('RF-99' in e for e in errores)


@pytest.mark.caso('RP-23 · la validación detecta un CHECK con un valor que no está en ninguna lista')
def test_detecta_valor_inexistente_en_check(entidades, codigos):
    check = entidades['foto']['checks'][0]
    check['sql'] = check['sql'].replace("'POR_REPETIR'", "'POR_REPITIR'")

    errores = validar(entidades, codigos)

    assert any('POR_REPITIR' in e for e in errores)


@pytest.mark.caso('RP-23 · la validación detecta una referencia a una entidad que no existe')
def test_detecta_referencia_inexistente(entidades, codigos):
    atributo(entidades['muestra'], 'lote_id')['referencia']['entidad'] = 'lotes'

    errores = validar(entidades, codigos)

    assert any('muestra.lote_id' in e for e in errores)


@pytest.mark.caso('RP-23 · una entidad con CRUD necesita modulo y ruta')
def test_crud_necesita_modulo_y_ruta(entidades, codigos):
    del entidades['empresa']['modulo']
    del entidades['empresa']['ruta']

    errores = validar(entidades, codigos)

    assert any('empresa' in e and 'modulo' in e for e in errores)
    assert any('empresa' in e and 'ruta' in e for e in errores)


@pytest.mark.caso('RP-23 · un atributo no puede usar el nombre de una columna implícita')
def test_detecta_columna_implicita_repetida(entidades, codigos):
    entidades['empresa']['atributos'].append(
        {**atributo(entidades['empresa'], 'nombre'), 'nombre': 'fecha_creacion'})

    errores = validar(entidades, codigos)

    assert any('fecha_creacion' in e for e in errores)
