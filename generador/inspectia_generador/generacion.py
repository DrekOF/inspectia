# Nombre: generacion.py
# Descripción: Arma la lista de archivos generados aplicando las plantillas a las entidades.
# Fecha de creación: 13/10/2026
# Actualización: 13/10/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Generación: aplica las plantillas de ``generador/plantillas`` a las entidades.

No escribe nada: devuelve la lista de archivos, que luego se escriben o se comprueban
(``salida.py``). La salida no depende de la fecha ni del equipo: las fechas de los headers salen
de los JSON.
"""

from datetime import datetime
from pathlib import Path

from jinja2 import Environment, FileSystemLoader, StrictUndefined

from inspectia_generador import VERSION
from inspectia_generador.salida import Archivo
from inspectia_generador.sql import construir_tablas

PLANTILLAS = Path(__file__).resolve().parent.parent / 'plantillas'
RUTA_MIGRACION = 'backend/src/generated/resources/db/migration/V1__esquema_inicial.sql'


def _entorno():
    return Environment(loader=FileSystemLoader(PLANTILLAS), undefined=StrictUndefined,
                       trim_blocks=True, lstrip_blocks=True, keep_trailing_newline=True,
                       autoescape=False)


def _fecha(texto):
    return datetime.strptime(texto, '%d/%m/%Y')


def datos_header(entidades):
    """Fechas y versiones del header de un archivo generado desde varias entidades.

    La fecha de creación es la del JSON más antiguo; la de actualización, la del más reciente.
    """
    headers = [e['_header'] for e in entidades.values()]
    return {
        'fecha_creacion': min(headers, key=lambda h: _fecha(h['fecha_creacion']))['fecha_creacion'],
        'actualizacion': max(headers, key=lambda h: _fecha(h['actualizacion']))['actualizacion'],
        'versiones': ', '.join(f"{n} {e['_header']['version']}" for n, e in entidades.items()),
    }


def generar_archivos(entidades):
    """Genera los archivos de las entidades (ya validadas).

    Returns:
        list: ``Archivo`` en orden por ruta.
    """
    entorno = _entorno()
    esquema = entorno.get_template('sql/esquema.sql.j2').render(
        version=VERSION, header=datos_header(entidades), tablas=construir_tablas(entidades))
    archivos = [Archivo(RUTA_MIGRACION, esquema)]
    return sorted(archivos, key=lambda a: a.ruta)
