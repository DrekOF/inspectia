# Nombre: casos.py
# Descripción: Lista de casos de prueba (RP-13): junta las marcas de las pruebas con los criterios de
#              aceptación de specs/fichas.json y señala los criterios sin prueba.
# Fecha de creación: 13/10/2026
# Actualización: 14/10/2026
# Autor: Equipo InspectIA
# Versión: 1.1
"""Lista de casos de prueba (RP-13).

Cada prueba dice qué cubre con una marca (ver CLAUDE.md):

- ``«<código> · C<n> · …»`` si cubre el criterio de aceptación ``n`` de una ficha de ``specs/fichas.json``.
- ``«<código> · …»`` si no cubre un criterio: una ficha, una pauta del docente (RP), una decisión (D)
  o un pendiente (P).

En Java la marca va en ``@DisplayName`` y en Python en ``@pytest.mark.caso``. Con las marcas y las
fichas se arma ``docs/casos-de-prueba.md``: cada criterio con sus pruebas, y los que no tienen ninguna
como pendientes. El documento no lleva números de línea, para que no cambie cada vez que se edita una
prueba.

También señala las fichas en estado «Implementado» que todavía tienen criterios sin prueba (D-29).
"""

import ast
import json
import re
from collections import defaultdict
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path

from jinja2 import Environment, FileSystemLoader, StrictUndefined

from inspectia_generador.carga import cargar_codigos

VERSION = '1.1'
RUTA_DOCUMENTO = 'docs/casos-de-prueba.md'
IMPLEMENTADO = 'Implementado'
#: Carpetas de pruebas de Java y si lo que tienen es generado.
CARPETAS_JAVA = (('backend/src/test/java', False), ('backend/src/test-generated/java', True))
CARPETA_PYTHON = 'generador/pruebas'
PLANTILLAS = Path(__file__).resolve().parent.parent / 'plantillas'

MARCA = re.compile(r'^(?P<codigo>(?:RF|RNF|RP|D|P)-\d{2})(?: · C(?P<criterio>\d+))? · (?P<texto>\S.*)$')
ELIMINADO = re.compile(r'^Eliminado \(D-\d{2}\):')
ANOTACIONES_DE_PRUEBA = {'Test', 'ParameterizedTest', 'RepeatedTest', 'TestFactory', 'TestTemplate'}
_ESCAPES = {'n': '\n', 't': '\t', 'r': '\r', '"': '"', "'": "'", '\\': '\\'}
_ANOTACION = re.compile(r'@\s*([A-Za-z_][\w.]*)')
_TIPO = re.compile(r'\b(class|interface|enum|record)\b')
_FECHA = re.compile(r'(Fecha de creación|Actualización):\s*(\d{2}/\d{2}/\d{4})')


@dataclass(frozen=True)
class Prueba:
    """Una prueba encontrada en el código, con su marca."""

    archivo: str
    linea: int
    nombre: str
    funcion: str
    marca: str
    parametrizada: bool = False
    generada: bool = False


@dataclass(frozen=True)
class Criterio:
    """Un criterio de aceptación de una ficha; el número es su posición en la lista, desde 1."""

    numero: int
    texto: str

    @property
    def eliminado(self):
        """``True`` si el criterio se marcó «Eliminado (D-xx): …»: conserva su número y no lleva prueba."""
        return bool(ELIMINADO.match(self.texto))


@dataclass(frozen=True)
class Ficha:
    """Una ficha de ``specs/fichas.json`` con sus criterios y su estado (D-29); las RNF no tienen estado."""

    codigo: str
    titulo: str
    criterios: tuple
    estado: str = None


@dataclass
class Revision:
    """Las pruebas repartidas por criterio y por código, y los errores de las marcas."""

    por_criterio: dict = field(default_factory=lambda: defaultdict(list))
    sin_criterio: dict = field(default_factory=lambda: defaultdict(list))
    errores: list = field(default_factory=list)


@dataclass
class Resultado:
    """El documento armado, los errores de las marcas, los criterios pendientes y las fichas
    «Implementado» que todavía tienen criterios sin prueba (D-29)."""

    texto: str
    errores: list
    pendientes: list
    implementadas_incompletas: list = field(default_factory=list)


# --- Fichas ---------------------------------------------------------------------------------------


def leer_fichas(ruta):
    """Fichas RF y RNF de ``fichas.json``, en su orden, con sus criterios."""
    datos = json.loads(Path(ruta).read_text(encoding='utf-8'))
    fichas = []
    for f in datos['rf'] + datos['rnf']:
        criterios = f.get('criterios', [])
        if isinstance(criterios, dict):
            criterios = criterios.get('b') or criterios.get('n') or []
        fichas.append(Ficha(f['codigo'], f['titulo'],
                            tuple(Criterio(n, texto) for n, texto in enumerate(criterios, 1)), f.get('estado')))
    return fichas


def titulos_de_otros(specs):
    """Título corto de cada pauta del docente (RP), decisión (D) y pendiente (P): hasta el primer
    punto o los primeros dos puntos de su texto en ``specs/``."""
    titulos = {}
    for archivo in ('requisitos-docente.md', 'decisiones.md'):
        for linea in (Path(specs) / archivo).read_text(encoding='utf-8').splitlines():
            m = re.match(r'^\| ((?:RP|D|P)-\d{2}) \| (.+?) \|', linea)
            if m:
                titulos[m[1]] = re.split(r':|\. ', m[2], maxsplit=1)[0].rstrip('.')
    return titulos


# --- Pruebas de Java ------------------------------------------------------------------------------


def _blanquear(fuente):
    """El código sin comentarios ni el contenido de los textos (en su lugar quedan espacios, así las
    posiciones no cambian), y los textos de comillas dobles según la posición de su comilla."""
    codigo, textos = list(fuente), {}
    i, n = 0, len(fuente)

    def borrar(desde, hasta):
        for k in range(desde, hasta):
            if codigo[k] != '\n':
                codigo[k] = ' '

    while i < n:
        if fuente.startswith('//', i):
            fin = fuente.find('\n', i)
            fin = n if fin < 0 else fin
            borrar(i, fin)
            i = fin
        elif fuente.startswith('/*', i):
            fin = fuente.find('*/', i + 2)
            fin = n if fin < 0 else fin + 2
            borrar(i, fin)
            i = fin
        elif fuente.startswith('"""', i):
            fin = fuente.find('"""', i + 3)
            fin = n if fin < 0 else fin
            textos[i] = fuente[i + 3:fin]
            borrar(i + 3, fin)
            i = fin + 3
        elif fuente[i] in '"\'':
            comilla, j, valor = fuente[i], i + 1, []
            while j < n and fuente[j] not in (comilla, '\n'):
                if fuente[j] == '\\' and j + 1 < n:
                    valor.append(_ESCAPES.get(fuente[j + 1], fuente[j + 1]))
                    j += 2
                else:
                    valor.append(fuente[j])
                    j += 1
            if comilla == '"':
                textos[i] = ''.join(valor)
            borrar(i + 1, j)
            i = j + 1
        else:
            i += 1
    return ''.join(codigo), textos


def _saltar_espacios(codigo, i):
    while i < len(codigo) and codigo[i].isspace():
        i += 1
    return i


def _cerrar(codigo, abre):
    """Posición del paréntesis que cierra el de ``abre`` (los textos ya están en blanco)."""
    nivel = 0
    for k in range(abre, len(codigo)):
        if codigo[k] == '(':
            nivel += 1
        elif codigo[k] == ')':
            nivel -= 1
            if nivel == 0:
                return k
    return len(codigo) - 1


def pruebas_java(fuente, archivo, generada=False):
    """Pruebas de un archivo de Java: cada método con ``@Test`` o ``@ParameterizedTest`` (y las demás
    anotaciones de prueba de JUnit) y la marca de su ``@DisplayName``.

    Returns:
        tuple: ``(pruebas, errores)``.
    """
    codigo, textos = _blanquear(fuente)
    clase = Path(archivo).stem
    pruebas, errores = [], []
    i = 0
    while (inicio := _ANOTACION.search(codigo, i)) is not None:
        if codigo.startswith('@interface', inicio.start()):
            i = inicio.end()
            continue
        anotaciones, j = {}, inicio.start()
        while (m := _ANOTACION.match(codigo, j)) is not None and not codigo.startswith('@interface', j):
            nombre = m.group(1).rsplit('.', 1)[-1]
            k = _saltar_espacios(codigo, m.end())
            argumentos = None
            if k < len(codigo) and codigo[k] == '(':
                cierre = _cerrar(codigo, k)
                argumentos = (k, cierre)
                k = cierre + 1
            anotaciones.setdefault(nombre, argumentos)
            j = _saltar_espacios(codigo, k)
        fines = [p for p in (codigo.find(c, j) for c in '({;=') if p >= 0]
        fin = min(fines) if fines else len(codigo)
        declaracion = codigo[j:fin]
        linea = codigo.count('\n', 0, j) + 1
        i = max(fin, inicio.end())
        es_prueba = bool(ANOTACIONES_DE_PRUEBA & anotaciones.keys())
        if _TIPO.search(declaracion):
            if 'DisplayName' in anotaciones:
                errores.append(f'{archivo}:{linea}: @DisplayName va en cada prueba, no en la clase')
            continue
        if fin >= len(codigo) or codigo[fin] != '(':
            continue
        metodo = re.search(r'(\w+)\s*$', declaracion)
        metodo = metodo[1] if metodo else '?'
        marca = None
        if anotaciones.get('DisplayName'):
            desde, hasta = anotaciones['DisplayName']
            marca = next((texto for posicion, texto in sorted(textos.items()) if desde < posicion < hasta), None)
        if es_prueba and marca is None:
            errores.append(f'{archivo}:{linea}: {clase}.{metodo} no tiene @DisplayName con su marca')
        elif marca is not None and not es_prueba:
            errores.append(f'{archivo}:{linea}: {clase}.{metodo} tiene @DisplayName pero no es una prueba')
        elif es_prueba:
            pruebas.append(Prueba(archivo, linea, clase, metodo, marca,
                                  parametrizada='ParameterizedTest' in anotaciones, generada=generada))
    return pruebas, errores


# --- Pruebas de Python ----------------------------------------------------------------------------


def _nombre(nodo):
    """Nombre con puntos de una expresión como ``pytest.mark.caso``."""
    if isinstance(nodo, ast.Call):
        return _nombre(nodo.func)
    if isinstance(nodo, ast.Attribute):
        return f'{_nombre(nodo.value)}.{nodo.attr}'
    return nodo.id if isinstance(nodo, ast.Name) else ''


def pruebas_python(fuente, archivo):
    """Pruebas de un archivo de pytest: cada función ``test...`` y la marca de ``@pytest.mark.caso``.

    Returns:
        tuple: ``(pruebas, errores)``.
    """
    pruebas, errores = [], []
    for nodo in ast.walk(ast.parse(fuente)):
        if not isinstance(nodo, (ast.FunctionDef, ast.AsyncFunctionDef)) or not nodo.name.startswith('test'):
            continue
        marca, parametrizada = None, False
        for decorador in nodo.decorator_list:
            nombre = _nombre(decorador)
            if (nombre == 'pytest.mark.caso' and isinstance(decorador, ast.Call) and decorador.args
                    and isinstance(decorador.args[0], ast.Constant)):
                marca = decorador.args[0].value
            parametrizada |= nombre == 'pytest.mark.parametrize'
        if marca is None:
            errores.append(f'{archivo}:{nodo.lineno}: {nodo.name} no tiene @pytest.mark.caso con su marca')
        else:
            pruebas.append(Prueba(archivo, nodo.lineno, Path(archivo).name, nodo.name, marca, parametrizada))
    return pruebas, errores


def buscar_pruebas(raiz):
    """Todas las pruebas del repositorio: las de Java del backend y las de Python del generador.

    Returns:
        tuple: ``(pruebas, errores)``, en orden de archivo.
    """
    raiz = Path(raiz)
    pruebas, errores = [], []
    for carpeta, generada in CARPETAS_JAVA:
        for ruta in sorted((raiz / carpeta).rglob('*.java')):
            encontradas, malas = pruebas_java(ruta.read_text(encoding='utf-8'),
                                              ruta.relative_to(raiz).as_posix(), generada)
            pruebas += encontradas
            errores += malas
    for ruta in sorted((raiz / CARPETA_PYTHON).rglob('test_*.py')):
        encontradas, malas = pruebas_python(ruta.read_text(encoding='utf-8'), ruta.relative_to(raiz).as_posix())
        pruebas += encontradas
        errores += malas
    return pruebas, errores


# --- Revisión y documento -------------------------------------------------------------------------


def revisar(fichas, pruebas, codigos):
    """Reparte las pruebas según su marca y revisa que cada marca cite algo que existe."""
    revision = Revision()
    por_codigo = {f.codigo: f for f in fichas}
    for p in pruebas:
        donde = f'{p.archivo}:{p.linea}'
        m = MARCA.match(p.marca)
        if m is None:
            revision.errores.append(f'{donde}: la marca «{p.marca}» no tiene la forma «<código> · C<n> · …» '
                                    'ni «<código> · …»')
            continue
        codigo, numero, texto = m['codigo'], m['criterio'], m['texto']
        if codigo not in codigos and codigo not in por_codigo:
            revision.errores.append(f'{donde}: {codigo} no existe en specs/')
        elif numero is None:
            revision.sin_criterio[codigo].append((p, texto))
        elif codigo not in por_codigo:
            revision.errores.append(f'{donde}: {codigo} no es una ficha: no tiene criterios (C{numero})')
        elif not 1 <= int(numero) <= len(por_codigo[codigo].criterios):
            revision.errores.append(f'{donde}: {codigo} no tiene el criterio C{numero}')
        elif por_codigo[codigo].criterios[int(numero) - 1].eliminado:
            revision.errores.append(f'{donde}: el criterio C{numero} de {codigo} está eliminado')
        else:
            revision.por_criterio[(codigo, int(numero))].append((p, texto))
    return revision


def _fechas(raiz, archivos):
    """Fecha de creación más antigua y actualización más reciente de los headers de las fuentes."""
    fechas = {'Fecha de creación': [], 'Actualización': []}
    for ruta in archivos:
        cabecera = '\n'.join((Path(raiz) / ruta).read_text(encoding='utf-8').splitlines()[:15])
        for campo, fecha in _FECHA.findall(cabecera):
            fechas[campo].append(datetime.strptime(fecha, '%d/%m/%Y'))
    fichas = json.loads((Path(raiz) / 'specs' / 'fichas.json').read_text(encoding='utf-8'))['_header']
    fechas['Fecha de creación'].append(datetime.strptime(fichas['fecha_creacion'], '%d/%m/%Y'))
    fechas['Actualización'].append(datetime.strptime(fichas['actualizacion'], '%d/%m/%Y'))
    return {'fecha_creacion': min(fechas['Fecha de creación']).strftime('%d/%m/%Y'),
            'actualizacion': max(fechas['Actualización']).strftime('%d/%m/%Y')}


def _referencia(entrada):
    """Una prueba en el documento: archivo o clase con enlace, su texto y si es parametrizada o generada."""
    prueba, texto = entrada
    notas = [n for n, si in (('parametrizada', prueba.parametrizada), ('generada', prueba.generada)) if si]
    sufijo = f" ({', '.join(notas)})" if notas else ''
    return f'[`{prueba.nombre}`](../{prueba.archivo}) · {_celda(texto)}{sufijo}'


def _celda(texto):
    return texto.replace('|', '\\|').replace('\n', ' ')


def _orden_otros(codigo):
    tipo, numero = codigo.split('-')
    return ('RP', 'D', 'P').index(tipo) if tipo in ('RP', 'D', 'P') else 3, int(numero)


def documento(fichas, pruebas, revision, titulos, header):
    """El texto de ``docs/casos-de-prueba.md``."""
    contexto_fichas, total = [], defaultdict(int)
    for f in fichas:
        criterios = [{'numero': c.numero, 'texto': c.texto, 'eliminado': c.eliminado,
                      'pruebas': sorted(revision.por_criterio.get((f.codigo, c.numero), []),
                                        key=lambda e: (e[0].archivo, e[1]))}
                     for c in f.criterios]
        con_prueba = sum(1 for c in criterios if c['pruebas'])
        eliminados = sum(1 for c in criterios if c['eliminado'])
        contexto_fichas.append({
            'codigo': f.codigo, 'titulo': f.titulo, 'criterios': criterios, 'total': len(criterios),
            'con_prueba': con_prueba, 'eliminados': eliminados,
            'pendientes': len(criterios) - con_prueba - eliminados,
            'otras': sorted(revision.sin_criterio.get(f.codigo, []), key=lambda e: (e[0].archivo, e[1]))})
        for clave in ('total', 'con_prueba', 'eliminados', 'pendientes'):
            total[clave] += contexto_fichas[-1][clave]
    fichas_con_codigo = {f.codigo for f in fichas}
    otros = [{'codigo': codigo, 'titulo': titulos.get(codigo, ''),
              'pruebas': sorted(revision.sin_criterio[codigo], key=lambda e: (e[0].archivo, e[1]))}
             for codigo in sorted(revision.sin_criterio, key=_orden_otros) if codigo not in fichas_con_codigo]
    java = [p for p in pruebas if p.archivo.endswith('.java')]
    resumen = dict(total, fichas=len(fichas), pruebas=len(pruebas), java=len(java),
                   generadas=sum(1 for p in java if p.generada), python=len(pruebas) - len(java))

    entorno = Environment(loader=FileSystemLoader(PLANTILLAS), undefined=StrictUndefined, trim_blocks=True,
                          lstrip_blocks=True, keep_trailing_newline=True, autoescape=False)
    entorno.filters['referencia'] = _referencia
    entorno.filters['celda'] = _celda
    return entorno.get_template('docs/casos_de_prueba.md.j2').render(
        version=VERSION, header=header, resumen=resumen, fichas=contexto_fichas, otros=otros)


def _sin_prueba(ficha, revision):
    """Los criterios de una ficha que no tienen prueba; los eliminados no cuentan."""
    return [c for c in ficha.criterios if not c.eliminado and (ficha.codigo, c.numero) not in revision.por_criterio]


def implementadas_incompletas(fichas, revision):
    """Fichas en estado «Implementado» que todavía tienen criterios sin prueba.

    D-29: una ficha solo está «Implementado» si funciona de punta a punta, todos sus criterios tienen
    prueba y está en main. Este script revisa la parte de las pruebas.

    Returns:
        list: un mensaje por ficha, con los criterios que les faltan.
    """
    mensajes = []
    for f in fichas:
        faltan = _sin_prueba(f, revision) if f.estado == IMPLEMENTADO else []
        if faltan:
            numeros = ', '.join(f'C{c.numero}' for c in faltan)
            mensajes.append(f'{f.codigo} está «{IMPLEMENTADO}», pero le faltan pruebas para {numeros} (D-29)')
    return mensajes


def armar_documento(raiz):
    """Arma el documento con las pruebas y las fichas del repositorio en ``raiz``."""
    raiz = Path(raiz)
    specs = raiz / 'specs'
    fichas = leer_fichas(specs / 'fichas.json')
    pruebas, errores = buscar_pruebas(raiz)
    revision = revisar(fichas, pruebas, cargar_codigos(specs))
    fuentes = sorted({p.archivo for p in pruebas}) + ['specs/requisitos-docente.md', 'specs/decisiones.md']
    texto = documento(fichas, pruebas, revision, titulos_de_otros(specs), _fechas(raiz, fuentes))
    pendientes = [f'{f.codigo} C{c.numero}' for f in fichas for c in _sin_prueba(f, revision)]
    return Resultado(texto, errores + revision.errores, pendientes, implementadas_incompletas(fichas, revision))
