# Nombre: carga.py
# Descripción: Lectura de los JSON de entidades, del alcance de la API y de los códigos que existen
#              en specs/.
# Fecha de creación: 13/10/2026
# Actualización: 13/10/2026
# Autor: Equipo InspectIA
# Versión: 1.1
"""Lectura de los JSON de entidades, de ``alcance.json`` y de los códigos (RF, RNF, D, P y RP) que
existen en specs/."""

import json
import re
from pathlib import Path


def cargar_entidades(*fuentes):
    """Lee las entidades de carpetas (todos sus ``*.json``) o de archivos sueltos.

    Args:
        *fuentes: rutas de carpetas o de archivos JSON.

    Returns:
        dict: ``{nombre de la entidad: JSON}``, ordenado por nombre.

    Raises:
        ValueError: si un archivo no se llama como su entidad o si una entidad se repite.
    """
    archivos = []
    for fuente in map(Path, fuentes):
        archivos.extend(sorted(fuente.glob('*.json')) if fuente.is_dir() else [fuente])
    entidades = {}
    for archivo in archivos:
        datos = json.loads(archivo.read_text(encoding='utf-8'))
        nombre = datos.get('entidad')
        if nombre != archivo.stem:
            raise ValueError(f'{archivo.name}: "entidad" debe ser "{archivo.stem}"')
        if nombre in entidades:
            raise ValueError(f'La entidad {nombre} está definida dos veces')
        entidades[nombre] = datos
    return dict(sorted(entidades.items()))


def cargar_alcance(ruta):
    """Entidades a las que se genera la API, según ``alcance.json``.

    Args:
        ruta: el archivo ``alcance.json``.

    Returns:
        tuple: los nombres de ``api``, en el orden del archivo.
    """
    datos = json.loads(Path(ruta).read_text(encoding='utf-8'))
    return tuple(datos.get('api', []))


def revisar_alcance(api, entidades):
    """Errores del alcance: cada entidad de ``api`` debe existir y tener CRUD.

    Returns:
        list: mensajes de error; vacía si todo está bien.
    """
    errores = []
    for nombre in api:
        if nombre not in entidades:
            errores.append(f'alcance.json: la entidad {nombre} no existe')
        elif not entidades[nombre]['crud']:
            errores.append(f'alcance.json: {nombre} no tiene CRUD')
    return errores


def cargar_codigos(specs):
    """Códigos que existen en la especificación.

    Args:
        specs: carpeta ``specs/`` del repositorio.

    Returns:
        set: RF y RNF de ``fichas.json``, D y P de ``decisiones.md`` y RP de ``requisitos-docente.md``.
    """
    specs = Path(specs)
    fichas = json.loads((specs / 'fichas.json').read_text(encoding='utf-8'))
    codigos = {f['codigo'] for f in fichas['rf'] + fichas['rnf']}
    decisiones = (specs / 'decisiones.md').read_text(encoding='utf-8')
    requisitos = (specs / 'requisitos-docente.md').read_text(encoding='utf-8')
    codigos |= set(re.findall(r'\b([DP]-\d{2})\b', decisiones))
    codigos |= set(re.findall(r'\b(RP-\d{2})\b', requisitos))
    return codigos
