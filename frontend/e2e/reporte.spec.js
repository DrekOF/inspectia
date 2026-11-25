/*
 * Nombre: reporte.spec.js
 * Descripción: Pruebas de punta a punta del reporte PDF del lote (RF-15 C1, C4 y C5, RNF-03): «Descargar PDF» en el
 *              resultado guarda el archivo InspectIA-<código>-es.pdf, la administradora también lo baja, descargarlo de
 *              nuevo da el mismo archivo, la segunda muestra lo rehace, y un lote de otra empresa no se descarga. El
 *              contenido del PDF lo revisan las pruebas del backend con Apache PDFBox.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { readFile } from 'node:fs/promises';
import {
  crearEmpresaConUsuarioFinal, expect, iniciarSesion, loteAnalizado, segundaMuestraPorLaApi, test, tokenDe,
} from './ayudas.js';

/**
 * Pulsa «Descargar PDF» y espera el archivo.
 *
 * @param {import('@playwright/test').Page} page la página, con el resultado de un lote analizado
 * @returns {Promise<{ nombre: string, contenido: Buffer }>} el nombre que propone el navegador y el contenido
 */
async function descargar(page) {
  const descarga = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Descargar PDF' }).click();
  const archivo = await descarga;
  return { nombre: archivo.suggestedFilename(), contenido: await readFile(await archivo.path()) };
}

/**
 * El reporte de un lote por la API, con la sesión de una persona.
 *
 * @param {import('@playwright/test').APIRequestContext} request el cliente de la API de Playwright
 * @param {{ correo: string, contrasena: string }} persona quien lo pide
 * @param {string} loteId el lote
 * @returns {Promise<import('@playwright/test').APIResponse>} la respuesta
 */
async function reportePorLaApi(request, persona, loteId) {
  return request.get(`/api/v1/lotes/${loteId}/reporte?idioma=es`,
    { headers: { Authorization: `Bearer ${await tokenDe(request, persona)}` } });
}

test('RF-15 · C1 · el usuario final pulsa «Descargar PDF» en el resultado del lote y guarda el reporte como InspectIA-<código>-es.pdf (de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const lote = await loteAnalizado(request, usuarioFinal, [106, 107]);
  await iniciarSesion(page, usuarioFinal);
  await page.goto(`/lotes/${lote.id}`);

  const { nombre, contenido } = await descargar(page);

  expect(nombre).toBe(`InspectIA-${lote.codigo}-es.pdf`);
  expect(contenido.subarray(0, 5).toString('latin1')).toBe('%PDF-');
  await expect(page.getByRole('button', { name: 'Descargar PDF' })).toBeEnabled();
});

test('RF-15 · C4 · al descargarlo de nuevo, también la administradora, se obtiene el mismo reporte (de punta a punta)', async ({ page, request }) => {
  const { empresa, usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const lote = await loteAnalizado(request, usuarioFinal, [106, 107]);
  await iniciarSesion(page, usuarioFinal);
  await page.goto(`/lotes/${lote.id}`);
  const primero = await descargar(page);

  const segundo = await reportePorLaApi(request, empresa.administrador, lote.id);

  expect(segundo.status()).toBe(200);
  expect(segundo.headers()['content-type']).toContain('application/pdf');
  expect(Buffer.compare(await segundo.body(), primero.contenido)).toBe(0);
});

test('RF-15 · C5 · después de la segunda muestra, el reporte se rehace con las dos: ya no es el de la primera (de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const lote = await loteAnalizado(request, usuarioFinal, [106, 107]);
  const deLaPrimera = await reportePorLaApi(request, usuarioFinal, lote.id);
  expect(deLaPrimera.status()).toBe(200);

  await segundaMuestraPorLaApi(request, usuarioFinal, lote.id, [108, 107]);
  await iniciarSesion(page, usuarioFinal);
  await page.goto(`/lotes/${lote.id}`);
  const deLasDos = await descargar(page);

  expect(deLasDos.contenido.subarray(0, 5).toString('latin1')).toBe('%PDF-');
  expect(Buffer.compare(deLasDos.contenido, await deLaPrimera.body())).not.toBe(0);
  expect(deLasDos.contenido.length).toBeGreaterThan((await deLaPrimera.body()).length);
});

test('RNF-03 · el reporte de un lote de otra empresa no se descarga: la API responde 404 (de punta a punta)', async ({ request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const lote = await loteAnalizado(request, usuarioFinal, [106, 107]);
  const { usuarioFinal: deOtra } = await crearEmpresaConUsuarioFinal(request);

  const respuesta = await reportePorLaApi(request, deOtra, lote.id);

  expect(respuesta.status()).toBe(404);
});
