# Nombre: campos.py
# Descripción: Las validaciones de un campo de la API y del formulario en un solo modelo, del que salen Bean
#              Validation y Yup, con sus casos de límite: los valores que rechaza cada validación y los del
#              borde que acepta (RP-23).
# Fecha de creación: 26/10/2026
# Actualización: 26/10/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Las validaciones de cada campo, en un solo modelo.

De aquí salen las anotaciones de Bean Validation (``java.py``) y las validaciones de Yup (``frontend.py``), en el
mismo orden y con la misma clave de mensaje: así el backend y el frontend validan igual. También salen los casos
de límite que revisan las pruebas generadas de los dos lados. Son datos, y cada lenguaje los escribe a su manera:
el dato que falta, por ejemplo, es ``null`` en la API y el campo vacío en el formulario.

El formato del correo es la excepción: lo valida solo el backend, como el dígito del NIT, y su error llega al
campo del formulario al guardar.
"""

from dataclasses import dataclass, field
from decimal import Decimal

from inspectia_generador.textos import GENERAL_DE, clave_mensaje, mensaje_esperado


@dataclass(frozen=True)
class Validacion:
    """Una validación de un campo, en el orden en que se revisa.

    Attributes:
        tipo: ``obligatorio``, ``no_en_blanco``, ``valor_permitido``, ``valores``, ``longitud_max``,
            ``longitud_min``, ``bytes_max``, ``patron``, ``correo``, ``minimo``, ``maximo`` o ``decimales``.
        valor: el límite, el patrón o los códigos que se pueden asignar.
        clave: la clave del mensaje: la propia del atributo, si el JSON la trae, o la general.
        parametro: el marcador del mensaje general que lleva el límite (``max``, ``min`` o ``value``), o
            ``None`` si el mensaje ya lo trae escrito.
        solo_backend: si solo la revisa el backend, como el formato del correo.
    """

    tipo: str
    valor: object = None
    clave: str = None
    parametro: str = None
    solo_backend: bool = False


@dataclass(frozen=True)
class Valor:
    """Un valor de un caso de límite, que cada lenguaje escribe a su manera.

    Attributes:
        tipo: ``falta`` (el dato no llega), ``repetir`` (``texto`` repetido ``veces``), ``texto``, ``numero``
            (``texto`` es el número tal cual, como 101 o 0.05) o ``correo_de_largo`` (un correo válido de
            ``veces`` caracteres).
    """

    tipo: str
    texto: str = ''
    veces: int = 0


#: El dato que no llega: null en la API, el campo vacío en el formulario.
FALTA = Valor('falta')


@dataclass(frozen=True)
class Caso:
    """Un valor que una validación rechaza, con el mensaje en español que responde la API."""

    valor: Valor
    mensaje: str
    solo_backend: bool = False


@dataclass
class Campo:
    """Las validaciones de un campo, cómo se normaliza antes de validarlo y sus casos de límite."""

    validaciones: list = field(default_factory=list)
    casos: list = field(default_factory=list)
    bordes: list = field(default_factory=list)
    recortar: bool = False
    minusculas: bool = False


def _validacion(e, a, tipo, nombre, valor=None, propio=True):
    """Una validación con la clave de su mensaje y, si el mensaje es el general, el marcador de su límite.

    Args:
        nombre: el nombre de la validación en el JSON (``longitud_max``, ``min``…), que da la clave del mensaje.
        propio: si el atributo puede traer su propio mensaje para esta validación.
    """
    clave = clave_mensaje(e, a, nombre) if propio else GENERAL_DE[nombre][0]
    general, parametro = GENERAL_DE[nombre]
    return Validacion(tipo, valor, clave, parametro if clave == general else None)


def del_atributo(e, a, obligatorio=None):
    """Las validaciones de un atributo como campo de una solicitud o de un formulario, y sus casos de límite.

    Args:
        e: entidad dueña del atributo: sus mensajes son los que responde la API.
        obligatorio: si es obligatorio, cuando no lo dice ``obligatorio`` del atributo, como una contraseña
            que al editar es opcional.

    Returns:
        Campo: sus validaciones, en el orden de Bean Validation, y sus casos de límite.
    """
    v = a.get('validaciones', {})
    requerido = (a.get('obligatorio') and 'por_defecto' not in a) if obligatorio is None else obligatorio
    sin_formato = 'patron' not in v and 'formato' not in v
    campo = Campo(recortar=bool(v.get('recortar')), minusculas=bool(v.get('minusculas')))

    if requerido:
        campo.validaciones.append(Validacion('obligatorio', a['tipo'] in ('texto', 'contrasena'),
                                             'validacion.obligatorio'))
        campo.casos.append(Caso(FALTA, mensaje_esperado(e, a, 'obligatorio')))
    elif a['tipo'] == 'contrasena':
        # Opcional, como al editar: si no llega, queda la que había; si llega en blanco, se rechaza con
        # el mismo mensaje que al crear, porque tampoco serviría para iniciar sesión.
        campo.validaciones.append(Validacion('no_en_blanco', clave='validacion.obligatorio'))
    if a['tipo'] == 'contrasena':
        # Solo espacios y con el largo mínimo: falla solo por estar en blanco (prueba de límite).
        campo.casos.append(Caso(Valor('repetir', ' ', v.get('longitud_min', 1)), mensaje_esperado(e, a, 'obligatorio')))
    if 'valores_en_formulario' in a:
        # Un valor que existe pero no se puede asignar responde 403, no 400: va en la prueba de permisos.
        campo.validaciones.append(_validacion(e, a, 'valor_permitido', 'valores_en_formulario',
                                              list(a['valores_en_formulario'])))
    elif a['tipo'] == 'lista' and 'valores' in a:
        # Un código que no existe lo rechaza el backend al leer la petición, con «El valor no es válido.».
        campo.validaciones.append(Validacion('valores', [x['codigo'] for x in a['valores']], 'error.valor_invalido'))
    if a['tipo'] == 'texto' or 'longitud_max' in v:
        limites = ([(a['longitud'], False)] if a['tipo'] == 'texto' else []) + (
            [(v['longitud_max'], True)] if 'longitud_max' in v else [])
        maximo, propio = min(limites)
        campo.validaciones.append(_validacion(e, a, 'longitud_max', 'longitud_max', maximo, propio))
        mensaje = mensaje_esperado(e, a if propio else {}, 'longitud_max', maximo)
        if sin_formato:
            campo.casos.append(Caso(Valor('repetir', 'x', maximo + 1), mensaje))
            campo.bordes.append(Valor('repetir', 'x', maximo))
        elif v.get('formato') == 'correo':
            campo.casos.append(Caso(Valor('correo_de_largo', veces=maximo + 1), mensaje))
            campo.bordes.append(Valor('correo_de_largo', veces=maximo))
    if 'longitud_min' in v:
        minimo = v['longitud_min']
        campo.validaciones.append(_validacion(e, a, 'longitud_min', 'longitud_min', minimo))
        if minimo > 1 and sin_formato:
            campo.casos.append(Caso(Valor('repetir', 'x', minimo - 1), mensaje_esperado(e, a, 'longitud_min', minimo)))
            campo.bordes.append(Valor('repetir', 'x', minimo))
    if 'bytes_max' in v:
        bytes_max = v['bytes_max']
        campo.validaciones.append(_validacion(e, a, 'bytes_max', 'bytes_max', bytes_max))
        # Cada eñe ocupa 2 bytes: con la mitad más una se pasa por poco, y con la mitad justa cabe.
        caracteres = bytes_max // 2 + 1
        maximo = v.get('longitud_max', a.get('longitud', caracteres))
        if sin_formato and v.get('longitud_min', 0) <= caracteres <= maximo:
            campo.casos.append(Caso(Valor('repetir', 'ñ', caracteres), mensaje_esperado(e, a, 'bytes_max')))
            campo.bordes.append(Valor('repetir', 'ñ', bytes_max // 2))
    if 'patron' in v:
        campo.validaciones.append(_validacion(e, a, 'patron', 'patron', v['patron']))
        campo.casos.append(Caso(Valor('texto', '?'), mensaje_esperado(e, a, 'patron')))
    if v.get('formato') == 'correo':
        campo.validaciones.append(Validacion('correo', None, clave_mensaje(e, a, 'formato'), solo_backend=True))
        campo.casos.append(Caso(Valor('texto', 'sin-arroba'), mensaje_esperado(e, a, 'formato'), solo_backend=True))
    for limite, tipo in (('min', 'minimo'), ('max', 'maximo')):
        if limite in v and a['tipo'] in ('entero', 'decimal'):
            valor = v[limite]
            campo.validaciones.append(_validacion(e, a, tipo, limite, valor))
            if a['tipo'] == 'entero':
                fuera = int(valor) - 1 if limite == 'min' else int(valor) + 1
            else:
                fuera = valor - 1 if limite == 'min' else valor + 1
            campo.casos.append(Caso(Valor('numero', _numero(fuera)), mensaje_esperado(e, a, limite, valor)))
            campo.bordes.append(Valor('numero', _numero(valor)))
    if a['tipo'] == 'decimal':
        # Los decimales de su escala, sin contar los ceros del final: con escala 1, 90.50 sirve y 90.55 no.
        escala = a['escala']
        nombre = 'decimales_uno' if escala == 1 else 'decimales'
        campo.validaciones.append(_validacion(e, a, 'decimales', nombre, escala))
        campo.casos.append(Caso(Valor('numero', _con_un_decimal_de_mas(a)), mensaje_esperado(e, a, nombre, escala)))
    return campo


def _numero(valor):
    """Un número como se escribe en JSON: ``0``, ``100`` o ``0.5``."""
    return str(valor)


def _con_un_decimal_de_mas(a):
    """Un número dentro de ``min`` y ``max`` con un decimal más de los que permite su escala, como 0.05 con
    escala 1: para la prueba de ``@MaximoDecimales``."""
    v = a.get('validaciones', {})
    base = Decimal(str(v['min'])) if 'min' in v else Decimal(str(v['max'])) - 1 if 'max' in v else Decimal(0)
    return str(base + Decimal(5).scaleb(-(a['escala'] + 1)))
