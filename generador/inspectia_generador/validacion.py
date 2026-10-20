# Nombre: validacion.py
# Descripción: Validación de los JSON de entidades contra su formato y contra specs/.
# Fecha de creación: 13/10/2026
# Actualización: 20/10/2026
# Autor: Equipo InspectIA
# Versión: 1.2
"""Validación de los JSON de entidades.

Revisa el formato de ``specs/entidades/README.md`` y que cada ficha, decisión o requisito citado
exista en ``specs/``. Devuelve todos los errores juntos, para corregirlos de una vez.
"""

import json
import re

from inspectia_generador.modelo import (COLUMNAS_IMPLICITAS, EMPRESA_ID, atributo, columnas,
                                        tiene_empresa, valores_lista)

TIPOS = {'texto', 'entero', 'decimal', 'booleano', 'fecha', 'fecha_hora', 'lista', 'referencia',
         'contrasena'}
OPERACIONES = {'listar', 'ver', 'crear', 'editar', 'desactivar', 'reactivar', 'restaurar_por_defecto'}
VALIDACIONES = {'min', 'max', 'longitud_min', 'longitud_max', 'bytes_max', 'patron', 'formato',
                'no_posterior_a_hoy', 'recortar', 'minusculas'}
CAMPOS_HEADER = {'nombre', 'descripcion', 'fecha_creacion', 'actualizacion', 'autor', 'version'}
CLAVES_OBLIGATORIAS = ('entidad', 'clase', 'descripcion', 'etiquetas', 'fichas', 'crud',
                       'por_empresa', 'atributos')
#: Palabras de SQL que pueden aparecer en un ``check`` sin ser columnas.
PALABRAS_SQL = {'and', 'or', 'not', 'in', 'is', 'null', 'true', 'false', 'between', 'num_nonnulls',
                'lower', 'btrim'}

NOMBRE = re.compile(r'^[a-z][a-z0-9_]*$')
#: Un correo con la forma mínima nombre@dominio.algo, para revisar los ejemplos.
CORREO = re.compile(r'^[^@\s]+@[^@\s]+\.[^@\s]+$')
CLASE = re.compile(r'^[A-Z][A-Za-z0-9]*$')
CODIGO_LISTA = re.compile(r'^[A-Z][A-Z0-9_]*$')
RUTA = re.compile(r'^[a-z][a-z0-9-]*$')


def validar(entidades, codigos):
    """Valida las entidades.

    Args:
        entidades: ``{nombre: JSON}``, como lo devuelve ``cargar_entidades``.
        codigos: códigos que existen en ``specs/`` (``cargar_codigos``).

    Returns:
        list: mensajes de error; vacía si todo está bien.
    """
    revision = _Revision(entidades, codigos)
    if any(tiene_empresa(e) for e in entidades.values()) and 'empresa' not in entidades:
        revision.error('', 'hay tablas por empresa pero falta la entidad empresa')
    for nombre, entidad in entidades.items():
        revision.entidad(nombre, entidad)
    return revision.errores


class _Revision:
    """Acumula los errores de una validación."""

    def __init__(self, entidades, codigos):
        self.entidades = entidades
        self.codigos = codigos
        self.errores = []
        usuario = entidades.get('usuario', {})
        rol = atributo(usuario, 'rol') if usuario else None
        self.roles = valores_lista(entidades, rol) if rol else []

    def error(self, donde, mensaje):
        self.errores.append(f'{donde}: {mensaje}' if donde else mensaje)

    def fichas(self, donde, lista):
        if not lista:
            self.error(donde, 'sin fichas')
        for codigo in lista or []:
            if codigo not in self.codigos:
                self.error(donde, f'la ficha {codigo} no existe en specs/')

    def textos(self, donde, textos, claves=('es', 'en')):
        if not isinstance(textos, dict) or set(textos) != set(claves):
            self.error(donde, 'necesita textos en ' + ' y '.join(claves))

    # --- Entidad -------------------------------------------------------------------------------

    def entidad(self, nombre, e):
        faltan = [c for c in CLAVES_OBLIGATORIAS if c not in e]
        if faltan:
            self.error(nombre, 'faltan las claves ' + ', '.join(faltan))
            return
        faltan_header = CAMPOS_HEADER - set(e.get('_header', {}))
        if faltan_header:
            self.error(nombre, 'al header le faltan ' + ', '.join(sorted(faltan_header)))
        if not NOMBRE.match(nombre):
            self.error(nombre, 'el nombre de la entidad va en snake_case y sin tildes')
        if not CLASE.match(e['clase']):
            self.error(nombre, 'la clase va en PascalCase')
        if e['por_empresa'] not in ('si', 'opcional', 'no'):
            self.error(nombre, 'por_empresa debe ser si, opcional o no')
        for idioma in ('es', 'en'):
            if set(e['etiquetas'].get(idioma, {})) != {'singular', 'plural'}:
                self.error(nombre, f'etiquetas.{idioma} necesita singular y plural')
        self.fichas(nombre, e['fichas'])
        self.modulo_y_ruta(nombre, e)
        self.operaciones(nombre, e)
        for a in e['atributos']:
            self.atributo(nombre, e, a)
        self.restricciones(nombre, e)
        self.reglas(nombre, e)
        self.crear_con(nombre, e)
        self.valores_iniciales(nombre, e)

    def modulo_y_ruta(self, nombre, e):
        if e['crud']:
            for clave in ('modulo', 'ruta'):
                if clave not in e:
                    self.error(nombre, f'una entidad con CRUD necesita {clave}')
        elif 'ruta' in e:
            self.error(nombre, 'ruta va solo en entidades con CRUD')
        if 'modulo' in e and not NOMBRE.match(e['modulo']):
            self.error(nombre, 'modulo va en minúsculas y sin tildes')
        if 'ruta' in e and not RUTA.match(e['ruta']):
            self.error(nombre, 'ruta va en minúsculas, con guiones y sin tildes')

    def operaciones(self, nombre, e):
        if e['crud'] and not e.get('operaciones'):
            self.error(nombre, 'una entidad con CRUD necesita operaciones')
        if not e['crud'] and 'operaciones' in e:
            self.error(nombre, 'operaciones va solo en entidades con CRUD')
        for operacion, roles in e.get('operaciones', {}).items():
            if operacion not in OPERACIONES:
                self.error(nombre, f'operación desconocida {operacion}')
            for rol in roles:
                if rol not in self.roles:
                    self.error(nombre, f'rol desconocido {rol} (los roles salen de usuario.rol)')
        if 'atributo_activo' in e:
            a = atributo(e, e['atributo_activo'])
            if a is None or a.get('tipo') != 'booleano':
                self.error(nombre, 'atributo_activo debe ser un atributo booleano')

    # --- Atributos -----------------------------------------------------------------------------

    def atributo(self, entidad, e, a):
        donde = f"{entidad}.{a.get('nombre')}"
        if not NOMBRE.match(a.get('nombre', '')):
            self.error(donde, 'el nombre va en snake_case y sin tildes')
        if a.get('nombre') in COLUMNAS_IMPLICITAS + (EMPRESA_ID,):
            self.error(donde, 'es una columna implícita: la agrega el generador')
        tipo = a.get('tipo')
        if tipo not in TIPOS:
            self.error(donde, f'tipo desconocido {tipo}')
        if tipo in ('texto', 'contrasena') and 'longitud' not in a:
            self.error(donde, 'falta longitud')
        if tipo == 'decimal' and not {'precision', 'escala'} <= set(a):
            self.error(donde, 'falta precision o escala')
        if tipo == 'contrasena' and 'entrada' not in a:
            self.error(donde, 'una contraseña necesita entrada')
        if tipo == 'lista':
            self.lista(donde, a)
        if tipo == 'referencia':
            self.referencia(donde, a)
        for validacion in a.get('validaciones', {}):
            if validacion not in VALIDACIONES:
                self.error(donde, f'validación desconocida {validacion}')
        permitidos = set(a.get('validaciones', {})) | {'unico', 'valores'}
        for clave in ('valores_en_formulario', 'solo_lectura_si'):
            if clave in a:
                permitidos.add(clave)
        for clave, mensaje in a.get('mensajes', {}).items():
            if clave not in permitidos:
                self.error(donde, f'mensaje para algo que el atributo no tiene ({clave})')
            self.textos(f'{donde}.mensajes.{clave}', mensaje)
            self.marcadores(f'{donde}.mensajes.{clave}', mensaje, a.get('validaciones', {}))
        if 'ejemplo' in a:
            self.ejemplo(donde, a)
        self.textos(f'{donde}.etiquetas', a.get('etiquetas'))
        if e['crud'] != ('en_lista' in a and 'en_formulario' in a):
            self.error(donde, 'en_lista y en_formulario van solo, y siempre, en entidades con CRUD')
        if 'solo_lectura_si' in a:
            condicion = a['solo_lectura_si']
            otro = atributo(e, condicion.get('atributo'))
            if otro is None or condicion.get('igual_a') not in valores_lista(self.entidades, otro):
                self.error(donde, 'solo_lectura_si no apunta a un valor de una lista')
            # Solo la edición en bloque sabe en qué filas no se edita el dato: en otra entidad se ignoraría.
            if not e.get('edicion_en_bloque'):
                self.error(donde, 'solo_lectura_si va solo en una entidad con edicion_en_bloque')
        self.fichas(donde, a.get('fichas'))

    def marcadores(self, donde, mensaje, valores):
        """Cada {nombre} del mensaje debe tener valor; $ no se permite (Bean Validation lo interpreta)."""
        texto = json.dumps(mensaje, ensure_ascii=False)
        for marcador in re.findall(r'\{(\w+)\}', texto):
            if marcador not in valores:
                self.error(donde, f'el mensaje usa {{{marcador}}}, que no está en validaciones')
        if '$' in texto:
            self.error(donde, 'los mensajes no pueden llevar $')

    def ejemplo(self, donde, a):
        """El ejemplo debe ser un valor válido del atributo: lo usan OpenAPI y las pruebas generadas."""
        valor, tipo, v = a['ejemplo'], a.get('tipo'), a.get('validaciones', {})
        if tipo in ('texto', 'contrasena'):
            if not isinstance(valor, str) or not valor.strip():
                self.error(donde, 'el ejemplo debe ser un texto con algo escrito')
                return
            maximo = min(a.get('longitud', len(valor)), v.get('longitud_max', len(valor)))
            if not v.get('longitud_min', 0) <= len(valor) <= maximo:
                self.error(donde, 'el ejemplo no cumple el largo del atributo')
            if 'patron' in v and not re.search(v['patron'], valor):
                self.error(donde, 'el ejemplo no cumple el patron')
            if v.get('formato') == 'correo' and not CORREO.match(valor):
                self.error(donde, 'el ejemplo no es un correo')
        elif tipo in ('entero', 'decimal'):
            if isinstance(valor, bool) or not isinstance(valor, (int, float)):
                self.error(donde, 'el ejemplo debe ser un número')
            elif not v.get('min', valor) <= valor <= v.get('max', valor):
                self.error(donde, 'el ejemplo está fuera de min y max')
        elif tipo == 'booleano' and not isinstance(valor, bool):
            self.error(donde, 'el ejemplo debe ser true o false')
        elif tipo == 'lista' and valor not in valores_lista(self.entidades, a):
            self.error(donde, 'el ejemplo no está en la lista')

    def lista(self, donde, a):
        if ('valores' in a) == ('valores_de' in a):
            self.error(donde, 'una lista necesita valores o valores_de (solo uno)')
            return
        codigos = valores_lista(self.entidades, a)
        if not codigos:
            self.error(donde, 'valores_de no apunta a otra lista')
        for v in a.get('valores', []):
            if not CODIGO_LISTA.match(v.get('codigo', '')):
                self.error(donde, f"el código {v.get('codigo')} va en mayúsculas y sin tildes")
            self.textos(f"{donde}.{v.get('codigo')}", v.get('etiquetas'))
        if 'por_defecto' in a and a['por_defecto'] not in codigos:
            self.error(donde, 'por_defecto no está en la lista')
        for v in a.get('valores_en_formulario', []):
            if v not in codigos:
                self.error(donde, f'valores_en_formulario tiene {v}, que no está en la lista')

    def referencia(self, donde, a):
        ref = a.get('referencia', {})
        if ref.get('entidad') not in self.entidades:
            self.error(donde, f"referencia a la entidad {ref.get('entidad')}, que no existe")
        if ref.get('al_borrar') not in ('restringir', 'cascada'):
            self.error(donde, 'al_borrar debe ser restringir o cascada')
        if not a.get('nombre', '').endswith('_id'):
            self.error(donde, 'una referencia termina en _id')

    # --- Únicos, checks, índices, reglas -------------------------------------------------------

    def restricciones(self, nombre, e):
        cols = set(columnas(e))
        for grupo in ('unicos', 'indices'):
            for u in e.get(grupo, []):
                for c in u.get('atributos', []):
                    if c not in cols:
                        self.error(f'{nombre}.{grupo}', f'la columna {c} no existe')
                if 'mensaje' in u:
                    self.textos(f'{nombre}.{grupo}.mensaje', u['mensaje'])
                self.fichas(f'{nombre}.{grupo}', u.get('fichas'))
        for check in e.get('checks', []):
            donde = f"{nombre}.checks.{check.get('nombre')}"
            sin_textos = re.sub(r"'[^']*'", '', check.get('sql', ''))
            for palabra in re.findall(r'[a-z_][a-z0-9_]*', sin_textos):
                if palabra not in PALABRAS_SQL and palabra not in cols:
                    self.error(donde, f'«{palabra}» no es una columna')
            for valor in re.findall(r"'([^']*)'", check.get('sql', '')):
                if not any(a.get('tipo') == 'lista' and valor in valores_lista(self.entidades, a)
                           for a in e['atributos']):
                    self.error(donde, f'el valor {valor} no está en ninguna lista')
            self.fichas(donde, check.get('fichas'))

    def reglas(self, nombre, e):
        for r in e.get('reglas', []):
            donde = f"{nombre}.reglas.{r.get('codigo')}"
            if r.get('tipo') not in ('validacion', 'efecto', 'accion'):
                self.error(donde, 'tipo debe ser validacion, efecto o accion')
            if r.get('tipo') == 'validacion':
                self.textos(f'{donde}.mensaje', r.get('mensaje'))
            for operacion in r.get('cuando', []):
                if operacion not in e.get('operaciones', {}):
                    self.error(donde, f'la operación {operacion} no está en operaciones')
            for rol in r.get('roles', []):
                if rol not in self.roles:
                    self.error(donde, f'rol desconocido {rol}')
            mensaje = json.dumps(r.get('mensaje', {}), ensure_ascii=False)
            for parametro in re.findall(r'\{(\w+)\}', mensaje):
                if parametro not in r.get('parametros', {}):
                    self.error(donde, f'el mensaje usa {{{parametro}}} sin ese parámetro')
            self.fichas(donde, r.get('fichas'))

    def crear_con(self, nombre, e):
        if 'crear_con' not in e:
            return
        cc = e['crear_con']
        if not NOMBRE.match(cc.get('nombre', '')):
            self.error(f'{nombre}.crear_con', 'falta nombre (snake_case): el campo de la solicitud, como administrador')
        elif atributo(e, cc['nombre']) is not None:
            self.error(f'{nombre}.crear_con', f"el nombre {cc['nombre']} ya es un atributo de {nombre}")
        otra = self.entidades.get(cc.get('entidad'))
        if otra is None:
            self.error(f'{nombre}.crear_con', f"la entidad {cc.get('entidad')} no existe")
            return
        for c in cc.get('atributos', []):
            if atributo(otra, c) is None:
                self.error(f'{nombre}.crear_con', f"{c} no existe en {cc['entidad']}")
        for c, valor in cc.get('valores', {}).items():
            a = atributo(otra, c)
            if a is None or (a.get('tipo') == 'lista' and valor not in valores_lista(self.entidades, a)):
                self.error(f'{nombre}.crear_con', f'valor inválido {c}={valor}')
        self.fichas(f'{nombre}.crear_con', cc.get('fichas'))

    def valores_iniciales(self, nombre, e):
        if 'valores_iniciales' not in e:
            return
        donde = f'{nombre}.valores_iniciales'
        if e['valores_iniciales'].get('modo') != 'al_crear_empresa':
            self.error(donde, 'el único modo es al_crear_empresa')
        if e.get('por_empresa') != 'si':
            self.error(donde, 'van solo en entidades con por_empresa si')
        for fila in e['valores_iniciales'].get('filas', []):
            for c, valor in fila.items():
                a = atributo(e, c)
                if a is None:
                    self.error(donde, f'{c} no existe')
                elif a.get('tipo') == 'lista' and valor not in valores_lista(self.entidades, a):
                    self.error(donde, f'{valor} no está en la lista de {c}')
                elif a.get('tipo') == 'texto' and len(valor) > a['longitud']:
                    self.error(donde, f"{c} pasa de {a['longitud']} caracteres")
            for a in e['atributos']:
                if a.get('obligatorio') and 'por_defecto' not in a and a['nombre'] not in fila:
                    self.error(donde, f"falta {a['nombre']}")
