# Nombre: conftest.py
# Descripción: Ayudas comunes de las pruebas del servicio de IA: fotos sintéticas de granos sobre papel dibujadas
#              en la prueba, un clasificador falso y la aplicación lista para probar.
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Ayudas de las pruebas.

Las fotos se dibujan en cada prueba (granos como elipses oscuras con textura sobre papel claro con algo de sombra
y ruido): así el repositorio no lleva fotos del dataset enteras (RNF-08) y cada prueba sabe dónde está cada grano.
"""

import io
import json
import random
import threading
from pathlib import Path

import cv2
import numpy as np
import pytest
from fastapi.testclient import TestClient

from inspectia_ia.api import crear_app
from inspectia_ia.clasificador import a_clasificacion
from inspectia_ia.configuracion import CARPETA_DEL_SERVICIO, Configuracion

CATALOGO = CARPETA_DEL_SERVICIO / 'modelos.json'

ANCHO, ALTO = 2000, 1500
EJES = (55, 30)
"""Semiejes de un grano dibujado, en píxeles: unos 110 x 60, como un grano en una foto de 50."""


def papel(ancho=ANCHO, alto=ALTO, semilla=0):
    """Papel claro con una sombra suave hacia las esquinas y algo de ruido de cámara (BGR)."""
    yy, xx = np.mgrid[0:alto, 0:ancho].astype(np.float32)
    sombra = 1 - 0.15 * (((xx - ancho / 2) / ancho) ** 2 + ((yy - alto / 2) / alto) ** 2) * 4
    base = np.empty((alto, ancho, 3), np.float32)
    base[:] = (205, 210, 214)
    base *= sombra[..., None]
    base += np.random.default_rng(semilla).normal(0, 2.0, base.shape)
    return base


def dibujar_grano(lienzo, centro, angulo, ejes=EJES, semilla=0):
    """Dibuja un grano (elipse café con textura) con el borde suavizado; solo toca el cuadro alrededor del grano."""
    alto, ancho = lienzo.shape[:2]
    r = max(ejes) + 3
    x0, y0 = max(int(centro[0]) - r, 0), max(int(centro[1]) - r, 0)
    x1, y1 = min(int(centro[0]) + r + 1, ancho), min(int(centro[1]) + r + 1, alto)
    if x0 >= x1 or y0 >= y1:
        return
    zona = lienzo[y0:y1, x0:x1]
    mascara = np.zeros(zona.shape[:2], np.uint8)
    cv2.ellipse(mascara, (int(centro[0]) - x0, int(centro[1]) - y0), ejes, angulo, 0, 360, 255, -1, cv2.LINE_AA)
    alfa = (mascara.astype(np.float32) / 255)[..., None]
    rng = np.random.default_rng(semilla)
    color = np.array([40 + rng.uniform(-10, 10), 52 + rng.uniform(-10, 10), 78 + rng.uniform(-15, 15)], np.float32)
    textura = rng.normal(0, 8, zona.shape[:2]).astype(np.float32)[..., None]
    zona[:] = zona * (1 - alfa) + (color + textura) * alfa


def centros_en_cuadricula(n, columnas=10, ancho=ANCHO, alto=ALTO, margen=150, semilla=0):
    """``n`` centros separados entre sí, en una cuadrícula con algo de desorden."""
    rnd = random.Random(semilla)
    filas = (n + columnas - 1) // columnas
    paso_x = (ancho - 2 * margen) / max(columnas - 1, 1)
    paso_y = (alto - 2 * margen) / max(filas - 1, 1)
    return [(margen + (i % columnas) * paso_x + rnd.uniform(-15, 15),
             margen + (i // columnas) * paso_y + rnd.uniform(-15, 15)) for i in range(n)]


def foto_con_granos(centros, angulos=None, ancho=ANCHO, alto=ALTO, semilla=0):
    """Una foto (BGR, ``uint8``) con un grano en cada centro."""
    lienzo = papel(ancho, alto, semilla)
    rnd = random.Random(semilla)
    for i, c in enumerate(centros):
        angulo = angulos[i] if angulos else rnd.uniform(0, 180)
        dibujar_grano(lienzo, c, angulo, semilla=semilla * 1000 + i)
    return np.clip(lienzo, 0, 255).astype(np.uint8)


def jpg(foto_bgr, calidad=92):
    """La foto codificada como JPG."""
    ok, datos = cv2.imencode('.jpg', foto_bgr, [cv2.IMWRITE_JPEG_QUALITY, calidad])
    assert ok
    return datos.tobytes()


class ClasificadorFalso:
    """Clasifica sin modelo: uno de cada 3 recortes es no sano (P(mala) 0,8123); guarda cuántos recibió."""

    def __init__(self):
        self.recibidos = []

    def clasificar(self, recortes_rgb):
        self.recibidos.append(len(recortes_rgb))
        salida = []
        for i, r in enumerate(recortes_rgb):
            assert r.ndim == 3 and r.shape[2] == 3 and r.dtype == np.uint8
            p_mala = 0.8123 if i % 3 == 0 else 0.1456
            salida.append(a_clasificacion(1 - p_mala, p_mala))
        return salida


@pytest.fixture
def configuracion(tmp_path):
    """Configuración de prueba: el catálogo del repositorio y una caché temporal."""
    return Configuracion(modelos_uri=f'file:///{tmp_path.as_posix()}', cache=tmp_path / 'cache', catalogo=CATALOGO)


@pytest.fixture
def clasificador():
    return ClasificadorFalso()


@pytest.fixture
def cliente(configuracion, clasificador):
    """La API con el clasificador falso, ya cargado."""
    listo = threading.Event()

    def cargar(_carpeta):
        listo.set()
        return clasificador

    app = crear_app(configuracion, obtener=lambda _e: Path('modelo'), cargar=cargar)
    with TestClient(app) as c:
        assert listo.wait(5)
        for _ in range(100):
            if app.state.estado.clasificador is not None:
                break
            threading.Event().wait(0.01)
        yield c


def leer_json(ruta):
    """Lee un JSON en UTF-8."""
    return json.loads(Path(ruta).read_text(encoding='utf-8'))


def png(foto_bgr):
    """La foto codificada como PNG."""
    ok, datos = cv2.imencode('.png', foto_bgr)
    assert ok
    return io.BytesIO(datos.tobytes()).getvalue()
