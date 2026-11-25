# Nombre: test_clasificacion.py
# Descripción: Pruebas de la regla de la clase y la confianza de un grano (RF-08): no sano desde 0,5 de
#              probabilidad de mala y confianza de la clase elegida x 100, con 2 decimales.
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Pruebas de ``inspectia_ia.clasificador.a_clasificacion`` (no necesita TensorFlow)."""

import pytest

from inspectia_ia.clasificador import NO_SANO, SANO, a_clasificacion


@pytest.mark.caso('RF-08 · C1 · un grano es no sano desde 0,5 de probabilidad de no sano, y sano por debajo')
@pytest.mark.parametrize('p_mala, clase', [(0.5, NO_SANO), (0.4999, SANO), (0.97, NO_SANO), (0.0, SANO),
                                           (1.0, NO_SANO)])
def test_umbral_de_no_sano(p_mala, clase):
    assert a_clasificacion(1 - p_mala, p_mala).clase == clase


@pytest.mark.caso('RF-08 · C2 · la confianza es la probabilidad de la clase elegida x 100, con 2 decimales')
@pytest.mark.parametrize('p_sana, p_mala, confianza', [(0.272343, 0.727657, 72.77), (0.92612, 0.07388, 92.61),
                                                       (0.5, 0.5, 50.0), (0.123456, 0.876544, 87.65)])
def test_confianza(p_sana, p_mala, confianza):
    c = a_clasificacion(p_sana, p_mala)
    assert c.confianza == confianza
    assert 0 <= c.confianza <= 100
