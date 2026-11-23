/*
 * Nombre: lotes.spec.js
 * Descripción: Pruebas de punta a punta de los lotes (RF-04, RF-14, RNF-03): registrar un lote en la pantalla y
 *              verlo pendiente en el historial, el código repetido, la fecha posterior a hoy o de hace más de un
 *              año, las sugerencias de finca o proveedor sin tildes, con el alto de los demás campos y sin el botón
 *              «Clear», el historial sin lotes de otra empresa y sus páginas de 20.
 * Fecha de creación: 22/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { randomUUID } from 'node:crypto';
import {
  crearEmpresaConUsuarioFinal, expect, iniciarSesion, RECEPCION, registrarLotes, test,
} from './ayudas.js';
import { diaAnterior, diaEnColombia, haceUnAnioEnColombia } from './fechas.js';

/**
 * Un código de lote nuevo en cada prueba y en cada navegador: las pruebas corren a la vez sobre la misma base.
 *
 * @param {string} [prefijo] el comienzo del código
 * @returns {string} el código, de 13 caracteres como máximo
 */
function codigoNuevo(prefijo = 'E2E') {
  return `${prefijo}-${randomUUID().slice(0, 8)}`;
}

/**
 * Escribe los datos del lote en Nuevo lote y pulsa «Continuar a las fotos». Cierra las sugerencias de la finca antes
 * de seguir, para que no tapen los demás campos.
 *
 * @param {import('@playwright/test').Page} page la página, en Nuevo lote
 * @param {{ codigo: string, fecha?: string, origen?: string }} datos lo que escribe
 */
async function registrarEnLaPantalla(page, { codigo, fecha = RECEPCION, origen = 'Finca La Esperanza' }) {
  await page.getByLabel('Código del lote').fill(codigo);
  await page.getByLabel('Fecha de recepción').fill(fecha);
  await page.getByLabel('Finca o proveedor').fill(origen);
  await page.getByLabel('Finca o proveedor').press('Escape');
  await page.getByRole('button', { name: 'Continuar a las fotos' }).click();
}

test('RF-04 · C1 · con datos válidos el lote queda registrado, la pantalla pasa a las fotos y el historial lo muestra pendiente (de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const codigo = codigoNuevo('L');
  await iniciarSesion(page, usuarioFinal);
  await page.goto('/lotes/nuevo');

  await registrarEnLaPantalla(page, { codigo });

  await expect(page.getByRole('heading', { name: 'Fotos de la muestra' })).toBeVisible();
  await expect(page.getByText(`Lote ${codigo} · Finca La Esperanza.`, { exact: false })).toBeVisible();
  await expect(page).toHaveURL(/\/lotes\/[0-9a-f-]+\/fotos$/);
  await expect(page.getByRole('region', { name: 'Guía de captura' })).toBeVisible();
  await page.goto('/historial');
  await expect(page.getByText(codigo, { exact: true })).toBeVisible();
  await expect(page.getByText('Pendiente de análisis')).toBeVisible();
});

test('RF-04 · C2 · si el código ya existe en la empresa, la pantalla lo avisa y no registra el lote (de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const codigo = codigoNuevo('L');
  await registrarLotes(request, usuarioFinal, [{ codigo }]);
  await iniciarSesion(page, usuarioFinal);
  await page.goto('/lotes/nuevo');

  await registrarEnLaPantalla(page, { codigo, origen: 'Finca El Paraíso' });

  await expect(page.getByText(`Ya existe un lote con el código ${codigo} en tu empresa. Usa otro código.`)).toBeVisible();
  await expect(page.getByLabel('Código del lote')).toHaveAttribute('aria-invalid', 'true');
  await expect(page.getByRole('heading', { name: 'Nuevo lote' })).toBeVisible();
});

test('RF-04 · C3 · el código en minúsculas y con un espacio al final cuenta como repetido (de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const codigo = codigoNuevo('L').toUpperCase();
  await registrarLotes(request, usuarioFinal, [{ codigo }]);
  await iniciarSesion(page, usuarioFinal);
  await page.goto('/lotes/nuevo');

  await registrarEnLaPantalla(page, { codigo: `${codigo.toLowerCase()} ` });

  await expect(page.getByText(`Ya existe un lote con el código ${codigo.toLowerCase()} en tu empresa. Usa otro código.`))
    .toBeVisible();
  await page.goto('/historial');
  await expect(page.getByText(codigo, { exact: true })).toBeVisible();
  await expect(page.getByText(codigo.toLowerCase(), { exact: true })).toHaveCount(0);
});

test('RF-04 · C4 · una fecha de recepción posterior a hoy en Colombia se rechaza en la pantalla, y la de hoy pasa (prueba de límite, de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  await iniciarSesion(page, usuarioFinal);
  await page.goto('/lotes/nuevo');
  await expect(page.getByLabel('Fecha de recepción')).toHaveAttribute('max', diaEnColombia(0));

  await registrarEnLaPantalla(page, { codigo: codigoNuevo('L'), fecha: diaEnColombia(1) });
  await expect(page.getByText('La fecha de recepción no puede ser posterior a hoy.')).toBeVisible();

  await page.getByLabel('Fecha de recepción').fill(diaEnColombia(0));
  await page.getByRole('button', { name: 'Continuar a las fotos' }).click();
  await expect(page.getByRole('heading', { name: 'Fotos de la muestra' })).toBeVisible();
});

test('RF-04 · una fecha de recepción de hace más de un año en Colombia se rechaza en la pantalla, y la de hace exactamente un año pasa (prueba de límite, de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const haceUnAnio = haceUnAnioEnColombia();
  await iniciarSesion(page, usuarioFinal);
  await page.goto('/lotes/nuevo');
  await expect(page.getByLabel('Fecha de recepción')).toHaveAttribute('min', haceUnAnio);

  await registrarEnLaPantalla(page, { codigo: codigoNuevo('L'), fecha: diaAnterior(haceUnAnio) });
  await expect(page.getByText('La fecha de recepción no puede ser de hace más de un año.')).toBeVisible();

  await page.getByLabel('Fecha de recepción').fill(haceUnAnio);
  await page.getByRole('button', { name: 'Continuar a las fotos' }).click();
  await expect(page.getByRole('heading', { name: 'Fotos de la muestra' })).toBeVisible();
});

test('RF-04 · al escribir la finca sin tilde, la pantalla sugiere la que ya usó la empresa con tilde, y al elegirla la escribe (de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  await registrarLotes(request, usuarioFinal, [{ codigo: codigoNuevo('L'), origen: 'Finca El Paraíso' }]);
  await iniciarSesion(page, usuarioFinal);
  await page.goto('/lotes/nuevo');

  await page.getByLabel('Finca o proveedor').fill('paraiso');

  const sugerencias = page.getByRole('listbox', { name: 'Fincas y proveedores que ya usó tu empresa' });
  await sugerencias.getByRole('option', { name: 'Finca El Paraíso' }).click();
  await expect(page.getByLabel('Finca o proveedor')).toHaveValue('Finca El Paraíso');
});

test('RF-04 · el campo «Finca o proveedor» mide lo mismo que los demás campos, como en el prototipo, y no tiene el botón «Clear» sin traducir (de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  await iniciarSesion(page, usuarioFinal);
  await page.goto('/lotes/nuevo');
  /**
   * El alto de la caja de un campo, sin su etiqueta ni su ayuda.
   *
   * @param {string} etiqueta la etiqueta del campo
   * @returns {Promise<number>} el alto en píxeles
   */
  const alto = async (etiqueta) => (await page.getByLabel(etiqueta).locator('xpath=..').boundingBox()).height;

  await page.getByLabel('Finca o proveedor').fill('Finca Nueva');

  expect(await alto('Finca o proveedor')).toBe(await alto('Código del lote'));
  await expect(page.getByRole('button', { name: 'Clear' })).toHaveCount(0);
});

test('RF-14 · C1 · el historial solo muestra los lotes de la empresa del usuario: los de otra empresa no aparecen (de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const otra = await crearEmpresaConUsuarioFinal(request);
  const propio = codigoNuevo('L');
  const ajeno = codigoNuevo('S');
  await registrarLotes(request, usuarioFinal, [{ codigo: propio }]);
  await registrarLotes(request, otra.usuarioFinal, [{ codigo: ajeno }]);
  await iniciarSesion(page, usuarioFinal);

  await page.goto('/historial');

  await expect(page.getByText(propio, { exact: true })).toBeVisible();
  await expect(page.getByText(ajeno, { exact: true })).toHaveCount(0);
  await expect(page.getByText('1 lote · 20 por página')).toBeVisible();
});

test('RF-14 · C4 · con más de 20 lotes, el historial se divide en páginas de 20, del más reciente al más antiguo (de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const prefijo = `P${randomUUID().slice(0, 6)}`;
  const codigos = Array.from({ length: 21 }, (_, indice) => `${prefijo}-${String(indice + 1).padStart(2, '0')}`);
  await registrarLotes(request, usuarioFinal, codigos.map((codigo) => ({ codigo })));
  await iniciarSesion(page, usuarioFinal);

  await page.goto('/historial');

  const paginas = page.getByRole('navigation', { name: 'Páginas del historial' });
  await expect(paginas.getByText('21 lotes · 20 por página')).toBeVisible();
  await expect(page.getByText(codigos[20], { exact: true })).toBeVisible();
  await expect(page.getByText(codigos[0], { exact: true })).toHaveCount(0);
  await paginas.getByRole('button', { name: 'Página siguiente' }).click();
  await expect(page.getByText(codigos[0], { exact: true })).toBeVisible();
  await expect(page.getByText(codigos[20], { exact: true })).toHaveCount(0);
  await expect(paginas.getByRole('button', { name: 'Página siguiente' })).toBeDisabled();
});
