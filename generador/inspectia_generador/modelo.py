# Nombre: modelo.py
# Descripción: Consultas sobre las entidades cargadas que comparten la validación y la generación.
# Fecha de creación: 13/10/2026
# Actualización: 27/10/2026
# Autor: Equipo InspectIA
# Versión: 1.3
"""Consultas sobre las entidades ya cargadas (los JSON como diccionarios).

Las usan la validación y la generación, para que las dos lean el formato de la misma manera
(formato en ``specs/entidades/README.md``).
"""

#: Columnas que el generador agrega a todas las tablas.
COLUMNAS_IMPLICITAS = ('id', 'fecha_creacion', 'fecha_actualizacion')

#: Columna que agrega el generador a las tablas de cada empresa (RNF-03).
EMPRESA_ID = 'empresa_id'

#: Las claves de ``pantalla``: el ícono del menú, el atributo que va debajo del nombre en la lista, dónde va el
#: formulario, la regla que limita los activos, los valores con que empieza el formulario al registrar y los textos.
CLAVES_DE_PANTALLA = ('icono', 'debajo_del_nombre', 'formulario', 'limite_de_activos', 'inicial', 'textos')

#: Dónde va el formulario en el PC: encima de la lista, como en Empresas, o al lado, como en Usuarios. En el
#: celular va siempre encima.
FORMULARIOS = ('encima', 'al_lado')

#: El marcador del resumen que da el máximo de la regla de ``limite_de_activos``, como 25. Solo existe si la
#: pantalla declara esa regla.
MAXIMO = 'maximo'

#: En un texto de una fila, cada ``{atributo}`` de la lista toma el valor de esa fila, como «Editar {nombre}».
FILA = 'fila'

#: Los textos de ``pantalla.textos``: si se cuentan, y entonces van en singular y en plural, y qué marcadores
#: pueden usar. Cuáles son obligatorios lo dicen las operaciones de la entidad (``frontend.py``).
TEXTOS_DE_PANTALLA = {
    'descripcion': (False, ()),
    'resumen': (False, ('total', 'activos', 'inactivos', MAXIMO)),
    'resumen_total': (True, ('cantidad',)),
    'resumen_activos': (True, ('cantidad', MAXIMO)),
    'resumen_inactivos': (True, ('cantidad',)),
    'vacia': (False, ()),
    'nuevo': (False, ()),
    'crear': (False, ()),
    'titulo_editar': (False, ()),
    'nota_formulario': (False, ()),
    'contrasena_al_crear': (False, ()),
    'ayuda_contrasena_al_crear': (False, ()),
    'contrasena_al_editar': (False, ()),
    'ayuda_contrasena_al_editar': (False, ()),
    'nota_contrasena_de_otro': (False, FILA),
    'fila_propia': (False, ()),
    'titulo_lista': (False, ()),
    'columna_nombre': (False, ()),
    'debajo_del_nombre': (False, FILA),
    'activo': (False, ()),
    'inactivo': (False, ()),
    'editar_fila': (False, FILA),
    'desactivar_fila': (False, FILA),
    'reactivar_fila': (False, FILA),
    'confirmar_desactivar': (False, FILA),
    'confirmar_desactivar_detalle': (False, FILA),
    'al_guardar': (False, FILA),
    'al_desactivar': (False, FILA),
    'al_reactivar': (False, FILA),
    'nota_lista': (False, ()),
}


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


def clave_en_bloque(entidad):
    """La lista que identifica cada fila de una entidad con ``edicion_en_bloque``, o ``None``.

    Es la que forma un único con ``empresa_id``: cada empresa tiene una fila por cada valor de la lista,
    como un rango por cada categoría (RF-13). Debe traer sus ``valores``, porque de ellos salen las filas
    y sus etiquetas.
    """
    for u in entidad.get('unicos', []):
        columnas = u.get('atributos', [])
        otras = [c for c in columnas if c != EMPRESA_ID]
        if len(columnas) == 2 and len(otras) == 1:
            atr = atributo(entidad, otras[0])
            if atr is not None and atr.get('tipo') == 'lista' and 'valores' in atr:
                return atr
    return None


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


def regla(entidad, codigo):
    """La regla ``codigo`` de una entidad, o ``None`` si no la tiene."""
    return next((r for r in entidad.get('reglas', []) if r.get('codigo') == codigo), None)


def reglas_de_la_fila_propia(entidad):
    """Las reglas que se aplican a la fila de quien tiene la sesión (``en_la_fila_propia``): la pantalla las cumple
    antes de llamar a la API."""
    return [r for r in entidad.get('reglas', []) if r.get('en_la_fila_propia')]


def contrasena_opcional_al_editar(entidad):
    """La contraseña del formulario que al editar es opcional, como la de un usuario, o ``None``: vacía, queda la
    que había."""
    if 'editar' not in entidad.get('operaciones', {}):
        return None
    return next((a for a in entidad['atributos'] if a['tipo'] == 'contrasena' and a.get('en_formulario')
                 and not a['entrada']['obligatoria_al_editar']), None)
