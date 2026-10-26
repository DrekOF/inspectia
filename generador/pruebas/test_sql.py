# Nombre: test_sql.py
# Descripción: Pruebas del esquema SQL que genera el generador desde specs/entidades.
# Fecha de creación: 13/10/2026
# Actualización: 26/10/2026
# Autor: Equipo InspectIA
# Versión: 1.1
"""Pruebas del script de creación de las tablas (migración V1 de Flyway)."""

import re

import pytest

from conftest import RUTA_MIGRACION
from inspectia_generador.generacion import generar_archivos


@pytest.fixture
def sql(entidades):
    archivos = {a.ruta: a.contenido for a in generar_archivos(entidades)}
    return archivos[RUTA_MIGRACION]


@pytest.mark.caso('RP-15 · el esquema crea las 8 tablas en orden de dependencias')
def test_crea_las_tablas_en_orden(sql):
    tablas = re.findall(r'^CREATE TABLE (\w+) \($', sql, re.M)

    assert tablas == ['empresa', 'modelo', 'rango_recomendacion', 'usuario',
                      'lote', 'muestra', 'foto', 'grano']


@pytest.mark.caso('RP-15 · cada tabla tiene id UUID v7, fechas y el trigger de fecha_actualizacion')
def test_columnas_comunes_y_triggers(sql):
    assert sql.count('    id uuid NOT NULL DEFAULT uuidv7(),') == 8
    assert sql.count('    fecha_creacion timestamptz NOT NULL DEFAULT now(),') == 8
    assert sql.count('    fecha_actualizacion timestamptz NOT NULL DEFAULT now(),') == 8
    assert len(re.findall(r'^CREATE TRIGGER \w+_fecha_actualizacion BEFORE UPDATE', sql, re.M)) == 8


@pytest.mark.caso('RP-15 · las listas, patrones y CHECK de los JSON salen como restricciones')
def test_restricciones_de_columnas(sql):
    assert "CONSTRAINT empresa_nit_unico UNIQUE (nit)" in sql
    assert "CONSTRAINT empresa_nit_patron CHECK (nit ~ '^[0-9]{6,10}-[0-9]$')" in sql
    assert ("CONSTRAINT usuario_rol_valores CHECK "
            "(rol IN ('USUARIO_FINAL', 'ADMIN_EMPRESA', 'ADMIN_PLATAFORMA'))") in sql
    assert ("CONSTRAINT lote_categoria_valores CHECK "
            "(categoria IN ('PREMIUM', 'ESTANDAR', 'CORRIENTE', 'FUERA_DE_NORMA'))") in sql
    assert "CONSTRAINT muestra_repeticiones_rango CHECK (repeticiones BETWEEN 0 AND 2)" in sql
    assert ("CONSTRAINT usuario_empresa_segun_rol CHECK "
            "((rol = 'ADMIN_PLATAFORMA') = (empresa_id IS NULL))") in sql


@pytest.mark.caso('RP-15 · la tabla usuario guarda la versión de sesión, un entero que empieza en 0 y no baja de 0, y ya no la hora sesiones_desde')
def test_version_de_sesion(sql):
    assert '    version_sesion integer NOT NULL DEFAULT 0,' in sql
    assert 'CONSTRAINT usuario_version_sesion_minimo CHECK (version_sesion >= 0)' in sql
    assert 'sesiones_desde' not in sql


@pytest.mark.caso('RP-15 · las tablas de cada empresa se referencian con FK compuestas (registro, empresa)')
def test_claves_foraneas(sql):
    assert ("CONSTRAINT lote_usuario_id_fk FOREIGN KEY (usuario_id, empresa_id) "
            "REFERENCES usuario (id, empresa_id) ON DELETE RESTRICT") in sql
    assert ("CONSTRAINT grano_foto_id_fk FOREIGN KEY (foto_id, empresa_id) "
            "REFERENCES foto (id, empresa_id) ON DELETE CASCADE") in sql
    assert ("CONSTRAINT muestra_modelo_id_fk FOREIGN KEY (modelo_id) "
            "REFERENCES modelo (id) ON DELETE RESTRICT") in sql
    assert "CONSTRAINT usuario_id_empresa_unico UNIQUE (id, empresa_id)" in sql


@pytest.mark.caso('RP-15 · los únicos normalizados y parciales salen como índices únicos')
def test_indices_unicos(sql):
    assert "CREATE UNIQUE INDEX lote_empresa_id_codigo_unico ON lote (empresa_id, lower(btrim(codigo)));" in sql
    assert "CREATE UNIQUE INDEX usuario_correo_unico ON usuario (lower(btrim(correo)));" in sql
    assert "CREATE UNIQUE INDEX modelo_en_uso_unico ON modelo (en_uso) WHERE en_uso;" in sql


@pytest.mark.caso('RP-15 · cada referencia tiene índice, salvo si otro índice ya empieza por ella')
def test_indices_de_referencias(sql):
    assert "CREATE INDEX muestra_usuario_id_idx ON muestra (usuario_id);" in sql
    assert "CREATE INDEX usuario_empresa_id_idx ON usuario (empresa_id);" in sql
    assert "muestra_lote_id_idx" not in sql
    assert "rango_recomendacion_empresa_id_idx" not in sql


@pytest.mark.caso('RP-15 · ningún nombre de restricción, índice o trigger pasa de 63 caracteres')
def test_nombres_caben_en_postgresql(sql):
    nombres = re.findall(r'(?:CONSTRAINT|INDEX|TRIGGER) (\w+)', sql)

    assert nombres
    assert max(len(n) for n in nombres) <= 63
