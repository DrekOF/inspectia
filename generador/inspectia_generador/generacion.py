# Nombre: generacion.py
# Descripción: Arma la lista de archivos generados aplicando las plantillas a las entidades.
# Fecha de creación: 13/10/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.3
"""Generación: aplica las plantillas de ``generador/plantillas`` a las entidades.

Genera:

- El esquema de la base (``V1__esquema_inicial.sql``) con todas las tablas. Su header lleva la versión de su
  plantilla, ``sql/esquema.sql.j2``, y no la del generador (D-49): así un cambio del generador que no toca el esquema
  no reescribe V1.
- Los textos en español e inglés de todas las entidades (``i18n/generado*.properties``).
- La persistencia de cada entidad con CRUD.
- La API y sus pruebas, solo para las entidades del alcance (``alcance.json``).
- Las pantallas del frontend y sus pruebas, solo para las entidades de ``pantallas`` en ``alcance.json``, y las
  listas de pantallas y de sus textos, que la app lee siempre.

No escribe nada: devuelve la lista de archivos, que luego se escriben o se comprueban
(``salida.py``). La salida no depende de la fecha ni del equipo: las fechas de los headers salen
de los JSON.
"""

import re
from pathlib import Path

from jinja2 import Environment, FileSystemLoader, StrictUndefined

from inspectia_generador import frontend, java
from inspectia_generador.encabezado import datos_header
from inspectia_generador.nombres import envolver, texto_java, texto_js, valor_properties
from inspectia_generador.salida import Archivo
from inspectia_generador.sql import ErrorGeneracion, construir_tablas
from inspectia_generador.textos import textos

PLANTILLAS = Path(__file__).resolve().parent.parent / 'plantillas'
PLANTILLA_ESQUEMA = 'sql/esquema.sql.j2'
RUTA_MIGRACION = 'backend/src/generated/resources/db/migration/V1__esquema_inicial.sql'
RUTA_TEXTOS = 'backend/src/generated/resources/i18n/generado{sufijo}.properties'


def _entorno():
    entorno = Environment(loader=FileSystemLoader(PLANTILLAS), undefined=StrictUndefined,
                          trim_blocks=True, lstrip_blocks=True, keep_trailing_newline=True,
                          autoescape=False)
    entorno.filters['envolver'] = envolver
    entorno.filters['java'] = texto_java
    entorno.filters['js'] = texto_js
    entorno.filters['properties'] = valor_properties
    return entorno


def version_de_la_plantilla(entorno, nombre):
    """La versión del header (RP-10) de una plantilla: la línea «Versión: x» de su primer comentario ``{# … #}``.

    Es la única fuente de la versión de V1 (D-49): no se repite en el código.

    Raises:
        ErrorGeneracion: si la plantilla no empieza con su header o el header no tiene «Versión:».
    """
    fuente = entorno.loader.get_source(entorno, nombre)[0]
    encontrada = None
    if fuente.lstrip().startswith('{#'):
        header = fuente.split('#}', 1)[0]
        encontrada = re.search(r'^[ \t]*Versión:[ \t]*(\S+)[ \t]*$', header, re.M)
    if encontrada is None:
        raise ErrorGeneracion(f'{nombre}: su header no tiene la línea «Versión:» (RP-10)')
    return encontrada.group(1)


def generar_archivos(entidades, api=(), pantallas=()):
    """Genera los archivos de las entidades (ya validadas).

    Args:
        entidades: ``{nombre: JSON}``.
        api: entidades a las que se genera la API (``alcance.json``).
        pantallas: entidades a las que se genera la pantalla del frontend (``alcance.json``); necesitan su API.

    Returns:
        list: ``Archivo`` en orden por ruta.

    Raises:
        ErrorGeneracion: si algo de las entidades no se puede generar.
    """
    entorno = _entorno()
    esquema = entorno.get_template(PLANTILLA_ESQUEMA).render(
        version=version_de_la_plantilla(entorno, PLANTILLA_ESQUEMA), header=datos_header(entidades),
        tablas=construir_tablas(entidades))
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
    archivos += _aplicar(entorno, frontend.archivos_frontend(entidades, pantallas))
    return sorted(archivos, key=lambda a: a.ruta)


def _aplicar(entorno, archivos):
    return [Archivo(ruta, entorno.get_template(plantilla).render(**contexto))
            for ruta, plantilla, contexto in archivos]
