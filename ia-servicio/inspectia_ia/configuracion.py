# Nombre: configuracion.py
# Descripción: Configuración del servicio de IA, leída de variables de entorno: el modelo, de dónde se baja, la
#              caché y los umbrales de la revisión de las fotos (RF-06, RF-07, D-09).
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.1
"""Configuración del servicio de IA.

Todo sale de variables de entorno, con un valor por defecto justificado en ``ia-servicio/README.md``. Los
umbrales de la revisión son provisionales hasta la prueba con fotos reales (P-12): por eso se pueden cambiar sin
tocar el código.
"""

import os
from dataclasses import dataclass, field
from pathlib import Path

CARPETA_DEL_SERVICIO = Path(__file__).resolve().parent.parent
"""La carpeta ``ia-servicio/``, donde está ``modelos.json``."""


@dataclass(frozen=True)
class Umbrales:
    """Umbrales de la separación y de la revisión rápida de una foto (RF-06, RF-07, D-09, D-46)."""

    lado_deteccion: int = 1156
    """Lado mayor, en píxeles, de la imagen en la que se buscan los granos. Es el de las fotos del dataset en
    ``generar_recortes.py`` (4624 px leídas a la mitad y reducidas otra vez a la mitad), así sus filtros (de 5 y de
    25 píxeles) trabajan a la misma escala con que se hicieron los recortes del entrenamiento."""

    razon_grano_papel: float = 0.6
    """Un píxel es grano si es más oscuro que esta fracción del papel (``UMBRAL`` de ``generar_recortes.py``)."""

    area_minima: float = 0.0003
    """Área mínima de una región, relativa a la imagen de detección. ``generar_recortes.py`` usa 0,0015 con un
    grano por foto; con unos 50 granos por foto un grano ocupa del 0,2 al 0,6 % de la imagen, así que el mínimo
    baja para no perder granos de una foto tomada más lejos. Las manchas que pasan este mínimo las quita
    ``factor_mancha``."""

    factor_mancha: float = 0.3
    """Una región con menos de esta fracción de la mediana de las regiones de la foto es una mancha (RF-07,
    paso 3)."""

    factor_pegados: float = 2.0
    """Una región con al menos este múltiplo de la mediana de los granos de la foto son granos pegados (D-09)."""

    nitidez_minima: float = 100.0
    """Varianza del laplaciano, en el borde de los granos, por debajo de la cual la foto está borrosa (RF-06 C1).
    En 30 fotos del dataset mide de 1346 a 4766; desenfocadas con un filtro gaussiano de 3 píxeles, de 168 a 424, y
    de 6 píxeles, de 26 a 58. 100 queda entre las dos últimas (ver ``ia-servicio/README.md``)."""

    luz_minima: float = 110.0
    """Brillo del papel (0 a 255) por debajo del cual la foto tiene poca luz (RF-06 C2). En 30 fotos del dataset el
    papel mide de 169 a 193 (mediana 182); con la mitad de la luz, de 84 a 96. 110 es el 60 % de la mediana."""


@dataclass(frozen=True)
class Configuracion:
    """Configuración completa del servicio."""

    modelo: str = 'EfficientNet-B0'
    """Nombre del modelo en uso, como en ``modelos.json`` (D-18)."""

    modelos_uri: str = ''
    """De dónde se baja el modelo: ``s3://bucket/modelos/`` o ``file:///carpeta/``."""

    cache: Path = Path('/var/lib/inspectia/modelos')
    """Carpeta (un volumen de Docker) donde queda el archivo del modelo para no bajarlo en cada arranque."""

    catalogo: Path = CARPETA_DEL_SERVICIO / 'modelos.json'
    """El catálogo de los 5 modelos, con su SHA-256 y sus métricas."""

    hilos: int = 0
    """Hilos de TensorFlow para la clasificación; 0 deja los que elija TensorFlow (todos los núcleos)."""

    lote_clasificacion: int = 16
    """Cuántos recortes clasifica el modelo de una vez. Con 16, 150 granos usan unos 0,8 GB y tardan lo mismo que
    con 64, que pasa de 1,5 GB, el límite del contenedor (medido; ver ``ia-servicio/README.md``)."""

    tamano_maximo: int = 20 * 1024 * 1024
    """Tamaño máximo de una foto, en bytes. El backend acepta hasta 10 MB (D-41); aquí hay margen."""

    umbrales: Umbrales = field(default_factory=Umbrales)


def _numero(entorno, nombre, tipo, defecto):
    """Lee una variable numérica de ``entorno`` o devuelve ``defecto`` si no está."""
    valor = entorno.get(nombre, '').strip()
    if not valor:
        return defecto
    try:
        return tipo(valor)
    except ValueError as error:
        raise ValueError(f'{nombre} debe ser un número, y es «{valor}»') from error


def desde_entorno(entorno=None):
    """Arma la configuración desde las variables de entorno.

    Args:
        entorno: las variables (por defecto, ``os.environ``).

    Returns:
        Configuracion: la configuración.
    """
    e = os.environ if entorno is None else entorno
    u = Umbrales()
    umbrales = Umbrales(
        lado_deteccion=_numero(e, 'INSPECTIA_LADO_DETECCION', int, u.lado_deteccion),
        razon_grano_papel=_numero(e, 'INSPECTIA_RAZON_GRANO_PAPEL', float, u.razon_grano_papel),
        area_minima=_numero(e, 'INSPECTIA_AREA_MINIMA', float, u.area_minima),
        factor_mancha=_numero(e, 'INSPECTIA_FACTOR_MANCHA', float, u.factor_mancha),
        factor_pegados=_numero(e, 'INSPECTIA_FACTOR_PEGADOS', float, u.factor_pegados),
        nitidez_minima=_numero(e, 'INSPECTIA_NITIDEZ_MINIMA', float, u.nitidez_minima),
        luz_minima=_numero(e, 'INSPECTIA_LUZ_MINIMA', float, u.luz_minima),
    )
    c = Configuracion()
    return Configuracion(
        modelo=e.get('INSPECTIA_MODELO', '').strip() or c.modelo,
        modelos_uri=e.get('INSPECTIA_MODELOS_URI', '').strip(),
        cache=Path(e.get('INSPECTIA_MODELOS_CACHE', '').strip() or c.cache),
        catalogo=Path(e.get('INSPECTIA_MODELOS_CATALOGO', '').strip() or c.catalogo),
        hilos=_numero(e, 'INSPECTIA_HILOS', int, c.hilos),
        lote_clasificacion=_numero(e, 'INSPECTIA_LOTE_CLASIFICACION', int, c.lote_clasificacion),
        tamano_maximo=_numero(e, 'INSPECTIA_TAMANO_MAXIMO', int, c.tamano_maximo),
        umbrales=umbrales,
    )
