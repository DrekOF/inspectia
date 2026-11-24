# Nombre: test_separacion.py
# Descripción: Pruebas de la separación de los granos y de la revisión rápida con fotos sintéticas: 50 granos
#              separados, granos pegados, cortados por el borde, borrosa, oscura, posiciones, orientación EXIF y
#              el tope de 120 megapíxeles (RF-06, RF-07, D-09, D-45, D-46).
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.1
"""Pruebas de ``inspectia_ia.separacion`` y de ``inspectia_ia.imagen``."""

import io

import cv2
import numpy as np
import pytest
from PIL import Image

from conftest import ALTO, ANCHO, EJES, centros_en_cuadricula, foto_con_granos, jpg
from inspectia_ia.configuracion import Umbrales
from inspectia_ia.imagen import MAX_MEGAPIXELES, ImagenInvalida, abrir_foto, leer_foto
from inspectia_ia.separacion import (APROBADA, BORROSA, GRANOS_PEGADOS, POCA_LUZ, POR_REPETIR, recortar,
                                     revisar)

U = Umbrales()


def _revisar(foto):
    """Revisa la foto como llega al servicio: codificada en JPG y leída de nuevo."""
    return revisar(leer_foto(jpg(foto)), U)


@pytest.mark.caso('RF-07 · C1 · en una foto con 50 granos separados encuentra los 50')
@pytest.mark.parametrize('semilla', [0, 1, 2])
def test_encuentra_los_50_granos_separados(semilla):
    revision = _revisar(foto_con_granos(centros_en_cuadricula(50, semilla=semilla), semilla=semilla))
    assert revision.estado == APROBADA
    assert revision.motivo is None
    assert len(revision.granos) == 50
    assert revision.a_json()['granosEncontrados'] == 50


@pytest.mark.caso('RF-07 · C2 · una foto con granos pegados se rechaza y dice dónde están')
def test_granos_pegados_se_rechazan_con_su_region():
    centros = centros_en_cuadricula(20)
    pegado = (1000, 760)
    # Dos granos que se tocan, punta con punta, forman una región del doble del área de uno (D-09).
    centros += [(pegado[0] - 56, pegado[1]), (pegado[0] + 56, pegado[1])]
    angulos = [30.0] * 20 + [0.0, 0.0]
    revision = _revisar(foto_con_granos(centros, angulos))
    assert revision.estado == POR_REPETIR
    assert revision.motivo == GRANOS_PEGADOS
    assert len(revision.regiones_pegadas) == 1
    r = revision.regiones_pegadas[0]
    # La región cubre los dos granos: de x 889 a 1111 y de y 730 a 790, aproximadamente.
    assert r.x == pytest.approx(pegado[0] - 56 - EJES[0], abs=8)
    assert r.x + r.ancho == pytest.approx(pegado[0] + 56 + EJES[0], abs=8)
    assert r.y == pytest.approx(pegado[1] - EJES[1], abs=8)
    assert r.y + r.alto == pytest.approx(pegado[1] + EJES[1], abs=8)
    json = revision.a_json()
    assert json['regionesPegadas'] == [{'x': r.x, 'y': r.y, 'ancho': r.ancho, 'alto': r.alto}]
    assert json['granosEncontrados'] == 20


@pytest.mark.caso('RF-07 · C4 · los granos cortados por el borde de la foto no se cuentan')
def test_granos_cortados_por_el_borde_no_cuentan():
    centros = centros_en_cuadricula(30, columnas=6, margen=300)
    # Cuatro granos a medias: uno en cada borde.
    cortados = [(10, 700), (ANCHO - 15, 500), (900, 5), (1200, ALTO - 20)]
    revision = _revisar(foto_con_granos(centros + cortados))
    assert revision.estado == APROBADA
    assert len(revision.granos) == 30
    for g in revision.granos:
        r = g.rectangulo
        assert r.x > 2 and r.y > 2 and r.x + r.ancho < ANCHO - 2 and r.y + r.alto < ALTO - 2


@pytest.mark.caso('RF-07 · C5 · cada recorte guarda su posición en la foto original')
def test_cada_grano_guarda_su_posicion():
    centros = centros_en_cuadricula(12, columnas=4, margen=300)
    revision = _revisar(foto_con_granos(centros, [0.0] * 12))
    assert len(revision.granos) == 12
    encontrados = sorted((g.rectangulo.x + g.rectangulo.ancho / 2, g.rectangulo.y + g.rectangulo.alto / 2)
                         for g in revision.granos)
    for (cx, cy), (ex, ey) in zip(encontrados, sorted(centros)):
        assert cx == pytest.approx(ex, abs=3)
        assert cy == pytest.approx(ey, abs=3)
    for g in revision.granos:
        # Rectángulo sin rotación de un grano horizontal: unos 110 x 60 píxeles (D-45).
        assert g.rectangulo.ancho == pytest.approx(2 * EJES[0] + 1, abs=6)
        assert g.rectangulo.alto == pytest.approx(2 * EJES[1] + 1, abs=6)


@pytest.mark.caso('RF-07 · C5 · las posiciones van en píxeles de la foto girada según su EXIF')
def test_las_posiciones_respetan_la_orientacion_exif():
    centro = (500, 400)
    foto = foto_con_granos([centro, (1500, 1100)], [0.0, 0.0])
    imagen = Image.fromarray(cv2.cvtColor(foto, cv2.COLOR_BGR2RGB))
    exif = Image.Exif()
    exif[0x0112] = 6  # Orientation: el navegador la muestra girada 90° a la derecha.
    datos = io.BytesIO()
    imagen.save(datos, 'JPEG', quality=92, exif=exif)
    revision = revisar(leer_foto(datos.getvalue()), U)
    assert (revision.ancho, revision.alto) == (ALTO, ANCHO)
    # Girar 90° a la derecha lleva el punto (x, y) a (alto - 1 - y, x).
    esperado = (ALTO - 1 - centro[1], centro[0])
    primero = min(revision.granos, key=lambda g: g.rectangulo.y)
    r = primero.rectangulo
    assert r.x + r.ancho / 2 == pytest.approx(esperado[0], abs=3)
    assert r.y + r.alto / 2 == pytest.approx(esperado[1], abs=3)
    assert r.ancho < r.alto  # el grano horizontal quedó vertical


@pytest.mark.caso('RF-06 · C1 · una foto borrosa se rechaza')
def test_foto_borrosa_se_rechaza():
    foto = foto_con_granos(centros_en_cuadricula(50))
    revision = _revisar(cv2.GaussianBlur(foto, (0, 0), 6))
    assert revision.estado == POR_REPETIR
    assert revision.motivo == BORROSA
    assert revision.nitidez < U.nitidez_minima
    assert revision.a_json()['regionesPegadas'] == []


@pytest.mark.caso('RF-06 · C2 · una foto oscura se rechaza por la luz')
def test_foto_oscura_se_rechaza():
    foto = foto_con_granos(centros_en_cuadricula(50))
    oscura = np.clip(foto.astype(np.float32) * 0.4, 0, 255).astype(np.uint8)
    revision = _revisar(oscura)
    assert revision.estado == POR_REPETIR
    assert revision.motivo == POCA_LUZ
    assert revision.luz < U.luz_minima


@pytest.mark.caso('RF-06 · C3 · una foto que cumple la guía pasa sin avisos')
def test_foto_que_cumple_pasa_sin_avisos():
    revision = _revisar(foto_con_granos(centros_en_cuadricula(50, semilla=5), semilla=5))
    assert revision.a_json() == {'estado': 'APROBADA', 'motivo': None, 'granosEncontrados': 50,
                                 'regionesPegadas': [], 'anchoImagen': ANCHO, 'altoImagen': ALTO}
    assert revision.luz >= U.luz_minima
    assert revision.nitidez >= U.nitidez_minima


@pytest.mark.caso('RF-06 · los umbrales de nitidez y luz se pueden cambiar sin tocar el código (P-12)')
def test_los_umbrales_son_configurables():
    foto = leer_foto(jpg(foto_con_granos(centros_en_cuadricula(10))))
    assert revisar(foto, Umbrales(luz_minima=250)).motivo == POCA_LUZ
    assert revisar(foto, Umbrales(nitidez_minima=1e9)).motivo == BORROSA


@pytest.mark.caso('RF-07 · las manchas pequeñas del papel no cuentan como granos')
def test_las_manchas_pequenas_no_cuentan():
    foto = foto_con_granos(centros_en_cuadricula(20, margen=250))
    for x, y in [(400, 300), (1300, 1200), (700, 900)]:
        cv2.circle(foto, (x, y), 6, (60, 60, 60), -1, cv2.LINE_AA)
    assert len(_revisar(foto).granos) == 20


@pytest.mark.caso('RF-07 · cada grano se recorta girado, con su eje largo horizontal, como en el entrenamiento (D-08)')
def test_el_recorte_deja_el_eje_largo_horizontal():
    foto = leer_foto(jpg(foto_con_granos([(1000, 750)], [70.0])))
    revision = revisar(foto, U)
    assert len(revision.granos) == 1
    recorte = recortar(foto, revision.granos[0].contorno)
    alto, ancho = recorte.shape[:2]
    # Eje largo de 110 y corto de 60, más el 15 % del lado corto a cada lado: unos 128 x 78.
    assert ancho == pytest.approx(2 * EJES[0] + 2 * 0.15 * 2 * EJES[1], abs=8)
    assert alto == pytest.approx(2 * EJES[1] * 1.3, abs=8)
    assert ancho > alto


@pytest.mark.caso('RF-06 · un archivo que no es una foto JPG o PNG no se puede revisar')
@pytest.mark.parametrize('contenido', [b'', b'no es una imagen', b'GIF89a\x01\x00\x01\x00\x00\x00\x00;'])
def test_un_archivo_que_no_es_foto_es_invalido(contenido):
    with pytest.raises(ImagenInvalida):
        leer_foto(contenido)


@pytest.mark.caso('RF-06 · acepta fotos PNG además de JPG')
def test_acepta_png():
    foto = foto_con_granos(centros_en_cuadricula(10))
    ok, datos = cv2.imencode('.png', foto)
    assert ok
    assert len(revisar(leer_foto(datos.tobytes()), U).granos) == 10


def _png_en_blanco(ancho, alto):
    """Un PNG de 1 bit, todo en negro: ocupa poco en disco aunque tenga muchos megapíxeles."""
    datos = io.BytesIO()
    Image.new('1', (ancho, alto)).save(datos, 'PNG')
    return datos.getvalue()


@pytest.mark.caso('RF-06 · una foto de hasta 120 megapíxeles se puede abrir (el tope de Pillow era de 89)')
def test_abre_una_foto_de_100_megapixeles():
    with abrir_foto(_png_en_blanco(10_000, 10_000)) as abierta:
        assert abierta.size == (10_000, 10_000)
    assert MAX_MEGAPIXELES == 120


@pytest.mark.caso('RF-06 · una foto de más de 120 megapíxeles no se puede revisar y dice el máximo')
@pytest.mark.parametrize('lado', [11_000, 16_000])
def test_rechaza_una_foto_de_mas_de_120_megapixeles(lado):
    with pytest.raises(ImagenInvalida, match='120 megapíxeles'):
        leer_foto(_png_en_blanco(lado, lado))
