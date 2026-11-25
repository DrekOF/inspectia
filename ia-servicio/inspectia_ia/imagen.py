# Nombre: imagen.py
# Descripción: Lectura de una foto (JPG o PNG) ya girada según su orientación EXIF, como la muestra el navegador
#              (RF-07 C5, D-45), con un tope explícito de 120 megapíxeles.
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.1
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

MAX_MEGAPIXELES = 120
"""Tamaño máximo de una foto, en megapíxeles. El tope de Pillow (unos 89) es menor que el de algunas cámaras de
celular de 108 MP; 120 deja margen y sigue frenando una bomba de descompresión (una foto de 120 MP abierta ocupa unos
360 MB). El backend, además, no acepta archivos de más de 10 MB (D-41)."""

Image.MAX_IMAGE_PIXELS = MAX_MEGAPIXELES * 1_000_000


class ImagenInvalida(ValueError):
    """El archivo no es una foto JPG o PNG que se pueda leer."""


def abrir_foto(contenido):
    """Abre una foto sin decodificar sus píxeles y comprueba su formato y su tamaño.

    Args:
        contenido (bytes): el archivo JPG o PNG.

    Returns:
        PIL.Image.Image: la foto abierta; quien la recibe la cierra (sirve con ``with``).

    Raises:
        ImagenInvalida: si está vacía, no es un JPG o un PNG o pasa de ``MAX_MEGAPIXELES``.
    """
    if not contenido:
        raise ImagenInvalida('El archivo está vacío')
    try:
        with warnings.catch_warnings():
            # Pillow avisa sobre MAX_IMAGE_PIXELS y falla sobre el doble: las dos son una foto demasiado grande.
            warnings.simplefilter('error', Image.DecompressionBombWarning)
            abierta = Image.open(io.BytesIO(contenido))
    except (Image.DecompressionBombError, Image.DecompressionBombWarning) as error:
        raise ImagenInvalida(f'La foto pasa de {MAX_MEGAPIXELES} megapíxeles') from error
    except (OSError, ValueError, SyntaxError) as error:
        raise ImagenInvalida(f'No se pudo leer la imagen: {error}') from error
    if abierta.format not in FORMATOS:
        abierta.close()
        raise ImagenInvalida(f'Formato no admitido: {abierta.format}')
    return abierta


def leer_foto(contenido):
    """Lee una foto y la gira según su EXIF.

    Args:
        contenido (bytes): el archivo JPG o PNG.

    Returns:
        numpy.ndarray: la foto en BGR (``uint8``, alto x ancho x 3), como la usa OpenCV.

    Raises:
        ImagenInvalida: si no es un JPG o un PNG que se pueda leer o pasa de ``MAX_MEGAPIXELES``.
    """
    with abrir_foto(contenido) as abierta:
        try:
            with warnings.catch_warnings():
                warnings.simplefilter('error', Image.DecompressionBombWarning)
                girada = ImageOps.exif_transpose(abierta)
                rgb = np.asarray(girada.convert('RGB'))
        except (OSError, ValueError, SyntaxError) as error:
            raise ImagenInvalida(f'No se pudo leer la imagen: {error}') from error
    return cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR)
