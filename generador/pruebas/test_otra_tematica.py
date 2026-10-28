# Nombre: test_otra_tematica.py
# Descripción: Demostración de que el generador sirve para otra temática (RP-23): la entidad producto de la
#              pauta del docente para el back-end y el café especial del Huila, por empresa, cada uno con su
#              API y su pantalla. Son los ejemplos de npm run demo:otra-tematica; no entran a la app.
# Fecha de creación: 13/10/2026
# Actualización: 28/10/2026
# Autor: Equipo InspectIA
# Versión: 1.3
"""Demostración de otra temática, junto con las entidades de la plataforma (empresa y usuario): el producto
de la pauta del docente (id, nombre, categoria, descripcion, estado) y el café especial del Huila (nombre,
variedad, region, proceso, estado), que es por empresa. Sus tablas, su API y su pantalla salen de las mismas
plantillas."""

import re

import pytest

from conftest import EJEMPLOS, RAIZ, RUTA_MIGRACION, cadena_yup, compacto
from inspectia_generador.carga import cargar_entidades
from inspectia_generador.generacion import generar_archivos
from inspectia_generador.salida import comprobar, escribir
from inspectia_generador.validacion import validar


@pytest.fixture
def entidades_producto():
    especificacion = RAIZ / 'specs' / 'entidades'
    return cargar_entidades(especificacion / 'empresa.json', especificacion / 'usuario.json',
                            EJEMPLOS / 'producto.json')


@pytest.fixture
def entidades_cafe():
    especificacion = RAIZ / 'specs' / 'entidades'
    return cargar_entidades(especificacion / 'empresa.json', especificacion / 'usuario.json',
                            EJEMPLOS / 'cafe.json')


@pytest.mark.caso('RP-23 · el JSON de producto de la pauta del docente pasa la validación')
def test_producto_es_valido(entidades_producto, codigos):
    assert validar(entidades_producto, codigos) == []


@pytest.mark.caso('RP-23 · el generador produce los archivos de otra temática: producto de la pauta del docente')
def test_genera_los_archivos_de_producto(entidades_producto, tmp_path):
    archivos = generar_archivos(entidades_producto)
    escribir(archivos, tmp_path)
    sql = (tmp_path / RUTA_MIGRACION).read_text(encoding='utf-8')

    assert re.findall(r'^CREATE TABLE (\w+) \($', sql, re.M) == ['empresa', 'producto', 'usuario']
    for columna in ('id uuid NOT NULL DEFAULT uuidv7()', 'nombre varchar(120) NOT NULL',
                    'categoria varchar(80) NOT NULL', 'descripcion varchar(500)',
                    "estado varchar(30) NOT NULL DEFAULT 'ACTIVO'"):
        assert f'\n    {columna},\n' in sql
    assert "CONSTRAINT producto_estado_valores CHECK (estado IN ('ACTIVO', 'INACTIVO'))" in sql
    assert comprobar(archivos, tmp_path).vacia


@pytest.mark.caso('RP-23 · el generador produce la API de producto, otra temática, con las mismas plantillas')
def test_genera_la_api_de_producto(entidades_producto):
    archivos = {a.ruta: a.contenido for a in generar_archivos(entidades_producto, api=('producto',))}
    base = 'backend/src/generated/java/co/edu/usco/inspectia/producto'

    for clase in ('Producto', 'ProductoEstado', 'ProductoRepositorio', 'ProductoServicio', 'ProductoControlador',
                  'ProductoAcceso', 'ProductoCrearSolicitud', 'ProductoEditarSolicitud', 'ProductoRespuesta',
                  'ProductoMapeador'):
        assert f'{base}/{clase}.java' in archivos
    assert 'backend/src/test-generated/java/co/edu/usco/inspectia/producto/ProductoApiGeneradaIT.java' in archivos
    controlador = archivos[f'{base}/ProductoControlador.java']
    assert '@RequestMapping("/api/v1/productos")' in controlador
    assert 'desactivar' not in controlador, 'producto no se desactiva: pasa a estado INACTIVO'
    assert 'name = "activo"' not in controlador, 'sin atributo_activo, el listado no se filtra por activo (D-33)'
    assert 'private ProductoEstado estado = ProductoEstado.ACTIVO;' in archivos[f'{base}/Producto.java']
    assert 'if (solicitud.estado() != null) {' in archivos[f'{base}/ProductoMapeador.java']


@pytest.mark.caso('RP-23 · el generador produce la pantalla de producto, otra temática, con las mismas plantillas del frontend')
def test_genera_la_pantalla_de_producto(entidades_producto, tmp_path):
    generados = generar_archivos(entidades_producto, api=('producto',), pantallas=('producto',))
    archivos = {a.ruta: a.contenido for a in generados}
    base = 'frontend/src/generated/producto'

    for nombre in ('productoApi.js', 'productoApi.test.js', 'productoEsquemas.js', 'productoEsquemas.test.js',
                   'ProductoPantalla.jsx', 'ProductoPantalla.test.jsx'):
        assert f'{base}/{nombre}' in archivos
    assert 'frontend/e2e/generated/producto.spec.js' in archivos
    pantallas = archivos['frontend/src/generated/pantallas.js']
    for propiedad in ("ruta: '/productos'", "roles: ['ADMIN_PLATAFORMA']", "icono: 'productos'"):
        assert propiedad in pantallas
    esquemas = archivos[f'{base}/productoEsquemas.js']
    assert cadena_yup(esquemas, 'esquemaParaCrear', 'descripcion') == [
        '.transform(recortar)', ".max(500, t('validacion.longitud_max', { max: 500 }))"], 'opcional: sin obligatorio'
    assert cadena_yup(esquemas, 'esquemaParaCrear', 'estado') == [
        ".oneOf(['ACTIVO', 'INACTIVO'], t('error.valor_invalido'))"], \
        'con valor por defecto no es obligatoria, y un código que no existe tiene el mensaje de la API'
    pantalla = compacto(archivos[f'{base}/ProductoPantalla.jsx'])
    assert "estado: 'ACTIVO'" in pantalla, 'el formulario empieza con el valor por defecto del JSON'
    assert "{ atributo: 'estado', etiqueta: 'producto.estado.etiqueta', valores: 'producto.estado' }" in pantalla
    assert "operaciones: { crear: ['ADMIN_PLATAFORMA'], editar: ['ADMIN_PLATAFORMA'] }" in pantalla
    assert 'atributoActivo: null' in pantalla
    api = archivos[f'{base}/productoApi.js']
    assert 'desactivar' not in api, 'producto no se desactiva: pasa a estado INACTIVO'
    assert "'activo'" not in api, 'sin atributo_activo, el listado no se filtra por activo (D-33)'
    assert "'producto.pantalla.nuevo': 'Nuevo producto'," in archivos['frontend/src/generated/textos.js']
    escribir(generados, tmp_path)
    assert comprobar(generados, tmp_path).vacia


@pytest.mark.caso('RP-23 · el JSON del café especial del Huila pasa la validación')
def test_cafe_es_valido(entidades_cafe, codigos):
    assert validar(entidades_cafe, codigos) == []


@pytest.mark.caso('RP-23 · el generador produce la tabla del café especial: por empresa y con los valores de sus listas')
def test_genera_la_tabla_de_cafe(entidades_cafe, tmp_path):
    archivos = generar_archivos(entidades_cafe)
    escribir(archivos, tmp_path)
    sql = (tmp_path / RUTA_MIGRACION).read_text(encoding='utf-8')

    assert re.findall(r'^CREATE TABLE (\w+) \($', sql, re.M) == ['empresa', 'cafe', 'usuario']
    for columna in ('id uuid NOT NULL DEFAULT uuidv7()', 'empresa_id uuid NOT NULL', 'nombre varchar(120) NOT NULL',
                    'variedad varchar(30) NOT NULL', 'region varchar(80) NOT NULL', 'proceso varchar(30) NOT NULL',
                    "estado varchar(30) NOT NULL DEFAULT 'ACTIVO'"):
        assert f'\n    {columna},\n' in sql
    for restriccion in ("cafe_variedad_valores CHECK (variedad IN ('CASTILLO', 'CATURRA', 'GEISHA'))",
                        "cafe_proceso_valores CHECK (proceso IN ('LAVADO', 'NATURAL', 'HONEY'))",
                        "cafe_estado_valores CHECK (estado IN ('ACTIVO', 'INACTIVO'))",
                        'cafe_empresa_id_fk FOREIGN KEY (empresa_id) REFERENCES empresa (id) ON DELETE RESTRICT'):
        assert f'CONSTRAINT {restriccion}' in sql
    assert comprobar(archivos, tmp_path).vacia


@pytest.mark.caso('RP-23 · el generador produce la API del café especial, por empresa, con sus pruebas de RNF-03')
def test_genera_la_api_de_cafe(entidades_cafe):
    archivos = {a.ruta: a.contenido for a in generar_archivos(entidades_cafe, api=('cafe',))}
    base = 'backend/src/generated/java/co/edu/usco/inspectia/cafe'

    for clase in ('Cafe', 'CafeVariedad', 'CafeProceso', 'CafeEstado', 'CafeRepositorio', 'CafeServicio',
                  'CafeControlador', 'CafeAcceso', 'CafeCrearSolicitud', 'CafeEditarSolicitud', 'CafeRespuesta',
                  'CafeMapeador'):
        assert f'{base}/{clase}.java' in archivos
    controlador = archivos[f'{base}/CafeControlador.java']
    assert '@RequestMapping("/api/v1/cafes")' in controlador
    assert controlador.count('@PreAuthorize("hasRole(\'ADMIN_EMPRESA\')")') == 4, 'listar, ver, crear y editar'
    prueba = archivos['backend/src/test-generated/java/co/edu/usco/inspectia/cafe/CafeApiGeneradaIT.java']
    assert re.findall(r'@DisplayName\("(RNF-03 · [^"]+)"\)', prueba) == [
        'RNF-03 · un registro de cafe de otra empresa responde 404 en cada operación que lo usa',
        'RNF-03 · C2 · el listado de cafe solo trae los registros de la empresa de la sesión',
        'RNF-03 · el registro nuevo de cafe queda en la empresa de la sesión',
    ], 'la empresa sale de la sesión: lo que se ve en la base con la columna empresa_id'


@pytest.mark.caso('RP-23 · el generador produce la pantalla del café especial: sus listas sin valor por defecto empiezan vacías')
def test_genera_la_pantalla_de_cafe(entidades_cafe, tmp_path):
    generados = generar_archivos(entidades_cafe, api=('cafe',), pantallas=('cafe',))
    archivos = {a.ruta: a.contenido for a in generados}
    base = 'frontend/src/generated/cafe'

    for nombre in ('cafeApi.js', 'cafeApi.test.js', 'cafeEsquemas.js', 'cafeEsquemas.test.js', 'CafePantalla.jsx',
                   'CafePantalla.test.jsx'):
        assert f'{base}/{nombre}' in archivos
    assert 'frontend/e2e/generated/cafe.spec.js' in archivos
    pantallas = archivos['frontend/src/generated/pantallas.js']
    for propiedad in ("ruta: '/cafes'", "roles: ['ADMIN_EMPRESA']", "icono: 'cafe'"):
        assert propiedad in pantallas
    esquemas = archivos[f'{base}/cafeEsquemas.js']
    for lista, valores in (('variedad', "['CASTILLO', 'CATURRA', 'GEISHA']"),
                           ('proceso', "['LAVADO', 'NATURAL', 'HONEY']")):
        assert cadena_yup(esquemas, 'esquemaParaCrear', lista) == [
            '.transform(vacioComoAusente)', ".test('obligatorio', t('validacion.obligatorio'), lleno)",
            f".oneOf({valores}, t('error.valor_invalido'))"], 'sin valor por defecto, elegir es obligatorio'
    pantalla = compacto(archivos[f'{base}/CafePantalla.jsx'])
    assert "return { nombre: '', variedad: '', region: '', proceso: '', estado: 'ACTIVO' };" in pantalla, \
        'al registrar, variedad y proceso empiezan sin elegir, y estado con su valor por defecto'
    assert ("{ atributo: 'nombre', etiqueta: 'cafe.nombre.etiqueta', "
            "debajo: { atributo: 'region', texto: null } }") in pantalla, 'la región va bajo el nombre'
    for columna in ('variedad', 'proceso', 'estado'):
        assert f"{{ atributo: '{columna}', etiqueta: 'cafe.{columna}.etiqueta', valores: 'cafe.{columna}' }}" in pantalla
    assert "operaciones: { crear: ['ADMIN_EMPRESA'], editar: ['ADMIN_EMPRESA'] }" in pantalla
    textos = archivos['frontend/src/generated/textos.js']
    assert "'cafe.pantalla.nuevo': 'Nuevo café especial'," in textos
    assert "'cafe.pantalla.nuevo': 'New specialty coffee'," in textos
    escribir(generados, tmp_path)
    assert comprobar(generados, tmp_path).vacia
