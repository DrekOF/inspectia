/*
 * Nombre: accesibilidad.spec.js
 * Descripción: Pruebas de accesibilidad de punta a punta (RNF-06 C4, RP-08): axe revisa las pautas WCAG 2.2 AA
 *              en cada pantalla de esta versión y en los dos temas, y se inicia sesión solo con el teclado.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import AxeBuilder from '@axe-core/playwright';
import { datos, escribirYEntrar, expect, iniciarSesion, test } from './ayudas.js';

const PAUTAS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'];

/**
 * Revisa una pantalla con axe y espera que no tenga fallas de las pautas WCAG AA.
 *
 * @param {import('@playwright/test').Page} page la página, ya en la pantalla
 * @param {string} pantalla el nombre de la pantalla, para el mensaje de error
 */
async function sinFallas(page, pantalla) {
  const resultado = await new AxeBuilder({ page }).withTags(PAUTAS).analyze();
  expect(resultado.violations.map((falla) => `${falla.id}: ${falla.help}`), pantalla).toEqual([]);
}

/**
 * Recorre las pantallas de esta versión en un tema y revisa cada una.
 *
 * @param {import('@playwright/test').Page} page la página
 * @param {'light'|'dark'} esquema el tema del navegador: la app usa el del sistema por defecto
 */
async function revisarPantallas(page, esquema) {
  await page.emulateMedia({ colorScheme: esquema });
  // Cada revisión espera a que la app dibuje la pantalla: sobre una página vacía, axe no encontraría nada.
  await page.goto('/iniciar-sesion');
  await expect(page.getByRole('main')).toBeVisible();
  await sinFallas(page, 'Iniciar sesión');
  await page.goto('/acerca-de');
  await expect(page.getByRole('main')).toBeVisible();
  await sinFallas(page, 'Acerca de, sin sesión');
  await iniciarSesion(page, datos.adminEmpresa);
  await sinFallas(page, 'Inicio');
  for (const ruta of ['/cambiar-contrasena', '/acerca-de', '/estadisticas']) {
    await page.goto(ruta);
    await expect(page.getByRole('main')).toBeVisible();
    await sinFallas(page, ruta);
  }
}

test('RNF-06 · C4 · las pantallas de esta versión no tienen fallas de accesibilidad WCAG AA en el tema claro', async ({ page }) => {
  await revisarPantallas(page, 'light');
});

test('RNF-06 · C4 · las pantallas de esta versión no tienen fallas de accesibilidad WCAG AA en el tema oscuro', async ({ page }) => {
  await revisarPantallas(page, 'dark');
});

test('RNF-06 · C4 · se inicia sesión solo con el teclado', async ({ page, isMobile }) => {
  test.skip(isMobile, 'En el celular no hay teclado físico');
  await page.goto('/iniciar-sesion');
  // La app dibuja el formulario después de cargar la página: el Tab va cuando ya está.
  await expect(page.getByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();

  await page.keyboard.press('Tab');
  await expect(page.getByLabel('Correo', { exact: true })).toBeFocused();
  await page.keyboard.type(datos.adminEmpresa.correo);
  await page.keyboard.press('Tab');
  await expect(page.getByLabel('Contraseña', { exact: true })).toBeFocused();
  await page.keyboard.type(datos.adminEmpresa.contrasena);
  await page.keyboard.press('Enter');

  await expect(page.getByRole('heading', { name: 'Hola, Ana' })).toBeVisible();
});

test('RNF-06 · C3 · un error de un campo dice qué pasó y queda unido al campo para los lectores de pantalla', async ({ page }) => {
  await page.goto('/iniciar-sesion');

  await escribirYEntrar(page, { correo: '', contrasena: '' });

  const correo = page.getByLabel('Correo', { exact: true });
  await expect(correo).toHaveAttribute('aria-invalid', 'true');
  await expect(correo).toHaveAccessibleDescription('Este dato es obligatorio.');
});
