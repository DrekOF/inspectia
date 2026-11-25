/*
 * Nombre: AcercaDe.test.jsx
 * Descripción: Pruebas de la página Acerca de (RNF-08): la cita del dataset con su autor, el artículo, la
 *              licencia CC BY-NC 4.0 y el enlace a Kaggle, la nota de que InspectIA no se cobra, y que se abre
 *              con sesión desde «Mi cuenta» y sin sesión. También que el README del repositorio y el del servicio
 *              de IA, y Acerca de en español y en inglés, citan el dataset y el artículo de los autores con su DOI
 *              (RNF-08 C2).
 * Fecha de creación: 20/10/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import i18n from '../i18n/i18n.js';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';

const KAGGLE = 'https://www.kaggle.com/datasets/luiszhinin979/ecuadorian-cocoa-beans-for-classification';

/** El artículo de los autores del dataset (RNF-08 C2): su título y su DOI. */
const ARTICULO = 'Artificial Vision Technique to Detect and Classify Cocoa Beans';
const DOI = 'https://doi.org/10.1007/978-3-031-43078-7_18';

/** La raíz del repositorio, tres carpetas arriba de esta (frontend/src/paginas). */
const RAIZ = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');

/**
 * Un archivo del repositorio, leído como texto.
 *
 * @param {string} ruta la ruta desde la raíz del repositorio
 * @returns {string} su contenido
 */
function delRepositorio(ruta) {
  return readFileSync(resolve(RAIZ, ruta), 'utf-8');
}

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

  it('RNF-08 · C2 · el README del repositorio cita el dataset de Kaggle con su autor y su licencia CC BY-NC 4.0, y el artículo de los autores (Zhinin-Vera et al., 2023) con su título y su DOI', () => {
    const readme = delRepositorio('README.md');
    const inicio = readme.indexOf('## Dataset y licencia (RNF-08)');
    const seccion = readme.slice(inicio);

    expect(inicio).toBeGreaterThanOrEqual(0);
    expect(seccion).toContain('Zhinin Vera, L. F. (2022). *cacao-beans-for-classification');
    expect(seccion).toContain(KAGGLE);
    expect(seccion).toContain('[CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/)');
    expect(seccion).toMatch(/Zhinin-Vera, L\., .+ \(2023\)\. Artificial Vision Technique/);
    expect(seccion).toContain(ARTICULO);
    expect(seccion).toContain(DOI);
  });

  it('RNF-08 · C2 · la documentación del servicio de IA, que usa el modelo y los recortes del dataset, cita el dataset, su licencia y el artículo de los autores con su DOI', () => {
    const readme = delRepositorio('ia-servicio/README.md');

    expect(readme).toContain('## Dataset');
    expect(readme).toContain('Ecuadorian cacao beans for classification');
    expect(readme).toContain('CC BY-NC 4.0');
    expect(readme).toContain(`Zhinin-Vera, L. et al. (2023). ${ARTICULO}`);
    expect(readme).toContain('DOI 10.1007/978-3-031-43078-7_18');
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
