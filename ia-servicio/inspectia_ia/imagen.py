# Nombre: imagen.py
# Descripción: Lectura de una foto (JPG o PNG) ya girada según su orientación EXIF, como la muestra el navegador
#              (RF-07 C5, D-45).
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Lectura de las fotos que manda el backend.

La orientación EXIF se aplica antes de todo, con Pillow: las posiciones de los granos y de las regiones van en
píxeles de la foto ya girada, que es como la muestra el navegador (D-45). ``generar_recortes.py`` también leía
las fotos del dataset respetando su EXIF (``cv2.imread``).
"""

import io
import warnings

import cv2
import numpy as np
from PIL import Image, ImageOps

FORMATOS = {'JPEG', 'PNG'}
"""Los formatos que acepta la app (RF-05)."""


class ImagenInvalida(ValueError):
    """El archivo no es una foto JPG o PNG que se pueda leer."""


def leer_foto(contenido):
    """Lee una foto y la gira según su EXIF.

    Args:
        contenido (bytes): el archivo JPG o PNG.

    Returns:
        numpy.ndarray: la foto en BGR (``uint8``, alto x ancho x 3), como la usa OpenCV.

    Raises:
        ImagenInvalida: si no es un JPG o un PNG que se pueda leer.
    """
    if not contenido:
        raise ImagenInvalida('El archivo está vacío')
    try:
        with warnings.catch_warnings():
            # Una imagen enorme (bomba de descompresión) es un error, no un aviso.
            warnings.simplefilter('error', Image.DecompressionBombWarning)
            with Image.open(io.BytesIO(contenido)) as abierta:
                if abierta.format not in FORMATOS:
                    raise ImagenInvalida(f'Formato no admitido: {abierta.format}')
                girada = ImageOps.exif_transpose(abierta)
                rgb = np.asarray(girada.convert('RGB'))
    except ImagenInvalida:
        raise
    except (OSError, ValueError, SyntaxError, Image.DecompressionBombError, Image.DecompressionBombWarning) as error:
        raise ImagenInvalida(f'No se pudo leer la imagen: {error}') from error
    return cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR)
