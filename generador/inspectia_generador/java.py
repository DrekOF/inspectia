# Nombre: java.py
# Descripción: Arma los datos de las plantillas de Java: la persistencia de cada CRUD y, para las
#              entidades de alcance.json, su API y sus pruebas (RP-23).
# Fecha de creación: 13/10/2026
# Actualización: 13/10/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Datos de las plantillas de Java.

- De cada entidad con ``"crud": true`` sale su persistencia: entidad JPA, repositorio, un ``enum``
  por lista y, si tiene ``valores_iniciales``, la clase que los arma.
- De las entidades de ``alcance.json`` sale además la API: reglas, servicio, controlador, reglas de
  acceso por ruta, solicitudes, respuesta, mapeador y una prueba de integración.

La API todavía no soporta todo el formato. Si una entidad del alcance usa algo que falta (por
ejemplo, ser por empresa, que necesita la sesión de RF-01), la generación se detiene y dice qué.
"""

from inspectia_generador.encabezado import datos_header
from inspectia_generador.modelo import atributo, tiene_empresa
from inspectia_generador.nombres import PAQUETE_BASE, camel, pascal, texto_java
from inspectia_generador.sql import ErrorGeneracion
from inspectia_generador.textos import clave_mensaje, clave_unico, mensaje_esperado, mensaje_unico

CARPETA_MAIN = 'backend/src/generated/java'
CARPETA_PRUEBAS = 'backend/src/test-generated/java'

TIPOS_JAVA = {'texto': 'String', 'contrasena': 'String', 'entero': 'Integer', 'decimal': 'BigDecimal',
              'booleano': 'Boolean', 'fecha': 'LocalDate', 'fecha_hora': 'OffsetDateTime',
              'referencia': 'UUID'}
IMPORTACION_DE_TIPO = {'BigDecimal': 'java.math.BigDecimal', 'LocalDate': 'java.time.LocalDate',
                       'OffsetDateTime': 'java.time.OffsetDateTime', 'UUID': 'java.util.UUID'}

#: Operaciones que genera la API, en el orden del controlador.
OPERACIONES_API = ('listar', 'ver', 'crear', 'editar', 'desactivar', 'reactivar')
#: Verbo de cada operación en la documentación y en los nombres de las reglas.
VERBOS = {'listar': 'Listar', 'ver': 'Ver', 'crear': 'Crear', 'editar': 'Editar',
          'desactivar': 'Desactivar', 'reactivar': 'Reactivar'}
#: Ruta y método HTTP de cada operación: ``{id}`` es el identificador del registro.
RUTAS = {'listar': ('GET', ''), 'ver': ('GET', '/{id}'), 'crear': ('POST', ''), 'editar': ('PUT', '/{id}'),
         'desactivar': ('POST', '/{id}/desactivar'), 'reactivar': ('POST', '/{id}/reactivar')}


def paquete(e):
    """Paquete de Java del módulo de la entidad (D-21)."""
    return f"{PAQUETE_BASE}.{e['modulo']}"


def ruta(e, clase, prueba=False):
    """Ruta del archivo de una clase del módulo de la entidad."""
    carpeta = CARPETA_PRUEBAS if prueba else CARPETA_MAIN
    return f"{carpeta}/{paquete(e).replace('.', '/')}/{clase}.java"


def var(e):
    """Nombre de una variable de la entidad: ``rango_recomendacion`` → ``rangoRecomendacion``."""
    return camel(e['entidad'])


# --- Tipos y valores ----------------------------------------------------------------------------


def lista(entidades, e, a):
    """``(paquete, clase)`` del ``enum`` de una lista; ``valores_de`` usa el de la lista original."""
    if 'valores' in a:
        return paquete(e), e['clase'] + pascal(a['nombre'])
    nombre_entidad, _, nombre_atributo = a['valores_de'].partition('.')
    origen = entidades[nombre_entidad]
    if not origen['crud']:
        raise ErrorGeneracion(f"{e['entidad']}.{a['nombre']}: valores_de apunta a {a['valores_de']}, "
                              'que no tiene CRUD y por eso no tiene enum')
    return lista(entidades, origen, atributo(origen, nombre_atributo))


def tipo_java(entidades, e, a):
    return lista(entidades, e, a)[1] if a['tipo'] == 'lista' else TIPOS_JAVA[a['tipo']]


def importaciones_tipo(entidades, e, a, paquete_actual):
    """Importaciones que necesita el tipo de un atributo en un archivo de ``paquete_actual``."""
    if a['tipo'] == 'lista':
        p, clase = lista(entidades, e, a)
        return set() if p == paquete_actual else {f'{p}.{clase}'}
    tipo = TIPOS_JAVA[a['tipo']]
    return {IMPORTACION_DE_TIPO[tipo]} if tipo in IMPORTACION_DE_TIPO else set()


def literal(entidades, e, a, valor):
    """Un valor del JSON como expresión de Java (por defecto y valores iniciales)."""
    tipo = a['tipo']
    if tipo == 'booleano':
        return 'true' if valor else 'false'
    if tipo == 'entero':
        return str(int(valor))
    if tipo == 'decimal':
        return f'new BigDecimal("{valor}")'
    if tipo == 'lista':
        return f'{lista(entidades, e, a)[1]}.{valor}'
    if tipo == 'fecha':
        return f'LocalDate.parse("{valor}")'
    if tipo == 'fecha_hora':
        return f'OffsetDateTime.parse("{valor}")'
    if tipo == 'referencia':
        return f'UUID.fromString("{valor}")'
    return texto_java(valor)


def ejemplo(a):
    """Valor válido de un atributo: el ``ejemplo`` del JSON o uno armado con sus validaciones."""
    if 'ejemplo' in a:
        return a['ejemplo']
    v = a.get('validaciones', {})
    tipo = a['tipo']
    if tipo == 'texto':
        if v.get('formato') == 'correo':
            return 'prueba@example.com'
        return (a['etiquetas']['es'] + ' de prueba')[:a['longitud']]
    if tipo == 'contrasena':
        minimo, maximo = v.get('longitud_min', 8), v.get('longitud_max', a['longitud'])
        return 'Prueba-Segura-2026'.ljust(minimo, 'x')[:maximo]
    if tipo in ('entero', 'decimal'):
        return v.get('min', 1)
    if tipo == 'booleano':
        return True
    if tipo == 'fecha':
        return '2026-01-15'
    if tipo == 'fecha_hora':
        return '2026-01-15T10:00:00-05:00'
    if tipo == 'lista':
        return a['valores'][0]['codigo'] if 'valores' in a else None
    return None


def json_java(valor):
    """Un valor de ejemplo como expresión de Java para armar el JSON de una prueba."""
    if valor is None:
        return 'null'
    if isinstance(valor, bool):
        return 'true' if valor else 'false'
    if isinstance(valor, (int, float)):
        return str(valor)
    return texto_java(valor)


# --- Persistencia -------------------------------------------------------------------------------


def campos_entidad(entidades, e):
    """Campos de la entidad JPA, sin ``id`` ni las fechas, que la plantilla escribe siempre."""
    campos = []
    if tiene_empresa(e):
        nulo = '' if e['por_empresa'] == 'opcional' else ', nullable = false'
        campos.append({'javadoc': 'Empresa dueña del registro (RNF-03).',
                       'anotaciones': [f'@Column(name = "empresa_id"{nulo})'],
                       'tipo': 'UUID', 'nombre': 'empresaId', 'inicial': None})
    for a in e['atributos']:
        columna = [f'name = "{a["nombre"]}"']
        if a.get('obligatorio'):
            columna.append('nullable = false')
        anotaciones = []
        if a['tipo'] in ('texto', 'contrasena'):
            columna.append(f'length = {a["longitud"]}')
        elif a['tipo'] == 'decimal':
            columna += [f'precision = {a["precision"]}', f'scale = {a["escala"]}']
        elif a['tipo'] == 'lista':
            anotaciones.append('@Enumerated(EnumType.STRING)')
            columna.append('length = 30')
        anotaciones.append(f'@Column({", ".join(columna)})')
        inicial = literal(entidades, e, a, a['por_defecto']) if 'por_defecto' in a else None
        campos.append({'javadoc': f"{a['etiquetas']['es']} ({', '.join(a['fichas'])}).",
                       'anotaciones': anotaciones, 'tipo': tipo_java(entidades, e, a),
                       'nombre': camel(a['nombre']), 'inicial': inicial})
    return campos


def consultas_unicas(e):
    """Consultas de los datos únicos de una sola columna, para revisarlos antes de guardar (409).

    Los únicos de varias columnas o parciales (``donde``) quedan solo en la base.
    """
    consultas, vistos = [], set()
    for a in e['atributos']:
        if a.get('unico'):
            consultas.append(_consulta(e, a, normalizar=False))
            vistos.add(a['nombre'])
    for u in e.get('unicos', []):
        columnas = u['atributos']
        a = atributo(e, columnas[0])
        if len(columnas) == 1 and 'donde' not in u and a is not None and a['nombre'] not in vistos:
            consultas.append(_consulta(e, a, normalizar=bool(u.get('normalizar')) and a['tipo'] == 'texto'))
            vistos.add(a['nombre'])
    return consultas


def _consulta(e, a, normalizar):
    campo = camel(a['nombre'])
    condicion = f'lower(trim(e.{campo})) = lower(trim(:valor))' if normalizar else f'e.{campo} = :valor'
    jpql = f"select case when count(e) > 0 then true else false end from {e['clase']} e where {condicion}"
    return {'atributo': a['nombre'], 'campo': campo, 'getter': 'get' + pascal(a['nombre']),
            'metodo': 'existe' + pascal(a['nombre']), 'metodo_en_otro': 'existe' + pascal(a['nombre']) + 'EnOtro',
            'jpql': jpql, 'jpql_en_otro': jpql + ' and e.id <> :id', 'normalizado': normalizar,
            'etiqueta': _en_frase(a['etiquetas']['es']), 'clave': clave_unico(e, [a['nombre']]),
            'mensaje': mensaje_unico(e, [a['nombre']])}


def _en_frase(etiqueta):
    """La etiqueta dentro de una frase: en minúscula, salvo las siglas como NIT."""
    return etiqueta if etiqueta.isupper() else etiqueta[0].lower() + etiqueta[1:]


def valores_iniciales(entidades, e):
    """Datos de la clase que arma las filas por defecto de cada empresa nueva."""
    vi = e['valores_iniciales']
    columnas = [a for a in e['atributos'] if any(a['nombre'] in f for f in vi['filas'])]
    return {'descripcion': vi['descripcion'],
            'parametros': [{'tipo': tipo_java(entidades, e, a), 'nombre': camel(a['nombre']),
                            'setter': 'set' + pascal(a['nombre'])} for a in columnas],
            'filas': [[literal(entidades, e, a, f[a['nombre']]) if a['nombre'] in f else 'null'
                       for a in columnas] for f in vi['filas']],
            'importaciones': sorted(set().union(*(importaciones_tipo(entidades, e, a, paquete(e))
                                                 for a in columnas)) | {'java.util.List', 'java.util.UUID'})}


def archivos_persistencia(entidades, e):
    """Archivos de la persistencia de una entidad con CRUD: ``(ruta, plantilla, contexto)``."""
    p = paquete(e)
    archivos = []
    for a in e['atributos']:
        if a['tipo'] == 'lista' and 'valores' in a:
            clase = lista(entidades, e, a)[1]
            archivos.append((ruta(e, clase), 'java/main/enum.java.j2', {
                'paquete': p, 'clase': clase, 'etiqueta': a['etiquetas']['es'], 'entidad': e['entidad'],
                'atributo': a['nombre'], 'fichas': a['fichas'],
                'valores': [(v['codigo'], v['etiquetas']['es']) for v in a['valores']],
                'header': _header(entidades, [e], f'{clase}.java',
                                  f"Valores de la lista {a['nombre']} de {e['entidad']}.")}))

    campos = campos_entidad(entidades, e)
    importaciones = {'jakarta.persistence.Column', 'jakarta.persistence.Entity', 'jakarta.persistence.Id',
                     'jakarta.persistence.Table', 'java.time.OffsetDateTime', 'java.util.UUID',
                     'lombok.Getter', 'lombok.Setter', 'org.hibernate.annotations.Generated',
                     'org.hibernate.annotations.UuidGenerator', 'org.hibernate.generator.EventType'}
    for a in e['atributos']:
        importaciones |= importaciones_tipo(entidades, e, a, p)
        if a['tipo'] == 'lista':
            importaciones |= {'jakarta.persistence.EnumType', 'jakarta.persistence.Enumerated'}
    archivos.append((ruta(e, e['clase']), 'java/main/entidad.java.j2', {
        'paquete': p, 'clase': e['clase'], 'tabla': e['entidad'], 'descripcion': e['descripcion'],
        'fichas': e['fichas'], 'campos': campos, 'importaciones': sorted(importaciones),
        'header': _header(entidades, [e], f"{e['clase']}.java", f"Entidad JPA de la tabla {e['entidad']}.")}))

    consultas = consultas_unicas(e)
    importaciones = {'java.util.UUID', 'org.springframework.data.jpa.repository.JpaRepository'}
    if consultas:
        importaciones |= {'org.springframework.data.jpa.repository.Query',
                          'org.springframework.data.repository.query.Param'}
    for c in consultas:
        importaciones |= importaciones_tipo(entidades, e, atributo(e, c['atributo']), p)
    archivos.append((ruta(e, e['clase'] + 'Repositorio'), 'java/main/repositorio.java.j2', {
        'paquete': p, 'clase': e['clase'], 'entidad': e['entidad'], 'importaciones': sorted(importaciones),
        'consultas': [dict(c, tipo=tipo_java(entidades, e, atributo(e, c['atributo']))) for c in consultas],
        'header': _header(entidades, [e], f"{e['clase']}Repositorio.java",
                          f"Repositorio de Spring Data JPA de la tabla {e['entidad']}.")}))

    if 'valores_iniciales' in e:
        clase = e['clase'] + 'ValoresIniciales'
        archivos.append((ruta(e, clase), 'java/main/valores_iniciales.java.j2', dict(
            valores_iniciales(entidades, e), paquete=p, clase=e['clase'], entidad=e['entidad'],
            header=_header(entidades, [e], f'{clase}.java',
                           f"Filas por defecto de {e['entidad']} para cada empresa nueva."))))
    return archivos


# --- API ----------------------------------------------------------------------------------------


def verificar_api(entidades, e):
    """Revisa que la API de la entidad se pueda generar con lo que soportan las plantillas.

    Raises:
        ErrorGeneracion: con todo lo que falta, para resolverlo de una vez.
    """
    problemas = []
    operaciones = e.get('operaciones', {})
    if not e['crud']:
        problemas.append('no tiene CRUD')
    if tiene_empresa(e):
        problemas.append('es por empresa: necesita la empresa de la sesión, que llega con RF-01')
    for operacion in operaciones:
        if operacion not in OPERACIONES_API:
            problemas.append(f'la operación {operacion}')
    if e.get('edicion_en_bloque'):
        problemas.append('edicion_en_bloque')
    if {'desactivar', 'reactivar'} & set(operaciones) and 'atributo_activo' not in e:
        problemas.append('desactivar o reactivar sin atributo_activo')
    for a in e['atributos']:
        if a.get('en_formulario'):
            problemas += _problemas_de_campo(a, a['nombre'])
        if not a.get('en_formulario') and a.get('obligatorio') and 'por_defecto' not in a:
            problemas.append(f"{a['nombre']} es obligatorio, pero no va en el formulario ni tiene por_defecto")
        if a.get('en_formulario') and a['tipo'] in ('referencia', 'contrasena'):
            problemas.append(f"{a['nombre']}, de tipo {a['tipo']}, en el formulario")
    for r in e.get('reglas', []):
        if r['tipo'] != 'validacion':
            problemas.append(f"la regla {r['codigo']}, de tipo {r['tipo']}")
        if r.get('parametros'):
            problemas.append(f"la regla {r['codigo']}, con parametros")
    if 'crear_con' in e:
        cc = e['crear_con']
        otra = entidades[cc['entidad']]
        if 'crear' not in operaciones:
            problemas.append('crear_con sin la operación crear')
        if not otra['crud']:
            problemas.append(f"crear_con con {cc['entidad']}, que no tiene CRUD")
        if tiene_empresa(otra) and e['entidad'] != 'empresa':
            problemas.append('crear_con de una entidad por empresa desde otra que no es empresa')
        for nombre in cc['atributos']:
            problemas += _problemas_de_campo(atributo(otra, nombre), f"crear_con.{nombre}")
        faltan = [a['nombre'] for a in otra['atributos'] if a.get('obligatorio') and 'por_defecto' not in a
                  and a['nombre'] not in cc['atributos'] and a['nombre'] not in cc.get('valores', {})]
        if faltan:
            problemas.append(f"crear_con no llena {', '.join(faltan)}, que son obligatorios en {cc['entidad']}")
    for otra in entidades.values():
        if 'valores_iniciales' in otra and e['entidad'] == 'empresa' and not otra['crud']:
            problemas.append(f"valores_iniciales de {otra['entidad']}, que no tiene CRUD")
    if problemas:
        raise ErrorGeneracion(f"No se puede generar la API de {e['entidad']}: " + '; '.join(problemas))


def _problemas_de_campo(a, donde):
    problemas = []
    for clave in ('valores_en_formulario', 'solo_lectura_si'):
        if clave in a:
            problemas.append(f'{clave} en {donde}')
    v = a.get('validaciones', {})
    if 'no_posterior_a_hoy' in v:
        problemas.append(f'no_posterior_a_hoy en {donde}')
    if 'patron' in v and 'ejemplo' not in a:
        problemas.append(f'{donde} tiene patron pero no ejemplo')
    return problemas


def campo_solicitud(e, a, campo_json, nombre=None, obligatorio=None):
    """Un campo de una solicitud: tipo, anotaciones, normalización y casos de prueba de validación.

    Args:
        e: entidad dueña del atributo (sus mensajes son los que responde la API).
        campo_json: nombre del campo en los errores, como ``administrador.correo``.
        nombre: nombre del campo en Java, si no es el del atributo (la entrada de una contraseña).
        obligatorio: si es obligatorio, cuando no lo dice ``obligatorio`` del atributo.
    """
    nombre = nombre or camel(a['nombre'])
    v = a.get('validaciones', {})
    texto = a['tipo'] in ('texto', 'contrasena')
    requerido = (a.get('obligatorio') and 'por_defecto' not in a) if obligatorio is None else obligatorio
    sin_formato = 'patron' not in v and 'formato' not in v
    anotaciones, casos = [], []

    if requerido:
        anotaciones.append(f'@{"NotBlank" if texto else "NotNull"}(message = "{{validacion.obligatorio}}")')
        casos.append(('null', mensaje_esperado(e, a, 'obligatorio')))
    if a['tipo'] == 'texto' or 'longitud_max' in v:
        limites = ([(a['longitud'], False)] if a['tipo'] == 'texto' else []) + (
            [(v['longitud_max'], True)] if 'longitud_max' in v else [])
        maximo, propio = min(limites)
        clave = clave_mensaje(e, a, 'longitud_max') if propio else 'validacion.longitud_max'
        anotaciones.append(f'@Size(max = {maximo}, message = "{{{clave}}}")')
        mensaje = mensaje_esperado(e, a if propio else {}, 'longitud_max', maximo)
        if sin_formato:
            casos.append((f'"x".repeat({maximo + 1})', mensaje))
        elif v.get('formato') == 'correo':
            casos.append((f'correoDeLargo({maximo + 1})', mensaje))
    if 'longitud_min' in v:
        minimo = v['longitud_min']
        anotaciones.append(f'@Size(min = {minimo}, message = "{{{clave_mensaje(e, a, "longitud_min")}}}")')
        if minimo > 1 and sin_formato:
            casos.append((f'"x".repeat({minimo - 1})', mensaje_esperado(e, a, 'longitud_min', minimo)))
    if 'bytes_max' in v:
        bytes_max = v['bytes_max']
        anotaciones.append(f'@MaximoBytes(value = {bytes_max}, message = "{{{clave_mensaje(e, a, "bytes_max")}}}")')
        caracteres = bytes_max // 2 + 1
        maximo = v.get('longitud_max', a.get('longitud', caracteres))
        if sin_formato and v.get('longitud_min', 0) <= caracteres <= maximo:
            casos.append((f'"ñ".repeat({caracteres})', mensaje_esperado(e, a, 'bytes_max')))
    if 'patron' in v:
        anotaciones.append(f'@Pattern(regexp = {texto_java(v["patron"])}, '
                           f'message = "{{{clave_mensaje(e, a, "patron")}}}")')
        casos.append(('"?"', mensaje_esperado(e, a, 'patron')))
    if v.get('formato') == 'correo':
        anotaciones.append(f'@Email(message = "{{{clave_mensaje(e, a, "formato")}}}")')
        casos.append(('"sin-arroba"', mensaje_esperado(e, a, 'formato')))
    for limite, anotacion in (('min', 'Min'), ('max', 'Max')):
        if limite in v and a['tipo'] in ('entero', 'decimal'):
            valor = v[limite]
            clave = clave_mensaje(e, a, limite)
            if a['tipo'] == 'entero':
                anotaciones.append(f'@{anotacion}(value = {int(valor)}, message = "{{{clave}}}")')
                fuera = int(valor) - 1 if limite == 'min' else int(valor) + 1
            else:
                anotaciones.append(f'@Decimal{anotacion}(value = "{valor}", message = "{{{clave}}}")')
                fuera = valor - 1 if limite == 'min' else valor + 1
            casos.append((json_java(fuera), mensaje_esperado(e, a, limite, valor)))

    normalizacion = nombre
    if v.get('recortar'):
        normalizacion = f'Textos.recortar({normalizacion})'
    if v.get('minusculas'):
        normalizacion = f'Textos.minusculas({normalizacion})'
    esquema = [f'description = {texto_java(a["etiquetas"]["es"])}']
    if a['tipo'] != 'contrasena' and ejemplo(a) is not None:
        esquema.append(f'example = {texto_java(ejemplo(a))}')
    if requerido:
        esquema.append('requiredMode = Schema.RequiredMode.REQUIRED')
    return {'nombre': nombre, 'campo_json': campo_json, 'atributo': a['nombre'], 'tipo_atributo': a['tipo'],
            'etiqueta': a['etiquetas']['es'],
            'esquema': ', '.join(esquema), 'anotaciones': anotaciones,
            'normalizacion': None if normalizacion == nombre else normalizacion,
            'casos': casos, 'ejemplo': json_java(ejemplo(a)), 'recorta': bool(v.get('recortar')),
            'opcional_con_defecto': a.get('obligatorio', False) and 'por_defecto' in a,
            'setter': 'set' + pascal(a['nombre'])}


def _importaciones_solicitud(entidades, e, campos):
    importaciones = {'io.swagger.v3.oas.annotations.media.Schema'}
    for c in campos:
        a = c['_atributo']
        importaciones |= importaciones_tipo(entidades, c['_entidad'], a, paquete(e))
        for anotacion in c['anotaciones']:
            nombre = anotacion[1:anotacion.index('(')]
            importaciones.add(f'co.edu.usco.inspectia.comun.validacion.{nombre}' if nombre == 'MaximoBytes'
                              else f'jakarta.validation.constraints.{nombre}')
        if c['normalizacion']:
            importaciones.add('co.edu.usco.inspectia.comun.texto.Textos')
    return importaciones


def _campos(entidades, e, atributos, prefijo=''):
    campos = []
    for a in atributos:
        if a['tipo'] == 'contrasena':
            entrada = a['entrada']
            nombre = camel(entrada['nombre'])
            c = campo_solicitud(e, a, prefijo + nombre, nombre, entrada['obligatoria_al_crear'])
        else:
            c = campo_solicitud(e, a, prefijo + camel(a['nombre']))
        c['tipo'] = tipo_java(entidades, e, a)
        c['_atributo'], c['_entidad'] = a, e
        campos.append(c)
    return campos


def _publicos(campos):
    """Los campos sin las claves internas (``_atributo`` y ``_entidad``), para las plantillas."""
    return [{k: v for k, v in c.items() if not k.startswith('_')} for c in campos]


def _cuando(operaciones):
    partes = [f'al {o}' for o in operaciones]
    return partes[0] if len(partes) == 1 else ', '.join(partes[:-1]) + ' y ' + partes[-1]


def modelo_api(entidades, e):
    """Todo lo que necesitan las plantillas de la API de una entidad."""
    verificar_api(entidades, e)
    n, clase, p = e['entidad'], e['clase'], paquete(e)
    operaciones = {o: e['operaciones'][o] for o in OPERACIONES_API if o in e['operaciones']}
    formulario = [a for a in e['atributos'] if a.get('en_formulario')]
    campos = _campos(entidades, e, formulario)
    singular = e['etiquetas']['es']['singular'].lower()

    crear_con = None
    if 'crear_con' in e:
        cc = e['crear_con']
        otra = entidades[cc['entidad']]
        campos_cc = _campos(entidades, otra, [atributo(otra, nombre) for nombre in cc['atributos']],
                            prefijo=camel(cc['nombre']) + '.')
        contrasena = next((c for c in campos_cc if c['tipo_atributo'] == 'contrasena'), None)
        fijos = [{'setter': 'set' + pascal(c), 'valor': literal(entidades, otra, atributo(otra, c), valor)}
                 for c, valor in cc.get('valores', {}).items()]
        consultas = [c for c in consultas_unicas(otra) if c['atributo'] in cc['atributos']]
        for consulta in consultas:
            campo = next(c for c in campos_cc if c['atributo'] == consulta['atributo'])
            consulta['valor'] = f"solicitud.{camel(cc['nombre'])}().{campo['nombre']}()"
            consulta['campo_json'] = campo['campo_json']
        crear_con = {
            'nombre': camel(cc['nombre']), 'etiqueta': cc['etiquetas']['es'],
            'clase_solicitud': pascal(cc['nombre']) + 'Solicitud', 'metodo': camel(cc['nombre']) + 'DeSolicitud',
            'entidad': otra['entidad'], 'clase': otra['clase'], 'paquete': paquete(otra),
            'repositorio': otra['clase'] + 'Repositorio', 'repositorio_var': var(otra) + 'Repositorio',
            'campos': _publicos(campos_cc), 'campos_internos': campos_cc,
            'contrasena': contrasena and contrasena['nombre'],
            'contrasena_setter': contrasena and contrasena['setter'],
            'fijos': fijos, 'con_empresa': tiene_empresa(otra), 'consultas': consultas,
            'fichas': cc['fichas']}

    iniciales = []
    if n == 'empresa':
        iniciales = [{'clase': o['clase'] + 'ValoresIniciales', 'paquete': paquete(o), 'entidad': o['entidad'],
                      'repositorio': o['clase'] + 'Repositorio', 'repositorio_var': var(o) + 'Repositorio'}
                     for o in entidades.values() if o.get('valores_iniciales', {}).get('modo') == 'al_crear_empresa']

    guardados = ([f"su {crear_con['etiqueta'].lower()} ({crear_con['entidad']})"] if crear_con else []) + [
        f"los valores por defecto de {i['entidad']}" for i in iniciales]
    al_crear = 'Guarda el registro nuevo.' + (
        f" En la misma transacción guarda también {' y '.join(guardados)}." if guardados else '')

    reglas = [{'codigo': r['codigo'], 'metodo': 'cumple' + pascal(r['codigo']), 'descripcion': r['descripcion'],
               'cuando': [o for o in OPERACIONES_API if o in r.get('cuando', [])],
               'cuando_texto': _cuando([o for o in OPERACIONES_API if o in r.get('cuando', [])]),
               'campo': camel(r['atributo']) if r.get('atributo') else None,
               'clave': f"{n}.reglas.{r['codigo']}", 'fichas': r['fichas']}
              for r in e.get('reglas', []) if r['tipo'] == 'validacion']
    reglas_por_operacion = {o: [r for r in reglas if o in r['cuando']] for o in operaciones}

    activo = None
    if 'atributo_activo' in e:
        activo = {'setter': 'set' + pascal(e['atributo_activo']), 'campo': camel(e['atributo_activo'])}

    nombres = [a['nombre'] for a in e['atributos']]
    orden = ('Sort.by(Sort.Order.asc("nombre").ignoreCase(), Sort.Order.asc("id"))' if 'nombre' in nombres
             else 'Sort.by(Sort.Order.asc("id"))')
    orden_texto = 'por nombre, sin distinguir mayúsculas' if 'nombre' in nombres else 'por antigüedad'

    ruta_api = f"/api/v1/{e['ruta']}"
    rutas = []
    for o, roles in operaciones.items():
        metodo, sufijo = RUTAS[o]
        quienes = ', '.join(f'"{r}"' for r in roles)
        # En la prueba generada, «registro» es la ruta de un registro que no existe.
        if sufijo.startswith('/{id}'):
            resto = sufijo[len('/{id}'):]
            uri_prueba = f'registro + "{resto}"' if resto else 'registro'
        else:
            uri_prueba = 'RUTA'
        rutas.append({'operacion': o, 'metodo': metodo, 'ruta': ruta_api + sufijo,
                      'regla': f'hasRole({quienes})' if len(roles) == 1 else f'hasAnyRole({quienes})',
                      'preautorizar': (f"hasRole('{roles[0]}')" if len(roles) == 1
                                       else 'hasAnyRole(' + ', '.join(f"'{r}'" for r in roles) + ')'),
                      'roles': roles, 'verbo': VERBOS[o], 'uri_prueba': uri_prueba,
                      'datos_prueba': {'crear': 'datosParaCrear()', 'editar': 'datosParaEditar()'}.get(o, 'null')})

    respuesta = [{'tipo': 'UUID', 'nombre': 'id', 'esquema': 'description = "Identificador (UUID v7)."',
                  'descripcion': 'identificador (UUID v7)'}]
    for a in e['atributos']:
        if a['tipo'] != 'contrasena':
            esquema = [f'description = {texto_java(a["etiquetas"]["es"])}']
            if ejemplo(a) is not None:
                esquema.append(f'example = {texto_java(ejemplo(a))}')
            respuesta.append({'tipo': tipo_java(entidades, e, a), 'nombre': camel(a['nombre']),
                              'getter': 'get' + pascal(a['nombre']), 'esquema': ', '.join(esquema),
                              'descripcion': a['etiquetas']['es']})
    respuesta += [{'tipo': 'OffsetDateTime', 'nombre': 'fechaCreacion', 'getter': 'getFechaCreacion',
                   'esquema': 'description = "Fecha de creación del registro."',
                   'descripcion': 'fecha de creación del registro'},
                  {'tipo': 'OffsetDateTime', 'nombre': 'fechaActualizacion', 'getter': 'getFechaActualizacion',
                   'esquema': 'description = "Fecha de la última actualización."',
                   'descripcion': 'fecha de la última actualización'}]
    respuesta[0]['getter'] = 'getId'

    return {'entidad': n, 'clase': clase, 'paquete': p, 'var': var(e), 'singular': singular,
            'plural': e['etiquetas']['es']['plural'].lower(), 'descripcion': e['descripcion'],
            'fichas': e['fichas'], 'operaciones': operaciones, 'rutas': rutas, 'ruta_api': ruta_api,
            'campos': _publicos(campos), 'campos_internos': campos, 'crear_con': crear_con,
            'iniciales': iniciales, 'al_crear': al_crear, 'reglas': reglas, 'reglas_por_operacion': reglas_por_operacion,
            'consultas': consultas_unicas(e), 'activo': activo, 'orden': orden, 'orden_texto': orden_texto,
            'respuesta': respuesta,
            'atributos_entidad': [{'setter': 'set' + pascal(a['nombre']), 'getter': 'get' + pascal(a['nombre'])}
                                  for a in e['atributos']],
            'con_empresa': tiene_empresa(e)}


def archivos_api(entidades, e):
    """Archivos de la API de una entidad del alcance: ``(ruta, plantilla, contexto)``."""
    m = modelo_api(entidades, e)
    clase, p = m['clase'], m['paquete']
    involucradas = [e] + ([entidades[m['crear_con']['entidad']]] if m['crear_con'] else []) + [
        o for o in entidades.values() if m['iniciales'] and 'valores_iniciales' in o]
    involucradas = list({x['entidad']: x for x in involucradas}.values())

    def archivo(nombre, plantilla, descripcion, prueba=False, **extra):
        return (ruta(e, nombre, prueba), plantilla,
                dict(m, **extra, header=_header(entidades, involucradas, f'{nombre}.java', descripcion)))

    archivos = []
    if m['reglas']:
        archivos.append(archivo(f'Reglas{clase}', 'java/main/reglas.java.j2',
                                f"Reglas de {m['entidad']} que se programan a mano ({m['fichas'][0]})."))
    if 'crear' in m['operaciones']:
        archivos.append(archivo(f'{clase}CrearSolicitud', 'java/main/solicitud.java.j2',
                                f'Datos para registrar {m["singular"]} por la API.',
                                solicitud={'clase': f'{clase}CrearSolicitud', 'campos': m['campos'],
                                           'crear_con': m['crear_con'],
                                           'descripcion': f'Datos para registrar {m["singular"]}.',
                                           'importaciones': sorted(_importaciones_solicitud(
                                               entidades, e, m['campos_internos']) | (
                                               {'jakarta.validation.Valid', 'jakarta.validation.constraints.NotNull'}
                                               if m['crear_con'] else set()))}))
        if m['crear_con']:
            cc = m['crear_con']
            archivos.append(archivo(cc['clase_solicitud'], 'java/main/solicitud.java.j2',
                                    f"Datos del {cc['etiqueta'].lower()} al registrar {m['singular']}.",
                                    solicitud={'clase': cc['clase_solicitud'], 'campos': cc['campos'],
                                               'crear_con': None,
                                               'descripcion': f"{cc['etiqueta']}, que se crea junto con "
                                                              f"{m['singular']} ({', '.join(cc['fichas'])}).",
                                               'importaciones': sorted(_importaciones_solicitud(
                                                   entidades, e, cc['campos_internos']))}))
    if 'editar' in m['operaciones']:
        archivos.append(archivo(f'{clase}EditarSolicitud', 'java/main/solicitud.java.j2',
                                f'Datos para editar {m["singular"]} por la API.',
                                solicitud={'clase': f'{clase}EditarSolicitud', 'campos': m['campos'],
                                           'crear_con': None, 'descripcion': f'Datos para editar {m["singular"]}.',
                                           'importaciones': sorted(_importaciones_solicitud(
                                               entidades, e, m['campos_internos']))}))
    archivos.append(archivo(f'{clase}Respuesta', 'java/main/respuesta.java.j2',
                            f'{m["singular"].capitalize()} como la responde la API.',
                            importaciones=sorted(_importaciones_respuesta(entidades, e))))
    archivos.append(archivo(f'{clase}Mapeador', 'java/main/mapeador.java.j2',
                            f'Conversión entre {m["entidad"]} y sus solicitudes y respuesta.',
                            importaciones=sorted(_importaciones_mapeador(m))))
    archivos.append(archivo(f'{clase}Servicio', 'java/main/servicio.java.j2',
                            f'Operaciones de {m["entidad"]}: validan, guardan y responden ({m["fichas"][0]}).',
                            importaciones=sorted(_importaciones_servicio(m))))
    archivos.append(archivo(f'{clase}Controlador', 'java/main/controlador.java.j2',
                            f'API REST de {m["entidad"]} en {m["ruta_api"]} ({m["fichas"][0]}).',
                            importaciones=sorted(_importaciones_controlador(m))))
    archivos.append(archivo(f'{clase}Acceso', 'java/main/acceso.java.j2',
                            f'Roles que pueden usar cada operación de la API de {m["entidad"]} (RNF-04).'))
    archivos.append(archivo(f'{clase}ApiGeneradaIT', 'java/test/api_generada.java.j2',
                            f'Pruebas generadas de la API de {m["entidad"]}: permisos, validaciones y datos únicos.',
                            prueba=True, prueba_api=_modelo_prueba(entidades, e, m)))
    return archivos


def _importaciones_respuesta(entidades, e):
    importaciones = {'io.swagger.v3.oas.annotations.media.Schema', 'java.time.OffsetDateTime', 'java.util.UUID'}
    for a in e['atributos']:
        if a['tipo'] != 'contrasena':
            importaciones |= importaciones_tipo(entidades, e, a, paquete(e))
    return importaciones


def _importaciones_mapeador(m):
    importaciones = set()
    if m['crear_con']:
        importaciones.add('java.util.UUID')
        if m['crear_con']['paquete'] != m['paquete']:
            importaciones.add(f"{m['crear_con']['paquete']}.{m['crear_con']['clase']}")
        for fijo in m['crear_con']['fijos']:
            clase_fijo = fijo['valor'].split('.')[0]
            if clase_fijo[0].isupper():
                importaciones.add(f"{m['crear_con']['paquete']}.{clase_fijo}")
    return importaciones


def _importaciones_servicio(m):
    importaciones = {'co.edu.usco.inspectia.comun.api.Pagina',
                     'co.edu.usco.inspectia.comun.errores.NoEncontradoExcepcion',
                     'java.util.UUID', 'lombok.RequiredArgsConstructor',
                     'org.springframework.data.domain.PageRequest', 'org.springframework.data.domain.Sort',
                     'org.springframework.stereotype.Service',
                     'org.springframework.transaction.annotation.Transactional'}
    if m['reglas'] or m['consultas'] or (m['crear_con'] and m['crear_con']['consultas']):
        importaciones |= {'co.edu.usco.inspectia.comun.errores.CampoInvalido', 'java.util.ArrayList',
                          'java.util.List'}
    if m['reglas']:
        importaciones.add('co.edu.usco.inspectia.comun.errores.ReglaIncumplidaExcepcion')
    if m['consultas'] or (m['crear_con'] and m['crear_con']['consultas']):
        importaciones.add('co.edu.usco.inspectia.comun.errores.DatoRepetidoExcepcion')
    if m['crear_con']:
        cc = m['crear_con']
        if cc['paquete'] != m['paquete']:
            importaciones.add(f"{cc['paquete']}.{cc['repositorio']}")
        if cc['contrasena']:
            importaciones.add('org.springframework.security.crypto.password.PasswordEncoder')
    for inicial in m['iniciales']:
        if inicial['paquete'] != m['paquete']:
            importaciones |= {f"{inicial['paquete']}.{inicial['repositorio']}",
                              f"{inicial['paquete']}.{inicial['clase']}"}
    return importaciones


def _importaciones_controlador(m):
    importaciones = {'io.swagger.v3.oas.annotations.Operation', 'io.swagger.v3.oas.annotations.tags.Tag',
                     'lombok.RequiredArgsConstructor', 'org.springframework.security.access.prepost.PreAuthorize',
                     'org.springframework.web.bind.annotation.RequestMapping',
                     'org.springframework.web.bind.annotation.RestController'}
    operaciones = m['operaciones']
    if 'listar' in operaciones:
        importaciones |= {'co.edu.usco.inspectia.comun.api.Pagina', 'io.swagger.v3.oas.annotations.Parameter',
                          'jakarta.validation.constraints.Max', 'jakarta.validation.constraints.Min',
                          'org.springframework.web.bind.annotation.GetMapping',
                          'org.springframework.web.bind.annotation.RequestParam'}
    if {'ver', 'editar', 'desactivar', 'reactivar'} & set(operaciones):
        importaciones |= {'java.util.UUID', 'org.springframework.web.bind.annotation.PathVariable'}
    if 'ver' in operaciones:
        importaciones.add('org.springframework.web.bind.annotation.GetMapping')
    if {'crear', 'desactivar', 'reactivar'} & set(operaciones):
        importaciones.add('org.springframework.web.bind.annotation.PostMapping')
    if 'crear' in operaciones:
        importaciones |= {'jakarta.validation.Valid', 'java.net.URI', 'org.springframework.http.ResponseEntity',
                          'org.springframework.web.bind.annotation.RequestBody',
                          'org.springframework.web.util.UriComponentsBuilder'}
    if 'editar' in operaciones:
        importaciones |= {'jakarta.validation.Valid', 'org.springframework.web.bind.annotation.PutMapping',
                          'org.springframework.web.bind.annotation.RequestBody'}
    return importaciones


def _modelo_prueba(entidades, e, m):
    """Datos de la prueba generada: datos válidos, permisos, validaciones y datos únicos."""
    roles = [v['codigo'] for v in atributo(entidades['usuario'], 'rol')['valores']]
    crear = m['campos_internos']
    validaciones_crear = [(c['campo_json'], valor, mensaje) for c in crear for valor, mensaje in c['casos']]
    cc = m['crear_con']
    if cc:
        validaciones_crear += [(c['campo_json'], valor, mensaje)
                               for c in cc['campos_internos'] for valor, mensaje in c['casos']]
    unicos = [(c['campo'], c['mensaje']) for c in m['consultas']]
    if cc:
        unicos += [(c['campo_json'], c['mensaje']) for c in cc['consultas']]
    tablas = [m['entidad']] + ([cc['entidad']] if cc else []) + [
        o['entidad'] for o in entidades.values() if m['iniciales'] and 'valores_iniciales' in o]
    return {
        'roles': roles,
        'permisos': [(r['operacion'], rol, rol in r['roles']) for r in m['rutas'] for rol in roles],
        'datos_crear': [(c['campo_json'], c['ejemplo']) for c in crear],
        'datos_crear_con': [(c['campo_json'].split('.', 1)[1], c['ejemplo']) for c in cc['campos_internos']] if cc else [],
        'datos_editar': [(c['campo_json'], c['ejemplo']) for c in crear],
        'validaciones_crear': validaciones_crear,
        'validaciones_editar': [(c['campo_json'], valor, mensaje) for c in crear for valor, mensaje in c['casos']],
        'unicos': unicos,
        'normalizados': [(c['campo_json'], c['ejemplo']) for c in crear if c['recorta'] and c['tipo'] == 'String'],
        'usa_correo_largo': any('correoDeLargo' in valor for _, valor, _ in validaciones_crear),
        'tablas': ', '.join(dict.fromkeys(tablas)),
        'ficha': m['fichas'][0],
    }


# --- Header -------------------------------------------------------------------------------------


def _header(entidades, involucradas, nombre_archivo, descripcion):
    datos = datos_header({x['entidad']: x for x in involucradas})
    return dict(datos, nombre=nombre_archivo, descripcion=descripcion,
                origen=', '.join(f"specs/entidades/{x['entidad']}.json" for x in involucradas))
