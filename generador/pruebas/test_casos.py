# Nombre: test_casos.py
# Descripción: Pruebas de la lista de casos de prueba que arma casos_de_prueba.py (RP-13).
# Fecha de creación: 13/10/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.4
"""Pruebas de la lista de casos de prueba (RP-13): la lectura de las marcas de Java, de Python y de
JavaScript, el reparto por criterio, las verificaciones documentadas de docs/verificaciones.md, el documento
del repositorio y las fichas «Implementado» sin todas sus pruebas (D-29)."""

import pytest

import casos_de_prueba
from conftest import RAIZ
from inspectia_generador.carga import cargar_codigos
from inspectia_generador.casos import (RUTA_DOCUMENTO, RUTA_VERIFICACIONES, Criterio, Ficha, Prueba, Resultado,
                                       armar_documento, buscar_pruebas, documento, implementadas_incompletas,
                                       leer_fichas, leer_verificaciones, pendientes_de, pruebas_java,
                                       pruebas_javascript, pruebas_python, revisar)

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


@pytest.mark.caso('RP-13 · se leen las pruebas de Python del generador y las del servicio de IA')
def test_pruebas_de_python_del_generador_y_del_servicio_de_ia(tmp_path):
    for carpeta in ('generador/pruebas', 'ia-servicio/pruebas'):
        (tmp_path / carpeta).mkdir(parents=True)
        (tmp_path / carpeta / 'test_ejemplo.py').write_text(PYTHON, encoding='utf-8')
    # conftest.py no es un archivo de prueba: lo que tenga no cuenta.
    (tmp_path / 'ia-servicio' / 'pruebas' / 'conftest.py').write_text('def test_ayuda():\n    pass\n',
                                                                      encoding='utf-8')

    pruebas, errores = buscar_pruebas(tmp_path)

    assert sorted({p.archivo for p in pruebas}) == ['generador/pruebas/test_ejemplo.py',
                                                    'ia-servicio/pruebas/test_ejemplo.py']
    assert len(pruebas) == 4
    assert errores == ['generador/pruebas/test_ejemplo.py:15: test_sin_marca no tiene @pytest.mark.caso con su marca',
                       'ia-servicio/pruebas/test_ejemplo.py:15: test_sin_marca no tiene @pytest.mark.caso con su marca']


@pytest.mark.caso('RP-13 · las marcas del servicio de IA citan códigos que existen, como D-18, D-40 y D-48')
def test_marcas_del_servicio_de_ia():
    pruebas, errores = buscar_pruebas(RAIZ)
    del_servicio = [p for p in pruebas if p.archivo.startswith('ia-servicio/pruebas/')]
    codigos = {p.marca.split(' · ')[0] for p in del_servicio}

    assert errores == []
    assert {'D-18', 'D-40', 'D-48', 'RF-06', 'RF-07', 'RF-08'} <= codigos
    assert codigos <= cargar_codigos(RAIZ / 'specs')
    assert revisar(leer_fichas(RAIZ / 'specs' / 'fichas.json'), del_servicio,
                   cargar_codigos(RAIZ / 'specs')).errores == []


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


# --- Verificaciones documentadas (docs/verificaciones.md) ---------------------------------------------------------

VERIFICACIONES = """<!--
Nombre: verificaciones.md
-->

# Verificaciones documentadas

## Cómo se lee

Un título que no empieza con un código no es una verificación.

## RF-02 · C2 · lo revisó el equipo a mano

**Estado:** Verificado.

Qué se revisó.

## RF-02 · C1 · falta que alguien lo confirme

**Estado:** Pendiente: falta la respuesta de Julián.
"""


@pytest.mark.caso('RP-13 · se leen las verificaciones documentadas de docs/verificaciones.md: su criterio, su estado y su línea')
def test_lee_las_verificaciones():
    verificaciones, errores = leer_verificaciones(VERIFICACIONES)

    assert errores == []
    assert [(v.linea, v.marca, v.verificada) for v in verificaciones] == [
        (11, 'RF-02 · C2 · lo revisó el equipo a mano', True),
        (17, 'RF-02 · C1 · falta que alguien lo confirme', False)]
    assert {v.archivo for v in verificaciones} == {RUTA_VERIFICACIONES}


@pytest.mark.caso('RP-13 · una verificación documentada verificada cubre su criterio y sale como «verificación documentada»')
def test_verificacion_verificada_cubre_el_criterio():
    verificaciones, _ = leer_verificaciones(VERIFICACIONES)
    revision = revisar([FICHA], [], {'RF-02', 'D-27'}, verificaciones)

    texto = documento([FICHA], [], revision, {}, HEADER)

    assert revision.errores == []
    assert ('| C2. Dos. | [`verificaciones.md`](verificaciones.md) · lo revisó el equipo a mano '
            '(verificación documentada, verificada) |') in texto
    assert '| RF-02 · Registrar empresas | 3 | 1 | 1 |' in texto
    assert '2 verificaciones documentadas' in texto


@pytest.mark.caso('RP-13 · una verificación documentada pendiente deja el criterio pendiente, aunque tenga pruebas')
def test_verificacion_pendiente_no_cubre_el_criterio():
    verificaciones, _ = leer_verificaciones(VERIFICACIONES)
    pruebas = [prueba('RF-02 · C1 · cubre una parte')]
    revision = revisar([FICHA], pruebas, {'RF-02', 'D-27'}, verificaciones)

    texto = documento([FICHA], pruebas, revision, {}, HEADER)

    assert ('| C1. Uno. | **Pendiente:** [`A`](../backend/src/test/java/A.java) · cubre una parte<br>'
            '[`verificaciones.md`](verificaciones.md) · falta que alguien lo confirme '
            '(verificación documentada, pendiente) |') in texto
    assert '| RF-02 · Registrar empresas | 3 | 1 | 1 |' in texto
    assert pendientes_de([FICHA], revision) == ['RF-02 C1']


@pytest.mark.caso('RP-13 · una verificación documentada mal escrita es un error: sin estado, sin criterio, repetida o que cita algo que no existe')
def test_verificaciones_invalidas():
    texto = '\n'.join([
        '## RF-02 · C1 · sin estado', '', 'Nada.', '',
        '## RF-02 · sin criterio', '', '**Estado:** Verificado.', '',
        '## RF-02 · C2 · la primera', '', '**Estado:** Verificado.', '',
        '## RF-02 · C2 · repetida', '', '**Estado:** Pendiente: otra vez.', '',
        '## RF-02 · C3 · eliminado', '', '**Estado:** Verificado.', '',
        '## RF-02 · C9 · no existe', '', '**Estado:** Verificado.', '',
        '## RF-02 · C2 · con un estado que no sirve', '', '**Estado:** Casi.', ''])

    verificaciones, errores = leer_verificaciones(texto)
    errores += revisar([FICHA], [], {'RF-02', 'D-27'}, verificaciones).errores

    assert errores == [
        'docs/verificaciones.md:1: la verificación «RF-02 · C1 · sin estado» no dice su estado: '
        '«**Estado:** Verificado.» o «**Estado:** Pendiente: …»',
        'docs/verificaciones.md:5: una verificación documentada cubre un criterio: «<código> · C<n> · …»',
        'docs/verificaciones.md:25: la verificación «RF-02 · C2 · con un estado que no sirve» no dice su estado: '
        '«**Estado:** Verificado.» o «**Estado:** Pendiente: …»',
        'docs/verificaciones.md:13: RF-02 C2 ya tiene otra verificación documentada',
        'docs/verificaciones.md:17: el criterio C3 de RF-02 está eliminado',
        'docs/verificaciones.md:21: RF-02 no tiene el criterio C9']


@pytest.mark.caso('RP-13 · sin verificaciones documentadas, el documento sale igual que antes: no las nombra')
def test_sin_verificaciones_el_documento_no_cambia():
    pruebas = [prueba('RF-02 · C1 · cubre el primero')]
    sin_archivo = documento([FICHA], pruebas, revisar([FICHA], pruebas, {'RF-02', 'D-27'}), {}, HEADER)
    vacio = documento([FICHA], pruebas, revisar([FICHA], pruebas, {'RF-02', 'D-27'}, []), {}, HEADER)

    assert sin_archivo == vacio
    assert 'verificaci' not in sin_archivo
    assert '| Ficha | Criterios | Con prueba | Pendientes |' in sin_archivo


@pytest.mark.caso('D-29 · una verificación documentada pendiente no deja completa una ficha «Implementado»; una verificada sí')
def test_implementada_con_verificaciones():
    implementada = Ficha('RF-02', 'Registrar empresas', FICHA.criterios, 'Implementado')
    verificaciones, _ = leer_verificaciones(VERIFICACIONES)
    codigos = {'RF-02', 'D-27'}
    con_pendiente = revisar([implementada], [prueba('RF-02 · C1 · uno')], codigos, verificaciones)
    solo_verificada = revisar([implementada], [prueba('RF-02 · C1 · uno')], codigos, verificaciones[:1])

    assert implementadas_incompletas([implementada], con_pendiente) == [
        'RF-02 está «Implementado», pero le faltan pruebas para C1 (D-29)']
    assert implementadas_incompletas([implementada], solo_verificada) == []


@pytest.mark.caso('RP-13 · las verificaciones documentadas del repositorio están bien escritas y citan criterios que existen')
def test_verificaciones_del_repositorio():
    ruta = RAIZ / RUTA_VERIFICACIONES
    verificaciones, errores = leer_verificaciones(ruta.read_text(encoding='utf-8'))

    assert errores == []
    assert verificaciones, f'{RUTA_VERIFICACIONES} no tiene verificaciones'
    assert armar_documento(RAIZ).errores == []
