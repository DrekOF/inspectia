/*
 * Nombre: historial.spec.js
 * Descripción: Pruebas de punta a punta de los filtros del Historial de lotes (RF-14 C2, RF-03 C6): la fecha del
 *              resultado, el estado, la categoría, el código y quien registró el lote, solos y combinados, en la
 *              pantalla y con los lotes de una empresa propia; la dirección que los conserva al volver de «Ver»; la
 *              lista vacía con «Limpiar filtros»; el chip «Sugiere otra muestra», y los lotes de un usuario
 *              desactivado, que siguen en el historial y en la lista «Usuario». Las fotos se hacen en la prueba
 *              (RNF-08): el clasificador falso da 50 granos por foto y tantos no sanos como su ancho módulo 50.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { randomUUID } from 'node:crypto';
import {
  abrirFiltros, crearEmpresaConUsuarioFinal, crearUsuarioFinalEn, desactivarUsuarioEn, expect, filtrarElHistorial,
  iniciarSesion, loteAnalizado, loteDelHistorial, registrarLotes, seVenSoloLosLotes, test,
} from './ayudas.js';
import { diaEnColombia } from './fechas.js';

/**
 * El comienzo de los códigos de los lotes de una prueba: una letra y 6 cifras hexadecimales, nuevas en cada prueba y
 * en cada navegador, que no tienen «s» ni «t».
 *
 * @returns {string} el comienzo
 */
function prefijoNuevo() {
  return `F${randomUUID().slice(0, 6)}`;
}

/**
 * Los parámetros de la dirección de la página, como un objeto.
 *
 * @param {import('@playwright/test').Page} page la página
 * @returns {Record<string, string>} los parámetros
 */
function parametrosDe(page) {
  return Object.fromEntries(new URL(page.url()).searchParams);
}

test('RF-14 · C2 · los filtros por fecha del resultado, estado, categoría, código y usuario funcionan solos y combinados en la pantalla, la dirección los conserva al volver de «Ver» y «Limpiar» los quita (de punta a punta)', async ({ page, request, isMobile }) => {
  // Cuatro análisis y muchos pasos por la pantalla.
  test.slow();
  const { empresa, usuarioFinal: marta } = await crearEmpresaConUsuarioFinal(request);
  const pedro = await crearUsuarioFinalEn(request, empresa, 'Pedro Salas');
  const prefijo = prefijoNuevo();
  const [est, pre, cor, fdn, pen] = ['EST', 'PRE', 'COR', 'FDN', 'PEN'].map((sufijo) => `${prefijo}-${sufijo}`);
  const todos = [est, pre, cor, fdn, pen];
  // Con los rangos por defecto: 87 de 100 sanos dan 90,6 % (Estándar, sugiere otra), 93 dan 100,0 % (Premium), 86
  // dan 88,3 % (Corriente, sin sugerencia) y 83 dan 81,6 % (Fuera de norma, a 0,4 de 82: sugiere otra).
  await loteAnalizado(request, marta, [106, 107], { codigo: est });
  await loteAnalizado(request, marta, [103, 104], { codigo: pre });
  await loteAnalizado(request, marta, [107, 107], { codigo: cor });
  await loteAnalizado(request, pedro, [108, 109], { codigo: fdn });
  await registrarLotes(request, marta, [{ codigo: pen }]);
  const hoy = diaEnColombia(0);
  const ayer = diaEnColombia(-1);
  await iniciarSesion(page, marta);

  await page.goto('/historial');

  await seVenSoloLosLotes(page, todos, todos);
  // El chip, junto al estado de los lotes que sugieren otra muestra.
  for (const codigo of todos) {
    await expect(loteDelHistorial(page, codigo).getByText('Sugiere otra muestra', { exact: true }), codigo)
      .toHaveCount([est, fdn].includes(codigo) ? 1 : 0);
  }
  const formulario = await abrirFiltros(page, isMobile);
  await expect(formulario.getByLabel('Usuario', { exact: true }).locator('option'))
    .toHaveText(['Todos', 'Marta Ruiz', 'Pedro Salas']);

  // Cada filtro solo.
  await filtrarElHistorial(formulario, { estado: 'Pendiente de análisis' });
  await seVenSoloLosLotes(page, todos, [pen]);
  expect(parametrosDe(page)).toEqual({ estado: 'PENDIENTE' });
  await filtrarElHistorial(formulario, { categoria: 'Estándar' });
  await seVenSoloLosLotes(page, todos, [est]);
  await filtrarElHistorial(formulario, { codigo: '  est ' });
  await seVenSoloLosLotes(page, todos, [est]);
  expect(parametrosDe(page)).toEqual({ codigo: 'est' });
  await filtrarElHistorial(formulario, { usuario: 'Pedro Salas' });
  await seVenSoloLosLotes(page, todos, [fdn]);
  expect(parametrosDe(page)).toEqual({ usuario: pedro.id });
  // Un lote pendiente no tiene fecha del resultado: no sale al filtrar por fecha.
  await filtrarElHistorial(formulario, { desde: hoy });
  await seVenSoloLosLotes(page, todos, [est, pre, cor, fdn]);
  await filtrarElHistorial(formulario, { hasta: ayer });
  await seVenSoloLosLotes(page, todos, []);
  await expect(page.getByText('Ningún lote cumple los filtros.')).toBeVisible();
  await page.getByRole('button', { name: 'Limpiar filtros' }).click();
  await seVenSoloLosLotes(page, todos, todos);
  expect(parametrosDe(page)).toEqual({});

  // «Hasta» anterior a «Resultado desde» se marca en su campo, sin pedir la lista.
  await filtrarElHistorial(formulario, { desde: hoy, hasta: ayer });
  await expect(formulario.getByText('La fecha «Hasta» no puede ser anterior a «Resultado desde».')).toBeVisible();
  await expect(formulario.getByLabel('Hasta', { exact: true })).toHaveAttribute('aria-invalid', 'true');
  expect(parametrosDe(page)).toEqual({});

  // Todos a la vez, con las dos fechas en hoy.
  const combinados = { desde: hoy, hasta: hoy, estado: 'ANALIZADO', categoria: 'ESTANDAR', codigo: 'est', usuario: marta.id };
  await filtrarElHistorial(formulario, {
    desde: hoy, hasta: hoy, estado: 'Analizado', categoria: 'Estándar', codigo: 'est', usuario: 'Marta Ruiz',
  });
  await seVenSoloLosLotes(page, todos, [est]);
  await expect.poll(() => parametrosDe(page)).toEqual(combinados);
  await expect(loteDelHistorial(page, est).getByText('Sugiere otra muestra', { exact: true })).toBeVisible();

  // «Ver» y «Atrás»: la dirección, el formulario y la lista conservan los filtros.
  await page.getByRole('link', { name: `Ver el lote ${est}` }).click();
  await expect(page.getByRole('heading', { level: 1, name: `Resultado del lote ${est}` })).toBeVisible();
  await page.goBack();
  await expect.poll(() => parametrosDe(page)).toEqual(combinados);
  const deVuelta = page.getByRole('form', { name: 'Filtros del historial' });
  await expect(deVuelta.getByLabel('Resultado desde', { exact: true })).toHaveValue(hoy);
  await expect(deVuelta.getByLabel('Hasta', { exact: true })).toHaveValue(hoy);
  await expect(deVuelta.getByLabel('Estado', { exact: true })).toHaveValue('ANALIZADO');
  await expect(deVuelta.getByLabel('Categoría', { exact: true })).toHaveValue('ESTANDAR');
  await expect(deVuelta.getByLabel('Código del lote', { exact: true })).toHaveValue('est');
  await expect(deVuelta.getByLabel('Usuario', { exact: true })).toHaveValue(marta.id);
  await seVenSoloLosLotes(page, todos, [est]);

  // «Limpiar» quita todos los filtros.
  await deVuelta.getByRole('button', { name: 'Limpiar', exact: true }).click();
  await seVenSoloLosLotes(page, todos, todos);
  expect(parametrosDe(page)).toEqual({});
  await expect(deVuelta.getByLabel('Código del lote', { exact: true })).toHaveValue('');
});

test('RF-03 · C6 · los lotes de un usuario desactivado siguen en el historial con su nombre, él ya no inicia sesión y sigue en la lista del filtro «Usuario», que trae solo sus lotes (de punta a punta)', async ({ page, request, isMobile }) => {
  const { empresa, usuarioFinal: marta } = await crearEmpresaConUsuarioFinal(request);
  const pedro = await crearUsuarioFinalEn(request, empresa, 'Pedro Salas');
  const prefijo = prefijoNuevo();
  const dePedro = `${prefijo}-PS`;
  const deMarta = `${prefijo}-MR`;
  await loteAnalizado(request, pedro, [106, 107], { codigo: dePedro });
  await registrarLotes(request, marta, [{ codigo: deMarta }]);

  await desactivarUsuarioEn(request, empresa, pedro.id);

  const sesion = await request.post('/api/v1/sesion', { data: { correo: pedro.correo, contrasena: pedro.contrasena } });
  expect(sesion.status()).toBe(401);
  await iniciarSesion(page, empresa.administrador);
  await page.goto('/historial');
  await seVenSoloLosLotes(page, [dePedro, deMarta], [dePedro, deMarta]);
  await expect(loteDelHistorial(page, dePedro)).toContainText('Pedro Salas');
  await expect(loteDelHistorial(page, dePedro)).toContainText('Analizado');
  const formulario = await abrirFiltros(page, isMobile);
  await expect(formulario.getByLabel('Usuario', { exact: true }).locator('option'))
    .toHaveText(['Todos', 'Marta Ruiz', 'Pedro Salas']);
  await filtrarElHistorial(formulario, { usuario: 'Pedro Salas' });
  await seVenSoloLosLotes(page, [dePedro, deMarta], [dePedro]);
});
