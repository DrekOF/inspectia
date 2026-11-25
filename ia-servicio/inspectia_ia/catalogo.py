# Nombre: catalogo.py
# Descripción: Lectura de modelos.json, el catálogo de los 5 modelos de la release modelos-v1.0 con su archivo, su
#              SHA-256 y sus métricas (D-18, D-40, D-48).
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Catálogo de los modelos (``ia-servicio/modelos.json``).

Cada modelo trae su nombre, su versión, su tipo (``CNN`` o ``TRANSFORMER``, los códigos de
``specs/entidades/modelo.json``), su formato, el archivo de la release, su SHA-256 y las métricas que el backend
guarda en la tabla ``modelo`` (D-48). ``GET /modelos`` las entrega tal cual.
"""

import json
import re
from dataclasses import dataclass
from pathlib import Path

TIPOS = {'CNN', 'TRANSFORMER'}
FORMATOS = {'savedmodel-zip', 'keras'}
METRICAS = ('f1Macro', 'precisionMacro', 'recallMacro', 'recallSano', 'recallNoSano', 'precisionSano',
            'precisionNoSano', 'sanosComoSanos', 'sanosComoNoSanos', 'noSanosComoSanos', 'noSanosComoNoSanos',
            'tiempoPorGranoMs')
"""Las métricas de un modelo, en el orden de las columnas de ``modelo.json`` (D-48)."""

_SHA256 = re.compile(r'^[0-9a-f]{64}$')
_ARCHIVO = re.compile(r'^[A-Za-z0-9._-]+$')


class CatalogoInvalido(ValueError):
    """``modelos.json`` no tiene la forma esperada."""


@dataclass(frozen=True)
class Archivo:
    """Un archivo de la release con su SHA-256."""

    nombre: str
    sha256: str


@dataclass(frozen=True)
class EntradaModelo:
    """Un modelo del catálogo."""

    nombre: str
    version: str
    tipo: str
    formato: str
    archivo: Archivo
    ficha: Archivo
    metricas: dict

    def a_json(self):
        """El modelo como lo entrega ``GET /modelos``."""
        return {'nombre': self.nombre, 'version': self.version, 'tipo': self.tipo,
                **{m: self.metricas[m] for m in METRICAS}}


@dataclass(frozen=True)
class Catalogo:
    """Los modelos y la release de donde salen."""

    etiqueta: str
    url_release: str
    manifest: Archivo
    modelos: tuple

    def modelo(self, nombre):
        """El modelo con ese nombre.

        Raises:
            CatalogoInvalido: si no está en el catálogo.
        """
        for m in self.modelos:
            if m.nombre == nombre:
                return m
        nombres = ', '.join(m.nombre for m in self.modelos)
        raise CatalogoInvalido(f'El modelo «{nombre}» no está en modelos.json (están: {nombres})')

    def archivos(self):
        """Todos los archivos de la release que van a S3: el manifest, y el archivo y la ficha de cada modelo."""
        return [self.manifest] + [a for m in self.modelos for a in (m.archivo, m.ficha)]


def _archivo(datos, donde):
    """Lee un ``{"archivo", "sha256"}`` y revisa que el nombre sea simple y el SHA-256 tenga 64 cifras."""
    nombre, sha = datos.get('archivo'), datos.get('sha256')
    if not isinstance(nombre, str) or not _ARCHIVO.match(nombre):
        raise CatalogoInvalido(f'{donde}: el archivo «{nombre}» no es un nombre de archivo simple')
    if not isinstance(sha, str) or not _SHA256.match(sha):
        raise CatalogoInvalido(f'{donde}: el SHA-256 de «{nombre}» no tiene 64 cifras hexadecimales en minúscula')
    return Archivo(nombre, sha)


def cargar(ruta):
    """Lee y revisa ``modelos.json``.

    Args:
        ruta (pathlib.Path | str): el archivo.

    Returns:
        Catalogo: el catálogo.

    Raises:
        CatalogoInvalido: si le falta algo o algo no tiene la forma esperada.
    """
    datos = json.loads(Path(ruta).read_text(encoding='utf-8'))
    release = datos.get('release', {})
    modelos = []
    for i, m in enumerate(datos.get('modelos', [])):
        donde = f'modelos[{i}]'
        if m.get('tipo') not in TIPOS:
            raise CatalogoInvalido(f'{donde}: el tipo debe ser CNN o TRANSFORMER')
        if m.get('formato') not in FORMATOS:
            raise CatalogoInvalido(f'{donde}: el formato debe ser savedmodel-zip o keras')
        metricas = m.get('metricas', {})
        faltan = [c for c in METRICAS if not isinstance(metricas.get(c), (int, float))]
        if faltan:
            raise CatalogoInvalido(f'{donde}: faltan métricas: {", ".join(faltan)}')
        modelos.append(EntradaModelo(m['nombre'], m['version'], m['tipo'], m['formato'],
                                     _archivo(m, donde), _archivo(m.get('ficha', {}), f'{donde}.ficha'),
                                     {c: metricas[c] for c in METRICAS}))
    if not modelos:
        raise CatalogoInvalido('modelos.json no tiene modelos')
    if len({m.nombre for m in modelos}) != len(modelos):
        raise CatalogoInvalido('modelos.json repite un nombre de modelo')
    return Catalogo(release.get('etiqueta', ''), release.get('url', ''),
                    _archivo(release.get('manifest', {}), 'release.manifest'), tuple(modelos))
