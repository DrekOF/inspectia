# Nombre: java.py
# Descripción: Arma los datos de las plantillas de Java: la persistencia de cada CRUD y, para las
#              entidades de alcance.json, su API y sus pruebas (RP-23).
# Fecha de creación: 13/10/2026
# Actualización: 26/10/2026
# Autor: Equipo InspectIA
# Versión: 1.7
"""Datos de las plantillas de Java.

- De cada entidad con ``"crud": true`` sale su persistencia: entidad JPA, repositorio, un ``enum``
  por lista y, si tiene ``valores_iniciales``, la clase que los arma.
- De las entidades de ``alcance.json`` sale además la API: reglas, servicio, controlador, reglas de
  acceso por ruta, solicitudes, respuesta, mapeador y una prueba de integración. Las validaciones de
  las solicitudes y sus casos de límite salen de ``campos.py``, el mismo modelo de Yup en el frontend.
- Si la entidad se desactiva, su listado se filtra con ``?activo=true`` o ``false`` (D-33).
- En una entidad por empresa, la empresa sale de la sesión y cada consulta se limita a ella (RNF-03).
- Una entidad con ``edicion_en_bloque`` tiene una fila por cada valor de una lista en cada empresa, como
  un rango por cada categoría (RF-13): su API lista las filas, las edita todas juntas y restaura sus
  valores por defecto, sin crear ni desactivar.

La API todavía no soporta todo el formato. Si una entidad del alcance usa algo que falta (por
ejemplo, una referencia en el formulario), la generación se detiene y dice qué.
"""

import re
from decimal import Decimal

from inspectia_generador import campos
from inspectia_generador.encabezado import datos_header
from inspectia_generador.modelo import atributo, clave_en_bloque, tiene_empresa, valores_lista
from inspectia_generador.nombres import PAQUETE_BASE, camel, en_frase, pascal, texto_java
from inspectia_generador.sql import ErrorGeneracion
from inspectia_generador.textos import (clave_mensaje, clave_unico, mensaje_esperado, mensaje_filas_completas,
                                        mensaje_unico)

CARPETA_MAIN = 'backend/src/generated/java'
CARPETA_PRUEBAS = 'backend/src/test-generated/java'

TIPOS_JAVA = {'texto': 'String', 'contrasena': 'String', 'entero': 'Integer', 'decimal': 'BigDecimal',
              'booleano': 'Boolean', 'fecha': 'LocalDate', 'fecha_hora': 'OffsetDateTime',
              'referencia': 'UUID'}
IMPORTACION_DE_TIPO = {'BigDecimal': 'java.math.BigDecimal', 'LocalDate': 'java.time.LocalDate',
                       'OffsetDateTime': 'java.time.OffsetDateTime', 'UUID': 'java.util.UUID'}

#: El usuario de la sesión: en una entidad por empresa, de él sale la empresa (RNF-03).
SESION = 'co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion'

#: Operaciones que genera la API, en el orden del controlador.
OPERACIONES_API = ('listar', 'ver', 'crear', 'editar', 'desactivar', 'reactivar')
#: Operaciones que cambian el registro: en ellas puede pasar un efecto.
OPERACIONES_CON_CAMBIOS = ('crear', 'editar', 'desactivar', 'reactivar')
#: Operaciones de una entidad con edición en bloque, en el orden del controlador.
OPERACIONES_EN_BLOQUE = ('listar', 'editar', 'restaurar_por_defecto')
#: Verbo de cada operación en la documentación y en los nombres de las reglas.
VERBOS = {'listar': 'Listar', 'ver': 'Ver', 'crear': 'Crear', 'editar': 'Editar',
          'desactivar': 'Desactivar', 'reactivar': 'Reactivar', 'restaurar_por_defecto': 'Restaurar'}
#: Ruta y método HTTP de cada operación: ``{id}`` es el identificador del registro.
RUTAS = {'listar': ('GET', ''), 'ver': ('GET', '/{id}'), 'crear': ('POST', ''), 'editar': ('PUT', '/{id}'),
         'desactivar': ('POST', '/{id}/desactivar'), 'reactivar': ('POST', '/{id}/reactivar')}
#: En la edición en bloque no hay rutas por fila: se editan todas las de la empresa a la vez.
RUTAS_EN_BLOQUE = {'listar': ('GET', ''), 'editar': ('PUT', ''),
                   'restaurar_por_defecto': ('POST', '/restaurar-por-defecto')}


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
        if 'valores_en_formulario' in a:
            return a['valores_en_formulario'][0]
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


def ejemplo_openapi(a):
    """El ``example`` de OpenAPI de un atributo, como texto de Java, o ``None`` si no tiene ejemplo.

    El valor va como se escribe en JSON: un booleano es ``true`` o ``false``, no ``True`` como en Python.
    """
    valor = ejemplo(a)
    if valor is None:
        return None
    if isinstance(valor, bool):
        valor = 'true' if valor else 'false'
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


def filtro_por_activo(e):
    """El filtro ``?activo=`` del listado de una entidad que se desactiva (D-33), o ``None`` si no lo tiene.

    Returns:
        dict: ``campo``, el atributo activo en Java, y ``consulta``, el método del repositorio que lista los
        activos o los desactivados; en una entidad por empresa, solo los de la empresa de la sesión.
    """
    if 'atributo_activo' not in e or 'listar' not in e.get('operaciones', {}):
        return None
    nombre = e['atributo_activo']
    prefijo = 'findAllByEmpresaIdAnd' if tiene_empresa(e) else 'findAllBy'
    return {'campo': camel(nombre), 'consulta': prefijo + pascal(nombre)}


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
            'etiqueta': en_frase(a['etiquetas']['es']), 'clave': clave_unico(e, [a['nombre']]),
            'mensaje': mensaje_unico(e, [a['nombre']])}


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
    # En bloque, las filas de la empresa se leen todas juntas, y para editarlas se bloquean.
    en_bloque = bool(e.get('edicion_en_bloque')) and tiene_empresa(e)
    activo = None if en_bloque else filtro_por_activo(e)
    importaciones = {'java.util.UUID', 'org.springframework.data.jpa.repository.JpaRepository'}
    if consultas or en_bloque:
        importaciones |= {'org.springframework.data.jpa.repository.Query',
                          'org.springframework.data.repository.query.Param'}
    if en_bloque:
        importaciones |= {'jakarta.persistence.LockModeType', 'java.util.List',
                          'org.springframework.data.jpa.repository.Lock',
                          'org.springframework.transaction.annotation.Propagation',
                          'org.springframework.transaction.annotation.Transactional'}
    elif tiene_empresa(e):
        importaciones |= {'java.util.Optional', 'org.springframework.data.domain.Page',
                          'org.springframework.data.domain.Pageable'}
    if activo:
        importaciones |= {'org.springframework.data.domain.Page', 'org.springframework.data.domain.Pageable'}
    for c in consultas:
        importaciones |= importaciones_tipo(entidades, e, atributo(e, c['atributo']), p)
    archivos.append((ruta(e, e['clase'] + 'Repositorio'), 'java/main/repositorio.java.j2', {
        'paquete': p, 'clase': e['clase'], 'entidad': e['entidad'], 'importaciones': sorted(importaciones),
        'con_empresa': tiene_empresa(e), 'en_bloque': en_bloque, 'activo': activo,
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
    en_bloque = bool(e.get('edicion_en_bloque'))
    clave = clave_en_bloque(e) if en_bloque else None
    if not e['crud']:
        problemas.append('no tiene CRUD')
    for operacion, roles in operaciones.items():
        if en_bloque and operacion not in OPERACIONES_EN_BLOQUE:
            problemas.append(f'edicion_en_bloque con la operación {operacion}')
        elif not en_bloque and operacion == 'restaurar_por_defecto':
            problemas.append('restaurar_por_defecto sin edicion_en_bloque')
        elif not en_bloque and operacion not in OPERACIONES_API:
            problemas.append(f'la operación {operacion}')
        if tiene_empresa(e) and 'ADMIN_PLATAFORMA' in roles:
            problemas.append(f'la operación {operacion} da permiso a ADMIN_PLATAFORMA, que no tiene empresa: '
                             'una entidad por empresa solo la usan roles con empresa (RNF-03)')
    if 'restaurar_por_defecto' in operaciones and 'valores_iniciales' not in e:
        problemas.append('restaurar_por_defecto sin valores_iniciales')
    if en_bloque:
        problemas += _problemas_en_bloque(entidades, e, clave)
    if {'desactivar', 'reactivar'} & set(operaciones) and 'atributo_activo' not in e:
        problemas.append('desactivar o reactivar sin atributo_activo')
    for a in e['atributos']:
        if a.get('en_formulario'):
            problemas += _problemas_de_campo(a, a['nombre'], en_bloque)
        # La lista que identifica cada fila de una edición en bloque no se edita, pero va en cada fila.
        if not a.get('en_formulario') and a.get('obligatorio') and 'por_defecto' not in a and a is not clave:
            problemas.append(f"{a['nombre']} es obligatorio, pero no va en el formulario ni tiene por_defecto")
        if a.get('en_formulario') and a['tipo'] == 'referencia':
            problemas.append(f"{a['nombre']}, de tipo {a['tipo']}, en el formulario")
    for r in e.get('reglas', []):
        # Las validaciones y los efectos se programan a mano y el código generado los llama. Las acciones
        # son operaciones extra que también se programan a mano: el generador no las genera.
        a = atributo(e, r['atributo']) if r.get('atributo') else None
        if a is not None and a['tipo'] == 'contrasena' and r.get('cuando') != ['editar']:
            problemas.append(f"la regla {r['codigo']}: la de una contraseña solo se revisa al editar, cuando llega "
                             'una nueva')
        if r['tipo'] == 'efecto':
            sin_cambios = [o for o in r.get('cuando', []) if o not in OPERACIONES_CON_CAMBIOS]
            if sin_cambios:
                problemas.append(f"el efecto {r['codigo']} en {', '.join(sin_cambios)}, que no cambia el registro")
            if a is not None and a['tipo'] != 'contrasena':
                problemas.append(f"el efecto {r['codigo']} depende de {a['nombre']}: solo se sabe llamar cuando llega "
                                 'una contraseña nueva')
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


def _problemas_en_bloque(entidades, e, clave):
    """Lo que necesita la edición en bloque: en cada empresa, una fila por cada valor de una lista, que se
    crean con la empresa (``valores_iniciales``) y que solo se editan todas juntas.

    Args:
        clave: la lista que identifica cada fila (``clave_en_bloque``), o ``None`` si no la hay.
    """
    problemas = []
    if 'editar' not in e.get('operaciones', {}):
        problemas.append('edicion_en_bloque sin la operación editar')
    if e.get('por_empresa') != 'si':
        problemas.append('edicion_en_bloque en una entidad que no es por empresa')
    if clave is None:
        problemas.append('edicion_en_bloque sin una lista que, con la empresa, identifique cada fila: '
                         'un único de empresa_id y una lista con sus valores')
    elif clave.get('en_formulario'):
        problemas.append(f"{clave['nombre']} identifica cada fila: no puede ir en el formulario")
    if 'valores_iniciales' not in e:
        problemas.append('edicion_en_bloque sin valores_iniciales, que son las filas de cada empresa nueva')
    elif clave is not None:
        codigos = [f.get(clave['nombre']) for f in e['valores_iniciales'].get('filas', [])]
        if len(codigos) != len(set(codigos)) or set(codigos) != set(valores_lista(entidades, clave)):
            problemas.append(f"valores_iniciales no tiene una fila por cada valor de {clave['nombre']}")
    for a in e['atributos']:
        condicion = a.get('solo_lectura_si')
        if condicion is None:
            continue
        if clave is not None and condicion.get('atributo') != clave['nombre']:
            problemas.append(f"solo_lectura_si de {a['nombre']} depende de {condicion.get('atributo')}, que no "
                             'identifica cada fila')
        if a['tipo'] not in ('entero', 'decimal'):
            problemas.append(f"solo_lectura_si en {a['nombre']}, de tipo {a['tipo']}: solo se soporta en números")
        if not a.get('en_formulario'):
            problemas.append(f"solo_lectura_si en {a['nombre']}, que no va en el formulario")
    for r in e.get('reglas', []):
        if r['tipo'] == 'efecto':
            problemas.append(f"edicion_en_bloque con el efecto {r['codigo']}")
        elif r['tipo'] == 'validacion' and r.get('atributo'):
            problemas.append(f"la regla {r['codigo']} depende de {r['atributo']}: en la edición en bloque, las "
                             'reglas revisan todas las filas juntas')
        elif r['tipo'] == 'validacion' and r.get('cuando') != ['editar']:
            problemas.append(f"la regla {r['codigo']} fuera de editar: en la edición en bloque solo se revisa al "
                             'editar')
    return problemas


def _problemas_de_campo(a, donde, en_bloque=False):
    problemas = []
    if 'solo_lectura_si' in a and not en_bloque:
        problemas.append(f'solo_lectura_si en {donde}')
    v = a.get('validaciones', {})
    if 'no_posterior_a_hoy' in v:
        problemas.append(f'no_posterior_a_hoy en {donde}')
    if 'patron' in v and 'ejemplo' not in a:
        problemas.append(f'{donde} tiene patron pero no ejemplo')
    return problemas


def anotacion(validacion, a):
    """La anotación de Bean Validation de una validación de ``campos.del_atributo``, con la restricción que dice
    el modelo, o ``None`` si Java no la necesita: un código que no existe en una lista ya lo rechaza Jackson al
    leer la petición."""
    x = validacion
    if x.restriccion is None:
        return None
    mensaje = f'message = "{{{x.clave}}}"'
    if x.tipo in ('obligatorio', 'no_en_blanco', 'correo'):
        return f'@{x.restriccion}({mensaje})'
    if x.tipo == 'valor_permitido':
        valores = ', '.join(f'"{codigo}"' for codigo in x.valor)
        return f'@{x.restriccion}(valores = {{{valores}}}, {mensaje})'
    if x.tipo in ('longitud_max', 'longitud_min'):
        return f"@{x.restriccion}({'max' if x.tipo == 'longitud_max' else 'min'} = {x.valor}, {mensaje})"
    if x.tipo == 'patron':
        return f'@{x.restriccion}(regexp = {texto_java(x.valor)}, {mensaje})'
    if x.tipo in ('minimo', 'maximo') and a['tipo'] == 'entero':
        return f'@{x.restriccion}(value = {int(x.valor)}, {mensaje})'
    if x.tipo in ('minimo', 'maximo'):
        return f'@{x.restriccion}(value = "{x.valor}", {mensaje})'
    return f'@{x.restriccion}(value = {x.valor}, {mensaje})'


def valor_java(valor):
    """Un valor de un caso de límite (``campos.Valor``) como expresión de Java para la prueba generada."""
    if valor.tipo == 'falta':
        return 'null'
    if valor.tipo == 'repetir':
        return f'{texto_java(valor.texto)}.repeat({valor.veces})'
    if valor.tipo == 'texto':
        return texto_java(valor.texto)
    if valor.tipo == 'numero':
        return valor.texto
    return f'correoDeLargo({valor.veces})'


def campo_solicitud(e, a, campo_json, nombre=None, obligatorio=None):
    """Un campo de una solicitud: tipo, anotaciones, normalización y casos de prueba de validación.

    Las validaciones y sus casos de límite salen de ``campos.del_atributo``, el mismo modelo de Yup en el
    frontend.

    Args:
        e: entidad dueña del atributo (sus mensajes son los que responde la API).
        campo_json: nombre del campo en los errores, como ``administrador.correo``.
        nombre: nombre del campo en Java, si no es el del atributo (la entrada de una contraseña).
        obligatorio: si es obligatorio, cuando no lo dice ``obligatorio`` del atributo.
    """
    nombre = nombre or camel(a['nombre'])
    datos = campos.del_atributo(e, a, obligatorio)
    requerido = any(x.tipo == 'obligatorio' for x in datos.validaciones)
    anotaciones = [texto for texto in (anotacion(x, a) for x in datos.validaciones) if texto]

    normalizacion = nombre
    if datos.recortar:
        normalizacion = f'Textos.recortar({normalizacion})'
    if datos.minusculas:
        normalizacion = f'Textos.minusculas({normalizacion})'
    esquema = [f'description = {texto_java(a["etiquetas"]["es"])}']
    if a['tipo'] != 'contrasena' and ejemplo_openapi(a) is not None:
        esquema.append(f'example = {ejemplo_openapi(a)}')
    if requerido:
        esquema.append('requiredMode = Schema.RequiredMode.REQUIRED')
    return {'nombre': nombre, 'campo_json': campo_json, 'atributo': a['nombre'], 'tipo_atributo': a['tipo'],
            'etiqueta': a['etiquetas']['es'],
            'esquema': ', '.join(esquema), 'anotaciones': anotaciones,
            'normalizacion': None if normalizacion == nombre else normalizacion,
            'casos': [(valor_java(c.valor), c.mensaje) for c in datos.casos],
            'bordes': [valor_java(valor) for valor in datos.bordes],
            'ejemplo': json_java(ejemplo(a)), 'recorta': datos.recortar,
            'opcional_con_defecto': a.get('obligatorio', False) and 'por_defecto' in a,
            'setter': 'set' + pascal(a['nombre']),
            # Lo que el mapeador copia al registro: un decimal, con la escala de su columna, como lo guarda la
            # base; así la respuesta de una edición lo dice igual que el listado (90.50 queda 90.5).
            'desde_solicitud': (f"Decimales.conEscala(solicitud.{nombre}(), {a['escala']})" if a['tipo'] == 'decimal'
                                else f'solicitud.{nombre}()')}


def _importaciones_solicitud(entidades, e, campos):
    importaciones = {'io.swagger.v3.oas.annotations.media.Schema'}
    for c in campos:
        a = c['_atributo']
        importaciones |= importaciones_tipo(entidades, c['_entidad'], a, paquete(e))
        for anotacion in c['anotaciones']:
            nombre = anotacion[1:anotacion.index('(')]
            importaciones.add(f'co.edu.usco.inspectia.comun.validacion.{nombre}'
                              if nombre in ('MaximoBytes', 'MaximoDecimales', 'NoEnBlanco', 'ValorPermitido')
                              else f'jakarta.validation.constraints.{nombre}')
        if c['normalizacion']:
            importaciones.add('co.edu.usco.inspectia.comun.texto.Textos')
    return importaciones


def _campos(entidades, e, atributos, prefijo='', al_editar=False):
    """Los campos de una solicitud. Al editar, la contraseña es obligatoria solo si su ``entrada`` lo dice."""
    campos = []
    for a in atributos:
        if a['tipo'] == 'contrasena':
            entrada = a['entrada']
            nombre = camel(entrada['nombre'])
            obligatoria = entrada['obligatoria_al_editar'] if al_editar else entrada['obligatoria_al_crear']
            c = campo_solicitud(e, a, prefijo + nombre, nombre, obligatoria)
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


def _de_la_respuesta(e):
    """Los atributos que van en la respuesta: los de la lista o del formulario, nunca una contraseña.

    Los demás son datos internos, como los intentos fallidos o el bloqueo de un usuario.
    """
    return [a for a in e['atributos']
            if a['tipo'] != 'contrasena' and (a.get('en_lista') or a.get('en_formulario'))]


def _constantes_de_reglas(e):
    """Los ``parametros`` de las reglas, como constantes de la interfaz que se programa a mano.

    Si la regla tiene un solo parámetro, la constante se llama como la regla; si tiene varios, como la
    regla y el parámetro. Así el valor vive solo en el JSON.
    """
    constantes = []
    for r in e.get('reglas', []):
        parametros = r.get('parametros', {})
        for nombre, valor in parametros.items():
            constante = r['codigo'] if len(parametros) == 1 else f"{r['codigo']}_{nombre}"
            if isinstance(valor, bool):
                tipo, java = 'boolean', 'true' if valor else 'false'
            elif isinstance(valor, int):
                tipo, java = 'int', str(valor)
            elif isinstance(valor, float):
                tipo, java = 'double', repr(valor)
            else:
                tipo, java = 'String', texto_java(valor)
            constantes.append({'nombre': constante.upper(), 'tipo': tipo, 'valor': java,
                               'descripcion': f"Parámetro {nombre} de la regla {r['codigo']}: {r['descripcion']}"})
    return constantes


def modelo_api(entidades, e):
    """Todo lo que necesitan las plantillas de la API de una entidad."""
    verificar_api(entidades, e)
    n, clase, p = e['entidad'], e['clase'], paquete(e)
    operaciones = {o: e['operaciones'][o] for o in OPERACIONES_API if o in e['operaciones']}
    formulario = [a for a in e['atributos'] if a.get('en_formulario')]
    campos = _campos(entidades, e, formulario)
    campos_editar = _campos(entidades, e, formulario, al_editar=True)
    singular = e['etiquetas']['es']['singular'].lower()
    con_empresa = tiene_empresa(e)
    # La contraseña del formulario no se copia al registro: el servicio guarda su hash BCrypt.
    campo_contrasena = next((c for c in campos if c['tipo_atributo'] == 'contrasena'), None)
    contrasena_formulario = campo_contrasena and {'nombre': campo_contrasena['nombre'],
                                                  'setter': campo_contrasena['setter']}

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

    reglas, efectos = [], []
    for r in e.get('reglas', []):
        a = atributo(e, r['atributo']) if r.get('atributo') else None
        de_contrasena = a is not None and a['tipo'] == 'contrasena'
        cuando = [o for o in OPERACIONES_API if o in r.get('cuando', [])]
        if r['tipo'] == 'validacion':
            reglas.append({
                'codigo': r['codigo'], 'metodo': 'cumple' + pascal(r['codigo']), 'descripcion': r['descripcion'],
                'cuando': cuando, 'cuando_texto': _cuando(cuando),
                # El error de una regla de la contraseña va en el campo del formulario, no en el del hash.
                'campo': (camel(a['entrada']['nombre']) if de_contrasena else camel(a['nombre'])) if a else None,
                'solo_con_contrasena': de_contrasena,
                'clave': f"{n}.reglas.{r['codigo']}", 'fichas': r['fichas']})
        elif r['tipo'] == 'efecto':
            # Un efecto cambia el registro antes de escribirlo en la base; el de una contraseña, solo si
            # llega una nueva y después de guardar su hash.
            efectos.append({
                'codigo': r['codigo'], 'metodo': camel(r['codigo']), 'descripcion': r['descripcion'],
                'cuando': cuando, 'cuando_texto': _cuando(cuando), 'solo_con_contrasena': de_contrasena,
                'fichas': r['fichas']})
    reglas_por_operacion = {o: [r for r in reglas if o in r['cuando']] for o in operaciones}
    efectos_por_operacion = {o: [f for f in efectos if o in f['cuando']] for o in operaciones}
    constantes = _constantes_de_reglas(e)

    # Lo que necesita el servicio. El constructor va escrito, no con Lombok: con el JDK 25, al recompilar
    # sin «clean», los constructores de Lombok quedan sin la llamada a super() (VerifyError).
    dependencias = [{'tipo': f'{clase}Repositorio', 'nombre': 'repositorio',
                     'descripcion': f'el repositorio de {{@code {n}}}'}]
    if reglas or efectos:
        dependencias.append({'tipo': f'Reglas{clase}', 'nombre': 'reglas',
                             'descripcion': 'las reglas que se programan a mano'})
    if crear_con:
        dependencias.append({'tipo': crear_con['repositorio'], 'nombre': crear_con['repositorio_var'],
                             'descripcion': f"el repositorio de {{@code {crear_con['entidad']}}}"})
        if crear_con['contrasena']:
            dependencias.append({'tipo': 'PasswordEncoder', 'nombre': 'codificador',
                                 'descripcion': 'el codificador BCrypt de las contraseñas'})
    if contrasena_formulario and not any(d['nombre'] == 'codificador' for d in dependencias):
        dependencias.append({'tipo': 'PasswordEncoder', 'nombre': 'codificador',
                             'descripcion': 'el codificador BCrypt de las contraseñas'})
    for i in iniciales:
        dependencias.append({'tipo': i['repositorio'], 'nombre': i['repositorio_var'],
                             'descripcion': f"el repositorio de {{@code {i['entidad']}}}"})

    activo = None
    if 'atributo_activo' in e:
        activo = {'setter': 'set' + pascal(e['atributo_activo']), 'campo': camel(e['atributo_activo']),
                  'consulta': (filtro_por_activo(e) or {}).get('consulta')}

    nombres = [a['nombre'] for a in e['atributos']]
    orden = ('Sort.by(Sort.Order.asc("nombre").ignoreCase(), Sort.Order.asc("id"))' if 'nombre' in nombres
             else 'Sort.by(Sort.Order.asc("id"))')
    orden_texto = 'por nombre, sin distinguir mayúsculas' if 'nombre' in nombres else 'por antigüedad'

    ruta_api = f"/api/v1/{e['ruta']}"
    rutas = []
    for o, roles in operaciones.items():
        metodo, sufijo = RUTAS[o]
        # En la prueba generada, «registro» es la ruta de un registro que no existe.
        if sufijo.startswith('/{id}'):
            resto = sufijo[len('/{id}'):]
            uri_prueba = f'registro + "{resto}"' if resto else 'registro'
        else:
            uri_prueba = 'RUTA'
        rutas.append(dict(_permisos(roles), operacion=o, metodo=metodo, ruta=ruta_api + sufijo, roles=roles,
                          verbo=VERBOS[o], uri_prueba=uri_prueba,
                          datos_prueba={'crear': 'datosParaCrear()', 'editar': 'datosParaEditar()'}.get(o, 'null')))

    return {'entidad': n, 'clase': clase, 'paquete': p, 'var': var(e), 'singular': singular,
            'plural': e['etiquetas']['es']['plural'].lower(), 'descripcion': e['descripcion'],
            'fichas': e['fichas'], 'operaciones': operaciones, 'rutas': rutas, 'ruta_api': ruta_api,
            'campos': _publicos(campos), 'campos_internos': campos, 'crear_con': crear_con,
            'campos_editar': _publicos(campos_editar), 'campos_editar_internos': campos_editar,
            'contrasena': contrasena_formulario, 'constantes': constantes,
            'editar_con_contrasena': any(r['solo_con_contrasena'] for r in reglas_por_operacion.get('editar', [])),
            'iniciales': iniciales, 'al_crear': al_crear, 'dependencias': dependencias,
            'reglas': reglas, 'reglas_por_operacion': reglas_por_operacion,
            'efectos': efectos, 'efectos_por_operacion': efectos_por_operacion,
            'consultas': consultas_unicas(e), 'activo': activo, 'orden': orden, 'orden_texto': orden_texto,
            'respuesta': _respuesta(entidades, e), 'atributos_entidad': _atributos_entidad(e),
            'con_empresa': con_empresa, 'en_bloque': False, 'solicitud_editar': f'{clase}EditarSolicitud'}


def _permisos(roles):
    """La regla de la cadena de seguridad y la de ``@PreAuthorize`` para los roles de una operación."""
    quienes = ', '.join(f'"{r}"' for r in roles)
    if len(roles) == 1:
        return {'regla': f'hasRole({quienes})', 'preautorizar': f"hasRole('{roles[0]}')"}
    return {'regla': f'hasAnyRole({quienes})',
            'preautorizar': 'hasAnyRole(' + ', '.join(f"'{r}'" for r in roles) + ')'}


def _respuesta(entidades, e):
    """Los campos de la respuesta: el identificador, los atributos de la lista o del formulario y las fechas."""
    respuesta = [{'tipo': 'UUID', 'nombre': 'id', 'getter': 'getId',
                  'esquema': 'description = "Identificador (UUID v7)."', 'descripcion': 'identificador (UUID v7)'}]
    for a in _de_la_respuesta(e):
        esquema = [f'description = {texto_java(a["etiquetas"]["es"])}']
        if ejemplo_openapi(a) is not None:
            esquema.append(f'example = {ejemplo_openapi(a)}')
        respuesta.append({'tipo': tipo_java(entidades, e, a), 'nombre': camel(a['nombre']),
                          'getter': 'get' + pascal(a['nombre']), 'esquema': ', '.join(esquema),
                          'descripcion': a['etiquetas']['es']})
    return respuesta + [{'tipo': 'OffsetDateTime', 'nombre': 'fechaCreacion', 'getter': 'getFechaCreacion',
                         'esquema': 'description = "Fecha de creación del registro."',
                         'descripcion': 'fecha de creación del registro'},
                        {'tipo': 'OffsetDateTime', 'nombre': 'fechaActualizacion', 'getter': 'getFechaActualizacion',
                         'esquema': 'description = "Fecha de la última actualización."',
                         'descripcion': 'fecha de la última actualización'}]


def _atributos_entidad(e):
    """Getter y setter de cada atributo: el mapeador los usa para copiar un registro."""
    return [{'setter': 'set' + pascal(a['nombre']), 'getter': 'get' + pascal(a['nombre'])} for a in e['atributos']]


def archivos_api(entidades, e):
    """Archivos de la API de una entidad del alcance: ``(ruta, plantilla, contexto)``."""
    if e.get('edicion_en_bloque'):
        return archivos_api_en_bloque(entidades, e)
    m = modelo_api(entidades, e)
    clase, p = m['clase'], m['paquete']
    involucradas = [e] + ([entidades[m['crear_con']['entidad']]] if m['crear_con'] else []) + [
        o for o in entidades.values() if m['iniciales'] and 'valores_iniciales' in o]
    involucradas = list({x['entidad']: x for x in involucradas}.values())

    def archivo(nombre, plantilla, descripcion, prueba=False, **extra):
        return (ruta(e, nombre, prueba), plantilla,
                dict(m, **extra, header=_header(entidades, involucradas, f'{nombre}.java', descripcion)))

    archivos = []
    if m['reglas'] or m['efectos']:
        archivos.append(archivo(f'Reglas{clase}', 'java/main/reglas.java.j2',
                                f"Reglas de {m['entidad']} que se programan a mano ({m['fichas'][0]}).",
                                importaciones=([SESION] if m['con_empresa'] else [])))
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
                                solicitud={'clase': f'{clase}EditarSolicitud', 'campos': m['campos_editar'],
                                           'crear_con': None, 'descripcion': f'Datos para editar {m["singular"]}.',
                                           'importaciones': sorted(_importaciones_solicitud(
                                               entidades, e, m['campos_editar_internos']))}))
    archivos.append(archivo(f'{clase}Respuesta', 'java/main/respuesta.java.j2',
                            f'{m["singular"].capitalize()} en la respuesta de la API.',
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
    for a in _de_la_respuesta(e):
        importaciones |= importaciones_tipo(entidades, e, a, paquete(e))
    return importaciones


def _importaciones_mapeador(m):
    importaciones = set()
    if any(c['tipo_atributo'] == 'decimal' for c in m['campos']):
        importaciones.add('co.edu.usco.inspectia.comun.numero.Decimales')
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
                     'java.util.UUID',
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
    if m['contrasena']:
        importaciones.add('org.springframework.security.crypto.password.PasswordEncoder')
    if m['con_empresa']:
        importaciones.add(SESION)
    if m['activo'] and 'listar' in m['operaciones']:
        importaciones.add('org.springframework.data.domain.Page')
    for inicial in m['iniciales']:
        if inicial['paquete'] != m['paquete']:
            importaciones |= {f"{inicial['paquete']}.{inicial['repositorio']}",
                              f"{inicial['paquete']}.{inicial['clase']}"}
    return importaciones


def _importaciones_controlador(m):
    importaciones = {'io.swagger.v3.oas.annotations.Operation', 'io.swagger.v3.oas.annotations.tags.Tag',
                     'org.springframework.security.access.prepost.PreAuthorize',
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
    if m['con_empresa']:
        importaciones |= {SESION, 'io.swagger.v3.oas.annotations.Parameter',
                          'org.springframework.security.core.annotation.AuthenticationPrincipal'}
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
        o['entidad'] for o in entidades.values() if m['iniciales'] and 'valores_iniciales' in o] + (
        ['empresa'] if m['con_empresa'] else [])
    editar = m['campos_editar_internos']
    listas = [c for c in crear if c['tipo_atributo'] == 'lista']
    # Un valor que existe pero no se puede asignar responde 403 (valores_en_formulario).
    no_permitidos = [(c['campo_json'], texto_java(v['codigo']),
                      mensaje_esperado(e, c['_atributo'], 'valores_en_formulario'))
                     for c in listas if 'valores_en_formulario' in c['_atributo']
                     for v in c['_atributo']['valores'] if v['codigo'] not in c['_atributo']['valores_en_formulario']]
    # Los valores del borde que acepta cada validación: el largo máximo, el mínimo y los bytes justos.
    bordes_crear = [(c['campo_json'], valor) for c in crear for valor in c['bordes']]
    if cc:
        bordes_crear += [(c['campo_json'], valor) for c in cc['campos_internos'] for valor in c['bordes']]
    bordes_editar = [(c['campo_json'], valor) for c in editar for valor in c['bordes']]
    valores = [valor for _, valor, _ in validaciones_crear] + [valor for _, valor in bordes_crear + bordes_editar]
    return {
        'roles': roles,
        'permisos': [(r['operacion'], rol, rol in r['roles']) for r in m['rutas'] for rol in roles],
        'datos_crear': [(c['campo_json'], c['ejemplo']) for c in crear],
        'datos_crear_con': [(c['campo_json'].split('.', 1)[1], c['ejemplo']) for c in cc['campos_internos']] if cc else [],
        'datos_editar': [(c['campo_json'], c['ejemplo']) for c in editar],
        'validaciones_crear': validaciones_crear,
        'validaciones_editar': [(c['campo_json'], valor, mensaje) for c in editar for valor, mensaje in c['casos']],
        'bordes_crear': bordes_crear,
        'bordes_editar': bordes_editar,
        'unicos': unicos,
        'normalizados': [(c['campo_json'], c['ejemplo']) for c in crear if c['recorta'] and c['tipo'] == 'String'],
        'usa_correo_largo': any('correoDeLargo' in valor for valor in valores),
        # Hace falta un registro y desactivarlo para ver que el filtro ?activo= lo cambia de lado (D-33).
        'filtra_por_activo': bool(m['activo'] and m['activo']['consulta'])
                             and {'crear', 'desactivar'} <= set(m['operaciones']),
        'tablas': ', '.join(dict.fromkeys(tablas)),
        'ficha': m['fichas'][0],
        'con_empresa': m['con_empresa'],
        'empresas': _empresas_de_prueba(entidades) if m['con_empresa'] else None,
        'no_permitidos': no_permitidos,
        'listas': [c['campo_json'] for c in listas],
        'con_registro': [o for o in ('ver', 'editar', 'desactivar', 'reactivar') if o in m['operaciones']],
    }


def _empresas_de_prueba(entidades):
    """Dos empresas para la prueba generada de una entidad por empresa: la de la sesión y otra.

    Llevan los ejemplos de los atributos obligatorios de empresa. En la segunda, cada dato único cambia
    cada dígito por el siguiente: así no se repite y sigue cumpliendo su patrón.
    """
    empresa = entidades['empresa']
    unicos = {a['nombre'] for a in empresa['atributos'] if a.get('unico')}
    unicos |= {u['atributos'][0] for u in empresa.get('unicos', []) if len(u['atributos']) == 1}
    obligatorios = [a for a in empresa['atributos'] if a.get('obligatorio') and 'por_defecto' not in a]
    primera = [ejemplo(a) for a in obligatorios]
    segunda = [_otro_valor(a, valor) if a['nombre'] in unicos else valor for a, valor in zip(obligatorios, primera)]
    return {'columnas': ', '.join(a['nombre'] for a in obligatorios),
            'marcadores': ', '.join('?' for _ in obligatorios),
            'primera': ', '.join(json_java(v) for v in primera),
            'segunda': ', '.join(json_java(v) for v in segunda)}


def _otro_valor(a, valor):
    """Un valor distinto del ejemplo que cumple las mismas validaciones: cada dígito pasa al siguiente."""
    otro = re.sub(r'\d', lambda digito: str((int(digito.group()) + 1) % 10), str(valor))
    patron = a.get('validaciones', {}).get('patron')
    if otro == str(valor) and a['tipo'] == 'texto' and not patron:
        otro = (str(valor) + ' 2')[:a['longitud']]
    if otro == str(valor) or (patron and not re.fullmatch(patron, otro)):
        raise ErrorGeneracion(f"empresa.{a['nombre']}: no se pudo armar a partir de su ejemplo otro valor válido, "
                              'que necesita la prueba generada de una entidad por empresa')
    return otro


# --- API en bloque ------------------------------------------------------------------------------


def modelo_api_en_bloque(entidades, e):
    """Todo lo que necesitan las plantillas de la API de una entidad con ``edicion_en_bloque``.

    Cada empresa tiene una fila por cada valor de la lista que las identifica (``clave_en_bloque``). La API
    lista las filas, las edita todas juntas y, si ``operaciones`` lo dice, vuelve a sus valores por defecto:
    los de ``valores_iniciales``, la misma fuente que usa la empresa nueva (RF-02).
    """
    verificar_api(entidades, e)
    n, clase = e['entidad'], e['clase']
    atr_clave = clave_en_bloque(e)
    codigos = valores_lista(entidades, atr_clave)
    clave = {'atributo': atr_clave['nombre'], 'nombre': camel(atr_clave['nombre']),
             'tipo': tipo_java(entidades, e, atr_clave), 'getter': 'get' + pascal(atr_clave['nombre']),
             'en_frase': en_frase(atr_clave['etiquetas']['es']), 'valores': codigos}
    operaciones = {o: e['operaciones'][o] for o in OPERACIONES_EN_BLOQUE if o in e['operaciones']}
    formulario = [a for a in e['atributos'] if a.get('en_formulario')]
    campos = _campos(entidades, e, formulario)
    # Cada fila trae la lista que la identifica, aunque no se edite: así se sabe cuál fila es.
    campo_clave = dict(campo_solicitud(e, atr_clave, clave['nombre'], obligatorio=True),
                       tipo=clave['tipo'], _atributo=atr_clave, _entidad=e)
    fila = [campo_clave] + campos

    solo_lectura = []
    for a in formulario:
        if 'solo_lectura_si' in a:
            codigo = a['solo_lectura_si']['igual_a']
            solo_lectura.append({
                'atributo': a['nombre'], 'campo': camel(a['nombre']), 'getter': 'get' + pascal(a['nombre']),
                'codigo': codigo, 'valor': f"{clave['tipo']}.{codigo}", 'indice': codigos.index(codigo),
                'decimal': a['tipo'] == 'decimal', 'clave': clave_mensaje(e, a, 'solo_lectura_si'),
                'mensaje': mensaje_esperado(e, a, 'solo_lectura_si')})

    reglas = [{'codigo': r['codigo'], 'metodo': 'cumple' + pascal(r['codigo']), 'descripcion': r['descripcion'],
               'cuando_texto': 'al editar', 'solo_con_contrasena': False, 'campo': None,
               'clave': f"{n}.reglas.{r['codigo']}", 'fichas': r['fichas']}
              for r in e.get('reglas', []) if r['tipo'] == 'validacion']
    dependencias = [{'tipo': f'{clase}Repositorio', 'nombre': 'repositorio',
                     'descripcion': f'el repositorio de {{@code {n}}}'}]
    if reglas:
        dependencias.append({'tipo': f'Reglas{clase}', 'nombre': 'reglas',
                             'descripcion': 'las reglas que se programan a mano'})

    # Al restaurar, cada fila vuelve a los datos de su fila de valores_iniciales, salvo la lista que la
    # identifica, que ya es la misma.
    restaurables = [{'setter': c['setter'], 'getter': 'get' + c['setter'][len('set'):]}
                    for c in valores_iniciales(entidades, e)['parametros'] if c['nombre'] != clave['nombre']]

    ruta_api = f"/api/v1/{e['ruta']}"
    rutas = []
    for o, roles in operaciones.items():
        metodo, sufijo = RUTAS_EN_BLOQUE[o]
        rutas.append(dict(_permisos(roles), operacion=o, metodo=metodo, ruta=ruta_api + sufijo, sufijo=sufijo,
                          roles=roles, verbo=VERBOS[o]))

    return {'entidad': n, 'clase': clase, 'paquete': paquete(e), 'var': var(e),
            'singular': e['etiquetas']['es']['singular'].lower(), 'plural': e['etiquetas']['es']['plural'].lower(),
            'descripcion': e['descripcion'], 'fichas': e['fichas'], 'operaciones': operaciones, 'rutas': rutas,
            'ruta_api': ruta_api, 'clave': clave, 'campos': _publicos(campos), 'campos_internos': campos,
            'fila': _publicos(fila), 'fila_interna': fila, 'solo_lectura': solo_lectura, 'reglas': reglas,
            'efectos': [], 'constantes': _constantes_de_reglas(e), 'dependencias': dependencias,
            'restaurables': restaurables, 'respuesta': _respuesta(entidades, e),
            'atributos_entidad': _atributos_entidad(e), 'con_empresa': True, 'en_bloque': True,
            'solicitud_editar': f'{clase}FilaSolicitud', 'crear_con': None, 'contrasena': None}


def archivos_api_en_bloque(entidades, e):
    """Archivos de la API de una entidad con edición en bloque: ``(ruta, plantilla, contexto)``."""
    m = modelo_api_en_bloque(entidades, e)
    clase, plural, clave = m['clase'], m['plural'], m['clave']

    def archivo(nombre, plantilla, descripcion, prueba=False, **extra):
        return (ruta(e, nombre, prueba), plantilla,
                dict(m, **extra, header=_header(entidades, [e], f'{nombre}.java', descripcion)))

    por_cada = f"una por cada {clave['en_frase']}"
    filas = {'nombre': 'filas', 'etiqueta': f'las filas, {por_cada}', 'tipo': f'List<{clase}FilaSolicitud>',
             'normalizacion': None,
             'esquema': (f'description = {texto_java(f"Las filas, {por_cada}, sin repetir.")}, '
                         'requiredMode = Schema.RequiredMode.REQUIRED'),
             'anotaciones': ['@NotNull(message = "{validacion.obligatorio}")', '@Valid']}
    archivos = []
    if m['reglas']:
        archivos.append(archivo(f'Reglas{clase}', 'java/main/reglas.java.j2',
                                f"Reglas de {m['entidad']} que se programan a mano ({m['fichas'][0]}).",
                                importaciones=['java.util.List', SESION]))
    archivos.append(archivo(f'{clase}EdicionSolicitud', 'java/main/solicitud.java.j2',
                            f'Datos para editar {plural} por la API: todas las filas juntas.',
                            solicitud={'clase': f'{clase}EdicionSolicitud', 'campos': [filas], 'crear_con': None,
                                       'descripcion': f'Las filas de {plural} de la empresa, {por_cada}: se editan '
                                                      'todas juntas.',
                                       'importaciones': ['io.swagger.v3.oas.annotations.media.Schema',
                                                         'jakarta.validation.Valid',
                                                         'jakarta.validation.constraints.NotNull', 'java.util.List']}))
    archivos.append(archivo(f'{clase}FilaSolicitud', 'java/main/solicitud.java.j2',
                            f'Una fila de {plural} en la edición en bloque por la API.',
                            solicitud={'clase': f'{clase}FilaSolicitud', 'campos': m['fila'], 'crear_con': None,
                                       'descripcion': f"Una fila de {plural}: el valor de {clave['en_frase']} que "
                                                      'la identifica y sus datos.',
                                       'importaciones': sorted(_importaciones_solicitud(entidades, e,
                                                                                        m['fila_interna']))}))
    archivos.append(archivo(f'{clase}Respuesta', 'java/main/respuesta.java.j2',
                            f'{m["singular"].capitalize()} en la respuesta de la API.',
                            importaciones=sorted(_importaciones_respuesta(entidades, e))))
    archivos.append(archivo(f'{clase}Mapeador', 'java/main/mapeador.java.j2',
                            f'Conversión entre {m["entidad"]} y sus filas y respuesta.',
                            importaciones=sorted(_importaciones_mapeador(m))))
    archivos.append(archivo(f'{clase}Servicio', 'java/main/servicio_en_bloque.java.j2',
                            f'Operaciones de {m["entidad"]}, que se edita en bloque: validan, guardan y responden '
                            f'({m["fichas"][0]}).',
                            importaciones=sorted(_importaciones_servicio_en_bloque(m))))
    archivos.append(archivo(f'{clase}Controlador', 'java/main/controlador_en_bloque.java.j2',
                            f'API REST de {m["entidad"]} en {m["ruta_api"]} ({m["fichas"][0]}).',
                            importaciones=sorted(_importaciones_controlador_en_bloque(m))))
    archivos.append(archivo(f'{clase}Acceso', 'java/main/acceso.java.j2',
                            f'Roles que pueden usar cada operación de la API de {m["entidad"]} (RNF-04).'))
    archivos.append(archivo(f'{clase}ApiGeneradaIT', 'java/test/api_en_bloque.java.j2',
                            f'Pruebas generadas de la API de {m["entidad"]}, que se edita en bloque: permisos, '
                            'validaciones, filas completas y empresas.',
                            prueba=True, prueba_api=_modelo_prueba_en_bloque(entidades, e, m)))
    return archivos


def _importaciones_servicio_en_bloque(m):
    importaciones = {'co.edu.usco.inspectia.comun.errores.CampoInvalido',
                     'co.edu.usco.inspectia.comun.errores.ReglaIncumplidaExcepcion', SESION,
                     'java.util.Comparator', 'java.util.EnumMap', 'java.util.EnumSet', 'java.util.List',
                     'java.util.Map', 'java.util.Set', 'org.springframework.stereotype.Service',
                     'org.springframework.transaction.annotation.Transactional'}
    if m['solo_lectura'] or m['reglas']:
        importaciones.add('java.util.ArrayList')
    if any(s['decimal'] for s in m['solo_lectura']):
        importaciones.add('java.math.BigDecimal')
    if any(not s['decimal'] for s in m['solo_lectura']):
        importaciones.add('java.util.Objects')
    return importaciones


def _importaciones_controlador_en_bloque(m):
    importaciones = {'io.swagger.v3.oas.annotations.Operation', 'io.swagger.v3.oas.annotations.Parameter',
                     'io.swagger.v3.oas.annotations.tags.Tag', SESION, 'java.util.List',
                     'org.springframework.security.access.prepost.PreAuthorize',
                     'org.springframework.security.core.annotation.AuthenticationPrincipal',
                     'org.springframework.web.bind.annotation.RequestMapping',
                     'org.springframework.web.bind.annotation.RestController'}
    if 'listar' in m['operaciones']:
        importaciones.add('org.springframework.web.bind.annotation.GetMapping')
    if 'editar' in m['operaciones']:
        importaciones |= {'jakarta.validation.Valid', 'org.springframework.web.bind.annotation.PutMapping',
                          'org.springframework.web.bind.annotation.RequestBody'}
    if 'restaurar_por_defecto' in m['operaciones']:
        importaciones.add('org.springframework.web.bind.annotation.PostMapping')
    return importaciones


def _modelo_prueba_en_bloque(entidades, e, m):
    """Datos de la prueba generada de una edición en bloque: filas válidas, permisos, validaciones, filas
    incompletas, datos de solo lectura y empresas.

    Las filas válidas son las de ``valores_iniciales``, en el orden de la lista: cumplen las validaciones del
    JSON y también las reglas que se programan a mano, porque son las de cada empresa nueva.
    """
    clave = m['clave']
    roles = [v['codigo'] for v in atributo(entidades['usuario'], 'rol')['valores']]
    formulario = [c['_atributo'] for c in m['campos_internos']]
    iniciales = sorted(e['valores_iniciales']['filas'], key=lambda f: clave['valores'].index(f[clave['atributo']]))
    validaciones = [(c['campo_json'], valor, mensaje) for c in m['fila_interna'] for valor, mensaje in c['casos']]
    return {
        'roles': roles,
        'permisos': [(r['operacion'], rol, rol in r['roles']) for r in m['rutas'] for rol in roles],
        'parametros': [c['nombre'] for c in m['fila']],
        'filas': [[texto_java(f[clave['atributo']])] + [json_java(f.get(a['nombre'])) for a in formulario]
                  for f in iniciales],
        'validaciones': validaciones,
        'usa_correo_largo': any('correoDeLargo' in valor for _, valor, _ in validaciones),
        'filas_completas': mensaje_filas_completas(e, 'es'),
        'solo_lectura': [dict(s, otro=_otro_numero(atributo(e, s['atributo']), iniciales[s['indice']][s['atributo']]))
                         for s in m['solo_lectura']],
        'normalizados': [(c['campo_json'], json_java(iniciales[0].get(c['atributo'])))
                         for c in m['campos_internos'] if c['recorta'] and c['tipo'] == 'String'],
        'tablas': ', '.join(dict.fromkeys([e['entidad'], 'empresa'])),
        'ficha': m['fichas'][0],
        'empresas': _empresas_de_prueba(entidades),
    }


def _otro_numero(a, valor):
    """Otro número válido, distinto de ``valor``: un paso de su escala más arriba, o más abajo si no cabe."""
    paso = Decimal(1).scaleb(-a['escala']) if a['tipo'] == 'decimal' else Decimal(1)
    maximo = a.get('validaciones', {}).get('max')
    otro = Decimal(str(valor)) + paso
    if maximo is not None and otro > Decimal(str(maximo)):
        otro = Decimal(str(valor)) - paso
    return str(otro)


# --- Header -------------------------------------------------------------------------------------


def _header(entidades, involucradas, nombre_archivo, descripcion):
    datos = datos_header({x['entidad']: x for x in involucradas})
    return dict(datos, nombre=nombre_archivo, descripcion=descripcion,
                origen=', '.join(f"specs/entidades/{x['entidad']}.json" for x in involucradas))
