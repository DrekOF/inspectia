# Nombre: salida.py
# Descripción: Escribe los archivos generados y comprueba que nadie los editó a mano.
# Fecha de creación: 13/10/2026
# Actualización: 13/10/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Escritura y comprobación de los archivos generados.

El generador solo escribe dentro de ``CARPETAS_GENERADAS``. Esas carpetas son suyas: lo que hay en
ellas y no sale de la generación actual sobra, y ``escribir`` lo borra.
"""

from dataclasses import dataclass, field
from pathlib import Path

#: Carpetas que solo escribe el generador (rutas relativas a la raíz del repositorio).
CARPETAS_GENERADAS = ('backend/src/generated', 'backend/src/test-generated')


@dataclass(frozen=True)
class Archivo:
    """Un archivo generado: su ruta relativa a la raíz (con /) y su contenido (con finales LF)."""

    ruta: str
    contenido: str


@dataclass
class Diferencias:
    """Diferencias entre lo que se generaría y lo que hay en disco."""

    modificados: list = field(default_factory=list)
    faltantes: list = field(default_factory=list)
    sobrantes: list = field(default_factory=list)

    @property
    def vacia(self):
        """``True`` si el disco coincide con lo que se generaría."""
        return not (self.modificados or self.faltantes or self.sobrantes)


@dataclass
class Resumen:
    """Lo que hizo ``escribir``: archivos escritos, sin cambios y borrados."""

    escritos: list = field(default_factory=list)
    sin_cambios: list = field(default_factory=list)
    borrados: list = field(default_factory=list)


def _en_carpeta_generada(ruta):
    return any(ruta.startswith(carpeta + '/') for carpeta in CARPETAS_GENERADAS)


def _existentes(raiz):
    """Rutas relativas (con /) de los archivos que hay hoy en las carpetas generadas."""
    rutas = []
    for carpeta in CARPETAS_GENERADAS:
        base = raiz / carpeta
        if base.is_dir():
            rutas += [p.relative_to(raiz).as_posix() for p in base.rglob('*') if p.is_file()]
    return sorted(rutas)


def _leer(ruta):
    """Contenido de un archivo con los finales de línea pasados a LF (git puede dejarlos en CRLF)."""
    return ruta.read_bytes().decode('utf-8').replace('\r\n', '\n')


def escribir(archivos, raiz):
    """Escribe los archivos generados y borra de las carpetas generadas lo que ya no se genera.

    Args:
        archivos: lista de ``Archivo``.
        raiz: raíz donde se escribe (el repositorio, o una carpeta temporal en las pruebas).

    Returns:
        Resumen: archivos escritos, sin cambios y borrados.

    Raises:
        ValueError: si un archivo queda fuera de las carpetas generadas.
    """
    raiz = Path(raiz)
    fuera = [a.ruta for a in archivos if not _en_carpeta_generada(a.ruta)]
    if fuera:
        raise ValueError('El generador solo escribe en sus carpetas; fuera de ellas: ' + ', '.join(fuera))
    resumen = Resumen()
    for archivo in archivos:
        destino = raiz / archivo.ruta
        if destino.is_file() and _leer(destino) == archivo.contenido:
            resumen.sin_cambios.append(archivo.ruta)
            continue
        destino.parent.mkdir(parents=True, exist_ok=True)
        destino.write_bytes(archivo.contenido.encode('utf-8'))
        resumen.escritos.append(archivo.ruta)
    generadas = {a.ruta for a in archivos}
    for ruta in _existentes(raiz):
        if ruta not in generadas:
            (raiz / ruta).unlink()
            resumen.borrados.append(ruta)
    for carpeta in CARPETAS_GENERADAS:
        base = raiz / carpeta
        if base.is_dir():
            for sub in sorted(base.rglob('*'), key=lambda p: len(p.parts), reverse=True):
                if sub.is_dir() and not any(sub.iterdir()):
                    sub.rmdir()
    return resumen


def comprobar(archivos, raiz):
    """Compara lo que se generaría con lo que hay en disco, sin escribir nada.

    Returns:
        Diferencias: archivos modificados a mano, faltantes y sobrantes.
    """
    raiz = Path(raiz)
    esperados = {a.ruta: a.contenido for a in archivos}
    diferencias = Diferencias()
    for ruta, contenido in sorted(esperados.items()):
        destino = raiz / ruta
        if not destino.is_file():
            diferencias.faltantes.append(ruta)
        elif _leer(destino) != contenido:
            diferencias.modificados.append(ruta)
    diferencias.sobrantes = [r for r in _existentes(raiz) if r not in esperados]
    return diferencias
