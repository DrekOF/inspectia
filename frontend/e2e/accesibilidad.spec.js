/*
 * Nombre: accesibilidad.spec.js
 * Descripción: Pruebas de accesibilidad de punta a punta (RNF-06 C4, RP-08): axe revisa las pautas WCAG 2.2 AA
 *              en cada pantalla de esta versión y en los dos temas, también en las de los lotes (RF-04, RF-14) y en
 *              las fotos de la muestra con una foto subida y un error a la vista (RF-05) y en el resultado de un lote,
 *              con el globo de un grano abierto (RF-10, RF-11), en el historial con los filtros, un error y el chip
 *              «Sugiere otra muestra» (RF-14 C2) y en el resultado con la sugerencia, con la espera de la muestra 2 y
 *              con dos muestras (RF-14 C6), en «Modelo de IA» (RF-17), y se inicia sesión solo con el
 *              teclado.
 * Fecha de creación: 20/10/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.5
 */
import { randomUUID } from 'node:crypto';
import AxeBuilder from '@axe-core/playwright';
import {
  abrirFiltros, casillaDeFoto, crearEmpresaConUsuarioFinal, datos, escribirYEntrar, expect, filtrarElHistorial,
  iniciarSesion, loteAnalizado, loteConDosMuestras, loteDelHistorial, registrarLotes, retenerLaEspera, seVeLaImagen,
  subirFotosPorLaApi, test,
} from './ayudas.js';
import { diaEnColombia } from './fechas.js';
import { PDF, png } from './imagenes.js';

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
  for (const ruta of ['/cambiar-contrasena', '/acerca-de', '/estadisticas', '/historial', '/modelo']) {
    await page.goto(ruta);
    await expect(page.getByRole('main')).toBeVisible();
    await expect(page.getByRole('progressbar')).toHaveCount(0);
    await sinFallas(page, ruta);
  }
}

test('RNF-06 · C4 · las pantallas de esta versión no tienen fallas de accesibilidad WCAG AA en el tema claro', async ({ page }) => {
  await revisarPantallas(page, 'light');
});

test('RNF-06 · C4 · las pantallas de esta versión no tienen fallas de accesibilidad WCAG AA en el tema oscuro', async ({ page }) => {
  await revisarPantallas(page, 'dark');
});

test('RNF-06 · C4 · las pantallas de los lotes no tienen fallas de accesibilidad WCAG AA en los dos temas, tampoco con los errores del formulario a la vista, con una foto subida y otra rechazada ni con el resultado y el globo de un grano abierto', async ({ page, request, isMobile }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const [lote] = await registrarLotes(request, usuarioFinal, [{ codigo: `A-${randomUUID().slice(0, 8)}` }]);
  await subirFotosPorLaApi(request, usuarioFinal, lote.id, [{ numero: 1, archivo: png() }]);
  const analizado = await loteAnalizado(request, usuarioFinal, [106, 107]);
  await iniciarSesion(page, usuarioFinal);
  for (const esquema of ['light', 'dark']) {
    await page.emulateMedia({ colorScheme: esquema });
    await page.goto('/lotes/nuevo');
    await expect(page.getByRole('heading', { name: 'Nuevo lote' })).toBeVisible();
    await sinFallas(page, `Nuevo lote, tema ${esquema}`);
    await page.getByRole('button', { name: 'Continuar a las fotos' }).click();
    await expect(page.getByText('Este dato es obligatorio.').first()).toBeVisible();
    await sinFallas(page, `Nuevo lote con los errores, tema ${esquema}`);
    await page.goto('/historial');
    await expect(page.getByText(lote.codigo, { exact: true })).toBeVisible();
    await sinFallas(page, `Historial, tema ${esquema}`);
    await page.goto(`/lotes/${lote.id}/fotos`);
    await expect(page.getByRole('region', { name: 'Guía de captura' })).toBeVisible();
    await seVeLaImagen(casillaDeFoto(page, 1).getByRole('img', { name: 'Foto 1 de la muestra' }));
    await page.getByLabel('Archivo de la foto 2').setInputFiles({ name: 'informe.pdf', mimeType: 'application/pdf',
      buffer: PDF });
    await expect(casillaDeFoto(page, 2).getByRole('alert')).toBeVisible();
    await sinFallas(page, `Fotos de la muestra, con una foto subida y otra rechazada, tema ${esquema}`);
    await page.goto(`/lotes/${analizado.id}`);
    const grano = page.getByRole('group', { name: 'Granos de la foto 1' }).getByRole('button', { name: 'Grano 1: no sano' });
    await expect(grano).toBeVisible();
    await sinFallas(page, `Resultado del lote, tema ${esquema}`);
    if (isMobile) {
      await grano.tap();
    } else {
      await grano.hover();
    }
    await expect(page.getByRole('tooltip')).toBeVisible();
    await sinFallas(page, `Resultado del lote con el globo de un grano abierto, tema ${esquema}`);
  }
});

test('RNF-06 · C4 · el historial con los filtros, un error y el chip «Sugiere otra muestra», y el resultado con la sugerencia, con la espera de la muestra 2 y con dos muestras, no tienen fallas de accesibilidad WCAG AA en los dos temas', async ({ page, request, isMobile }) => {
  // Cuatro análisis y diez revisiones.
  test.slow();
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const conSugerencia = await loteAnalizado(request, usuarioFinal, [106, 107]);
  // La muestra 2 con sus fotos subidas, sin enviar: la prueba la retiene en la cola para mirar su espera.
  const enEspera = await loteAnalizado(request, usuarioFinal, [106, 107]);
  await subirFotosPorLaApi(request, usuarioFinal, enEspera.id, [
    { numero: 1, archivo: png({ ancho: 108, alto: 100 }) }, { numero: 2, archivo: png({ ancho: 107, alto: 100 }) },
  ], { muestra: 2 });
  const dosMuestras = await loteConDosMuestras(request, usuarioFinal);
  await iniciarSesion(page, usuarioFinal);
  for (const esquema of ['light', 'dark']) {
    await page.emulateMedia({ colorScheme: esquema });
    await page.goto('/historial?estado=ANALIZADO');
    await expect(loteDelHistorial(page, conSugerencia.codigo).getByText('Sugiere otra muestra', { exact: true }))
      .toBeVisible();
    const formulario = await abrirFiltros(page, isMobile);
    await sinFallas(page, `Historial con un filtro y el chip, tema ${esquema}`);
    await filtrarElHistorial(formulario, { desde: diaEnColombia(0), hasta: diaEnColombia(-1), estado: 'Analizado' });
    await expect(formulario.getByText('La fecha «Hasta» no puede ser anterior a «Resultado desde».')).toBeVisible();
    await sinFallas(page, `Historial con un error en los filtros, tema ${esquema}`);
    await page.goto(`/lotes/${conSugerencia.id}`);
    await expect(page.getByRole('region', { name: 'Sugerencia' })).toBeVisible();
    await sinFallas(page, `Resultado con la sugerencia de otra muestra, tema ${esquema}`);
    const espera = await retenerLaEspera(page, request, usuarioFinal, enEspera.id);
    await page.goto(`/lotes/${enEspera.id}`);
    await expect(page.getByRole('note')).toBeVisible();
    await sinFallas(page, `Resultado con la espera de la muestra 2, tema ${esquema}`);
    await espera.soltar();
    await page.goto(`/lotes/${dosMuestras.id}`);
    await expect(page.getByRole('heading', { level: 3, name: 'Muestra 2' })).toBeVisible();
    await sinFallas(page, `Resultado con dos muestras, tema ${esquema}`);
  }
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
