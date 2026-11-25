# Nombre: test_sin_cobro.py
# Descripción: Pruebas de RNF-08 C3 sobre el repositorio: InspectIA no tiene con qué cobrar (ninguna tabla, API,
#              pantalla ni el servicio de IA habla de precios, pagos o suscripciones) y el README y Acerca de dicen que
#              no se cobra mientras use modelos entrenados con el dataset de licencia CC BY-NC 4.0.
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Pruebas de RNF-08 C3: «Mientras use modelos entrenados con este dataset, InspectIA no se cobra».

Que no se cobre es un compromiso del equipo, no algo que el código pueda demostrar; lo que sí se puede revisar es que
el sistema no tenga con qué cobrar y que lo diga donde lo leen las personas. ``docs/verificaciones.md`` explica el
resto. No se revisa ``infra/``: el presupuesto de AWS es lo que la cuenta del equipo gasta, no algo que se cobre.
"""

import re

import pytest

from conftest import RAIZ

#: Las palabras de un cobro, en español y en inglés, como palabras enteras (``apagar`` no cuenta). Se buscan en el
#: texto separado por ``palabras``, así también aparecen dentro de un identificador, como ``precio_unitario``.
COBRO = re.compile(r'\b(precios?|pagos?|pagar|tarifas?|cobros?|cobrar|facturas?|facturaci[oó]n|suscripci[oó]n(es)?|'
                   r'payments?|billing|invoices?|subscriptions?|stripe|paypal|checkout|prices?|pricing)\b',
                   re.IGNORECASE)

#: Lo que arma el sistema: las entidades de las que salen las tablas, la API y las pantallas generadas (RP-23), el
#: código del backend, del frontend y del servicio de IA, y las plantillas del generador.
CARPETAS = ('specs/entidades', 'backend/src/main', 'backend/src/generated', 'frontend/src', 'ia-servicio/inspectia_ia',
            'generador/plantillas')
EXTENSIONES = {'.json', '.java', '.properties', '.sql', '.yml', '.yaml', '.js', '.jsx', '.py', '.j2'}


def palabras(linea):
    """La línea con sus identificadores separados en palabras: ``precio_unitario`` y ``precioUnitario`` dan
    ``precio unitario``."""
    return re.sub(r'([a-z0-9])([A-Z])', r'\1 \2', linea).replace('_', ' ')


@pytest.mark.caso('RNF-08 · C3 · ninguna tabla, API, pantalla ni el servicio de IA tiene precios, pagos, facturas ni '
                  'suscripciones: InspectIA no tiene con qué cobrar')
def test_no_hay_con_que_cobrar():
    encontradas = []
    for carpeta in CARPETAS:
        for ruta in sorted((RAIZ / carpeta).rglob('*')):
            if ruta.suffix not in EXTENSIONES or 'node_modules' in ruta.parts or not ruta.is_file():
                continue
            for numero, linea in enumerate(ruta.read_text(encoding='utf-8').splitlines(), 1):
                if COBRO.search(palabras(linea)):
                    encontradas.append(f'{ruta.relative_to(RAIZ).as_posix()}:{numero}: {linea.strip()}')

    assert encontradas == [], 'Hay algo para cobrar (RNF-08 C3):\n' + '\n'.join(encontradas)


@pytest.mark.caso('RNF-08 · C3 · el README y Acerca de, en español y en inglés, dicen que InspectIA no se cobra mientras '
                  'use modelos entrenados con el dataset')
def test_dice_que_no_se_cobra():
    readme = (RAIZ / 'README.md').read_text(encoding='utf-8')
    espanol = (RAIZ / 'frontend/src/i18n/es.js').read_text(encoding='utf-8')
    ingles = (RAIZ / 'frontend/src/i18n/en.js').read_text(encoding='utf-8')

    assert 'Mientras InspectIA use modelos entrenados con este dataset, no se cobra.' in readme
    assert "'acercaDe.nota': 'Por esa licencia, InspectIA no se cobra mientras use modelos entrenados con estos " \
           "datos.'" in espanol
    assert "'acercaDe.nota': 'Because of that license, InspectIA is free of charge while it uses models trained " \
           "with this data.'" in ingles


@pytest.mark.caso('RNF-08 · C3 · la búsqueda de cobros encuentra las palabras de un cobro y no confunde «apagar» ni '
                  '«plan»')
@pytest.mark.parametrize('texto, encuentra', [
    ('precio_por_lote', True),
    ('precioPorLote', True),
    ('subscriptionId', True),
    ('El precio del análisis', True),
    ('POST /api/v1/pagos', True),
    ('const billing = 1;', True),
    ('apagar el servidor', False),
    ('plan de despliegue', False),
])
def test_la_busqueda_de_cobros(texto, encuentra):
    assert bool(COBRO.search(palabras(texto))) is encuentra
