/*
 * Nombre: acerca-de.spec.js
 * Descripción: Pruebas de punta a punta de Acerca de (RNF-08 C1): se abre sin sesión desde Iniciar sesión y
 *              con sesión desde «Mi cuenta», con la cita del dataset y su licencia.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { abrirMenu, datos, expect, iniciarSesion, test } from './ayudas.js';

/**
 * Revisa que la página muestre la cita del dataset: nombre, autor, artículo, licencia y enlace.
 *
 * @param {import('@playwright/test').Page} page la página, en Acerca de
 */
async function citaElDataset(page) {
  await expect(page.getByRole('heading', { name: 'Acerca de InspectIA' })).toBeVisible();
  for (const texto of ['cacao-beans-for-classification', 'Luis Fernando Zhinin Vera', 'Zhinin-Vera, L. et al. (2023)',
    'CC BY-NC 4.0', 'InspectIA no se cobra']) {
    await expect(page.getByText(texto, { exact: false }).first()).toBeVisible();
  }
  await expect(page.getByRole('link', { name: 'Ver el dataset en Kaggle' }))
    .toHaveAttribute('href', 'https://www.kaggle.com/datasets/luiszhinin979/ecuadorian-cocoa-beans-for-classification');
}

test('RNF-08 · C1 · sin sesión, desde Iniciar sesión se abre Acerca de con la cita del dataset, sus autores y su licencia', async ({ page }) => {
  await page.goto('/iniciar-sesion');

  await page.getByRole('link', { name: 'Acerca de InspectIA' }).click();

  await citaElDataset(page);
});

test('RNF-08 · C1 · con sesión, Acerca de se abre desde «Mi cuenta»', async ({ page, isMobile }) => {
  await iniciarSesion(page, datos.adminEmpresa);

  await (await abrirMenu(page, isMobile)).getByRole('link', { name: 'Acerca de' }).click();

  await citaElDataset(page);
});
