# Nombre: test_api.py
# Descripción: Pruebas de la API del servicio de IA con un clasificador falso: salud, catálogo de modelos, revisión
#              rápida, análisis de las fotos de una muestra y el límite de 2 trabajos a la vez (RF-06, RF-07, RF-08,
#              RNF-01, D-18, D-46, D-48).
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.1
"""Pruebas de ``inspectia_ia.api`` sin TensorFlow: el modelo es un clasificador falso."""

import threading
from pathlib import Path

import cv2
import numpy as np
import pytest
from fastapi.testclient import TestClient

from conftest import ALTO, ANCHO, CATALOGO, centros_en_cuadricula, foto_con_granos, jpg, leer_json
from inspectia_ia.api import crear_app
from inspectia_ia.clasificador import a_clasificacion
from inspectia_ia.modelos import ModeloInvalido

SHA_EFFICIENTNET = 'eb9a64394c51fdff8fc16e01fff71e3b3693bc36e43cafb0ddbb34ca5b765bfb'


def _foto(n=50, semilla=0):
    return jpg(foto_con_granos(centros_en_cuadricula(n, semilla=semilla), semilla=semilla))


@pytest.mark.caso('RF-08 · el servicio responde 503 en /salud mientras carga el modelo y 200 cuando está listo')
def test_salud_mientras_carga_y_cuando_esta_listo(configuracion, clasificador):
    soltar = threading.Event()

    def cargar(_carpeta):
        soltar.wait(5)
        return clasificador

    app = crear_app(configuracion, obtener=lambda _e: Path('modelo'), cargar=cargar)
    with TestClient(app) as cliente:
        r = cliente.get('/salud')
        assert r.status_code == 503
        assert r.json() == {'listo': False, 'modelo': {'nombre': 'EfficientNet-B0', 'version': '1.0',
                                                        'sha256': SHA_EFFICIENTNET}}
        assert cliente.post('/analizar', files={'foto1': ('1.jpg', _foto(5), 'image/jpeg')}).status_code == 503
        soltar.set()
        for _ in range(200):
            if app.state.estado.clasificador is not None:
                break
            threading.Event().wait(0.01)
        r = cliente.get('/salud')
        assert r.status_code == 200
        assert r.json() == {'listo': True, 'modelo': {'nombre': 'EfficientNet-B0', 'version': '1.0',
                                                       'sha256': SHA_EFFICIENTNET}}


@pytest.mark.caso('RF-08 · si el modelo no se puede obtener o su SHA-256 no coincide, el servicio no arranca')
def test_no_arranca_si_el_modelo_no_es_el_esperado(configuracion):
    def obtener(_entrada):
        raise ModeloInvalido('El SHA-256 no coincide')

    app = crear_app(configuracion, obtener=obtener, cargar=lambda _c: None)
    with pytest.raises(ModeloInvalido):
        with TestClient(app):
            pass


@pytest.mark.caso('RF-08 · si el modelo no carga en segundo plano, el servicio avisa y se detiene')
def test_si_la_carga_falla_se_detiene(configuracion):
    fallas = []
    llamado = threading.Event()

    def al_fallar(error):
        fallas.append(error)
        llamado.set()

    def cargar(_carpeta):
        raise RuntimeError('SavedModel dañado')

    app = crear_app(configuracion, obtener=lambda _e: Path('x'), cargar=cargar, al_fallar=al_fallar)
    with TestClient(app) as cliente:
        assert llamado.wait(5)
        assert cliente.get('/salud').status_code == 503
    assert str(fallas[0]) == 'SavedModel dañado'


@pytest.mark.caso('D-48 · GET /modelos entrega los 5 modelos con sus métricas y cuál está en uso')
def test_modelos_entrega_los_5_con_sus_metricas(cliente):
    r = cliente.get('/modelos')
    assert r.status_code == 200
    datos = r.json()
    assert datos['enUso'] == 'EfficientNet-B0'
    assert [m['nombre'] for m in datos['modelos']] == ['MobileNetV3-L', 'EfficientNet-B0', 'ResNet-50',
                                                        'Swin-Tiny', 'DeiT-Small']
    efficientnet = datos['modelos'][1]
    assert efficientnet == {
        'nombre': 'EfficientNet-B0', 'version': '1.0', 'tipo': 'CNN', 'f1Macro': 73.5, 'precisionMacro': 75.2,
        'recallMacro': 72.31, 'recallSano': 91.2, 'recallNoSano': 53.4, 'precisionSano': 87.5,
        'precisionNoSano': 62.9, 'sanosComoSanos': 238, 'sanosComoNoSanos': 23, 'noSanosComoSanos': 34,
        'noSanosComoNoSanos': 39, 'tiempoPorGranoMs': 19.0}
    assert {m['tipo'] for m in datos['modelos']} == {'CNN', 'TRANSFORMER'}
    assert sum(m['tipo'] == 'TRANSFORMER' for m in datos['modelos']) == 2


@pytest.mark.caso('RF-06 · C3 · POST /revisar aprueba una foto que cumple la guía, sin avisos')
def test_revisar_aprueba_una_foto_buena(cliente):
    r = cliente.post('/revisar', files={'foto': ('foto.jpg', _foto(), 'image/jpeg')})
    assert r.status_code == 200
    assert r.json() == {'estado': 'APROBADA', 'motivo': None, 'granosEncontrados': 50, 'regionesPegadas': [],
                        'anchoImagen': ANCHO, 'altoImagen': ALTO}


@pytest.mark.caso('RF-06 · C1 · POST /revisar rechaza una foto borrosa con el motivo BORROSA')
def test_revisar_rechaza_una_foto_borrosa(cliente):
    borrosa = cv2.GaussianBlur(foto_con_granos(centros_en_cuadricula(50)), (0, 0), 6)
    r = cliente.post('/revisar', files={'foto': ('foto.jpg', jpg(borrosa), 'image/jpeg')})
    assert r.status_code == 200
    assert (r.json()['estado'], r.json()['motivo']) == ('POR_REPETIR', 'BORROSA')


@pytest.mark.caso('RF-06 · C2 · POST /revisar rechaza una foto oscura con el motivo POCA_LUZ')
def test_revisar_rechaza_una_foto_oscura(cliente):
    oscura = (foto_con_granos(centros_en_cuadricula(50)) * 0.4).astype(np.uint8)
    r = cliente.post('/revisar', files={'foto': ('foto.jpg', jpg(oscura), 'image/jpeg')})
    assert (r.json()['estado'], r.json()['motivo']) == ('POR_REPETIR', 'POCA_LUZ')


@pytest.mark.caso('RF-07 · C2 · POST /revisar rechaza los granos pegados y devuelve sus regiones')
def test_revisar_devuelve_las_regiones_pegadas(cliente):
    centros = centros_en_cuadricula(20) + [(944, 760), (1056, 760)]
    foto = foto_con_granos(centros, [30.0] * 20 + [0.0, 0.0])
    r = cliente.post('/revisar', files={'foto': ('foto.png', jpg(foto), 'image/png')}).json()
    assert (r['estado'], r['motivo']) == ('POR_REPETIR', 'GRANOS_PEGADOS')
    assert len(r['regionesPegadas']) == 1
    assert set(r['regionesPegadas'][0]) == {'x', 'y', 'ancho', 'alto'}


@pytest.mark.caso('RF-06 · POST /revisar responde 400 si el archivo no es una imagen')
def test_revisar_400_si_no_es_imagen(cliente):
    r = cliente.post('/revisar', files={'foto': ('foto.jpg', b'esto no es una foto', 'image/jpeg')})
    assert r.status_code == 400
    assert r.json()['error'] == 'IMAGEN_INVALIDA'


@pytest.mark.caso('RF-06 · POST /revisar responde 413 si la foto pasa del tamaño máximo')
def test_revisar_413_si_la_foto_es_muy_grande(configuracion, clasificador):
    from dataclasses import replace
    app = crear_app(replace(configuracion, tamano_maximo=1000), obtener=lambda _e: Path('m'),
                    cargar=lambda _c: clasificador)
    with TestClient(app) as cliente:
        r = cliente.post('/revisar', files={'foto': ('foto.jpg', _foto(5), 'image/jpeg')})
    assert r.status_code == 413


@pytest.mark.caso('RF-08 · C1 · cada grano detectado recibe una sola clase: SANO o NO_SANO')
def test_analizar_da_una_clase_a_cada_grano(cliente, clasificador):
    r = cliente.post('/analizar', files={'foto1': ('1.jpg', _foto(50, 1), 'image/jpeg'),
                                         'foto2': ('2.jpg', _foto(50, 2), 'image/jpeg')})
    assert r.status_code == 200
    fotos = r.json()['fotos']
    assert [f['numero'] for f in fotos] == [1, 2]
    assert [len(f['granos']) for f in fotos] == [50, 50]
    assert clasificador.recibidos == [100]  # una sola llamada, por lotes
    for f in fotos:
        assert f['revision']['estado'] == 'APROBADA'
        for g in f['granos']:
            assert g['clase'] in ('SANO', 'NO_SANO')
            assert set(g) == {'x', 'y', 'ancho', 'alto', 'clase', 'confianza'}


@pytest.mark.caso('RF-08 · C2 · cada clasificación trae su confianza, de 0 a 100 con 2 decimales')
def test_analizar_trae_la_confianza(cliente):
    r = cliente.post('/analizar', files={'foto1': ('1.jpg', _foto(9), 'image/jpeg'),
                                         'foto2': ('2.jpg', _foto(9, 3), 'image/jpeg')}).json()
    granos = [g for f in r['fotos'] for g in f['granos']]
    # El falso da P(mala) 0,8123 a uno de cada 3 y 0,1456 a los demás: la confianza es la de la clase elegida.
    assert [(g['clase'], g['confianza']) for g in granos[:3]] == [('NO_SANO', 81.23), ('SANO', 85.44),
                                                                   ('SANO', 85.44)]


@pytest.mark.caso('RF-08 · C3 · el análisis dice qué modelo y qué versión lo hicieron')
def test_analizar_dice_el_modelo_y_la_version(cliente):
    r = cliente.post('/analizar', files={'foto1': ('1.jpg', _foto(5), 'image/jpeg'),
                                         'foto2': ('2.jpg', _foto(5, 1), 'image/jpeg')}).json()
    assert r['modelo'] == {'nombre': 'EfficientNet-B0', 'version': '1.0'}


@pytest.mark.caso('RF-07 · C5 · /analizar devuelve la posición de cada grano en la foto original')
def test_analizar_devuelve_la_posicion(cliente):
    centros = centros_en_cuadricula(6, columnas=3, margen=400)
    foto = jpg(foto_con_granos(centros, [0.0] * 6))
    r = cliente.post('/analizar', files={'foto1': ('1.jpg', foto, 'image/jpeg'),
                                         'foto2': ('2.jpg', foto, 'image/jpeg')}).json()
    encontrados = sorted((g['x'] + g['ancho'] / 2, g['y'] + g['alto'] / 2) for g in r['fotos'][0]['granos'])
    for (cx, cy), (ex, ey) in zip(encontrados, sorted(centros)):
        assert (cx, cy) == (pytest.approx(ex, abs=3), pytest.approx(ey, abs=3))


@pytest.mark.caso('RF-07 · C4 · /analizar no clasifica los granos cortados por el borde')
def test_analizar_no_clasifica_los_cortados(cliente, clasificador):
    foto = jpg(foto_con_granos(centros_en_cuadricula(10, margen=300) + [(8, 700), (ANCHO - 10, 300)]))
    r = cliente.post('/analizar', files={'foto1': ('1.jpg', foto, 'image/jpeg'),
                                         'foto2': ('2.jpg', foto, 'image/jpeg')}).json()
    assert [len(f['granos']) for f in r['fotos']] == [10, 10]
    assert clasificador.recibidos == [20]


@pytest.mark.caso('RF-06 · una foto por repetir va en /analizar sin granos, con su motivo')
def test_analizar_foto_por_repetir_va_sin_granos(cliente, clasificador):
    borrosa = jpg(cv2.GaussianBlur(foto_con_granos(centros_en_cuadricula(50)), (0, 0), 6))
    r = cliente.post('/analizar', files={'foto1': ('1.jpg', _foto(50), 'image/jpeg'),
                                         'foto2': ('2.jpg', borrosa, 'image/jpeg'),
                                         'foto3': ('3.jpg', _foto(40, 4), 'image/jpeg')}).json()
    assert [f['numero'] for f in r['fotos']] == [1, 2, 3]
    assert r['fotos'][1]['revision']['motivo'] == 'BORROSA'
    assert r['fotos'][1]['granos'] == []
    assert [len(f['granos']) for f in r['fotos']] == [50, 0, 40]
    assert clasificador.recibidos == [90]


@pytest.mark.caso('RF-08 · /analizar responde 400 si una de las fotos no es una imagen')
def test_analizar_400_si_una_foto_no_es_imagen(cliente):
    r = cliente.post('/analizar', files={'foto1': ('1.jpg', _foto(5), 'image/jpeg'),
                                         'foto2': ('2.jpg', b'nada', 'image/jpeg')})
    assert r.status_code == 400


@pytest.mark.caso('D-18 · modelos.json del repositorio es el que lee el servicio')
def test_el_catalogo_es_el_del_repositorio():
    assert leer_json(CATALOGO)['release']['etiqueta'] == 'modelos-v1.0'


class ClasificadorQueEspera:
    """Clasificador falso que se queda dentro de ``clasificar`` hasta que la prueba lo suelta y cuenta cuántos
    análisis están dentro a la vez."""

    def __init__(self):
        self.dentro = 0
        self.maximo = 0
        self.candado = threading.Lock()
        self.entraron = threading.Semaphore(0)
        self.soltar = threading.Event()

    def clasificar(self, recortes_rgb):
        with self.candado:
            self.dentro += 1
            self.maximo = max(self.maximo, self.dentro)
        self.entraron.release()
        assert self.soltar.wait(10)
        with self.candado:
            self.dentro -= 1
        return [a_clasificacion(0.9, 0.1) for _ in recortes_rgb]


@pytest.mark.caso('RNF-01 · el servicio lee, revisa y clasifica a lo sumo 2 fotos o análisis a la vez, para no pasar '
                  'de su memoria')
def test_a_lo_sumo_dos_trabajos_a_la_vez(configuracion):
    lento = ClasificadorQueEspera()
    app = crear_app(configuracion, obtener=lambda _e: Path('m'), cargar=lambda _c: lento)
    foto = _foto(5)
    respuestas = {}

    def pedir(nombre, ruta, campo):
        respuestas[nombre] = cliente.post(ruta, files={campo: ('1.jpg', foto, 'image/jpeg')})

    with TestClient(app, raise_server_exceptions=False) as cliente:
        for _ in range(100):
            if app.state.estado.clasificador is not None:
                break
            threading.Event().wait(0.01)
        hilos = [threading.Thread(target=pedir, args=(f'analisis{i}', '/analizar', 'foto1')) for i in range(3)]
        hilos.append(threading.Thread(target=pedir, args=('revision', '/revisar', 'foto')))
        for h in hilos:
            h.start()
        try:
            # Dos análisis entran a clasificar; el tercero y la revisión esperan su turno.
            assert lento.entraron.acquire(timeout=10)
            assert lento.entraron.acquire(timeout=10)
            assert not lento.entraron.acquire(timeout=0.5)
            assert 'revision' not in respuestas
        finally:
            lento.soltar.set()
            for h in hilos:
                h.join(10)
    assert lento.maximo == 2
    assert [respuestas[n].status_code for n in ('analisis0', 'analisis1', 'analisis2', 'revision')] == [200] * 4
