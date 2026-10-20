# Nombre: test_salida.py
# Descripción: Pruebas de la escritura y la comprobación de los archivos generados.
# Fecha de creación: 13/10/2026
# Actualización: 20/10/2026
# Autor: Equipo InspectIA
# Versión: 1.2
"""Pruebas de cómo se escriben los archivos generados y de cómo se detecta que alguien los editó."""

import re

import pytest

from conftest import GENERADOR, RUTA_MIGRACION
from inspectia_generador.carga import cargar_alcance
from inspectia_generador.generacion import generar_archivos
from inspectia_generador.salida import comprobar, escribir


@pytest.mark.caso('RP-23 · la salida es determinista y usa finales de línea LF')
def test_salida_determinista(entidades):
    primera = generar_archivos(entidades)
    segunda = generar_archivos(entidades)

    assert primera == segunda
    assert all('\r' not in a.contenido for a in primera)


@pytest.mark.caso('RP-10 · cada archivo generado lleva su header y la marca de no editar a mano')
def test_header_de_los_archivos(entidades):
    for archivo in generar_archivos(entidades):
        cabecera = archivo.contenido.split('\n', 12)[:12]
        texto = '\n'.join(cabecera)
        for campo in ('Nombre:', 'Descripción:', 'Fecha de creación: 13/10/2026',
                      'Actualización:', 'Autor: Equipo InspectIA', 'Versión:'):
            assert campo in texto, f'{archivo.ruta}: falta «{campo}» en el header'
        assert 'no se edita a mano' in texto


@pytest.mark.caso('RP-23 · lo generado no lleva valores de Python: un booleano sale como true o false, también en los ejemplos de OpenAPI, y nada sale como None')
def test_sin_valores_de_python(entidades):
    for archivo in generar_archivos(entidades, cargar_alcance(GENERADOR / 'alcance.json')):
        encontrados = re.findall(r'\b(?:True|False|None)\b', archivo.contenido)
        assert not encontrados, f'{archivo.ruta}: {encontrados}'


@pytest.mark.caso('RP-23 · comprobar no encuentra diferencias justo después de generar')
def test_comprobar_sin_diferencias(entidades, tmp_path):
    archivos = generar_archivos(entidades)
    escribir(archivos, tmp_path)

    assert comprobar(archivos, tmp_path).vacia


@pytest.mark.caso('RP-23 · comprobar detecta un archivo generado editado a mano')
def test_comprobar_detecta_edicion(entidades, tmp_path):
    archivos = generar_archivos(entidades)
    escribir(archivos, tmp_path)
    migracion = tmp_path / RUTA_MIGRACION
    migracion.write_text(migracion.read_text(encoding='utf-8') + '-- cambio a mano\n', encoding='utf-8')

    diferencias = comprobar(archivos, tmp_path)

    assert diferencias.modificados == [RUTA_MIGRACION]


@pytest.mark.caso('RP-23 · comprobar detecta archivos que faltan o que sobran en las carpetas generadas')
def test_comprobar_detecta_faltantes_y_sobrantes(entidades, tmp_path):
    archivos = generar_archivos(entidades)
    escribir(archivos, tmp_path)
    (tmp_path / RUTA_MIGRACION).unlink()
    sobrante = tmp_path / 'backend/src/generated/java/Extra.java'
    sobrante.parent.mkdir(parents=True, exist_ok=True)
    sobrante.write_text('class Extra {}\n', encoding='utf-8')

    diferencias = comprobar(archivos, tmp_path)

    assert diferencias.faltantes == [RUTA_MIGRACION]
    assert diferencias.sobrantes == ['backend/src/generated/java/Extra.java']


@pytest.mark.caso('RP-23 · comprobar ignora la diferencia entre LF y CRLF')
def test_comprobar_ignora_crlf(entidades, tmp_path):
    archivos = generar_archivos(entidades)
    escribir(archivos, tmp_path)
    migracion = tmp_path / RUTA_MIGRACION
    migracion.write_bytes(migracion.read_bytes().replace(b'\n', b'\r\n'))

    assert comprobar(archivos, tmp_path).vacia


@pytest.mark.caso('RP-23 · generar borra de las carpetas generadas lo que ya no se genera')
def test_generar_borra_lo_que_sobra(entidades, tmp_path):
    viejo = tmp_path / 'backend/src/generated/java/viejo/Viejo.java'
    viejo.parent.mkdir(parents=True)
    viejo.write_text('class Viejo {}\n', encoding='utf-8')
    a_mano = tmp_path / 'backend/src/main/java/AMano.java'
    a_mano.parent.mkdir(parents=True)
    a_mano.write_text('class AMano {}\n', encoding='utf-8')

    escribir(generar_archivos(entidades), tmp_path)

    assert not viejo.exists()
    assert not viejo.parent.exists()
    assert a_mano.exists()
