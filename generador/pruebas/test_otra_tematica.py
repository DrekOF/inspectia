# Nombre: test_otra_tematica.py
# Descripción: Demostración de que el generador sirve para otra temática: la entidad producto de la
#              pauta del docente para el back-end (RP-23). No entra a la app.
# Fecha de creación: 13/10/2026
# Actualización: 13/10/2026
# Autor: Equipo InspectIA
# Versión: 1.1
"""Demostración de otra temática: el producto de la pauta del docente (id, nombre, categoria,
descripcion, estado), junto con las entidades de la plataforma (empresa y usuario)."""

import re

import pytest

from conftest import EJEMPLOS, RAIZ, RUTA_MIGRACION
from inspectia_generador.carga import cargar_entidades
from inspectia_generador.generacion import generar_archivos
from inspectia_generador.salida import comprobar, escribir
from inspectia_generador.validacion import validar


@pytest.fixture
def entidades_producto():
    especificacion = RAIZ / 'specs' / 'entidades'
    return cargar_entidades(especificacion / 'empresa.json', especificacion / 'usuario.json',
                            EJEMPLOS / 'producto.json')


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
    assert 'private ProductoEstado estado = ProductoEstado.ACTIVO;' in archivos[f'{base}/Producto.java']
    assert 'if (solicitud.estado() != null) {' in archivos[f'{base}/ProductoMapeador.java']
