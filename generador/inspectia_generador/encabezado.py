# Nombre: encabezado.py
# Descripción: Datos del header (RP-10) de los archivos generados, tomados de los JSON de entidades.
# Fecha de creación: 13/10/2026
# Actualización: 13/10/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Datos del header (RP-10) de los archivos generados.

Las fechas salen de los JSON, no del día en que se genera: así la salida es siempre la misma.
"""

from datetime import datetime

from inspectia_generador import VERSION


def _fecha(texto):
    return datetime.strptime(texto, '%d/%m/%Y')


def datos_header(entidades):
    """Fechas y versiones del header de un archivo generado desde una o varias entidades.

    La fecha de creación es la del JSON más antiguo; la de actualización, la del más reciente.

    Args:
        entidades: ``{nombre: JSON}`` de las entidades de las que sale el archivo.
    """
    headers = [e['_header'] for e in entidades.values()]
    return {
        'fecha_creacion': min(headers, key=lambda h: _fecha(h['fecha_creacion']))['fecha_creacion'],
        'actualizacion': max(headers, key=lambda h: _fecha(h['actualizacion']))['actualizacion'],
        'versiones': ', '.join(f"{n} {e['_header']['version']}" for n, e in entidades.items()),
        'version': VERSION,
    }
