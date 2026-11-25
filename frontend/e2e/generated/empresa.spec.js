/*
 * Nombre: empresa.spec.js
 * Descripción: Pruebas generadas de punta a punta de la pantalla de empresa: permisos, accesibilidad y
 *              ancho de celular.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.11)
 * Versión: 1.11
 * Entidades: empresa 1.3, usuario 1.8
 */
import AxeBuilder from '@axe-core/playwright';
import { expect, iniciarSesion, personaDelRol, test } from '../ayudas.js';

/** La pantalla en la app. */
const RUTA = '/empresas';

/** Las pautas WCAG que revisa axe (RNF-06 C4). */
const PAUTAS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'];

/**
 * Abre la pantalla, con la sesión ya iniciada, y espera a que termine de cargar su lista.
 *
 * @param {import('@playwright/test').Page} page la página
 */
async function abrirPantalla(page) {
  await page.goto(RUTA);
  await expect(page.getByRole('heading', { name: 'Empresas', level: 1 })).toBeVisible();
  await expect(page.getByRole('progressbar')).toHaveCount(0);
}

test('RNF-04 · la pantalla de empresa la abre ADMIN_PLATAFORMA, uno de los roles de listar (de punta a punta)', async ({ page }) => {
  await iniciarSesion(page, personaDelRol('ADMIN_PLATAFORMA'));
  await abrirPantalla(page);
});

test('RNF-04 · la pantalla de empresa no la abre USUARIO_FINAL, que no está en los roles de listar (de punta a punta)', async ({ page }) => {
  await iniciarSesion(page, personaDelRol('USUARIO_FINAL'));
  await page.goto(RUTA);
  await expect(page.getByRole('heading', { name: 'Sin permiso', level: 1 })).toBeVisible();
});

test('RNF-04 · la pantalla de empresa no la abre ADMIN_EMPRESA, que no está en los roles de listar (de punta a punta)', async ({ page }) => {
  await iniciarSesion(page, personaDelRol('ADMIN_EMPRESA'));
  await page.goto(RUTA);
  await expect(page.getByRole('heading', { name: 'Sin permiso', level: 1 })).toBeVisible();
});

test('RNF-06 · C4 · la pantalla de empresa no tiene fallas de accesibilidad WCAG AA, ni en el tema claro ni en el oscuro', async ({ page }) => {
  await iniciarSesion(page, personaDelRol('ADMIN_PLATAFORMA'));
  for (const esquema of ['light', 'dark']) {
    await page.emulateMedia({ colorScheme: esquema });
    await abrirPantalla(page);
    const resultado = await new AxeBuilder({ page }).withTags(PAUTAS).analyze();
    expect(resultado.violations.map((falla) => `${falla.id}: ${falla.help}`), esquema).toEqual([]);
  }
});

test('RNF-05 · C1 · la pantalla de empresa no se desplaza a los lados desde 360 px de ancho', async ({ page }) => {
  await page.setViewportSize({ width: 360, height: 740 });
  await iniciarSesion(page, personaDelRol('ADMIN_PLATAFORMA'));
  await abrirPantalla(page);
  const { contenido, ventana } = await page.evaluate(() => ({
    contenido: document.documentElement.scrollWidth, ventana: document.documentElement.clientWidth }));
  expect(contenido).toBeLessThanOrEqual(ventana);
});
