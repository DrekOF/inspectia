/*
 * Nombre: responsive.spec.js
 * Descripción: Pruebas de punta a punta del diseño para PC y celular (RNF-05): ninguna pantalla se desplaza a
 *              los lados desde 360 px de ancho, también las de los lotes (RF-04, RF-14), y las pruebas corren en
 *              tamaño de PC y de celular.
 * Fecha de creación: 20/10/2026
 * Actualización: 22/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { randomUUID } from 'node:crypto';
import { crearEmpresaConUsuarioFinal, datos, expect, iniciarSesion, registrarLotes, test } from './ayudas.js';

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
  // Cada medida espera a que la app dibuje la pantalla: una página vacía nunca se desplaza.
  await page.goto('/iniciar-sesion');
  await expect(page.getByRole('main')).toBeVisible();
  await sinDesplazamientoLateral(page, 'Iniciar sesión');
  await page.goto('/acerca-de');
  await expect(page.getByRole('main')).toBeVisible();
  await sinDesplazamientoLateral(page, 'Acerca de, sin sesión');
  await iniciarSesion(page, datos.adminEmpresa);
  await sinDesplazamientoLateral(page, 'Inicio');
  for (const ruta of ['/cambiar-contrasena', '/acerca-de', '/estadisticas', '/historial']) {
    await page.goto(ruta);
    await expect(page.getByRole('main')).toBeVisible();
    await expect(page.getByRole('progressbar')).toHaveCount(0);
    await sinDesplazamientoLateral(page, ruta);
  }
});

test('RNF-05 · C1 · las pantallas de los lotes no se desplazan a los lados desde 360 px de ancho', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const [lote] = await registrarLotes(request, usuarioFinal, [{ codigo: `R-${randomUUID().slice(0, 8)}` }]);
  await page.setViewportSize({ width: 360, height: 740 });
  await iniciarSesion(page, usuarioFinal);

  await page.goto('/lotes/nuevo');
  await expect(page.getByRole('heading', { name: 'Nuevo lote' })).toBeVisible();
  await sinDesplazamientoLateral(page, 'Nuevo lote');
  await page.goto('/historial');
  await expect(page.getByText(lote.codigo, { exact: true })).toBeVisible();
  await sinDesplazamientoLateral(page, 'Historial');
  await page.goto(`/lotes/${lote.id}/fotos`);
  await expect(page.getByRole('region', { name: 'Guía de captura' })).toBeVisible();
  await sinDesplazamientoLateral(page, 'Fotos de la muestra');
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
