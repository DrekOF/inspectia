/*
 * Nombre: responsive.spec.js
 * Descripción: Pruebas de punta a punta del diseño para PC y celular (RNF-05): ninguna pantalla se desplaza a
 *              los lados desde 360 px de ancho, y las pruebas corren en tamaño de PC y de celular.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { datos, expect, iniciarSesion, test } from './ayudas.js';

/**
 * Espera que la página no se desplace a los lados: su contenido cabe en el ancho de la ventana.
 *
 * @param {import('@playwright/test').Page} page la página
 * @param {string} pantalla el nombre de la pantalla, para el mensaje de error
 */
async function sinDesplazamientoLateral(page, pantalla) {
  const { contenido, ventana } = await page.evaluate(() => ({
    contenido: document.documentElement.scrollWidth, ventana: document.documentElement.clientWidth }));
  expect(contenido, pantalla).toBeLessThanOrEqual(ventana);
}

test('RNF-05 · C1 · ninguna pantalla se desplaza a los lados desde 360 px de ancho', async ({ page }) => {
  await page.setViewportSize({ width: 360, height: 740 });
  await page.goto('/iniciar-sesion');
  await sinDesplazamientoLateral(page, 'Iniciar sesión');
  await page.goto('/acerca-de');
  await sinDesplazamientoLateral(page, 'Acerca de, sin sesión');
  await iniciarSesion(page, datos.adminEmpresa);
  await sinDesplazamientoLateral(page, 'Inicio');
  for (const ruta of ['/cambiar-contrasena', '/acerca-de', '/estadisticas']) {
    await page.goto(ruta);
    await expect(page.getByRole('main')).toBeVisible();
    await sinDesplazamientoLateral(page, ruta);
  }
});

test('RNF-05 · C3 · las pruebas corren en tamaño de PC y de celular: en PC el menú está a la vista y en el celular, detrás del botón', async ({ page, isMobile }) => {
  await iniciarSesion(page, datos.adminEmpresa);
  const menu = page.getByRole('navigation', { name: 'Menú principal' });

  if (isMobile) {
    expect(page.viewportSize().width).toBeLessThanOrEqual(430);
    await expect(menu).toBeHidden();
    await page.getByRole('button', { name: 'Abrir el menú' }).click();
  } else {
    expect(page.viewportSize().width).toBeGreaterThanOrEqual(1280);
  }

  await expect(menu).toBeVisible();
});
