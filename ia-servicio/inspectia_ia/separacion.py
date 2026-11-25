# Nombre: separacion.py
# Descripción: Separación de los granos sobre el papel blanco, revisión rápida de la foto (luz, nitidez y granos
#              pegados) y recorte de cada grano igual que en el entrenamiento (RF-06, RF-07, D-08, D-09, D-46).
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Separación de los granos y revisión rápida de una foto.

La separación es la de ``generar_recortes.py``, el script con el que se hicieron los recortes con que Julián
entrenó el modelo (D-08, RF-07): se estima la iluminación del papel y se divide la imagen por ella, se marca
como grano lo más oscuro que 0,6 veces el papel, se limpia la máscara y se descartan las regiones pequeñas y las
que tocan el borde de la foto (RF-07 C4). Cada grano se gira para dejar su eje largo horizontal y se recorta con
un margen del 15 % de su lado corto, igual que en el script. Cambia solo lo que exige una foto con muchos granos:

- La imagen de detección tiene un lado mayor fijo (``Umbrales.lado_deteccion``) y no la mitad de la mitad de la
  foto, así los filtros trabajan a la misma escala con cualquier cámara.
- Se guardan todas las regiones y no solo la más grande, con un área mínima más baja y un filtro de manchas
  relativo a la mediana de la foto.
- Los recortes salen de la foto en su resolución completa, que da a cada grano tantos píxeles como se pueda
  (P-12: en una foto de un lote cada grano tiene menos píxeles que en las del dataset).

La revisión rápida (D-46) mide la luz (el brillo del papel), la nitidez (la varianza del laplaciano en el borde
de los granos) y busca granos pegados: una región con al menos el doble del área mediana de los granos de la foto
(D-09).
"""

from dataclasses import dataclass, field

import cv2
import numpy as np

MARGEN = 0.15
"""Margen alrededor del grano, relativo a su lado corto (``generar_recortes.py``)."""

APROBADA, POR_REPETIR = 'APROBADA', 'POR_REPETIR'
BORROSA, POCA_LUZ, GRANOS_PEGADOS = 'BORROSA', 'POCA_LUZ', 'GRANOS_PEGADOS'
"""Los códigos de ``specs/entidades/foto.json``."""


@dataclass(frozen=True)
class Rectangulo:
    """Rectángulo sin rotación, en píxeles de la foto ya girada según su EXIF (D-45)."""

    x: int
    y: int
    ancho: int
    alto: int

    def a_json(self):
        """El rectángulo como lo manda la API."""
        return {'x': self.x, 'y': self.y, 'ancho': self.ancho, 'alto': self.alto}


@dataclass
class Grano:
    """Un grano separado: su contorno en la foto completa y el rectángulo que lo contiene."""

    contorno: np.ndarray
    rectangulo: Rectangulo


@dataclass
class Revision:
    """Resultado de la revisión rápida de una foto (RF-06, RF-07, D-46)."""

    estado: str
    motivo: str | None
    granos: list = field(default_factory=list)
    """Los granos sueltos y completos de la foto (sin los pegados ni los cortados por el borde)."""
    regiones_pegadas: list = field(default_factory=list)
    ancho: int = 0
    alto: int = 0
    luz: float = 0.0
    nitidez: float = 0.0

    def a_json(self):
        """La revisión como la manda la API (``POST /revisar``)."""
        return {
            'estado': self.estado,
            'motivo': self.motivo,
            'granosEncontrados': len(self.granos),
            'regionesPegadas': [r.a_json() for r in self.regiones_pegadas],
            'anchoImagen': self.ancho,
            'altoImagen': self.alto,
        }


def _mascara(gris, razon):
    """Máscara de los granos y brillo estimado del papel, como ``segmentar`` de ``generar_recortes.py``."""
    h, w = gris.shape
    peq = cv2.resize(gris, (max(w // 8, 1), max(h // 8, 1)), interpolation=cv2.INTER_AREA)
    fondo = cv2.dilate(peq, cv2.getStructuringElement(cv2.MORPH_RECT, (25, 25)))
    fondo = cv2.GaussianBlur(fondo, (0, 0), 6)
    fondo = cv2.resize(fondo, (w, h), interpolation=cv2.INTER_LINEAR)
    m = ((gris / np.maximum(fondo, 1.0)) < razon).astype(np.uint8) * 255
    m = cv2.medianBlur(m, 5)
    k = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5))
    m = cv2.morphologyEx(cv2.morphologyEx(m, cv2.MORPH_OPEN, k), cv2.MORPH_CLOSE, k)
    return m, fondo


def _toca_el_borde(contorno, w, h):
    """Si la región toca el borde de la imagen, como en ``generar_recortes.py`` (RF-07 C4)."""
    x, y, bw, bh = cv2.boundingRect(contorno)
    return x <= 1 or y <= 1 or x + bw >= w - 1 or y + bh >= h - 1


def _rectangulo(contorno, ancho, alto):
    """Rectángulo sin rotación de un contorno en la foto completa, recortado a la foto."""
    puntos = contorno.reshape(-1, 2)
    x0 = max(int(np.floor(puntos[:, 0].min())), 0)
    y0 = max(int(np.floor(puntos[:, 1].min())), 0)
    x1 = min(int(np.ceil(puntos[:, 0].max())) + 1, ancho)
    y1 = min(int(np.ceil(puntos[:, 1].max())) + 1, alto)
    return Rectangulo(x0, y0, x1 - x0, y1 - y0)


def _nitidez(gris, mascara):
    """Varianza del laplaciano en una franja alrededor del borde de los granos.

    Una foto movida o desenfocada pierde primero el borde entre el grano y el papel; medir solo esa franja hace
    que la cifra no dependa de cuánto papel vacío tiene la foto. Sin granos, se mide toda la imagen.
    """
    laplaciano = cv2.Laplacian(gris, cv2.CV_32F)
    if not mascara.any():
        return float(laplaciano.var())
    k = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (7, 7))
    franja = cv2.dilate(mascara, k) > cv2.erode(mascara, k)
    return float(laplaciano[franja].var())


def revisar(foto, umbrales):
    """Revisión rápida de una foto (D-46): luz, nitidez, granos pegados y los granos que tiene.

    Args:
        foto (numpy.ndarray): la foto en BGR, ya girada según su EXIF.
        umbrales (Umbrales): los umbrales de la configuración.

    Returns:
        Revision: el estado de la foto, su motivo si hay que repetirla, sus granos y sus regiones pegadas.
    """
    alto, ancho = foto.shape[:2]
    escala = min(1.0, umbrales.lado_deteccion / max(alto, ancho))
    gris = cv2.cvtColor(foto, cv2.COLOR_BGR2GRAY)
    if escala < 1.0:
        gris = cv2.resize(gris, (max(round(ancho * escala), 1), max(round(alto * escala), 1)),
                          interpolation=cv2.INTER_AREA)
    gris = gris.astype(np.float32)
    h, w = gris.shape
    mascara, fondo = _mascara(gris, umbrales.razon_grano_papel)
    contornos, _ = cv2.findContours(mascara, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

    # Regiones completas y no tan pequeñas (RF-07, paso 3); las que tocan el borde no cuentan (C4).
    regiones = [(c, cv2.contourArea(c)) for c in contornos
                if cv2.contourArea(c) >= umbrales.area_minima * h * w and not _toca_el_borde(c, w, h)]
    if regiones:
        mediana = float(np.median([a for _, a in regiones]))
        regiones = [(c, a) for c, a in regiones if a >= umbrales.factor_mancha * mediana]
    mediana = float(np.median([a for _, a in regiones])) if regiones else 0.0

    sueltos, pegados = [], []
    for contorno, area in regiones:
        completo = contorno.astype(np.float32) / escala
        destino = pegados if area >= umbrales.factor_pegados * mediana else sueltos
        destino.append(Grano(completo, _rectangulo(completo, ancho, alto)))
    # De arriba abajo y de izquierda a derecha, para que el orden no dependa de OpenCV.
    sueltos.sort(key=lambda g: (g.rectangulo.y, g.rectangulo.x))
    pegados.sort(key=lambda g: (g.rectangulo.y, g.rectangulo.x))

    solo_granos = np.zeros_like(mascara)
    cv2.drawContours(solo_granos, [c for c, _ in regiones], -1, 255, thickness=cv2.FILLED)
    luz = float(np.median(fondo))
    nitidez = _nitidez(gris, solo_granos)

    revision = Revision(APROBADA, None, sueltos, [], ancho, alto, luz, nitidez)
    if luz < umbrales.luz_minima:
        revision.estado, revision.motivo = POR_REPETIR, POCA_LUZ
    elif nitidez < umbrales.nitidez_minima:
        revision.estado, revision.motivo = POR_REPETIR, BORROSA
    elif pegados:
        revision.estado, revision.motivo = POR_REPETIR, GRANOS_PEGADOS
        revision.regiones_pegadas = [g.rectangulo for g in pegados]
    return revision


def recortar(foto, contorno):
    """Gira el grano para dejar su eje largo horizontal y lo recorta con margen.

    Es ``recortar`` de ``generar_recortes.py``, sin cambios: el mismo recorte que vio el modelo al entrenar.

    Args:
        foto (numpy.ndarray): la foto completa (BGR).
        contorno (numpy.ndarray): el contorno del grano en la foto completa (``float32``).

    Returns:
        numpy.ndarray: el recorte en BGR.
    """
    (cx, cy), (w, h), ang = cv2.minAreaRect(contorno)
    if w < h:
        w, h = h, w
        ang += 90
    m = MARGEN * h
    mw, mh = int(round(w + 2 * m)), int(round(h + 2 * m))
    r = int(np.hypot(mw, mh) / 2) + 4
    x0, y0 = int(max(cx - r, 0)), int(max(cy - r, 0))
    x1, y1 = int(min(cx + r, foto.shape[1])), int(min(cy + r, foto.shape[0]))
    roi = foto[y0:y1, x0:x1]
    c = (cx - x0, cy - y0)
    rot = cv2.warpAffine(roi, cv2.getRotationMatrix2D(c, ang, 1.0), (roi.shape[1], roi.shape[0]),
                         flags=cv2.INTER_CUBIC, borderMode=cv2.BORDER_REPLICATE)
    xa, ya = int(round(c[0] - mw / 2)), int(round(c[1] - mh / 2))
    xa, ya = max(xa, 0), max(ya, 0)
    return rot[ya:ya + mh, xa:xa + mw]
