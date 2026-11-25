# Nombre: test_metricas_del_modelo.py
# Descripción: Pruebas de RNF-07 con los datos de la release modelos-v1.0: el F1 macro del modelo en uso, la precisión,
#              el recall y la matriz de confusión de cada clase de los 5 modelos, recalculados desde la matriz, el mismo
#              conjunto de prueba de 334 granos para los 5 y su paso a la tabla comparativa por GET /modelos.
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Pruebas de RNF-07 sobre ``modelos.json``, el catálogo de la release ``modelos-v1.0`` (D-40, D-48).

No vuelven a medir los modelos: el conjunto de prueba (los 334 granos de ``recortes_script/val``, que son el ``test``
de Julián) no está en el repositorio (RNF-08) y la prueba se usa una sola vez (RNF-07 C5). Lo que comprueban es que
las cifras del catálogo son las de la tabla «Resultados» de la sección «Medición final en `test`» del README de Julián
(versión 4.3), copiada aquí abajo, y que cada métrica sale de la matriz de confusión con el redondeo que dice el
catálogo: F1 macro y recall de cada clase con 3 decimales (el README), precisión de cada clase con 4 (la ficha JSON de
cada modelo) y precisión y recall macro recalculados de la matriz con 2 decimales de porcentaje. Lo que solo Julián
puede confirmar está en ``docs/verificaciones.md``.
"""

import pytest

from conftest import CATALOGO
from inspectia_ia import catalogo as catalogo_mod
from inspectia_ia.configuracion import Configuracion

SANOS, NO_SANOS = 261, 73
"""El conjunto de prueba: 334 granos, 261 sanos y 73 no sanos (RNF-07, D-18)."""

F1_MINIMO = 65
"""El F1 macro mínimo del modelo en uso, en porcentaje (P-02)."""

README_DE_JULIAN = {
    # nombre: (F1 macro, precisión de no sano, recall de no sano, recall de sano, (VN, FP, FN, VP))
    'EfficientNet-B0': (0.735, 0.629, 0.534, 0.912, (238, 23, 34, 39)),
    'Swin-Tiny': (0.713, 0.647, 0.452, 0.931, (243, 18, 40, 33)),
    'ResNet-50': (0.699, 0.535, 0.521, 0.874, (228, 33, 35, 38)),
    'DeiT-Small': (0.689, 0.612, 0.411, 0.927, (242, 19, 43, 30)),
    'MobileNetV3-L': (0.638, 0.731, 0.260, 0.973, (254, 7, 54, 19)),
}
"""La tabla «Resultados» del README de Julián (versión 4.3, «Medición final en `test`»), sin los ensembles. VN: sano
bien clasificado; FP: sano clasificado como no sano; FN: no sano clasificado como sano; VP: no sano bien clasificado."""

MATRIZ = ('sanosComoSanos', 'sanosComoNoSanos', 'noSanosComoSanos', 'noSanosComoNoSanos')


def _catalogo():
    return catalogo_mod.cargar(CATALOGO)


def _de_la_matriz(vn, fp, fn, vp):
    """Las métricas que salen de la matriz de confusión, en porcentaje y sin redondear."""
    recall_sano, recall_no_sano = vn / (vn + fp), vp / (fn + vp)
    precision_sano, precision_no_sano = vn / (vn + fn), vp / (fp + vp)
    f1_sano = 2 * precision_sano * recall_sano / (precision_sano + recall_sano)
    f1_no_sano = 2 * precision_no_sano * recall_no_sano / (precision_no_sano + recall_no_sano)
    return {k: v * 100 for k, v in {
        'recallSano': recall_sano, 'recallNoSano': recall_no_sano, 'precisionSano': precision_sano,
        'precisionNoSano': precision_no_sano, 'f1Macro': (f1_sano + f1_no_sano) / 2,
        'precisionMacro': (precision_sano + precision_no_sano) / 2,
        'recallMacro': (recall_sano + recall_no_sano) / 2}.items()}


def _redondeado(valor, decimales_de_fraccion):
    """Un porcentaje redondeado como su fracción con esos decimales: 91,188 con 3 da 91,2 (0,912)."""
    return round(valor / 100, decimales_de_fraccion) * 100


@pytest.mark.caso('RNF-07 · C1 · el modelo en uso, EfficientNet-B0, el que carga el servicio por defecto, tiene en el '
                  'catálogo de modelos-v1.0 un F1 macro de 73,5 %, el 0,735 del README de Julián: 65 % o más (P-02)')
def test_el_modelo_en_uso_tiene_el_f1_minimo():
    en_uso = _catalogo().modelo(Configuracion().modelo)

    assert en_uso.nombre == 'EfficientNet-B0'
    assert en_uso.metricas['f1Macro'] == pytest.approx(README_DE_JULIAN['EfficientNet-B0'][0] * 100)
    assert en_uso.metricas['f1Macro'] >= F1_MINIMO
    assert _de_la_matriz(*(en_uso.metricas[k] for k in MATRIZ))['f1Macro'] >= F1_MINIMO


@pytest.mark.caso('RNF-07 · C1 · el F1 macro de cada modelo sale de su matriz de confusión: recalculado con los 4 '
                  'enteros da el del catálogo con 3 decimales, y solo MobileNetV3-L (63,8 %) queda bajo el mínimo de 65')
def test_el_f1_de_cada_modelo_sale_de_su_matriz():
    catalogo = _catalogo()

    for m in catalogo.modelos:
        recalculado = _de_la_matriz(*(m.metricas[k] for k in MATRIZ))['f1Macro']
        assert m.metricas['f1Macro'] == pytest.approx(_redondeado(recalculado, 3)), m.nombre
    assert [m.nombre for m in catalogo.modelos if m.metricas['f1Macro'] < F1_MINIMO] == ['MobileNetV3-L']


@pytest.mark.caso('RNF-07 · C2 · cada uno de los 5 modelos reporta la precisión y el recall de cada clase y la matriz '
                  'de confusión, y coinciden con la matriz: recall con 3 decimales, precisión con 4 y las macro con 2 '
                  'decimales de porcentaje')
@pytest.mark.parametrize('nombre', list(README_DE_JULIAN))
def test_precision_recall_y_matriz_de_cada_clase(nombre):
    m = _catalogo().modelo(nombre)
    matriz = tuple(m.metricas[k] for k in MATRIZ)
    esperado = _de_la_matriz(*matriz)

    assert all(isinstance(n, int) and n >= 0 for n in matriz)
    for clave in ('recallSano', 'recallNoSano'):
        assert m.metricas[clave] == pytest.approx(_redondeado(esperado[clave], 3)), clave
    for clave in ('precisionSano', 'precisionNoSano'):
        assert m.metricas[clave] == pytest.approx(_redondeado(esperado[clave], 4)), clave
    for clave in ('precisionMacro', 'recallMacro'):
        assert m.metricas[clave] == pytest.approx(round(esperado[clave], 2)), clave


@pytest.mark.caso('RNF-07 · C2 · las cifras del catálogo son las de la tabla «Resultados» del README de Julián: F1 '
                  'macro, precisión y recall de no sano, recall de sano y matriz de confusión de los 5 modelos')
def test_el_catalogo_copia_el_readme_de_julian():
    catalogo = _catalogo()

    assert {m.nombre for m in catalogo.modelos} == set(README_DE_JULIAN)
    for m in catalogo.modelos:
        f1, precision_no_sano, recall_no_sano, recall_sano, matriz = README_DE_JULIAN[m.nombre]
        assert m.metricas['f1Macro'] == pytest.approx(f1 * 100), m.nombre
        assert m.metricas['precisionNoSano'] == pytest.approx(precision_no_sano * 100, abs=0.05), m.nombre
        assert m.metricas['recallNoSano'] == pytest.approx(recall_no_sano * 100), m.nombre
        assert m.metricas['recallSano'] == pytest.approx(recall_sano * 100), m.nombre
        assert tuple(m.metricas[k] for k in MATRIZ) == matriz, m.nombre


@pytest.mark.caso('RNF-07 · C3 · los 5 modelos se midieron con el mismo conjunto de prueba: cada matriz suma 261 sanos '
                  'y 73 no sanos, los 334 granos de recortes_script/val (el test de Julián)')
def test_los_5_modelos_con_el_mismo_conjunto_de_prueba():
    catalogo = _catalogo()

    assert len(catalogo.modelos) == 5
    assert {m.tipo for m in catalogo.modelos} == {'CNN', 'TRANSFORMER'}
    for m in catalogo.modelos:
        vn, fp, fn, vp = (m.metricas[k] for k in MATRIZ)
        assert (vn + fp, fn + vp) == (SANOS, NO_SANOS), m.nombre
        assert vn + fp + fn + vp == SANOS + NO_SANOS == 334


@pytest.mark.caso('RNF-07 · C4 · GET /modelos entrega para la tabla comparativa los 5 modelos del catálogo, cada uno con '
                  'todas sus métricas tal como están en modelos.json, y el que está en uso')
def test_get_modelos_entrega_los_5_para_la_tabla(cliente):
    datos = cliente.get('/modelos').json()
    catalogo = _catalogo()

    assert datos['enUso'] == 'EfficientNet-B0'
    assert datos['modelos'] == [m.a_json() for m in catalogo.modelos]
    assert len(datos['modelos']) == 5
    for modelo in datos['modelos']:
        assert set(modelo) == {'nombre', 'version', 'tipo', *catalogo_mod.METRICAS}
        assert all(modelo[k] is not None for k in catalogo_mod.METRICAS), modelo['nombre']
