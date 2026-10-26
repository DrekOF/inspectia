# Nombre: test_casos.py
# Descripción: Pruebas de la lista de casos de prueba que arma casos_de_prueba.py (RP-13).
# Fecha de creación: 13/10/2026
# Actualización: 20/10/2026
# Autor: Equipo InspectIA
# Versión: 1.2
"""Pruebas de la lista de casos de prueba (RP-13): la lectura de las marcas de Java, de Python y de
JavaScript, el reparto por criterio, el documento del repositorio y las fichas «Implementado» sin todas
sus pruebas (D-29)."""

import pytest

import casos_de_prueba
from conftest import RAIZ
from inspectia_generador.casos import (RUTA_DOCUMENTO, Criterio, Ficha, Prueba, Resultado, armar_documento,
                                       documento, implementadas_incompletas, pruebas_java, pruebas_javascript,
                                       pruebas_python, revisar)

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

JAVASCRIPT = r"""/*
 * Nombre: ejemplo.test.jsx
 * test('RF-01 · C9 · en el header no cuenta', () => {});
 */
import { describe, expect, it, test } from 'vitest';

// it('RF-01 · C9 · en un comentario no cuenta', () => {});
describe('RF-01 · un describe agrupa: no es una prueba', () => {
  it('RF-01 · C1 · con datos correctos muestra el menú del rol', async () => {
    const texto = "it('RF-01 · C9 · dentro de un texto no cuenta', () => {})";
    expect(/test\(/.test(texto)).toBe(true);
  });

  it.each([7, 65])('RF-18 · C3 · con %i caracteres se rechaza', (caracteres) => {
    expect(caracteres).toBeGreaterThan(0);
  });

  test(`RNF-08 · C1 · Acerca de cita el dataset`, async ({ page }) => {
    await test.step('abre la página', async () => {});
  });

  test('RF-18 · la marca puede llevar \'comillas\' escapadas', () => {});

  it(`RF-01 · con ${'algo'} que cambia`, () => {});
});
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


@pytest.mark.caso('RP-13 · se leen las marcas de las pruebas de JavaScript: el título de test o it, en Vitest, Playwright y Selenium')
def test_marcas_de_javascript():
    pruebas, errores = pruebas_javascript(JAVASCRIPT, 'frontend/src/ejemplo.test.jsx')

    assert [(p.linea, p.marca, p.parametrizada) for p in pruebas] == [
        (9, 'RF-01 · C1 · con datos correctos muestra el menú del rol', False),
        (14, 'RF-18 · C3 · con %i caracteres se rechaza', True),
        (18, 'RNF-08 · C1 · Acerca de cita el dataset', False),
        (22, "RF-18 · la marca puede llevar 'comillas' escapadas", False)]
    assert {p.nombre for p in pruebas} == {'ejemplo.test.jsx'}
    assert errores == ['frontend/src/ejemplo.test.jsx:24: la marca de una prueba es un texto fijo, sin ${…}']


@pytest.mark.caso('RP-13 · las pruebas del frontend en generated/ cuentan como generadas')
def test_javascript_generado():
    pruebas, _ = pruebas_javascript("test('RF-03 · C1 · una prueba generada', () => {});\n",
                                    'frontend/e2e/generated/usuario.spec.js')

    assert [(p.marca, p.generada) for p in pruebas] == [('RF-03 · C1 · una prueba generada', True)]


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


@pytest.mark.caso('D-29 · una ficha «Implementado» con criterios sin prueba se señala; los eliminados no cuentan')
def test_implementada_con_criterios_pendientes():
    implementada = Ficha('RF-02', 'Registrar empresas', FICHA.criterios, 'Implementado')
    aprobada = Ficha('RF-01', 'Iniciar sesión', (Criterio(1, 'Uno.'),), 'Aprobado')
    codigos = {'RF-01', 'RF-02', 'D-27'}
    a_medias = revisar([implementada, aprobada], [prueba('RF-02 · C1 · cubre el primero')], codigos)
    completa = revisar([implementada], [prueba('RF-02 · C1 · uno'), prueba('RF-02 · C2 · dos', 2)], codigos)

    assert implementadas_incompletas([implementada, aprobada], a_medias) == [
        'RF-02 está «Implementado», pero le faltan pruebas para C2 (D-29)']
    assert implementadas_incompletas([implementada], completa) == []


@pytest.mark.caso('D-29 · comprobar falla si una ficha «Implementado» tiene criterios sin prueba; generar solo avisa')
def test_comprobar_falla_con_una_implementada_incompleta(monkeypatch, capsys):
    # El mismo texto del documento, para que «generar» no escriba nada.
    texto = (RAIZ / RUTA_DOCUMENTO).read_bytes().decode('utf-8').replace('\r\n', '\n')
    aviso = 'RF-02 está «Implementado», pero le faltan pruebas para C12 (D-29)'
    monkeypatch.setattr(casos_de_prueba, 'armar_documento',
                        lambda raiz: Resultado(texto, [], ['RF-02 C12'], [aviso]))

    assert casos_de_prueba.main(['comprobar']) == 1
    assert aviso in capsys.readouterr().out
    assert casos_de_prueba.main(['generar']) == 0
    assert aviso in capsys.readouterr().out


@pytest.mark.caso('D-29 · ninguna ficha del repositorio está «Implementado» con criterios sin prueba')
def test_implementadas_del_repositorio():
    assert armar_documento(RAIZ).implementadas_incompletas == []
