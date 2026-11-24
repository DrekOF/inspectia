# Nombre: test_configuracion.py
# Descripción: Pruebas de la configuración del servicio de IA leída de variables de entorno: el lote de
#              clasificación por defecto cabe en el límite de memoria del contenedor (RNF-01).
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Pruebas de ``configuracion.py``."""

import pytest

from inspectia_ia.configuracion import Configuracion, desde_entorno


@pytest.mark.caso('RNF-01 · el lote de clasificación por defecto es 16, para que 150 granos quepan en la memoria '
                  'del contenedor')
def test_lote_de_clasificacion_por_defecto():
    assert Configuracion().lote_clasificacion == 16
    assert desde_entorno({}).lote_clasificacion == 16


@pytest.mark.caso('RNF-01 · INSPECTIA_LOTE_CLASIFICACION cambia el lote de clasificación')
def test_lote_de_clasificacion_desde_el_entorno():
    assert desde_entorno({'INSPECTIA_LOTE_CLASIFICACION': '32'}).lote_clasificacion == 32
    with pytest.raises(ValueError, match='INSPECTIA_LOTE_CLASIFICACION'):
        desde_entorno({'INSPECTIA_LOTE_CLASIFICACION': 'muchos'})
