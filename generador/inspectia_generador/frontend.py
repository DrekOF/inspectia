# Nombre: frontend.py
# Descripción: Arma los datos de las plantillas del frontend: la pantalla de cada entidad de «pantallas» en
#              alcance.json, con su cliente de la API, sus esquemas de Yup, sus textos y sus pruebas (RP-23),
#              también la de una entidad que se edita en bloque, como los rangos (RF-13).
# Fecha de creación: 26/10/2026
# Actualización: 27/10/2026
# Autor: Equipo InspectIA
# Versión: 1.2
"""Datos de las plantillas del frontend (React, Material UI, Formik y Yup).

- ``pantallas.js``: la ruta, la entrada del menú y los roles de cada pantalla, que son los de ``listar``.
- ``textos.js``: los textos de ``pantalla.textos`` de cada JSON, en español y en inglés.
- De cada entidad de ``pantallas``: el cliente de su API, sus esquemas de Yup, su pantalla y sus pruebas. Las
  validaciones salen de ``campos.del_atributo``, el mismo modelo de Bean Validation, y la prueba de Yup revisa
  los mismos casos de límite que la prueba generada de la API. También revisa los valores de una lista que existen
  pero el formulario no ofrece, como ADMIN_PLATAFORMA en el rol, con el mensaje del 403 de la API, que en la prueba
  de la API van en la de permisos.
- Una entidad con ``edicion_en_bloque``, como los rangos (RF-13), tiene otra pantalla: una fila por cada valor de la
  lista que identifica las filas, en su orden, todas en un solo formulario, que se guarda junto y puede volver a
  los valores por defecto. Sus campos pueden ser textos o decimales; un decimal se escribe como texto, con coma o
  con punto, y Yup lo lee como número.

Lo que el JSON no puede declarar se escribe a mano: las piezas comunes de ``frontend/src/crud`` y las extensiones
de cada pantalla, que le pasa ``Rutas.jsx``. Las pantallas todavía no soportan todo el formato: si una entidad de
``pantallas`` usa algo que falta, la generación se detiene y dice qué.
"""

import json
from decimal import Decimal

from inspectia_generador import campos
from inspectia_generador.encabezado import datos_header
from inspectia_generador.java import consultas_unicas, ejemplo
from inspectia_generador.modelo import (atributo, clave_en_bloque, contrasena_opcional_al_editar, regla,
                                        reglas_de_la_fila_propia, valores_lista)
from inspectia_generador.nombres import camel, en_frase, patron_js, texto_js
from inspectia_generador.sql import ErrorGeneracion
from inspectia_generador.textos import clave_mensaje, mensaje_esperado, mensaje_filas_completas, reemplazar

CARPETA = 'frontend/src/generated'
CARPETA_E2E = 'frontend/e2e/generated'

#: Tipos que la pantalla sabe poner en el formulario y en la lista; los demás llegan cuando una ficha los pida.
TIPOS_EN_FORMULARIO = ('texto', 'contrasena', 'lista')
TIPOS_EN_LISTA = ('texto', 'lista')
#: Tipos que una pantalla en bloque sabe poner en cada fila (RF-13).
TIPOS_EN_BLOQUE = ('texto', 'decimal')
#: El control del formulario de cada tipo.
COMPONENTES = {'texto': 'CampoDeTexto', 'contrasena': 'CampoContrasena', 'lista': 'CampoDeLista'}
#: Ícono del menú de una pantalla cuyo JSON no dice cuál usar.
ICONO_POR_DEFECTO = 'registros'
#: Los textos de pantalla.textos que usa la pantalla, con su nombre en JavaScript; el resumen y la confirmación
#: de desactivar van aparte.
TEXTOS_JS = (('descripcion', 'descripcion'), ('vacia', 'vacia'), ('nuevo', 'nuevo'), ('crear', 'crear'),
             ('tituloEditar', 'titulo_editar'), ('notaFormulario', 'nota_formulario'),
             ('notaContrasenaDeOtro', 'nota_contrasena_de_otro'),
             ('tituloLista', 'titulo_lista'), ('activo', 'activo'), ('inactivo', 'inactivo'),
             ('editarFila', 'editar_fila'), ('desactivarFila', 'desactivar_fila'),
             ('reactivarFila', 'reactivar_fila'), ('alGuardar', 'al_guardar'), ('alDesactivar', 'al_desactivar'),
             ('alReactivar', 'al_reactivar'), ('notaLista', 'nota_lista'))
#: Los textos de una pantalla en bloque, con su nombre en JavaScript.
TEXTOS_EN_BLOQUE_JS = (('descripcion', 'descripcion'), ('tituloEditar', 'titulo_editar'),
                       ('notaFormulario', 'nota_formulario'), ('alGuardar', 'al_guardar'),
                       ('notaRestaurar', 'nota_restaurar'),
                       ('confirmarRestaurarDetalle', 'confirmar_restaurar_detalle'), ('alRestaurar', 'al_restaurar'))
#: El texto que escribe la prueba generada de una pantalla en bloque en el campo de texto de una fila.
TEXTO_DE_PRUEBA = 'Texto propio de la empresa.'
#: Lo que cuenta el resumen: cada parte que usa su frase, como {activos}, tiene su texto contado (resumen_activos).
PARTES_DEL_RESUMEN = ('total', 'activos', 'inactivos')
#: Los límites de un campo que exportan los esquemas generados, con los nombres que usa el frontend.
LIMITES_JS = (('longitud_min', 'minimo'), ('longitud_max', 'maximo'), ('bytes_max', 'maximoBytes'))
#: Identificadores de los dos registros de las pruebas generadas, como los UUID v7 de la API.
IDS_DE_PRUEBA = ('0199e2a0-0000-7000-8000-0000000000a1', '0199e2a0-0000-7000-8000-0000000000a2')


# --- Valores de JavaScript ------------------------------------------------------------------------


def valor_js(valor):
    """Un valor de un caso de límite (``campos.Valor``) como expresión de JavaScript para la prueba generada."""
    if valor.tipo == 'falta':
        return "''"
    if valor.tipo == 'repetir':
        return f'{texto_js(valor.texto)}.repeat({valor.veces})'
    if valor.tipo == 'texto':
        return texto_js(valor.texto)
    if valor.tipo == 'numero':
        # Como se escribe en el formulario en español: 100.55 es '100,55'.
        return texto_js(valor.texto.replace('.', ','))
    return f'correoDeLargo({valor.veces})'


def literal_js(valor):
    """Un valor de Python como literal de JavaScript: texto, número, booleano, null, lista u objeto en una línea."""
    if valor is None:
        return 'null'
    if isinstance(valor, bool):
        return 'true' if valor else 'false'
    if isinstance(valor, (int, float)):
        return str(valor)
    if isinstance(valor, (list, tuple)):
        return '[' + ', '.join(literal_js(v) for v in valor) + ']'
    if isinstance(valor, dict):
        return '{ ' + ', '.join(f'{clave}: {literal_js(v)}' for clave, v in valor.items()) + ' }' if valor else '{}'
    return texto_js(valor)


# --- Lo que necesita la pantalla ------------------------------------------------------------------


def nombre_de_campo(a):
    """El nombre de un campo en la API y en el formulario: el atributo en camelCase o, si es una contraseña, su
    entrada (``contrasena_hash`` → ``contrasena``)."""
    return camel(a['entrada']['nombre'] if a['tipo'] == 'contrasena' else a['nombre'])


def _formulario(entidades, e):
    """Los campos del formulario: ``(entidad dueña, atributo, nombre del campo, grupo)``. Primero los de la
    entidad, en el orden del JSON, y después los de ``crear_con``, dentro de su grupo, como ``administrador``."""
    campos_del_formulario = [(e, a, nombre_de_campo(a), None) for a in e['atributos'] if a.get('en_formulario')]
    if 'crear_con' in e:
        cc = e['crear_con']
        otra = entidades[cc['entidad']]
        grupo = camel(cc['nombre'])
        for nombre in cc['atributos']:
            a = atributo(otra, nombre)
            campos_del_formulario.append((otra, a, f'{grupo}.{nombre_de_campo(a)}', grupo))
    return campos_del_formulario


def textos_obligatorios(e):
    """Los textos de ``pantalla.textos`` que necesita la pantalla, con lo que los pide: cada operación pide los
    suyos; el resumen, los textos contados que usa su frase; la confirmación de desactivar, si está, va completa; la
    fila propia pide su marca, y la contraseña opcional al editar, sus textos y la nota de D-30. Una pantalla en
    bloque no tiene lista ni registros sueltos: editar pide el nombre de su formulario y el mensaje al guardar, y
    restaurar, el detalle de su confirmación y su mensaje.

    Returns:
        list: ``(texto, quién lo pide)``.
    """
    operaciones = e.get('operaciones', {})
    textos = e.get('pantalla', {}).get('textos', {})
    pedidos = []
    if e.get('edicion_en_bloque'):
        if 'editar' in operaciones:
            pedidos += [('titulo_editar', 'editar'), ('al_guardar', 'editar')]
        if 'restaurar_por_defecto' in operaciones:
            pedidos += [('confirmar_restaurar_detalle', 'restaurar_por_defecto'),
                        ('al_restaurar', 'restaurar_por_defecto')]
        return pedidos
    if 'listar' in operaciones:
        pedidos += [('vacia', 'listar'), ('titulo_lista', 'listar')]
    if 'crear' in operaciones:
        pedidos += [('nuevo', 'crear'), ('crear', 'crear')]
    if 'editar' in operaciones:
        pedidos += [('titulo_editar', 'editar'), ('editar_fila', 'editar')]
    if 'crear' in operaciones or 'editar' in operaciones:
        pedidos.append(('al_guardar', 'crear o editar'))
    if 'desactivar' in operaciones:
        pedidos += [('desactivar_fila', 'desactivar'), ('al_desactivar', 'desactivar')]
    if 'reactivar' in operaciones:
        pedidos += [('reactivar_fila', 'reactivar'), ('al_reactivar', 'reactivar')]
    if 'atributo_activo' in e:
        pedidos += [('activo', 'atributo_activo'), ('inactivo', 'atributo_activo')]
    if any(clave.startswith('resumen') for clave in textos):
        pedidos.append(('resumen', 'el resumen'))
        frase = json.dumps(textos.get('resumen', {}), ensure_ascii=False)
        pedidos += [(f'resumen_{parte}', 'el resumen') for parte in PARTES_DEL_RESUMEN if f'{{{parte}}}' in frase]
    if 'confirmar_desactivar' in textos or 'confirmar_desactivar_detalle' in textos:
        pedidos += [('confirmar_desactivar', 'la confirmación de desactivar'),
                    ('confirmar_desactivar_detalle', 'la confirmación de desactivar')]
    if reglas_de_la_fila_propia(e):
        pedidos.append(('fila_propia', 'en_la_fila_propia'))
    if contrasena_opcional_al_editar(e):
        pedidos += [(clave, 'la contraseña opcional al editar')
                    for clave in ('contrasena_al_editar', 'ayuda_contrasena_al_editar', 'nota_contrasena_de_otro')]
    return pedidos


def verificar_pantalla(entidades, e):
    """Revisa que la pantalla de la entidad se pueda generar con lo que soportan las plantillas.

    Raises:
        ErrorGeneracion: con todo lo que falta, para resolverlo de una vez.
    """
    problemas = []
    operaciones = e.get('operaciones', {})
    en_bloque = bool(e.get('edicion_en_bloque'))
    if 'listar' not in operaciones:
        problemas.append('una pantalla necesita la operación listar')
    if 'pantalla' not in e:
        problemas.append('falta la clave pantalla, con sus textos')
    textos = e.get('pantalla', {}).get('textos', {})
    for clave, quien in textos_obligatorios(e):
        if clave not in textos:
            problemas.append(f'falta pantalla.textos.{clave}, que pide {quien}')
    for duena, a, campo, grupo in _formulario(entidades, e):
        donde = campo if grupo else a['nombre']
        if a['tipo'] not in (TIPOS_EN_BLOQUE if en_bloque else TIPOS_EN_FORMULARIO):
            problemas.append(f"{donde}, de tipo {a['tipo']}, en el formulario")
    for a in [] if en_bloque else e['atributos']:
        if a.get('en_lista') and a['nombre'] != e.get('atributo_activo') and a['tipo'] not in TIPOS_EN_LISTA:
            problemas.append(f"{a['nombre']}, de tipo {a['tipo']}, en la lista")
    if problemas:
        raise ErrorGeneracion(f"No se puede generar la pantalla de {e['entidad']}: " + '; '.join(problemas))


# --- Validaciones de Yup --------------------------------------------------------------------------


def _mensaje_js(validacion):
    """El mensaje de una validación: la misma clave de Bean Validation y, si es el general, su límite."""
    if validacion.parametro:
        return f"t('{validacion.clave}', {{ {validacion.parametro}: {literal_js(validacion.valor)} }})"
    return f"t('{validacion.clave}')"


def lineas_yup(datos, numero=False):
    """Las validaciones de Yup de un campo, una por línea, en el orden en que la API escoge el error de un campo
    (``campos.orden_de_la_api``): Yup dice el error de la primera que falla, así un valor que falla por dos da el
    mismo mensaje en el formulario y en la API.

    Un número, como un decimal, se escribe como texto: primero se lee, con coma o con punto, y el vacío es el dato
    que falta. Lo que no es un número da su propio error, que la API no tiene, porque recibe números de JSON.

    Primero se normaliza el texto, como lo hace la solicitud de la API antes de validar. Un campo opcional, como la
    contraseña al editar, vacío no se revisa: no llega a la API y queda el valor que había. En un campo obligatorio
    con valores, como el rol, el vacío también cuenta como ausente: Yup revisa oneOf antes que las demás pruebas, y
    así el error es el de obligatorio, como en la API. El formato del correo no va: lo valida solo el backend, y su
    error llega al campo al guardar.

    Args:
        datos: el campo, de ``campos.del_atributo``.
        numero: si el campo es un número, como un decimal.
    """
    lineas = ['.transform(numeroDecimal)', ".typeError(t('validacion.numero'))"] if numero else []
    if datos.recortar:
        lineas.append('.transform(recortar)')
    if datos.minusculas:
        lineas.append('.transform(minusculas)')
    tipos = {v.tipo for v in datos.validaciones if not v.solo_backend}
    if 'no_en_blanco' in tipos or ('obligatorio' in tipos and tipos & {'valor_permitido', 'valores'}):
        lineas.append('.transform(vacioComoAusente)')
    for v in sorted(datos.validaciones, key=campos.orden_de_la_api):
        if v.solo_backend:
            continue
        mensaje = _mensaje_js(v)
        if v.tipo == 'obligatorio':
            lineas.append(f".test('obligatorio', {mensaje}, lleno)")
        elif v.tipo == 'no_en_blanco':
            lineas.append(f".test('obligatorio', {mensaje}, noEnBlanco)")
        elif v.tipo in ('valor_permitido', 'valores'):
            lineas.append(f'.oneOf({literal_js(v.valor)}, {mensaje})')
        elif v.tipo == 'longitud_max':
            lineas.append(f'.max({v.valor}, {mensaje})')
        elif v.tipo == 'longitud_min':
            lineas.append(f'.min({v.valor}, {mensaje})')
        elif v.tipo == 'bytes_max':
            lineas.append(f".test('bytes_max', {mensaje}, cabeEnBytes({v.valor}))")
        elif v.tipo == 'patron':
            lineas.append(f'.matches({patron_js(v.valor)}, {mensaje})')
        elif v.tipo == 'maximo':
            lineas.append(f'.max({v.valor}, {mensaje})')
        elif v.tipo == 'minimo':
            lineas.append(f'.min({v.valor}, {mensaje})')
        elif v.tipo == 'decimales':
            lineas.append(f".test('decimales', {mensaje}, maximoDecimales({v.valor}))")
        else:
            raise ErrorGeneracion(f'La validación {v.tipo} todavía no tiene su prueba de Yup')
    return lineas


# --- Campos, columnas y textos --------------------------------------------------------------------


def _prefijo_de_valores(entidades, e, a):
    """La clave de los textos de los valores de una lista, como ``producto.estado`` (``producto.estado.ACTIVO``);
    con ``valores_de``, la de la lista original."""
    if 'valores' in a:
        return f"{e['entidad']}.{a['nombre']}"
    nombre_entidad, _, nombre_atributo = a['valores_de'].partition('.')
    return _prefijo_de_valores(entidades, entidades[nombre_entidad], atributo(entidades[nombre_entidad], nombre_atributo))


def _campo(entidades, e, duena, a, campo, grupo, al_editar=False):
    """Un campo del formulario: su control, sus textos, su valor inicial, sus validaciones de Yup y sus casos de
    límite, del mismo modelo que la solicitud de la API."""
    obligatorio = None
    if a['tipo'] == 'contrasena':
        entrada = a['entrada']
        obligatorio = entrada['obligatoria_al_editar'] if al_editar else entrada['obligatoria_al_crear']
    datos = campos.del_atributo(duena, a, obligatorio)
    textos = e['pantalla'].get('textos', {})
    base = f"{duena['entidad']}.{a['nombre']}"
    etiqueta, etiqueta_es = f'{base}.etiqueta', a['etiquetas']['es']
    ayuda = f'{base}.ayuda' if 'ayuda' in a else None
    if a['tipo'] == 'contrasena':
        # Sus textos al registrar y al editar, como «Contraseña inicial» y «Contraseña nueva».
        momento = 'editar' if al_editar else 'crear'
        if f'contrasena_al_{momento}' in textos:
            etiqueta = f"{e['entidad']}.pantalla.contrasena_al_{momento}"
            etiqueta_es = textos[f'contrasena_al_{momento}']['es']
        if f'ayuda_contrasena_al_{momento}' in textos:
            ayuda = f"{e['entidad']}.pantalla.ayuda_contrasena_al_{momento}"
    opciones = []
    no_ofrecidos = []
    if a['tipo'] == 'lista':
        prefijo = _prefijo_de_valores(entidades, duena, a)
        codigos = a.get('valores_en_formulario') or valores_lista(entidades, a)
        opciones = [(codigo, f'{prefijo}.{codigo}') for codigo in codigos]
        # Los valores que existen pero el formulario no ofrece, como ADMIN_PLATAFORMA en el rol: Yup los rechaza con
        # el mensaje del 403 de la API. No están en datos.casos porque la prueba de la API los revisa en la de
        # permisos (no_permitidos de java.py), no en la de validaciones.
        mensaje = mensaje_esperado(duena, a, 'valores_en_formulario')
        no_ofrecidos = [(texto_js(campo), texto_js(codigo), texto_js(mensaje))
                        for codigo in valores_lista(entidades, a) if codigo not in codigos]
    v = a.get('validaciones', {})
    # Al registrar, el valor de pantalla.inicial, como el rol de un usuario nuevo; si no, el por defecto de una lista.
    iniciales = e['pantalla'].get('inicial', {}) if grupo is None and not al_editar else {}
    if a['nombre'] in iniciales:
        inicial = iniciales[a['nombre']]
    else:
        inicial = a['por_defecto'] if a['tipo'] == 'lista' and 'por_defecto' in a else ''
    return {
        'campo': campo, 'clave': campo.split('.')[-1], 'grupo': grupo, 'tipo': a['tipo'],
        'componente': COMPONENTES[a['tipo']], 'etiqueta': etiqueta, 'etiqueta_es': etiqueta_es, 'ayuda': ayuda,
        'tipo_html': 'email' if v.get('formato') == 'correo' else None,
        'autocompletar': 'new-password' if a['tipo'] == 'contrasena' else 'off',
        'opciones': opciones, 'inicial': inicial, 'inicial_js': literal_js(inicial), 'por_defecto': a.get('por_defecto'),
        'yup': lineas_yup(datos),
        'casos': [(texto_js(campo), valor_js(c.valor), texto_js(c.mensaje)) for c in datos.casos if not c.solo_backend]
                 + no_ofrecidos,
        'bordes': [(texto_js(campo), valor_js(valor)) for valor in datos.bordes],
        'solo_backend': [(campo, c.mensaje) for c in datos.casos if c.solo_backend],
        'usa_correo_largo': any(valor.tipo == 'correo_de_largo' for valor in datos.bordes)
                            or any(c.valor.tipo == 'correo_de_largo' and not c.solo_backend for c in datos.casos),
        'ejemplo': ejemplo(a),
        'nombre': a['nombre'],
        'limites': _limites(datos),
        'opcional': a['tipo'] == 'contrasena' and not obligatorio,
        'varias_lineas': bool(a.get('varias_lineas')),
    }


def _limites(datos):
    """Los límites de un campo, como ``{ minimo: 8, maximo: 64, maximoBytes: 72 }``: los exportan los esquemas
    generados para las páginas escritas a mano que validan lo mismo, como Cambiar contraseña (RF-18)."""
    valores = {v.tipo: v.valor for v in datos.validaciones}
    return {js: valores[tipo] for tipo, js in LIMITES_JS if tipo in valores}


def _limite(e):
    """El límite de activos de la pantalla (``limite_de_activos``): el máximo de su regla y la clave de su mensaje, o
    ``None``."""
    codigo = e.get('pantalla', {}).get('limite_de_activos')
    if codigo is None:
        return None
    r = regla(e, codigo)
    return {'maximo': r['parametros']['maximo'], 'mensaje': f"{e['entidad']}.reglas.{r['codigo']}"}


def _de_la_respuesta(e):
    """Los atributos que responde la API: los de la lista o del formulario, nunca una contraseña."""
    return [a for a in e['atributos'] if a['tipo'] != 'contrasena' and (a.get('en_lista') or a.get('en_formulario'))]


def _otro_valor(a, valor):
    """Otro valor para el segundo registro de las pruebas: cada dígito pasa al siguiente o, si no tiene dígitos,
    se le agrega « 2». Solo se muestra: no tiene que cumplir las validaciones."""
    if isinstance(valor, bool) or not isinstance(valor, str) or a['tipo'] == 'lista':
        return valor
    otro = ''.join(str((int(c) + 1) % 10) if c.isdigit() else c for c in valor)
    return otro if otro != valor else (valor + ' 2')[:a.get('longitud', len(valor) + 2)]


def _registro(e, otro=False):
    """Un registro como lo responde la API, con los ejemplos del JSON; el otro, con valores distintos y, si la
    entidad se desactiva, desactivado."""
    registro = {'id': IDS_DE_PRUEBA[1 if otro else 0]}
    for a in _de_la_respuesta(e):
        valor = ejemplo(a)
        registro[camel(a['nombre'])] = _otro_valor(a, valor) if otro else valor
    if 'atributo_activo' in e:
        registro[camel(e['atributo_activo'])] = not otro
    return registro


def _columnas(entidades, e):
    """Las columnas de la lista: los atributos de la lista, salvo el activo, que va en la columna Estado, y el
    que va debajo del nombre en la primera columna."""
    n, pantalla = e['entidad'], e['pantalla']
    textos = pantalla.get('textos', {})
    debajo = pantalla.get('debajo_del_nombre')
    de_la_lista = [a for a in e['atributos']
                   if a.get('en_lista') and a['nombre'] not in (e.get('atributo_activo'), debajo)]
    columnas = []
    for posicion, a in enumerate(de_la_lista):
        columna = {'atributo': camel(a['nombre']), 'etiqueta': f"{n}.{a['nombre']}.etiqueta"}
        if posicion == 0 and 'columna_nombre' in textos:
            columna['etiqueta'] = f'{n}.pantalla.columna_nombre'
        if posicion == 0 and debajo:
            columna['debajo'] = {'atributo': camel(debajo),
                                 'texto': f'{n}.pantalla.debajo_del_nombre' if 'debajo_del_nombre' in textos else None}
        if a['tipo'] == 'lista':
            columna['valores'] = _prefijo_de_valores(entidades, e, a)
        columnas.append(columna)
    return columnas


def _con_valores(texto, registro):
    """Un texto de una fila con los valores del registro en vez de sus marcadores, como «Editar Neiva»."""
    return reemplazar(texto, {clave: valor for clave, valor in registro.items() if isinstance(valor, str)})


def _contar(textos, clave, cantidad, maximo=None):
    """Un texto que se cuenta, en español, en singular o en plural según la cantidad, con el máximo del límite."""
    forma = 'singular' if cantidad == 1 else 'plural'
    texto = textos[clave]['es'][forma].replace('{cantidad}', str(cantidad))
    return texto if maximo is None else texto.replace('{maximo}', str(maximo))


def _resumen_es(e, activos, inactivos):
    """El resumen en español, como lo arma la pantalla: «2 empresas registradas: 1 activa y 1 desactivada.» o «1 de
    25 usuarios activos, contándote a ti.»."""
    textos = e['pantalla']['textos']
    if 'resumen' not in textos:
        return None
    limite = _limite(e)
    maximo = limite['maximo'] if limite else None
    cuentas = {'total': activos + inactivos, 'activos': activos, 'inactivos': inactivos}
    frase = textos['resumen']['es']
    for parte in PARTES_DEL_RESUMEN:
        if f'{{{parte}}}' in frase:
            frase = frase.replace(f'{{{parte}}}', _contar(textos, f'resumen_{parte}', cuentas[parte], maximo))
    return frase if maximo is None else frase.replace('{maximo}', str(maximo))


def _textos(e):
    """Las claves de los textos de la pantalla, o null los que el JSON no trae."""
    n, textos = e['entidad'], e['pantalla'].get('textos', {})
    lista = [(js, literal_js(f'{n}.pantalla.{clave}' if clave in textos else None)) for js, clave in TEXTOS_JS]
    resumen = None
    if 'resumen' in textos:
        frase = json.dumps(textos['resumen'], ensure_ascii=False)
        resumen = {'frase': f'{n}.pantalla.resumen'} | {
            parte: f'{n}.pantalla.resumen_{parte}' for parte in PARTES_DEL_RESUMEN if f'{{{parte}}}' in frase}
    confirmar = None
    if 'confirmar_desactivar' in textos:
        confirmar = {'titulo': f'{n}.pantalla.confirmar_desactivar',
                     'detalle': f'{n}.pantalla.confirmar_desactivar_detalle'}
    return lista + [('resumen', literal_js(resumen))], literal_js(confirmar)


def _objeto(campos_del_formulario, valor):
    """El objeto de los campos del formulario, con los de ``crear_con`` dentro de su grupo, como literal de
    JavaScript en una línea: ``{ nombre: '', administrador: { correo: '' } }``.

    Args:
        valor: qué va en cada campo, a partir del campo (dict de ``_campo``).
    """
    objeto = {}
    for c in campos_del_formulario:
        destino = objeto.setdefault(c['grupo'], {}) if c['grupo'] else objeto
        destino[c['clave']] = valor(c)
    return objeto


# --- La pantalla de una entidad -------------------------------------------------------------------


def modelo_pantalla(entidades, e):
    """Todo lo que necesitan las plantillas de la pantalla de una entidad."""
    verificar_pantalla(entidades, e)
    n, operaciones, pantalla = e['entidad'], e['operaciones'], e['pantalla']
    textos = pantalla.get('textos', {})
    del_formulario = _formulario(entidades, e)
    # El formulario para registrar lleva también los campos de crear_con; el de editar, solo los de la entidad.
    crear = [_campo(entidades, e, duena, a, campo, grupo)
             for duena, a, campo, grupo in del_formulario] if 'crear' in operaciones else []
    editar = [_campo(entidades, e, duena, a, campo, grupo, al_editar=True)
              for duena, a, campo, grupo in del_formulario if grupo is None] if 'editar' in operaciones else []
    cc = e.get('crear_con')
    grupo = cc and {'nombre': camel(cc['nombre']), 'etiqueta': f"{n}.{cc['nombre']}.etiqueta",
                    'etiqueta_es': cc['etiquetas']['es']}
    textos_js, confirmar = _textos(e)
    columnas = _columnas(entidades, e)
    limite = _limite(e)
    # Lo que impiden en la fila de quien tiene la sesión sus reglas: las acciones, y los campos que al editarla no
    # se cambian, con la regla que los impide.
    propias = reglas_de_la_fila_propia(e)
    sin_acciones = [o for r in propias if 'atributo' not in r for o in r['cuando']]
    impedidas = {nombre_de_campo(atributo(e, r['atributo'])): r for r in propias if 'atributo' in r}
    fila_propia = {'marca': f'{n}.pantalla.fila_propia', 'sinAcciones': sin_acciones} if propias else None
    campos_formulario = _campos_del_formulario(
        crear, editar, {campo: f"{n}.reglas.{r['codigo']}" for campo, r in impedidas.items()})
    props_campos = [nombre for nombre, usa in (
        ('paraCrear', bool(cc) or any(f['modo'] == 'distinto' for f in campos_formulario)),
        ('propia', bool(impedidas))) if usa]
    opcionales = [c['clave'] for c in editar if c['opcional']]
    contrasena_al_editar = next((c['campo'] for c in editar if c['opcional'] and c['tipo'] == 'contrasena'), None)
    al_editar = {c['campo']: c for c in editar}
    nombres = [a['nombre'] for a in e['atributos'] if a.get('en_lista')]
    atributo_del_nombre = 'nombre' if 'nombre' in nombres else columnas[0]['atributo']
    acciones = {o: operaciones[o] for o in ('crear', 'editar', 'desactivar', 'reactivar') if o in operaciones}
    roles = [v['codigo'] for v in atributo(entidades['usuario'], 'rol')['valores']]
    rol = operaciones['listar'][0]
    registro, otro = _registro(e), _registro(e, otro=True)
    activo = e.get('atributo_activo')
    puede = {operacion: rol in quienes for operacion, quienes in acciones.items()}

    # Lo que revisan las pruebas generadas de la pantalla, en español. Lo que solo revisa el backend: las reglas de
    # un campo, como el dígito del NIT, y el formato del correo.
    reglas_del_campo = [(nombre_de_campo(atributo(e, r['atributo'])),
                         reemplazar(r['mensaje']['es'], r.get('parametros', {})))
                        for r in e.get('reglas', []) if r['tipo'] == 'validacion' and r.get('atributo')
                        and 'crear' in r.get('cuando', []) and atributo(e, r['atributo']).get('en_formulario')]
    solo_backend = reglas_del_campo + [caso for c in crear for caso in c['solo_backend']]
    unicos = [(c['campo'], c['mensaje']) for c in consultas_unicas(e)]
    if cc:
        otra = entidades[cc['entidad']]
        unicos += [(f"{camel(cc['nombre'])}.{c['campo']}", c['mensaje'])
                   for c in consultas_unicas(otra) if c['atributo'] in cc['atributos']]
    grupos = (e['etiquetas']['es']['singular'], cc['etiquetas']['es']) if cc else None
    pasos_al_crear = []
    for c in crear:
        donde = _localizador(c, grupos)
        if c['tipo'] != 'lista':
            pasos_al_crear.append(f"await usuario.type({donde}, {texto_js(c['ejemplo'])});")
        elif c['ejemplo'] != c['inicial']:
            pasos_al_crear.append(f"await usuario.selectOptions({donde}, {texto_js(c['ejemplo'])});")
    editable = next((c for c in editar if c['tipo'] == 'texto'), None)
    # Un campo opcional que no se escribe, como la contraseña al editar, va como null.
    cuerpo_al_editar = {c['clave']: None if c['opcional'] else registro.get(c['clave'], '') for c in editar}
    if editable:
        cuerpo_al_editar[editable['clave']] = otro[editable['clave']]
    prueba = {
        'rol': rol,
        'roles_sin_permiso': [r for r in roles if r not in operaciones['listar']],
        'pasos_al_crear': pasos_al_crear,
        'al_guardar': _con_valores(textos['al_guardar']['es'], registro) if 'al_guardar' in textos else None,
        'al_guardar_lo_editado': (_con_valores(textos['al_guardar']['es'], dict(registro, **cuerpo_al_editar))
                                  if 'al_guardar' in textos else None),
        'al_desactivar': _con_valores(textos['al_desactivar']['es'], registro) if 'al_desactivar' in textos else None,
        'al_reactivar': _con_valores(textos['al_reactivar']['es'], otro) if 'al_reactivar' in textos else None,
        'obligatorio': next((c['campo'] for c in crear if any(l.startswith(".test('obligatorio'") for l in c['yup'])),
                            None),
        'registro': literal_js(registro), 'otro_registro': literal_js(otro),
        'nombre': registro[atributo_del_nombre], 'nombre_otro': otro[atributo_del_nombre],
        'clave_del_nombre': atributo_del_nombre,
        'titulo_lista': textos['titulo_lista']['es'],
        'vacia': textos['vacia']['es'],
        'debajo': (_con_valores(textos['debajo_del_nombre']['es'], registro) if 'debajo_del_nombre' in textos
                   else registro[camel(pantalla['debajo_del_nombre'])]) if pantalla.get('debajo_del_nombre') else None,
        'inactivo': textos['inactivo']['es'] if activo else None,
        'resumen': _resumen_es(e, *((1, 1) if activo else (2, 0))),
        'nuevo': textos.get('nuevo', {}).get('es'), 'crear': textos.get('crear', {}).get('es'),
        'cuerpo_al_crear': literal_js(_objeto(crear, lambda c: c['ejemplo'])),
        'solo_backend': [literal_js({'campo': campo, 'mensaje': mensaje}) for campo, mensaje in solo_backend],
        'solo_backend_mensajes': [mensaje for _, mensaje in solo_backend],
        'unicos': [literal_js({'campo': campo, 'mensaje': mensaje}) for campo, mensaje in unicos],
        'unicos_mensajes': [mensaje for _, mensaje in unicos],
        'editar_fila': _con_valores(textos['editar_fila']['es'], registro) if 'editar_fila' in textos else None,
        'editable': editable, 'valor_editado': otro[editable['clave']] if editable else None,
        'cuerpo_al_editar': literal_js(cuerpo_al_editar),
        'desactivar_fila': (_con_valores(textos['desactivar_fila']['es'], registro)
                            if 'desactivar_fila' in textos else None),
        'reactivar_fila': _con_valores(textos['reactivar_fila']['es'], otro) if 'reactivar_fila' in textos else None,
        'confirmar_titulo': (_con_valores(textos['confirmar_desactivar']['es'], registro)
                             if 'confirmar_desactivar' in textos else None),
        'confirmar_detalle': (_con_valores(textos['confirmar_desactivar_detalle']['es'], registro)
                              if 'confirmar_desactivar_detalle' in textos else None),
        'puede': puede,
        'html': literal_js(dict(registro, **{atributo_del_nombre: '<img src=x onerror=alert(1)>'})),
        'limite': _limite_de_la_prueba(e, limite),
        'fila_propia': _fila_propia_de_la_prueba(e, sin_acciones, impedidas, al_editar, registro, otro),
        'nota_contrasena': ({'etiqueta': al_editar[contrasena_al_editar]['etiqueta_es'],
                             'valor': al_editar[contrasena_al_editar]['ejemplo'],
                             'nota': _con_valores(textos['nota_contrasena_de_otro']['es'], registro)}
                            if contrasena_al_editar and 'nota_contrasena_de_otro' in textos else None),
        'opcionales_al_editar': opcionales,
        'iniciales': [(c['etiqueta_es'], c['inicial']) for c in crear
                      if not c['grupo'] and c['nombre'] in pantalla.get('inicial', {})],
    }
    return {
        'entidad': n, 'clase': e['clase'], 'var': camel(n), 'modulo': e['modulo'], 'ficha': e['fichas'][0],
        'singular': e['etiquetas']['es']['singular'].lower(), 'plural': e['etiquetas']['es']['plural'].lower(),
        'titulo_es': e['etiquetas']['es']['plural'],
        'ruta': f"/{e['ruta']}", 'operaciones': operaciones, 'acciones': literal_js(acciones),
        'roles_listar': literal_js(operaciones['listar']),
        'atributo_activo': literal_js(camel(activo) if activo else None), 'tiene_activo': bool(activo),
        'atributo_del_nombre': literal_js(atributo_del_nombre),
        'columnas': [literal_js(c) for c in columnas], 'textos': textos_js, 'confirmar': confirmar,
        'crear': crear, 'editar': editar, 'grupo': grupo,
        'campos_al_crear': literal_js([c['campo'] for c in crear]),
        'campos_al_editar': literal_js([c['campo'] for c in editar]),
        'valores_al_crear': literal_js(_objeto(crear, lambda c: c['inicial'])),
        'usa': sorted({ayudante for c in crear + editar for ayudante in _ayudantes(c['yup'])}),
        'componentes': sorted({c['componente'] for c in crear + editar}),
        'casos_al_crear': [caso for c in crear for caso in c['casos']],
        'bordes_al_crear': [borde for c in crear for borde in c['bordes']],
        'casos_al_editar': [caso for c in editar for caso in c['casos']],
        'bordes_al_editar': [borde for c in editar for borde in c['bordes']],
        'datos_al_crear': prueba['cuerpo_al_crear'],
        'datos_al_editar': literal_js(_objeto(editar, lambda c: c['ejemplo'])),
        'usa_correo_largo': any(c['usa_correo_largo'] for c in crear + editar),
        'icono': pantalla.get('icono', ICONO_POR_DEFECTO), 'prueba': prueba,
        'formulario': pantalla.get('formulario', 'encima'),
        'limite_js': literal_js(limite), 'fila_propia_js': literal_js(fila_propia),
        'campos_formulario': campos_formulario, 'props_campos': props_campos,
        'firma_campos': '{ ' + ', '.join(props_campos) + ' }' if props_campos else '',
        'opcionales_al_editar': opcionales, 'contrasena_al_editar': contrasena_al_editar,
        'usa_mensaje_propio': any(f['propia'] and f['modo'] == 'distinto' for f in campos_formulario),
        'limites': [(f['una']['clave'], literal_js(f['una']['limites'])) for f in campos_formulario
                    if f['una']['limites']],
        'activo_js': camel(activo) if activo else None,
    }


def _campos_del_formulario(crear, editar, impedidos):
    """Los campos propios del formulario, sin el grupo de ``crear_con``, cada uno con cómo va al registrar y al
    editar: igual en los dos si tiene el mismo control y los mismos textos; si no, como la contraseña, cada
    formulario lleva el suyo.

    Args:
        impedidos: los campos que una regla de la fila propia no deja cambiar, con la clave de su mensaje.
    """
    para_editar = {c['campo']: c for c in editar}
    propios = [c for c in crear if not c['grupo']] if crear else editar
    lista = []
    for c in propios:
        al_crear = c if crear else None
        al_editar = para_editar.get(c['campo']) if crear else c
        distinto = al_crear is not None and al_editar is not None and any(
            al_crear[clave] != al_editar[clave] for clave in ('componente', 'etiqueta', 'ayuda'))
        lista.append({'modo': 'distinto' if distinto else 'igual', 'crear': al_crear, 'editar': al_editar,
                      'una': al_crear or al_editar, 'propia': impedidos.get(c['campo'])})
    return lista


def _limite_de_la_prueba(e, limite):
    """Lo que revisa la prueba generada del límite de activos: el máximo y el mensaje de su regla, en español."""
    if limite is None:
        return None
    r = regla(e, e['pantalla']['limite_de_activos'])
    return {'maximo': limite['maximo'], 'mensaje': reemplazar(r['mensaje']['es'], r.get('parametros', {}))}


def _fila_propia_de_la_prueba(e, sin_acciones, impedidas, al_editar, registro, otro):
    """Lo que revisa la prueba generada de la fila propia, en español: su marca, los botones que no tiene, que sí
    tiene otra fila activa, y los campos que al editarla no se cambian, con el mensaje de su regla."""
    textos = e['pantalla']['textos']
    if 'fila_propia' not in textos:
        return None
    otra_activa = dict(otro, **{camel(e['atributo_activo']): True}) if 'atributo_activo' in e else otro
    botones = [f'{o}_fila' for o in sin_acciones if f'{o}_fila' in textos]
    return {
        'marca': textos['fila_propia']['es'],
        'sin_acciones': [_con_valores(textos[clave]['es'], registro) for clave in botones],
        'en_otra': [_con_valores(textos[clave]['es'], otra_activa) for clave in botones],
        'campos': [{'etiqueta': al_editar[campo]['etiqueta_es'],
                    'mensaje': reemplazar(r['mensaje']['es'], r.get('parametros', {})),
                    'oculto': al_editar[campo]['tipo'] == 'contrasena'} for campo, r in impedidas.items()],
    }


def _localizador(c, grupos):
    """Cómo encuentra una prueba de Testing Library un campo del formulario: por su etiqueta y, si el formulario
    tiene grupos, dentro del suyo, porque dos campos pueden llamarse igual, como el nombre de la empresa y el de
    su administrador.

    Args:
        grupos: los nombres en español del grupo de la entidad y del de ``crear_con``, o ``None``.
    """
    etiqueta = texto_js(c['etiqueta_es'])
    if grupos is None:
        return f'screen.getByLabelText({etiqueta})'
    grupo = texto_js(grupos[1] if c['grupo'] else grupos[0])
    return f"within(screen.getByRole('group', {{ name: {grupo} }})).getByLabelText({etiqueta})"


def _ayudantes(lineas):
    """Las funciones de ``src/validacion/reglas.js`` que usan las validaciones de Yup de un campo."""
    texto = '\n'.join(lineas)
    return [nombre for nombre in ('cabeEnBytes', 'lleno', 'maximoDecimales', 'minusculas', 'noEnBlanco', 'numeroDecimal',
                                  'recortar', 'vacioComoAusente')
            if nombre in texto]


# --- Pantalla en bloque ---------------------------------------------------------------------------


def _valores_de_la_lista(entidades, a):
    """Los valores de una lista, con su etiqueta en español: los suyos o, con ``valores_de``, los de la lista
    original."""
    if 'valores' in a:
        return [(v['codigo'], v['etiquetas']['es']) for v in a['valores']]
    nombre_entidad, _, nombre_atributo = a['valores_de'].partition('.')
    return _valores_de_la_lista(entidades, atributo(entidades[nombre_entidad], nombre_atributo))


def _texto_del_decimal(valor, escala):
    """Un decimal como lo muestra el formulario en español: con su escala y con coma, como 96,0."""
    return f'{Decimal(str(valor)):.{escala}f}'.replace('.', ',')


def _campo_en_bloque(e, a):
    """Un campo de cada fila de una pantalla en bloque: cómo lo describe la pantalla (su tipo, su escala, si va en
    varias líneas y en qué fila es de solo lectura), sus validaciones de Yup y sus casos de límite, del mismo modelo
    que la fila de la API."""
    datos = campos.del_atributo(e, a)
    nombre = camel(a['nombre'])
    descripcion = {'nombre': nombre, 'tipo': a['tipo'], 'etiqueta': f"{e['entidad']}.{a['nombre']}.etiqueta"}
    if a['tipo'] == 'decimal':
        descripcion['escala'] = a['escala']
    if a.get('varias_lineas'):
        descripcion['variasLineas'] = True
    solo_lectura = a.get('solo_lectura_si')
    if solo_lectura:
        descripcion['soloLecturaSi'] = {'codigo': solo_lectura['igual_a'],
                                        'mensaje': clave_mensaje(e, a, 'solo_lectura_si')}
    return {
        'nombre': nombre, 'atributo': a['nombre'], 'tipo': a['tipo'], 'escala': a.get('escala'),
        'etiqueta_es': a['etiquetas']['es'], 'descripcion': literal_js(descripcion),
        'base': 'number()' if a['tipo'] == 'decimal' else 'string()',
        'yup': lineas_yup(datos, numero=a['tipo'] == 'decimal'),
        'casos': [(texto_js(nombre), valor_js(c.valor), texto_js(c.mensaje)) for c in datos.casos if not c.solo_backend],
        'bordes': [(texto_js(nombre), valor_js(valor)) for valor in datos.bordes],
        # Para la prueba de la pantalla: el mensaje del dato que falta y el de otro valor que se rechaza.
        'falta': next((c.mensaje for c in datos.casos if c.valor.tipo == 'falta'), None),
        'rechazo': next((c.mensaje for c in datos.casos if c.valor.tipo != 'falta' and not c.solo_backend), None),
        'solo_lectura': ({'codigo': solo_lectura['igual_a'], 'mensaje': mensaje_esperado(e, a, 'solo_lectura_si')}
                         if solo_lectura else None),
    }


def modelo_pantalla_en_bloque(entidades, e):
    """Todo lo que necesitan las plantillas de la pantalla de una entidad con ``edicion_en_bloque``, como los rangos
    (RF-13): una fila por cada valor de la lista que identifica las filas, en su orden, todas en un solo
    formulario, que se guarda junto y vuelve a los valores por defecto si ``operaciones`` lo dice."""
    verificar_pantalla(entidades, e)
    n, operaciones, textos = e['entidad'], e['operaciones'], e['pantalla'].get('textos', {})
    atr_clave = clave_en_bloque(e)
    clave_js = camel(atr_clave['nombre'])
    prefijo = _prefijo_de_valores(entidades, e, atr_clave)
    valores = _valores_de_la_lista(entidades, atr_clave)
    formulario = [a for a in e['atributos'] if a.get('en_formulario')]
    campos_de_fila = [_campo_en_bloque(e, a) for a in formulario]
    acciones = {js: operaciones[o] for o, js in (('editar', 'editar'), ('restaurar_por_defecto', 'restaurarPorDefecto'))
                if o in operaciones}
    roles = [v['codigo'] for v in atributo(entidades['usuario'], 'rol')['valores']]

    # Las filas de las pruebas generadas: las de valores_iniciales, en el orden de la lista, como las responde la API
    # y como las muestra el formulario, con cada decimal en español.
    orden = [codigo for codigo, _ in valores]
    iniciales = sorted(e['valores_iniciales']['filas'], key=lambda f: orden.index(f[atr_clave['nombre']]))

    def fila(inicial, como):
        return {clave_js: inicial[atr_clave['nombre']]} | {camel(a['nombre']): como(a, inicial.get(a['nombre']))
                                                           for a in formulario}

    de_la_api = [fila(f, lambda a, valor: valor) for f in iniciales]
    del_formulario = [fila(f, lambda a, valor: _texto_del_decimal(valor, a['escala']) if a['tipo'] == 'decimal'
                           else valor) for f in iniciales]
    etiquetas_de_filas = [etiqueta for _, etiqueta in valores]
    decimal = next((c for c in campos_de_fila if c['tipo'] == 'decimal'), None)
    texto = next((c for c in campos_de_fila if c['tipo'] == 'texto'), None)
    solo_lectura = next((c for c in campos_de_fila if c['solo_lectura']), None)
    # Al guardar, la prueba vuelve a escribir el decimal de la primera fila como lo muestra el formulario y cambia su
    # texto: así revisa que los números vayan como números, sin mover ningún dato que revise una regla del bloque
    # escrita a mano, como el orden de los rangos. Al restaurar, la empresa tiene ese texto propio en cada fila.
    con_texto_propio = [dict(f, **({texto['nombre']: TEXTO_DE_PRUEBA} if texto else {})) for f in de_la_api]
    guardadas = [con_texto_propio[0]] + de_la_api[1:]
    reglas_del_bloque = [reemplazar(r['mensaje']['es'], r.get('parametros', {})) for r in e.get('reglas', [])
                         if r['tipo'] == 'validacion' and not r.get('atributo')]
    errores_del_bloque = ([{'campo': None, 'mensaje': mensaje} for mensaje in reglas_del_bloque[:1]]
                          + [{'campo': 'filas', 'mensaje': mensaje_filas_completas(e, 'es')}])
    rol = operaciones['listar'][0]
    prueba = {
        'rol': rol,
        'roles_sin_permiso': [r for r in roles if r not in operaciones['listar']],
        'puede': {'editar': rol in operaciones.get('editar', []),
                  'restaurar': rol in operaciones.get('restaurar_por_defecto', [])},
        'filas': [literal_js(f) for f in de_la_api],
        'etiquetas_de_filas': literal_js(etiquetas_de_filas),
        'titulo_editar': textos['titulo_editar']['es'] if 'titulo_editar' in textos else None,
        'primera': etiquetas_de_filas[0],
        'segunda': etiquetas_de_filas[1] if len(etiquetas_de_filas) > 1 else None,
        'decimal': ({'etiqueta': decimal['etiqueta_es'], 'valor': del_formulario[0][decimal['nombre']]}
                    if decimal else None),
        'texto': ({'nombre': texto['nombre'], 'etiqueta': texto['etiqueta_es'],
                   'valor': de_la_api[0][texto['nombre']], 'falta': texto['falta'], 'rechazo': texto['rechazo']}
                  if texto else None),
        'texto_de_prueba': TEXTO_DE_PRUEBA,
        'solo_lectura': ({'codigo': solo_lectura['solo_lectura']['codigo'], 'campo': solo_lectura['nombre'],
                          'fila': dict(valores)[solo_lectura['solo_lectura']['codigo']],
                          'etiqueta': solo_lectura['etiqueta_es'], 'mensaje': solo_lectura['solo_lectura']['mensaje']}
                         if solo_lectura else None),
        'guardadas': [literal_js(f) for f in guardadas],
        'con_texto_propio': [literal_js(f) for f in con_texto_propio],
        'al_guardar': textos['al_guardar']['es'] if 'al_guardar' in textos else None,
        'al_restaurar': textos['al_restaurar']['es'] if 'al_restaurar' in textos else None,
        'confirmar_detalle': (textos['confirmar_restaurar_detalle']['es']
                              if 'confirmar_restaurar_detalle' in textos else None),
        'errores_del_bloque': [literal_js(error) for error in errores_del_bloque],
    }
    return {
        'entidad': n, 'clase': e['clase'], 'var': camel(n), 'modulo': e['modulo'], 'ficha': e['fichas'][0],
        'singular': e['etiquetas']['es']['singular'].lower(), 'plural': e['etiquetas']['es']['plural'].lower(),
        'titulo_es': e['etiquetas']['es']['plural'], 'ruta': f"/{e['ruta']}", 'operaciones': operaciones,
        'roles_listar': literal_js(operaciones['listar']), 'icono': e['pantalla'].get('icono', ICONO_POR_DEFECTO),
        'en_bloque': True, 'acciones': literal_js(acciones),
        'clave': {'atributo': clave_js, 'en_frase': en_frase(atr_clave['etiquetas']['es']),
                  'valores': [literal_js({'codigo': codigo, 'etiqueta': f'{prefijo}.{codigo}'})
                              for codigo, _ in valores]},
        'campos': campos_de_fila,
        'textos': [(js, literal_js(f'{n}.pantalla.{clave}' if clave in textos else None))
                   for js, clave in TEXTOS_EN_BLOQUE_JS],
        'usa': sorted({ayudante for c in campos_de_fila for ayudante in _ayudantes(c['yup'])}),
        'de_yup': sorted({'array', 'object'} | {c['base'].removesuffix('()') for c in campos_de_fila}),
        'casos': [caso for c in campos_de_fila for caso in c['casos']],
        'bordes': [borde for c in campos_de_fila for borde in c['bordes']],
        'filas_del_formulario': [literal_js(f) for f in del_formulario],
        'decimales': [c for c in campos_de_fila if c['tipo'] == 'decimal'],
        'prueba': prueba,
    }


# --- Archivos -------------------------------------------------------------------------------------


def _header(entidades, involucradas, nombre_archivo, descripcion):
    datos = datos_header({x['entidad']: x for x in involucradas})
    return dict(datos, nombre=nombre_archivo, descripcion=descripcion)


def archivos_frontend(entidades, pantallas):
    """Archivos del frontend: las listas de pantallas y de textos, que la app lee siempre, y los de cada entidad
    de ``pantallas``. Devuelve ``(ruta, plantilla, contexto)``."""
    todas = list(entidades.values())
    modelos = [modelo_pantalla_en_bloque(entidades, entidades[nombre]) if entidades[nombre].get('edicion_en_bloque')
               else modelo_pantalla(entidades, entidades[nombre]) for nombre in pantallas]
    textos = []
    for nombre in pantallas:
        for clave, texto in entidades[nombre]['pantalla'].get('textos', {}).items():
            base = f'{nombre}.pantalla.{clave}'
            if isinstance(texto['es'], dict):
                textos += [(f'{base}.{forma}', texto['es'][forma], texto['en'][forma]) for forma in ('singular', 'plural')]
            else:
                textos.append((base, texto['es'], texto['en']))
    archivos = [
        (f'{CARPETA}/pantallas.js', 'frontend/pantallas.js.j2', {
            'pantallas': modelos,
            'header': _header(entidades, todas, 'pantallas.js', 'Las pantallas generadas: la ruta, la entrada del '
                                                               'menú y los roles de cada una (RP-23, RNF-04).')}),
        (f'{CARPETA}/textos.js', 'frontend/textos.js.j2', {
            'textos': [(texto_js(clave), texto_js(es), texto_js(en)) for clave, es, en in textos],
            'header': _header(entidades, todas, 'textos.js', 'Los textos de las pantallas generadas, en español y '
                                                             'en inglés: salen de pantalla.textos de cada JSON (RP-11).')}),
    ]
    for m in modelos:
        e = entidades[m['entidad']]
        involucradas = [e] + ([entidades[e['crear_con']['entidad']]] if 'crear_con' in e else [])
        carpeta, var, clase, ficha = f"{CARPETA}/{m['modulo']}", m['var'], m['clase'], m['ficha']

        def archivo(ruta, plantilla, descripcion, m=m, involucradas=involucradas):
            return (ruta, plantilla, dict(m, header=_header(entidades, involucradas, ruta.rsplit('/', 1)[1],
                                                            descripcion)))

        if m.get('en_bloque'):
            archivos += [
                archivo(f'{carpeta}/{var}Api.js', 'frontend/api_en_bloque.js.j2',
                        f"Cliente de la API de {m['entidad']} en /api/v1{m['ruta']}, que se edita en bloque ({ficha})."),
                archivo(f'{carpeta}/{var}Api.test.js', 'frontend/api_en_bloque.test.js.j2',
                        f"Pruebas generadas del cliente de la API de {m['entidad']}: la ruta y el método de cada "
                        'operación.'),
                archivo(f'{carpeta}/{var}Esquemas.js', 'frontend/esquemas_en_bloque.js.j2',
                        f"Validaciones de Yup de las filas de {m['entidad']}: las del JSON, con los mensajes de la API "
                        f'({ficha}).'),
                archivo(f'{carpeta}/{var}Esquemas.test.js', 'frontend/esquemas_en_bloque.test.js.j2',
                        f"Pruebas generadas de las validaciones de las filas de {m['entidad']}: los mismos casos de "
                        'límite que la prueba generada de la API.'),
                archivo(f'{carpeta}/{clase}Pantalla.jsx', 'frontend/pantalla_en_bloque.jsx.j2',
                        f"Pantalla de {m['entidad']} ({ficha}): una fila por cada valor de {m['clave']['en_frase']}, "
                        'editadas juntas.'),
                archivo(f'{carpeta}/{clase}Pantalla.test.jsx', 'frontend/pantalla_en_bloque.test.jsx.j2',
                        f"Pruebas generadas de la pantalla de {m['entidad']}: las filas, la solo lectura, guardar, los "
                        'errores de la API, restaurar y permisos.'),
                archivo(f"{CARPETA_E2E}/{m['entidad']}.spec.js", 'frontend/pantalla.spec.js.j2',
                        f"Pruebas generadas de punta a punta de la pantalla de {m['entidad']}: permisos, accesibilidad "
                        'y ancho de celular.'),
            ]
            continue
        archivos += [
            archivo(f'{carpeta}/{var}Api.js', 'frontend/api.js.j2',
                    f"Cliente de la API de {m['entidad']} en /api/v1{m['ruta']} ({ficha})."),
            archivo(f'{carpeta}/{var}Api.test.js', 'frontend/api.test.js.j2',
                    f"Pruebas generadas del cliente de la API de {m['entidad']}: la ruta y el método de cada operación."),
            archivo(f'{carpeta}/{var}Esquemas.js', 'frontend/esquemas.js.j2',
                    f"Validaciones de Yup del formulario de {m['entidad']}: las del JSON, con los mensajes de la API "
                    f'({ficha}).'),
            archivo(f'{carpeta}/{var}Esquemas.test.js', 'frontend/esquemas.test.js.j2',
                    f"Pruebas generadas de las validaciones de {m['entidad']}: los mismos casos de límite que la prueba "
                    'generada de la API.'),
            archivo(f'{carpeta}/{clase}Pantalla.jsx', 'frontend/pantalla.jsx.j2',
                    f"Pantalla de {m['entidad']} ({ficha}): su lista, su formulario y sus acciones."),
            archivo(f'{carpeta}/{clase}Pantalla.test.jsx', 'frontend/pantalla.test.jsx.j2',
                    f"Pruebas generadas de la pantalla de {m['entidad']}: lista, formulario, errores de la API, "
                    'acciones y permisos.'),
            archivo(f"{CARPETA_E2E}/{m['entidad']}.spec.js", 'frontend/pantalla.spec.js.j2',
                    f"Pruebas generadas de punta a punta de la pantalla de {m['entidad']}: permisos, accesibilidad y "
                    'ancho de celular.'),
        ]
    return archivos
