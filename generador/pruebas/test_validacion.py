# Nombre: test_validacion.py
# Descripción: Pruebas de la validación de specs/entidades/*.json.
# Fecha de creación: 13/10/2026
# Actualización: 19/10/2026
# Autor: Equipo InspectIA
# Versión: 1.2
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


@pytest.mark.caso('RP-23 · la validación pide el nombre de crear_con, que es el campo de la solicitud')
def test_crear_con_necesita_nombre(entidades, codigos):
    del entidades['empresa']['crear_con']['nombre']

    errores = validar(entidades, codigos)

    assert any('crear_con' in e and 'falta nombre' in e for e in errores)


@pytest.mark.caso('RP-23 · la validación revisa que el ejemplo de un atributo cumpla sus validaciones')
def test_ejemplo_debe_ser_valido(entidades, codigos):
    atributo(entidades['empresa'], 'nit')['ejemplo'] = '900.123.456-8'
    atributo(entidades['usuario'], 'correo')['ejemplo'] = 'sin-arroba'

    errores = validar(entidades, codigos)

    assert 'empresa.nit: el ejemplo no cumple el patron' in errores
    assert 'usuario.correo: el ejemplo no es un correo' in errores


@pytest.mark.caso('RP-23 · la validación rechaza un mensaje con un marcador que no está en validaciones')
def test_marcador_de_mensaje_inexistente(entidades, codigos):
    mensajes = atributo(entidades['usuario'], 'contrasena_hash')['mensajes']
    mensajes['longitud_min']['es'] = 'Debe tener al menos {minimo} caracteres.'

    errores = validar(entidades, codigos)

    assert any('{minimo}' in e for e in errores)


@pytest.mark.caso('RP-23 · la validación rechaza solo_lectura_si en una entidad sin edicion_en_bloque, en vez de ignorarlo')
def test_solo_lectura_si_solo_en_bloque(entidades, codigos):
    atributo(entidades['usuario'], 'nombre')['solo_lectura_si'] = {'atributo': 'rol', 'igual_a': 'ADMIN_PLATAFORMA'}

    errores = validar(entidades, codigos)

    assert 'usuario.nombre: solo_lectura_si va solo en una entidad con edicion_en_bloque' in errores


@pytest.mark.caso('RP-23 · un atributo con solo_lectura_si puede traer su mensaje; uno sin solo_lectura_si, no')
def test_mensaje_de_solo_lectura(entidades, codigos):
    mensaje = {'es': 'No cambia.', 'en': 'It does not change.'}
    atributo(entidades['rango_recomendacion'], 'limite_inferior').setdefault('mensajes', {})['solo_lectura_si'] = mensaje
    atributo(entidades['rango_recomendacion'], 'recomendacion')['mensajes'] = {'solo_lectura_si': mensaje}

    errores = validar(entidades, codigos)

    assert errores == ['rango_recomendacion.recomendacion: mensaje para algo que el atributo no tiene '
                       '(solo_lectura_si)']
