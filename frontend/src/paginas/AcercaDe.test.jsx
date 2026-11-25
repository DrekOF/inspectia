/*
 * Nombre: AcercaDe.test.jsx
 * Descripción: Pruebas de la página Acerca de (RNF-08): la cita del dataset con su autor, el artículo, la
 *              licencia CC BY-NC 4.0 y el enlace a Kaggle, la nota de que InspectIA no se cobra, y que se abre
 *              con sesión desde «Mi cuenta» y sin sesión. También que Acerca de, en español y en inglés, cita el
 *              dataset y el artículo de los autores con su DOI (RNF-08 C2); la cita en los README se revisa en
 *              generador/pruebas/test_cita_dataset.py, porque Stryker copia solo frontend/.
 * Fecha de creación: 20/10/2026
 * Actualización: 25/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
import { screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import i18n from '../i18n/i18n.js';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';

const KAGGLE = 'https://www.kaggle.com/datasets/luiszhinin979/ecuadorian-cocoa-beans-for-classification';

/** El artículo de los autores del dataset (RNF-08 C2): su título y su DOI. */
const ARTICULO = 'Artificial Vision Technique to Detect and Classify Cocoa Beans';
const DOI = 'https://doi.org/10.1007/978-3-031-43078-7_18';

/**
 * El valor de un dato de la cita, buscado por su nombre («Dataset», «Autor», «Artículo» o «Licencia»).
 *
 * @param {string} nombre el nombre del dato
 * @returns {string} su valor
 */
function dato(nombre) {
  const termino = screen.getByText(nombre, { selector: 'dt' });
  return termino.nextElementSibling.textContent;
}

describe('Acerca de', () => {
  it('RNF-08 · C1 · Acerca de cita el dataset con su nombre, su autor, el artículo de los autores, la licencia CC BY-NC 4.0 y el enlace a Kaggle', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    const { usuario } = abrirApp('/');

    await usuario.click(within(await screen.findByRole('navigation', { name: 'Menú principal' }))
      .getByRole('link', { name: 'Acerca de' }));

    expect(await screen.findByRole('heading', { name: 'Acerca de InspectIA' })).toBeInTheDocument();
    expect(dato('Dataset')).toContain('cacao-beans-for-classification');
    expect(dato('Autor')).toContain('Luis Fernando Zhinin Vera');
    expect(dato('Artículo')).toContain('Zhinin-Vera, L. et al. (2023)');
    expect(dato('Licencia')).toContain('CC BY-NC 4.0');
    expect(screen.getByRole('link', { name: 'Ver el dataset en Kaggle' })).toHaveAttribute('href', KAGGLE);
  });

  it('RNF-08 · Acerca de dice que InspectIA no se cobra mientras use modelos entrenados con esos datos', () => {
    abrirApp('/acerca-de');

    expect(screen.getByText('Por esa licencia, InspectIA no se cobra mientras use modelos entrenados con estos datos.'))
      .toBeInTheDocument();
  });

  it('RNF-08 · C1 · Acerca de se abre sin sesión, con un enlace para volver a Iniciar sesión', async () => {
    const { usuario } = abrirApp('/acerca-de');

    expect(screen.getByRole('heading', { name: 'Acerca de InspectIA' })).toBeInTheDocument();
    expect(screen.queryByRole('navigation', { name: 'Menú principal' })).not.toBeInTheDocument();

    await usuario.click(screen.getByRole('link', { name: 'Volver a Iniciar sesión' }));
    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();
  });

  it('RNF-08 · Acerca de muestra la versión de la app', () => {
    abrirApp('/acerca-de');

    expect(screen.getByText(/^Versión \d+\.\d+\.\d+$/)).toBeInTheDocument();
  });

  it.each([
    ['es', 'Acerca de InspectIA', 'Artículo', 'Licencia', 'Ver el artículo (DOI 10.1007/978-3-031-43078-7_18)'],
    ['en', 'About InspectIA', 'Article', 'License', 'See the article (DOI 10.1007/978-3-031-43078-7_18)'],
  ])('RNF-08 · C2 · Acerca de cita el dataset y el artículo de los autores (Zhinin-Vera et al., 2023) con su título y el enlace a su DOI, con la licencia CC BY-NC 4.0: %s', async (idioma, titulo, articulo, licencia, enlace) => {
    await i18n.changeLanguage(idioma);
    abrirApp('/acerca-de');

    expect(screen.getByRole('heading', { name: titulo })).toBeInTheDocument();
    expect(dato('Dataset')).toContain('cacao-beans-for-classification');
    expect(dato(articulo)).toContain('Zhinin-Vera, L. et al. (2023)');
    expect(dato(articulo)).toContain(ARTICULO);
    expect(dato(licencia)).toContain('CC BY-NC 4.0');
    expect(screen.getByRole('link', { name: enlace })).toHaveAttribute('href', DOI);
    expect(screen.getByRole('link', { name: idioma === 'es' ? 'Ver el dataset en Kaggle' : 'See the dataset on Kaggle' }))
      .toHaveAttribute('href', KAGGLE);
  });
});
