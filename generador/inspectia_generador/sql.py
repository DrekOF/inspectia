# Nombre: sql.py
# Descripción: Arma las tablas de PostgreSQL (columnas, restricciones e índices) desde las entidades.
# Fecha de creación: 13/10/2026
# Actualización: 13/10/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Arma la descripción de cada tabla de PostgreSQL que luego escribe la plantilla del esquema.

Las reglas de nombres y de orden son fijas, para que la salida sea siempre la misma (determinista).
"""

from dataclasses import dataclass

from inspectia_generador.modelo import (EMPRESA_ID, atributo, orden_de_creacion, tiene_empresa,
                                        valores_lista)

#: Largo máximo de un identificador en PostgreSQL.
LARGO_IDENTIFICADOR = 63


class ErrorGeneracion(Exception):
    """Algo de las entidades no se puede llevar a SQL (por ejemplo, un nombre demasiado largo)."""


@dataclass(frozen=True)
class ColumnaSql:
    """Una columna: su nombre, su definición (tipo, NOT NULL, DEFAULT) y su comentario."""

    nombre: str
    definicion: str
    comentario: str


@dataclass(frozen=True)
class TablaSql:
    """Una tabla lista para la plantilla: columnas, restricciones, índices y comentario."""

    nombre: str
    comentario: str
    columnas: tuple
    restricciones: tuple
    indices: tuple


def literal(texto):
    """Literal de texto de SQL, con las comillas simples duplicadas."""
    return "'" + str(texto).replace("'", "''") + "'"


def tipo_sql(atr):
    """Tipo de PostgreSQL de un atributo (tabla de tipos de specs/entidades/README.md)."""
    tipo = atr['tipo']
    if tipo in ('texto', 'contrasena'):
        return f"varchar({atr['longitud']})"
    if tipo == 'decimal':
        return f"numeric({atr['precision']},{atr['escala']})"
    return {'entero': 'integer', 'booleano': 'boolean', 'fecha': 'date', 'fecha_hora': 'timestamptz',
            'lista': 'varchar(30)', 'referencia': 'uuid'}[tipo]


def _valor_por_defecto(valor):
    if isinstance(valor, bool):
        return 'true' if valor else 'false'
    if isinstance(valor, (int, float)):
        return str(valor)
    return literal(valor)


def _expresion(entidad, columna, normalizar):
    """Columna tal cual o, si se normaliza un texto, ``lower(btrim(columna))``."""
    atr = atributo(entidad, columna)
    if normalizar and atr is not None and atr['tipo'] == 'texto':
        return f'lower(btrim({columna}))'
    return columna


def _numero(valor):
    return str(int(valor)) if float(valor).is_integer() else str(valor)


def construir_tablas(entidades):
    """Describe las tablas de todas las entidades, en orden de creación.

    Raises:
        ErrorGeneracion: si algún nombre de restricción, índice o trigger pasa de 63 caracteres.
    """
    referenciadas_por_empresa = set()
    for e in entidades.values():
        for a in e['atributos']:
            destino = entidades[a['referencia']['entidad']] if a['tipo'] == 'referencia' else None
            if destino is not None and tiene_empresa(e) and tiene_empresa(destino):
                referenciadas_por_empresa.add(destino['entidad'])
    tablas = [_tabla(entidades, entidades[n], n in referenciadas_por_empresa)
              for n in orden_de_creacion(entidades)]
    largos = [n for t in tablas for n in _nombres(t) if len(n) > LARGO_IDENTIFICADOR]
    if largos:
        raise ErrorGeneracion('Nombres de más de 63 caracteres: ' + ', '.join(largos))
    return tablas


def _nombres(tabla):
    nombres = [r.split()[1] for r in tabla.restricciones]
    nombres += [i.split(' INDEX ')[1].split()[0] for i in tabla.indices]
    return nombres + [f'{tabla.nombre}_fecha_actualizacion']


def _tabla(entidades, e, referenciada_por_empresa):
    t = e['entidad']
    columnas = [ColumnaSql('id', 'uuid NOT NULL DEFAULT uuidv7()', literal('Identificador (UUID v7)'))]
    if tiene_empresa(e):
        obligatoria = ' NOT NULL' if e['por_empresa'] == 'si' else ''
        columnas.append(ColumnaSql(EMPRESA_ID, f'uuid{obligatoria}',
                                   literal('Empresa dueña del registro (RNF-03)')))
    for a in e['atributos']:
        definicion = tipo_sql(a) + (' NOT NULL' if a.get('obligatorio') else '')
        if 'por_defecto' in a:
            definicion += ' DEFAULT ' + _valor_por_defecto(a['por_defecto'])
        comentario = f"{a['etiquetas']['es']} ({', '.join(a['fichas'])})"
        columnas.append(ColumnaSql(a['nombre'], definicion, literal(comentario)))
    columnas += [
        ColumnaSql('fecha_creacion', 'timestamptz NOT NULL DEFAULT now()',
                   literal('Fecha de creación del registro')),
        ColumnaSql('fecha_actualizacion', 'timestamptz NOT NULL DEFAULT now()',
                   literal('Fecha de la última actualización; la mantiene un trigger')),
    ]

    restricciones = [f'CONSTRAINT {t}_pk PRIMARY KEY (id)']
    cubiertas = {'id'}  # columnas que ya encabezan un índice
    for a in e['atributos']:
        if a.get('unico'):
            restricciones.append(f"CONSTRAINT {t}_{a['nombre']}_unico UNIQUE ({a['nombre']})")
            cubiertas.add(a['nombre'])
    if referenciada_por_empresa:
        restricciones.append(f'CONSTRAINT {t}_id_empresa_unico UNIQUE (id, {EMPRESA_ID})')
    indices_unicos = []
    for u in e.get('unicos', []):
        cols = u['atributos']
        nombre = f"{t}_{'_'.join(cols)}_unico"
        cubiertas.add(cols[0])
        if u.get('normalizar') or 'donde' in u:
            expresiones = ', '.join(_expresion(e, c, u.get('normalizar')) for c in cols)
            donde = f" WHERE {u['donde']}" if 'donde' in u else ''
            indices_unicos.append(f'CREATE UNIQUE INDEX {nombre} ON {t} ({expresiones}){donde};')
        else:
            restricciones.append(f"CONSTRAINT {nombre} UNIQUE ({', '.join(cols)})")
    for a in e['atributos']:
        restricciones += _checks_de_columna(entidades, t, a)
    for check in e.get('checks', []):
        restricciones.append(f"CONSTRAINT {check['nombre']} CHECK ({check['sql']})")
    restricciones += _claves_foraneas(entidades, e)

    indices_extra = []
    for indice in e.get('indices', []):
        cols = indice['atributos']
        cubiertas.add(cols[0])
        expresiones = ', '.join(_expresion(e, c, indice.get('normalizar')) for c in cols)
        indices_extra.append(f"CREATE INDEX {t}_{'_'.join(cols)}_idx ON {t} ({expresiones});")
    referencias = ([EMPRESA_ID] if tiene_empresa(e) else []) + [
        a['nombre'] for a in e['atributos'] if a['tipo'] == 'referencia']
    indices_fk = [f'CREATE INDEX {t}_{c}_idx ON {t} ({c});' for c in referencias if c not in cubiertas]

    return TablaSql(t, literal(e['descripcion']), tuple(columnas), tuple(restricciones),
                    tuple(indices_unicos + indices_fk + indices_extra))


def _checks_de_columna(entidades, t, a):
    columna = a['nombre']
    checks = []
    if a['tipo'] == 'lista':
        valores = ', '.join(literal(v) for v in valores_lista(entidades, a))
        checks.append(f'CONSTRAINT {t}_{columna}_valores CHECK ({columna} IN ({valores}))')
    v = a.get('validaciones', {})
    if a['tipo'] in ('entero', 'decimal'):
        if 'min' in v and 'max' in v:
            checks.append(f"CONSTRAINT {t}_{columna}_rango CHECK "
                          f"({columna} BETWEEN {_numero(v['min'])} AND {_numero(v['max'])})")
        elif 'min' in v:
            checks.append(f"CONSTRAINT {t}_{columna}_minimo CHECK ({columna} >= {_numero(v['min'])})")
        elif 'max' in v:
            checks.append(f"CONSTRAINT {t}_{columna}_maximo CHECK ({columna} <= {_numero(v['max'])})")
    if 'patron' in v:
        checks.append(f"CONSTRAINT {t}_{columna}_patron CHECK ({columna} ~ {literal(v['patron'])})")
    return checks


def _claves_foraneas(entidades, e):
    t = e['entidad']
    claves = []
    if tiene_empresa(e):
        claves.append(f'CONSTRAINT {t}_{EMPRESA_ID}_fk FOREIGN KEY ({EMPRESA_ID}) '
                      f'REFERENCES empresa (id) ON DELETE RESTRICT')
    for a in e['atributos']:
        if a['tipo'] != 'referencia':
            continue
        destino = entidades[a['referencia']['entidad']]
        al_borrar = 'CASCADE' if a['referencia']['al_borrar'] == 'cascada' else 'RESTRICT'
        if tiene_empresa(e) and tiene_empresa(destino):
            origen, referidas = f"{a['nombre']}, {EMPRESA_ID}", f'id, {EMPRESA_ID}'
        else:
            origen, referidas = a['nombre'], 'id'
        claves.append(f"CONSTRAINT {t}_{a['nombre']}_fk FOREIGN KEY ({origen}) "
                      f"REFERENCES {destino['entidad']} ({referidas}) ON DELETE {al_borrar}")
    return claves
