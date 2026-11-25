# Nombre: test_cita_dataset.py
# Descripción: Pruebas de RNF-08 C2 sobre el repositorio: el README y el del servicio de IA citan el dataset de Kaggle
#              con su autor y su licencia CC BY-NC 4.0, y el artículo de los autores con su título y su DOI.
# Fecha de creación: 25/11/2026
# Actualización: 25/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Pruebas de RNF-08 C2 sobre los README del repositorio.

Están aquí, y no en las pruebas del frontend, porque leen archivos fuera de ``frontend/``: la prueba de mutación
(Stryker, RP-14) copia solo ``frontend/`` y no los encontraría. La cita en la página Acerca de, en español y en inglés,
se revisa en ``frontend/src/paginas/AcercaDe.test.jsx``.
"""

import re

import pytest

from conftest import RAIZ

KAGGLE = 'https://www.kaggle.com/datasets/luiszhinin979/ecuadorian-cocoa-beans-for-classification'
ARTICULO = 'Artificial Vision Technique to Detect and Classify Cocoa Beans'
DOI = 'https://doi.org/10.1007/978-3-031-43078-7_18'


@pytest.mark.caso('RNF-08 · C2 · el README del repositorio cita el dataset de Kaggle con su autor y su licencia '
                  'CC BY-NC 4.0, y el artículo de los autores (Zhinin-Vera et al., 2023) con su título y su DOI')
def test_readme_cita_dataset_y_articulo():
    readme = (RAIZ / 'README.md').read_text(encoding='utf-8')
    inicio = readme.find('## Dataset y licencia (RNF-08)')
    seccion = readme[inicio:]

    assert inicio >= 0
    assert 'Zhinin Vera, L. F. (2022). *cacao-beans-for-classification' in seccion
    assert KAGGLE in seccion
    assert '[CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/)' in seccion
    assert re.search(r'Zhinin-Vera, L\., .+ \(2023\)\. Artificial Vision Technique', seccion)
    assert ARTICULO in seccion
    assert DOI in seccion


@pytest.mark.caso('RNF-08 · C2 · la documentación del servicio de IA, que usa el modelo y los recortes del dataset, '
                  'cita el dataset, su licencia y el artículo de los autores con su DOI')
def test_readme_del_servicio_de_ia_cita_dataset_y_articulo():
    readme = (RAIZ / 'ia-servicio' / 'README.md').read_text(encoding='utf-8')

    assert '## Dataset' in readme
    assert 'Ecuadorian cacao beans for classification' in readme
    assert 'CC BY-NC 4.0' in readme
    assert f'Zhinin-Vera, L. et al. (2023). {ARTICULO}' in readme
    assert 'DOI 10.1007/978-3-031-43078-7_18' in readme
