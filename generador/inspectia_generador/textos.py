# Nombre: textos.py
# Descripción: Textos en español e inglés de las entidades (etiquetas, valores y mensajes) y mensajes
#              generales de las validaciones (RP-11).
# Fecha de creación: 13/10/2026
# Actualización: 20/10/2026
# Autor: Equipo InspectIA
# Versión: 1.2
"""Textos en español e inglés que van a ``i18n/generado.properties`` y ``generado_en.properties``.

Las claves siguen la forma ``entidad.atributo.algo``. Los mensajes del JSON llevan ``{nombre}`` con
un valor de ``validaciones`` o de ``parametros``, y se reemplaza aquí. Los mensajes generales
conservan ``{max}``, ``{min}`` y ``{value}``, que reemplaza Bean Validation con el valor de cada
anotación.

Una entidad con ``edicion_en_bloque`` tiene además el mensaje de las filas incompletas, que se arma con
la etiqueta y los valores de la lista que identifica cada fila: así cambia solo con el JSON.
"""

import re

from inspectia_generador.modelo import atributo, clave_en_bloque
from inspectia_generador.nombres import en_frase

#: Mensajes generales de las validaciones: clave, español e inglés.
MENSAJES_GENERALES = (
    ('validacion.obligatorio', 'Este dato es obligatorio.', 'This field is required.'),
    ('validacion.longitud_min', 'Debe tener al menos {min} caracteres.', 'It must have at least {min} characters.'),
    ('validacion.longitud_max', 'Puede tener como máximo {max} caracteres.', 'It can have at most {max} characters.'),
    ('validacion.bytes_max', 'Es demasiado largo: usa menos caracteres o menos tildes y eñes.',
     'It is too long: use fewer characters or fewer accented letters.'),
    ('validacion.patron', 'El formato no es válido.', 'The format is not valid.'),
    ('validacion.correo', 'Escribe un correo válido.', 'Enter a valid email address.'),
    ('validacion.minimo', 'Debe ser mayor o igual a {value}.', 'It must be greater than or equal to {value}.'),
    ('validacion.maximo', 'Debe ser menor o igual a {value}.', 'It must be less than or equal to {value}.'),
    ('validacion.decimales_uno', 'Usa como máximo 1 decimal.', 'Use at most 1 decimal place.'),
    ('validacion.decimales', 'Usa como máximo {value} decimales.', 'Use at most {value} decimal places.'),
    ('validacion.unico', 'Ya existe un registro con este dato.', 'A record with this value already exists.'),
    ('validacion.valor_no_permitido', 'No puedes asignar este valor.', 'You cannot assign this value.'),
    ('validacion.solo_lectura', 'Este dato no se puede cambiar.', 'This value cannot be changed.'),
)

_GENERALES = {clave: (es, en) for clave, es, en in MENSAJES_GENERALES}

#: Mensaje general de cada validación del JSON y el parámetro de Bean Validation que usa. Los decimales
#: salen de la escala del atributo: con 1, el mensaje va en singular.
GENERAL_DE = {
    'obligatorio': ('validacion.obligatorio', None),
    'longitud_min': ('validacion.longitud_min', 'min'),
    'longitud_max': ('validacion.longitud_max', 'max'),
    'bytes_max': ('validacion.bytes_max', None),
    'patron': ('validacion.patron', None),
    'formato': ('validacion.correo', None),
    'min': ('validacion.minimo', 'value'),
    'max': ('validacion.maximo', 'value'),
    'decimales_uno': ('validacion.decimales_uno', None),
    'decimales': ('validacion.decimales', 'value'),
    'unico': ('validacion.unico', None),
    'valores_en_formulario': ('validacion.valor_no_permitido', None),
    'solo_lectura_si': ('validacion.solo_lectura', None),
}


def reemplazar(texto, valores):
    """Reemplaza cada ``{nombre}`` del texto por ``valores[nombre]`` (si existe)."""
    return re.sub(r'\{(\w+)\}', lambda m: _numero(valores[m[1]]) if m[1] in valores else m[0], texto)


def _numero(valor):
    if isinstance(valor, float) and valor.is_integer():
        return str(int(valor))
    return str(valor)


def clave_mensaje(entidad, atr, validacion):
    """Clave del mensaje de una validación: la propia del atributo, si el JSON la trae, o la general."""
    if validacion in atr.get('mensajes', {}):
        return f"{entidad['entidad']}.{atr['nombre']}.{validacion}"
    return GENERAL_DE[validacion][0]


def mensaje_esperado(entidad, atr, validacion, valor=None):
    """Mensaje en español que responde la API cuando el atributo no cumple la validación.

    Args:
        valor: el límite de la anotación (por ejemplo, el máximo de caracteres), para los mensajes
            generales que lo citan.
    """
    if validacion in atr.get('mensajes', {}):
        return reemplazar(atr['mensajes'][validacion]['es'], atr.get('validaciones', {}))
    clave, parametro = GENERAL_DE[validacion]
    texto = _GENERALES[clave][0]
    return reemplazar(texto, {parametro: valor}) if parametro else texto


def clave_unico(entidad, columnas):
    """Clave del mensaje de un dato repetido: la del JSON (``unico`` o ``unicos``) o la general."""
    nombre = '_'.join(columnas)
    if len(columnas) == 1:
        atr = atributo(entidad, columnas[0])
        if atr is not None and 'unico' in atr.get('mensajes', {}):
            return f"{entidad['entidad']}.{nombre}.unico"
    for u in entidad.get('unicos', []):
        if u['atributos'] == list(columnas) and 'mensaje' in u:
            return f"{entidad['entidad']}.{nombre}.unico"
    return 'validacion.unico'


def mensaje_unico(entidad, columnas):
    """Mensaje en español de un dato repetido."""
    nombre = '_'.join(columnas)
    clave = clave_unico(entidad, columnas)
    if clave == 'validacion.unico':
        return _GENERALES[clave][0]
    if len(columnas) == 1:
        atr = atributo(entidad, columnas[0])
        if atr is not None and 'unico' in atr.get('mensajes', {}):
            return atr['mensajes']['unico']['es']
    return next(u['mensaje']['es'] for u in entidad.get('unicos', [])
                if '_'.join(u['atributos']) == nombre and 'mensaje' in u)


def mensaje_filas_completas(entidad, idioma):
    """Mensaje de una edición en bloque que no trae una fila por cada valor de su lista, en ``es`` o ``en``.

    Por ejemplo: «Envía una fila por cada categoría, sin repetir: Premium o Especial, Estándar, Corriente y
    Fuera de norma.».
    """
    clave = clave_en_bloque(entidad)
    etiqueta = en_frase(clave['etiquetas'][idioma])
    valores = [v['etiquetas'][idioma] for v in clave['valores']]
    if idioma == 'es':
        return f'Envía una fila por cada {etiqueta}, sin repetir: {_unir(valores, "y")}.'
    return f'Send one row for each {etiqueta}, with no repeats: {_unir(valores, "and")}.'


def _unir(valores, conjuncion):
    """``[a, b, c]`` → ``a, b y c``."""
    if len(valores) == 1:
        return valores[0]
    return f"{', '.join(valores[:-1])} {conjuncion} {valores[-1]}"


def textos(entidades):
    """Todos los textos, por secciones: primero los mensajes generales y luego cada entidad.

    Returns:
        list: ``(título, [(clave, español, inglés), ...])``.
    """
    secciones = [('Mensajes generales de las validaciones', list(MENSAJES_GENERALES))]
    for nombre, e in entidades.items():
        secciones.append((nombre, _textos_entidad(e)))
    return secciones


def _textos_entidad(e):
    n = e['entidad']
    filas = [(f'{n}.etiqueta.singular', e['etiquetas']['es']['singular'], e['etiquetas']['en']['singular']),
             (f'{n}.etiqueta.plural', e['etiquetas']['es']['plural'], e['etiquetas']['en']['plural'])]
    for a in e['atributos']:
        base = f"{n}.{a['nombre']}"
        filas.append((f'{base}.etiqueta', a['etiquetas']['es'], a['etiquetas']['en']))
        if 'ayuda' in a:
            filas.append((f'{base}.ayuda', a['ayuda']['es'], a['ayuda']['en']))
        for v in a.get('valores', []):
            filas.append((f"{base}.{v['codigo']}", v['etiquetas']['es'], v['etiquetas']['en']))
        validaciones = a.get('validaciones', {})
        for clave, mensaje in a.get('mensajes', {}).items():
            filas.append((f'{base}.{clave}', reemplazar(mensaje['es'], validaciones),
                          reemplazar(mensaje['en'], validaciones)))
    for u in e.get('unicos', []):
        if 'mensaje' in u:
            filas.append((f"{n}.{'_'.join(u['atributos'])}.unico", u['mensaje']['es'], u['mensaje']['en']))
    if 'crear_con' in e:
        cc = e['crear_con']
        filas.append((f"{n}.{cc['nombre']}.etiqueta", cc['etiquetas']['es'], cc['etiquetas']['en']))
    if e.get('edicion_en_bloque') and clave_en_bloque(e) is not None:
        filas.append((f'{n}.filas_completas', mensaje_filas_completas(e, 'es'), mensaje_filas_completas(e, 'en')))
    for r in e.get('reglas', []):
        if 'mensaje' in r:
            parametros = r.get('parametros', {})
            filas.append((f"{n}.reglas.{r['codigo']}", reemplazar(r['mensaje']['es'], parametros),
                          reemplazar(r['mensaje']['en'], parametros)))
    return filas
