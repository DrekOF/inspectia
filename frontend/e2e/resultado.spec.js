/*
 * Nombre: resultado.spec.js
 * Descripción: Pruebas de punta a punta del análisis y del resultado de un lote (RF-05 C2, RF-10 C9, RF-11 C1 a C4 y
 *              C6, RF-12 C10, RF-14 C3, RNF-03 C1): «Analizar la muestra» con el mínimo de 2 fotos, las cifras del
 *              resultado con el clasificador falso, el rango recortado en 100, los granos marcados con su color y su
 *              símbolo, el globo con el cursor y el teclado, la categoría probable, «Ver» de la administradora y un lote
 *              de otra empresa. Las fotos se hacen en la prueba (RNF-08): el clasificador falso da 50 granos por foto y
 *              tantos no sanos como su ancho módulo 50.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { randomUUID } from 'node:crypto';
import {
  crearEmpresaConUsuarioFinal, expect, iniciarSesion, loteAnalizado, registrarLotes, subirFotosPorLaApi, test,
  tokenDe,
} from './ayudas.js';
import { png } from './imagenes.js';

/**
 * La cifra grande del resultado, con su espacio sin corte antes de «%».
 *
 * @param {import('@playwright/test').Page} page la página
 * @param {string} cifra la cifra, como 90,6
 * @returns {import('@playwright/test').Locator} la cifra
 */
function cifra(page, cifra) {
  return page.getByRole('region', { name: 'Resumen del resultado' }).getByText(`${cifra}\u00a0%`, { exact: true });
}

test('RF-05 · C2 · con una foto la página no deja analizar y la API responde 400; con dos, «Analizar la muestra» lleva al resultado (prueba de límite, de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const [lote] = await registrarLotes(request, usuarioFinal, [{ codigo: `A-${randomUUID().slice(0, 8)}` }]);
  await subirFotosPorLaApi(request, usuarioFinal, lote.id, [{ numero: 1, archivo: png({ ancho: 106, alto: 100 }) }]);
  await iniciarSesion(page, usuarioFinal);
  await page.goto(`/lotes/${lote.id}/fotos`);

  const analizar = page.getByRole('button', { name: 'Analizar la muestra' });
  await expect(analizar).toHaveAttribute('aria-disabled', 'true');
  await expect(analizar).toHaveAccessibleDescription('Sube al menos 2 fotos para analizar la muestra.');
  const rechazo = await request.post(`/api/v1/lotes/${lote.id}/analisis`,
    { headers: { Authorization: `Bearer ${await tokenDe(request, usuarioFinal)}` } });
  expect(rechazo.status()).toBe(400);
  expect((await rechazo.json()).errores).toEqual([
    expect.objectContaining({ campo: 'fotos', mensaje: 'Sube al menos 2 fotos para analizar la muestra.' })]);

  await subirFotosPorLaApi(request, usuarioFinal, lote.id, [{ numero: 2, archivo: png({ ancho: 107, alto: 100 }) }]);
  await page.reload();
  await expect(analizar).not.toHaveAttribute('aria-disabled');
  await analizar.click();

  await expect(page).toHaveURL(new RegExp(`/lotes/${lote.id}$`));
  await expect(page.getByRole('heading', { level: 1, name: `Resultado del lote ${lote.codigo}` })).toBeVisible();
  await expect(cifra(page, '90,6')).toBeVisible({ timeout: 30_000 });
});

test('RF-11 · C3 · las cifras de la pantalla coinciden con el cálculo de RF-10: 87 de 100 sanos dan 90,6 %, con su rango probable, como los guarda la API (de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const lote = await loteAnalizado(request, usuarioFinal, [106, 107]);
  await iniciarSesion(page, usuarioFinal);

  await page.goto(`/lotes/${lote.id}`);

  const resumen = page.getByRole('region', { name: 'Resumen del resultado' });
  await expect(cifra(page, '90,6')).toBeVisible();
  await expect(resumen.getByText('Rango probable: entre 83,1 y 98,1 %')).toBeVisible();
  const api = await request.get(`/api/v1/lotes/${lote.id}/resultado`,
    { headers: { Authorization: `Bearer ${await tokenDe(request, usuarioFinal)}` } });
  expect((await api.json()).lote).toEqual(expect.objectContaining({
    porcentajeSanos: 90.6, margen: 7.5, totalGranos: 100, granosSanos: 87 }));
});

test('RF-11 · C6 · se ve lo que contó el modelo, «87 de 100 marcados como sanos», y la frase de la corrección (de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const lote = await loteAnalizado(request, usuarioFinal, [106, 107]);
  await iniciarSesion(page, usuarioFinal);

  await page.goto(`/lotes/${lote.id}`);

  const resumen = page.getByRole('region', { name: 'Resumen del resultado' });
  await expect(resumen.getByText('87 de 100 marcados como sanos')).toBeVisible();
  await expect(resumen.getByText('El porcentaje corrige los errores conocidos del modelo.')).toBeVisible();
});

test('RF-12 · C10 · la categoría se muestra como probable: con 90,6 %, Estándar, con la nota de la prueba de corte y la sugerencia de otra muestra (de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const lote = await loteAnalizado(request, usuarioFinal, [106, 107]);
  await iniciarSesion(page, usuarioFinal);

  await page.goto(`/lotes/${lote.id}`);

  const resumen = page.getByRole('region', { name: 'Resumen del resultado' });
  await expect(resumen.getByText('Categoría probable', { exact: true })).toBeVisible();
  await expect(resumen.getByText('Estándar', { exact: true })).toBeVisible();
  await expect(resumen.getByText('Es una categoría probable: la oficial se confirma con la prueba de corte.'))
    .toBeVisible();
  await expect(page.getByText('El resultado quedó a menos de 1 punto del límite de una categoría.')).toBeVisible();
  const api = await request.get(`/api/v1/lotes/${lote.id}/resultado`,
    { headers: { Authorization: `Bearer ${await tokenDe(request, usuarioFinal)}` } });
  expect((await api.json()).lote).toEqual(expect.objectContaining({ categoria: 'ESTANDAR', sugerirOtraMuestra: true }));
});

test('RF-10 · C9 · 93 de 100 sanos dan 100,0 %, y el rango probable se recorta en 100 (D-43, prueba de límite, de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const lote = await loteAnalizado(request, usuarioFinal, [103, 104]);
  await iniciarSesion(page, usuarioFinal);

  await page.goto(`/lotes/${lote.id}`);

  await expect(cifra(page, '100,0')).toBeVisible();
  await expect(page.getByText('Rango probable: entre 94,3 y 100 %')).toBeVisible();
  await expect(page.getByRole('region', { name: 'Resumen del resultado' }).getByText('Premium o Especial')).toBeVisible();
});

test('RF-11 · C1 · cada grano aparece marcado en su foto, dentro de la imagen: con el borde verde si es sano y rojo si no (de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const lote = await loteAnalizado(request, usuarioFinal, [106, 107]);
  await iniciarSesion(page, usuarioFinal);
  await page.goto(`/lotes/${lote.id}`);

  const pestanas = page.getByRole('tablist');
  await expect(pestanas.getByRole('tab')).toHaveCount(2);
  const granos = page.getByRole('group', { name: 'Granos de la foto 1' });
  await expect(granos.getByRole('button', { name: /^Grano \d+: / })).toHaveCount(50);
  await expect(granos.getByRole('button', { name: /^Grano \d+: no sano/ })).toHaveCount(6);
  const imagen = await page.getByRole('img', { name: 'Foto 1 de la muestra, con sus granos marcados' }).boundingBox();
  for (const marca of await granos.getByRole('button').all()) {
    const caja = await marca.boundingBox();
    expect(caja.x).toBeGreaterThanOrEqual(imagen.x - 1);
    expect(caja.x + caja.width).toBeLessThanOrEqual(imagen.x + imagen.width + 1);
    expect(caja.y).toBeGreaterThanOrEqual(imagen.y - 1);
    expect(caja.y + caja.height).toBeLessThanOrEqual(imagen.y + imagen.height + 1);
  }
  // Los colores de tokens.js (marcasDeFoto): sano #2E6B34 y no sano #B3261E.
  await expect(granos.getByRole('button', { name: 'Grano 1: no sano' })).toHaveCSS('border-top-color', 'rgb(179, 38, 30)');
  await expect(granos.getByRole('button', { name: 'Grano 7: sano' })).toHaveCSS('border-top-color', 'rgb(46, 107, 52)');

  await pestanas.getByRole('tab').nth(1).click();
  await expect(page.getByRole('group', { name: 'Granos de la foto 2' }).getByRole('button', { name: /^Grano \d+: no sano/ }))
    .toHaveCount(7);
});

test('RF-11 · C2 · además del color, cada grano lleva el símbolo de su clase: uno los 44 sanos de la foto 1 y otro los 6 no sanos (de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const lote = await loteAnalizado(request, usuarioFinal, [106, 107]);
  await iniciarSesion(page, usuarioFinal);
  await page.goto(`/lotes/${lote.id}`);

  const granos = page.getByRole('group', { name: 'Granos de la foto 1' });
  await expect(granos.getByRole('button')).toHaveCount(50);
  // La página compilada no lleva el data-testid de los íconos: el símbolo se reconoce por su dibujo.
  const simbolos = await granos.getByRole('button').evaluateAll((marcas) => marcas.map((marca) => ({
    clase: marca.getAttribute('aria-label').replace(/^Grano \d+: /, ''),
    dibujo: marca.querySelector('svg path')?.getAttribute('d') ?? null,
  })));
  const dibujoDe = (clase) => [...new Set(simbolos.filter((s) => s.clase === clase).map((s) => s.dibujo))];
  expect(dibujoDe('sano')).toHaveLength(1);
  expect(dibujoDe('no sano')).toHaveLength(1);
  expect(dibujoDe('sano')[0]).toBeTruthy();
  expect(dibujoDe('no sano')[0]).toBeTruthy();
  expect(dibujoDe('sano')[0]).not.toBe(dibujoDe('no sano')[0]);
  expect(simbolos.filter((s) => s.clase === 'sano')).toHaveLength(44);
  expect(simbolos.filter((s) => s.clase === 'no sano')).toHaveLength(6);
});

test('RF-11 · C4 · al pasar el cursor por un grano, o al tocarlo, se ve su clase sin la confianza, y con el teclado se pasa de un grano a otro (de punta a punta)', async ({ page, request, isMobile }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const lote = await loteAnalizado(request, usuarioFinal, [106, 107]);
  await iniciarSesion(page, usuarioFinal);
  await page.goto(`/lotes/${lote.id}`);

  const pestanas = page.getByRole('tablist');
  const granos = page.getByRole('group', { name: 'Granos de la foto 1' });
  const primero = granos.getByRole('button', { name: 'Grano 1: no sano' });
  if (isMobile) {
    await primero.tap();
  } else {
    await primero.hover();
  }
  await expect(page.getByRole('tooltip')).toHaveText('No sano');
  await expect(page.getByRole('tooltip')).not.toHaveText(/\d/);
  await page.keyboard.press('Escape');
  await expect(page.getByRole('tooltip')).toHaveCount(0);

  await pestanas.getByRole('tab').nth(1).click();
  const segunda = page.getByRole('group', { name: 'Granos de la foto 2' });
  await expect(segunda.getByRole('button', { name: /^Grano \d+: no sano/ })).toHaveCount(7);
  if (!isMobile) {
    await segunda.getByRole('button').first().focus();
    await page.keyboard.press('ArrowRight');
    await expect(segunda.getByRole('button', { name: 'Grano 2: no sano' })).toBeFocused();
    await page.keyboard.press('End');
    await expect(segunda.getByRole('button', { name: 'Grano 50: sano' })).toBeFocused();
  }
});

test('RF-14 · C3 · la administradora abre «Ver» en el historial y ve las mismas cifras del resultado guardado, sin las acciones del usuario final (de punta a punta)', async ({ page, request }) => {
  const { empresa, usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const lote = await loteAnalizado(request, usuarioFinal, [106, 107]);
  await iniciarSesion(page, empresa.administrador);

  await page.goto('/historial');
  await page.getByRole('link', { name: `Ver el lote ${lote.codigo}` }).click();

  await expect(page).toHaveURL(new RegExp(`/lotes/${lote.id}$`));
  await expect(cifra(page, '90,6')).toBeVisible();
  await expect(page.getByText('Rango probable: entre 83,1 y 98,1 %')).toBeVisible();
  await expect(page.getByText('Analizada por Marta Ruiz', { exact: false })).toBeVisible();
  await expect(page.getByRole('main').getByRole('link', { name: 'Nuevo lote' })).toHaveCount(0);
  await expect(page.getByRole('main').getByRole('list', { name: 'Pasos del análisis' })).toHaveCount(0);
});

test('RNF-03 · C1 · un usuario final de otra empresa que abre el resultado de un lote ajeno ve que no existe, y la API le responde 404 (de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const lote = await loteAnalizado(request, usuarioFinal, [106, 107]);
  const otra = await crearEmpresaConUsuarioFinal(request);
  await iniciarSesion(page, otra.usuarioFinal);

  await page.goto(`/lotes/${lote.id}`);

  await expect(page.getByRole('alert')).toHaveText('No existe un registro con ese identificador.');
  await expect(page.getByRole('button', { name: 'Reintentar' })).toHaveCount(0);
  const ajeno = await request.get(`/api/v1/lotes/${lote.id}/resultado`,
    { headers: { Authorization: `Bearer ${await tokenDe(request, otra.usuarioFinal)}` } });
  expect(ajeno.status()).toBe(404);
});
