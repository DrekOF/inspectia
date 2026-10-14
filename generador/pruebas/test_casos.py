# Nombre: test_casos.py
# Descripción: Pruebas de la lista de casos de prueba que arma casos_de_prueba.py (RP-13).
# Fecha de creación: 13/10/2026
# Actualización: 13/10/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Pruebas de la lista de casos de prueba (RP-13): la lectura de las marcas de Java y de Python, el
reparto por criterio y el documento del repositorio."""

import pytest

from conftest import RAIZ
from inspectia_generador.casos import (RUTA_DOCUMENTO, Criterio, Ficha, Prueba, armar_documento, documento,
                                       pruebas_java, pruebas_python, revisar)

JAVA = '''package co.edu.usco.inspectia.ejemplo;

/** Una prueba de ejemplo; {@code @Test} en el JavaDoc no cuenta. */
@PruebaIntegracion
class EjemploIT {

    @Test
    @DisplayName("RF-02 · C1 · con datos válidos, la empresa queda registrada")
    void registra() {
        String texto = "@Test (no es una anotación)";
    }

    @ParameterizedTest(name = "{0} caracteres")
    @DisplayName("RF-02 · C5 · la contraseña acepta de 8 a 64 caracteres")
    @CsvSource(delimiter = '|', textBlock = """
            7  | La contraseña (corta) debe tener "8" caracteres. @Test
            8  |
            """)
    void limites(int caracteres, String error) {
        @SuppressWarnings("unchecked") String nada = null;
    }

    @Test
    void sinMarca() {
    }

    private void ayuda(@SuppressWarnings("x") String valor) {
    }
}
'''

PYTHON = """import pytest


@pytest.mark.caso('RP-23 · una prueba del generador')
def test_uno():
    pass


@pytest.mark.parametrize('valor', [1, 2])
@pytest.mark.caso("RF-02 · C4 · otra prueba")
def test_dos(valor):
    pass


def test_sin_marca():
    pass


def ayuda():
    pass
"""

FICHA = Ficha('RF-02', 'Registrar empresas', (Criterio(1, 'Uno.'), Criterio(2, 'Dos.'),
                                              Criterio(3, 'Eliminado (D-27): ya no aplica.')))
HEADER = {'fecha_creacion': '13/10/2026', 'actualizacion': '13/10/2026'}


def prueba(marca, linea=1):
    return Prueba('backend/src/test/java/A.java', linea, 'A', 'metodo', marca)


@pytest.mark.caso('RP-13 · se leen las marcas de Java aunque las anotaciones lleven paréntesis, comillas o @')
def test_marcas_de_java():
    pruebas, errores = pruebas_java(JAVA, 'backend/src/test/java/EjemploIT.java')

    assert [(p.funcion, p.marca, p.parametrizada) for p in pruebas] == [
        ('registra', 'RF-02 · C1 · con datos válidos, la empresa queda registrada', False),
        ('limites', 'RF-02 · C5 · la contraseña acepta de 8 a 64 caracteres', True)]
    assert pruebas[0].nombre == 'EjemploIT'
    assert errores == ['backend/src/test/java/EjemploIT.java:24: EjemploIT.sinMarca no tiene @DisplayName con su marca']


@pytest.mark.caso('RP-13 · se leen las marcas de las pruebas de Python')
def test_marcas_de_python():
    pruebas, errores = pruebas_python(PYTHON, 'generador/pruebas/test_ejemplo.py')

    assert sorted((p.funcion, p.marca, p.parametrizada) for p in pruebas) == [
        ('test_dos', 'RF-02 · C4 · otra prueba', True),
        ('test_uno', 'RP-23 · una prueba del generador', False)]
    assert errores == ['generador/pruebas/test_ejemplo.py:15: test_sin_marca no tiene @pytest.mark.caso con su marca']


@pytest.mark.caso('RP-13 · un criterio sin prueba sale como pendiente; uno eliminado no')
def test_pendientes_y_eliminados():
    pruebas = [prueba('RF-02 · C1 · cubre el primero'), prueba('RF-02 · sin criterio', 2)]
    revision = revisar([FICHA], pruebas, {'RF-02', 'D-27'})

    texto = documento([FICHA], pruebas, revision, {}, HEADER)

    assert revision.errores == []
    assert '| C1. Uno. | [`A`](../backend/src/test/java/A.java) · cubre el primero |' in texto
    assert '| C2. Dos. | **Pendiente** |' in texto
    assert '| C3. Eliminado (D-27): ya no aplica. | Eliminado |' in texto
    assert '- [`A`](../backend/src/test/java/A.java) · sin criterio' in texto
    assert '| RF-02 · Registrar empresas | 3 | 1 | 1 |' in texto


@pytest.mark.caso('RP-13 · una marca que cita algo que no existe o un criterio eliminado es un error')
def test_marcas_invalidas():
    pruebas = [prueba('RF-02 · C3 · cita un criterio eliminado', 1), prueba('RF-02 · C9 · no existe', 2),
               prueba('RP-99 · no existe', 3), prueba('RP-23 · C1 · una pauta no tiene criterios', 4),
               prueba('sin la forma de una marca', 5)]

    errores = revisar([FICHA], pruebas, {'RF-02', 'RP-23', 'D-27'}).errores

    assert errores == [
        'backend/src/test/java/A.java:1: el criterio C3 de RF-02 está eliminado',
        'backend/src/test/java/A.java:2: RF-02 no tiene el criterio C9',
        'backend/src/test/java/A.java:3: RP-99 no existe en specs/',
        'backend/src/test/java/A.java:4: RP-23 no es una ficha: no tiene criterios (C1)',
        'backend/src/test/java/A.java:5: la marca «sin la forma de una marca» no tiene la forma '
        '«<código> · C<n> · …» ni «<código> · …»']


@pytest.mark.caso('RP-13 · todas las pruebas del repositorio tienen una marca válida')
def test_marcas_del_repositorio():
    assert armar_documento(RAIZ).errores == []


@pytest.mark.caso('RP-13 · docs/casos-de-prueba.md está al día con las pruebas y las fichas')
def test_documento_al_dia():
    esperado = armar_documento(RAIZ).texto
    actual = (RAIZ / RUTA_DOCUMENTO).read_bytes().decode('utf-8').replace('\r\n', '\n')

    assert actual == esperado, 'Corre «generador/.venv/Scripts/python generador/casos_de_prueba.py generar»'


@pytest.mark.caso('RP-13 · el documento sale igual cada vez que se arma')
def test_documento_determinista():
    assert armar_documento(RAIZ).texto == armar_documento(RAIZ).texto
