# Nombre: generacion.py
# Descripción: Arma la lista de archivos generados aplicando las plantillas a las entidades.
# Fecha de creación: 13/10/2026
# Actualización: 13/10/2026
# Autor: Equipo InspectIA
# Versión: 1.1
"""Generación: aplica las plantillas de ``generador/plantillas`` a las entidades.

Genera:

- El esquema de la base (``V1__esquema_inicial.sql``) con todas las tablas.
- Los textos en español e inglés de todas las entidades (``i18n/generado*.properties``).
- La persistencia de cada entidad con CRUD.
- La API y sus pruebas, solo para las entidades del alcance (``alcance.json``).

No escribe nada: devuelve la lista de archivos, que luego se escriben o se comprueban
(``salida.py``). La salida no depende de la fecha ni del equipo: las fechas de los headers salen
de los JSON.
"""

from pathlib import Path

from jinja2 import Environment, FileSystemLoader, StrictUndefined

from inspectia_generador import VERSION, java
from inspectia_generador.encabezado import datos_header
from inspectia_generador.nombres import envolver, texto_java, valor_properties
from inspectia_generador.salida import Archivo
from inspectia_generador.sql import construir_tablas
from inspectia_generador.textos import textos

PLANTILLAS = Path(__file__).resolve().parent.parent / 'plantillas'
RUTA_MIGRACION = 'backend/src/generated/resources/db/migration/V1__esquema_inicial.sql'
RUTA_TEXTOS = 'backend/src/generated/resources/i18n/generado{sufijo}.properties'


def _entorno():
    entorno = Environment(loader=FileSystemLoader(PLANTILLAS), undefined=StrictUndefined,
                          trim_blocks=True, lstrip_blocks=True, keep_trailing_newline=True,
                          autoescape=False)
    entorno.filters['envolver'] = envolver
    entorno.filters['java'] = texto_java
    entorno.filters['properties'] = valor_properties
    return entorno


def generar_archivos(entidades, api=()):
    """Genera los archivos de las entidades (ya validadas).

    Args:
        entidades: ``{nombre: JSON}``.
        api: entidades a las que se genera la API (``alcance.json``).

    Returns:
        list: ``Archivo`` en orden por ruta.

    Raises:
        ErrorGeneracion: si algo de las entidades no se puede generar.
    """
    entorno = _entorno()
    esquema = entorno.get_template('sql/esquema.sql.j2').render(
        version=VERSION, header=datos_header(entidades), tablas=construir_tablas(entidades))
    archivos = [Archivo(RUTA_MIGRACION, esquema)]

    secciones = textos(entidades)
    for idioma, sufijo in (('es', ''), ('en', '_en')):
        ruta = RUTA_TEXTOS.format(sufijo=sufijo)
        contenido = entorno.get_template('i18n/textos.properties.j2').render(
            nombre=ruta.rsplit('/', 1)[1], idioma=idioma, header=datos_header(entidades), secciones=secciones)
        archivos.append(Archivo(ruta, contenido))

    for entidad in entidades.values():
        if entidad['crud']:
            archivos += _aplicar(entorno, java.archivos_persistencia(entidades, entidad))
    for nombre in api:
        archivos += _aplicar(entorno, java.archivos_api(entidades, entidades[nombre]))
    return sorted(archivos, key=lambda a: a.ruta)


def _aplicar(entorno, archivos):
    return [Archivo(ruta, entorno.get_template(plantilla).render(**contexto))
            for ruta, plantilla, contexto in archivos]
