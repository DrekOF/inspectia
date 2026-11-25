# Nombre: test_paridad.py
# Descripción: Prueba de paridad con el modelo real: el servicio clasifica 5 recortes reales igual que
#              inferencia.py de Julián (diferencia menor que 0,002, sin cambios de clase) (RF-08, D-40).
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Paridad del servicio con ``inferencia.py`` de Julián.

Necesita el SavedModel de EfficientNet-B0: la variable ``INSPECTIA_MODELO_PRUEBA`` dice dónde está el zip de la
release (``inspectia_EfficientNet-B0_v1.0_savedmodel.zip``) o su carpeta ya descomprimida. Sin ella, las pruebas
se saltan con el motivo; la CI baja el zip y las corre.
"""

import os
from pathlib import Path

import numpy as np
import pytest
from PIL import Image

from conftest import CATALOGO, leer_json
from inspectia_ia import catalogo as catalogo_mod
from inspectia_ia.modelos import OrigenCarpeta, preparar_modelo

DATOS = Path(__file__).resolve().parent / 'datos' / 'recortes'
TOLERANCIA = 0.002
"""La diferencia que Julián midió entre grafos de TensorFlow distintos (D-40)."""


@pytest.fixture(scope='module')
def modelo(tmp_path_factory):
    """El clasificador con el SavedModel real, o se salta si no está."""
    ruta = os.environ.get('INSPECTIA_MODELO_PRUEBA', '').strip()
    if not ruta or not Path(ruta).exists():
        pytest.skip('Sin el modelo real: define INSPECTIA_MODELO_PRUEBA con la ruta del zip del SavedModel de '
                    'EfficientNet-B0 (release modelos-v1.0) o de su carpeta descomprimida')
    ruta = Path(ruta)
    if ruta.is_file():
        entrada = catalogo_mod.cargar(CATALOGO).modelo('EfficientNet-B0')
        assert ruta.name == entrada.archivo.nombre
        carpeta = preparar_modelo(entrada, OrigenCarpeta(ruta.parent), tmp_path_factory.mktemp('cache'))
    else:
        carpeta = ruta
    from inspectia_ia.clasificador import ClasificadorSavedModel
    return ClasificadorSavedModel(carpeta)


def _recortes():
    """Los recortes en JPG, tal como están en disco, y lo que dio ``inferencia.py``."""
    esperado = leer_json(DATOS / 'esperado.json')['probabilidades']
    nombres = sorted(esperado)
    return nombres, [(DATOS / n).read_bytes() for n in nombres], esperado


@pytest.mark.caso('D-40 · el servicio da las mismas probabilidades que inferencia.py de Julián (diferencia < 0,002)')
def test_paridad_con_inferencia_py(modelo):
    nombres, jpgs, esperado = _recortes()
    p = modelo.probabilidades_jpg(jpgs)
    for nombre, (p_sana, p_mala) in zip(nombres, p):
        assert abs(p_mala - esperado[nombre]['pMala']) < TOLERANCIA, nombre
        assert abs(p_sana - esperado[nombre]['pSana']) < TOLERANCIA, nombre


@pytest.mark.caso('RF-08 · C1 · con el modelo real, cada grano recibe la misma clase que en inferencia.py')
def test_misma_clase_que_inferencia_py(modelo):
    """Como en el servicio: el recorte llega en RGB (leído con Pillow) y vuelve a pasar por un JPG de calidad 95, así
    que no es idéntico al del disco; la clase debe ser la misma y la probabilidad, cercana."""
    nombres, _, esperado = _recortes()
    imagenes = [np.asarray(Image.open(DATOS / n).convert('RGB')) for n in nombres]
    clases = modelo.clasificar(imagenes)
    traduccion = {'sana': 'SANO', 'mala': 'NO_SANO'}
    assert [c.clase for c in clases] == [traduccion[esperado[n]['clase']] for n in nombres]
    for c, n in zip(clases, nombres):
        elegida = esperado[n]['pMala'] if c.clase == 'NO_SANO' else esperado[n]['pSana']
        assert c.confianza == pytest.approx(elegida * 100, abs=3)


@pytest.mark.caso('RF-08 · clasificar por lotes da lo mismo que de a un recorte')
def test_por_lotes_da_lo_mismo(modelo):
    _, jpgs, _ = _recortes()
    juntos = modelo.probabilidades_jpg(jpgs)
    uno_a_uno = np.concatenate([modelo.probabilidades_jpg([j]) for j in jpgs])
    assert np.abs(juntos - uno_a_uno).max() < 1e-4


@pytest.mark.caso('D-40 · la preparación da un recorte de 224 x 224 x 3 con valores de 0 a 255')
def test_la_preparacion_da_224(modelo):
    _, jpgs, _ = _recortes()
    x = modelo.preparar(jpgs[0])
    assert x.shape == (224, 224, 3)
    assert 0 <= x.min() and x.max() <= 255
