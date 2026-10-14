# Nombre: modelo.py
# Descripción: Consultas sobre las entidades cargadas que comparten la validación y la generación.
# Fecha de creación: 13/10/2026
# Actualización: 13/10/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Consultas sobre las entidades ya cargadas (los JSON como diccionarios).

Las usan la validación y la generación, para que las dos lean el formato de la misma manera
(formato en ``specs/entidades/README.md``).
"""

#: Columnas que el generador agrega a todas las tablas.
COLUMNAS_IMPLICITAS = ('id', 'fecha_creacion', 'fecha_actualizacion')

#: Columna que agrega el generador a las tablas de cada empresa (RNF-03).
EMPRESA_ID = 'empresa_id'


def tiene_empresa(entidad):
    """Indica si la tabla lleva ``empresa_id`` (``por_empresa`` es ``si`` u ``opcional``)."""
    return entidad.get('por_empresa') in ('si', 'opcional')


def columnas(entidad):
    """Nombres de las columnas de la tabla, en el orden en que se crean."""
    resultado = ['id']
    if tiene_empresa(entidad):
        resultado.append(EMPRESA_ID)
    resultado += [a['nombre'] for a in entidad.get('atributos', [])]
    return resultado + ['fecha_creacion', 'fecha_actualizacion']


def atributo(entidad, nombre):
    """Devuelve el atributo ``nombre`` de la entidad, o ``None`` si no existe."""
    return next((a for a in entidad.get('atributos', []) if a.get('nombre') == nombre), None)


def valores_lista(entidades, atr):
    """Códigos de un atributo ``lista``, siguiendo ``valores_de`` si hace falta.

    Returns:
        list: los códigos, o una lista vacía si ``valores_de`` apunta a algo que no existe.
    """
    if 'valores' in atr:
        return [v['codigo'] for v in atr['valores']]
    entidad, _, nombre = atr.get('valores_de', '').partition('.')
    otro = atributo(entidades.get(entidad, {}), nombre)
    if otro is None or otro is atr or otro.get('tipo') != 'lista':
        return []
    return valores_lista(entidades, otro)


def dependencias(entidad):
    """Entidades que deben existir antes que esta: sus referencias y, si la tiene, ``empresa``."""
    deps = {a['referencia']['entidad'] for a in entidad.get('atributos', [])
            if a.get('tipo') == 'referencia' and 'referencia' in a}
    if tiene_empresa(entidad):
        deps.add('empresa')
    deps.discard(entidad.get('entidad'))
    return deps


def orden_de_creacion(entidades):
    """Orden en que se crean las tablas: primero las referenciadas; los empates, en orden alfabético.

    Raises:
        ValueError: si las referencias forman un ciclo.
    """
    pendientes = {nombre: dependencias(e) & entidades.keys() for nombre, e in entidades.items()}
    orden = []
    while pendientes:
        listas = sorted(n for n, deps in pendientes.items() if not deps)
        if not listas:
            raise ValueError('Las referencias entre entidades forman un ciclo: '
                             + ', '.join(sorted(pendientes)))
        siguiente = listas[0]
        orden.append(siguiente)
        del pendientes[siguiente]
        for deps in pendientes.values():
            deps.discard(siguiente)
    return orden
