/*
 * Nombre: resultado.spec.js
 * Descripción: Pruebas de punta a punta del análisis y del resultado de un lote (RF-05 C2, RF-10 C9, RF-11 C1 a C4 y
 *              C6, RF-12 C8 y C10, RF-14 C3 y C6, RNF-03 C1, D-47): «Analizar la muestra» con el mínimo de 2 fotos, las
 *              cifras del resultado con el clasificador falso, el rango recortado en 100, los granos marcados con su
 *              color y su símbolo, el globo con el cursor y el teclado, la categoría probable con el aviso de la
 *              sugerencia y su límite, «Ver» de la administradora, un lote de otra empresa, la segunda muestra desde el
 *              aviso, con su espera sobre el resultado actual y la suma de las dos, y los lotes que no admiten otra.
 *              Las fotos se hacen en la prueba (RNF-08): el clasificador falso da 50 granos por foto y tantos no sanos
 *              como su ancho módulo 50.
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { randomUUID } from 'node:crypto';
import {
  casillaDeFoto, crearEmpresaConUsuarioFinal, elegirFoto, expect, iniciarSesion, loteAnalizado, loteConDosMuestras,
  registrarLotes, retenerLaEspera, seVeLaImagen, subirFotosPorLaApi, test, tokenDe,
} from './ayudas.js';
import { png } from './imagenes.js';

/** El motivo con que la API rechaza una foto de otra muestra que el resultado no sugiere (RF-12 C8). */
const SIN_OTRA_MUESTRA = 'Este lote ya tiene su resultado y no admite otra muestra: solo se analiza otra cuando el '
  + 'resultado la sugiere, y como máximo 2.';

/** El motivo con que la API rechaza volver a analizar un lote que ya tiene su resultado (RF-12 C8). */
const LOTE_ANALIZADO = 'Este lote ya tiene su resultado: no se puede volver a analizar.';

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

test('RF-12 · C10 · la categoría se muestra como probable: con 90,6 %, Estándar, con la nota de la prueba de corte y la sugerencia de otra muestra, que dice a cuántos puntos quedó y de qué límite (de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const lote = await loteAnalizado(request, usuarioFinal, [106, 107]);
  await iniciarSesion(page, usuarioFinal);

  await page.goto(`/lotes/${lote.id}`);

  const resumen = page.getByRole('region', { name: 'Resumen del resultado' });
  await expect(resumen.getByText('Categoría probable', { exact: true })).toBeVisible();
  await expect(resumen.getByText('Estándar', { exact: true })).toBeVisible();
  await expect(resumen.getByText('Es una categoría probable: la oficial se confirma con la prueba de corte.'))
    .toBeVisible();
  await expect(page.getByRole('region', { name: 'Sugerencia' }))
    .toContainText('El resultado quedó a 0,6 puntos del límite de Estándar (90 %).');
  const api = await request.get(`/api/v1/lotes/${lote.id}/resultado`,
    { headers: { Authorization: `Bearer ${await tokenDe(request, usuarioFinal)}` } });
  expect((await api.json()).lote).toEqual(expect.objectContaining({
    categoria: 'ESTANDAR', sugerirOtraMuestra: true, limiteSugerencia: 90, categoriaDelLimite: 'ESTANDAR' }));
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

test('D-47 · el aviso de la sugerencia dice el límite más cercano aunque sea de otra categoría: con 95,1 %, Estándar, quedó a 0,9 puntos del límite de Premium o Especial (96 %) (de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  // 5 + 6 no sanos: 89 de 100 sanos, 95,1 % corregido.
  const lote = await loteAnalizado(request, usuarioFinal, [105, 106]);
  await iniciarSesion(page, usuarioFinal);

  await page.goto(`/lotes/${lote.id}`);

  await expect(cifra(page, '95,1')).toBeVisible();
  await expect(page.getByRole('region', { name: 'Resumen del resultado' }).getByText('Estándar', { exact: true }))
    .toBeVisible();
  await expect(page.getByRole('region', { name: 'Sugerencia' }))
    .toContainText('El resultado quedó a 0,9 puntos del límite de Premium o Especial (96 %).');
  const api = await request.get(`/api/v1/lotes/${lote.id}/resultado`,
    { headers: { Authorization: `Bearer ${await tokenDe(request, usuarioFinal)}` } });
  expect((await api.json()).lote).toEqual(expect.objectContaining({
    categoria: 'ESTANDAR', limiteSugerencia: 96, categoriaDelLimite: 'PREMIUM' }));
});

test('RF-14 · C6 · desde el detalle de un lote con otra muestra sugerida, el usuario final pulsa «Analizar otra muestra», sube 2 fotos, ve la espera de la muestra 2 sobre el resultado actual y al final la suma de las dos: 172 de 200 dan 88,3 %, Corriente, sin otra sugerencia (de punta a punta)', async ({ page, request }) => {
  test.slow();
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const lote = await loteAnalizado(request, usuarioFinal, [106, 107]);
  await iniciarSesion(page, usuarioFinal);
  await page.goto(`/lotes/${lote.id}`);

  const sugerencia = page.getByRole('region', { name: 'Sugerencia' });
  await expect(sugerencia).toContainText('El resultado quedó a 0,6 puntos del límite de Estándar (90 %).');
  await expect(sugerencia).toContainText('Analiza una segunda muestra del mismo lote para confirmarlo. El resultado '
    + 'sumará los granos de las dos muestras.');
  await sugerencia.getByRole('link', { name: 'Analizar otra muestra' }).click();

  await expect(page).toHaveURL(new RegExp(`/lotes/${lote.id}/fotos$`));
  await expect(page.getByRole('heading', { level: 1, name: 'Fotos de la muestra 2' })).toBeVisible();
  await expect(page.getByText(`Muestra 2 del lote ${lote.codigo} · Finca La Esperanza. Sube 2 o 3 fotos de otra `
    + 'muestra del mismo lote; si sales ahora, la retomas desde el resultado del lote.')).toBeVisible();
  // Las casillas de la muestra 2 empiezan vacías: las fotos de la muestra 1 no se tocan.
  await expect(casillaDeFoto(page, 1).getByRole('img')).toHaveCount(0);
  // 8 + 7 no sanos: 85 de 100 sanos en la muestra 2.
  for (const [indice, ancho] of [108, 107].entries()) {
    const numero = indice + 1;
    await elegirFoto(page, numero,
      { name: `muestra-2-${numero}.png`, mimeType: 'image/png', buffer: png({ ancho, alto: 100 }) });
    await seVeLaImagen(casillaDeFoto(page, numero).getByRole('img', { name: `Foto ${numero} de la muestra` }));
  }
  const espera = await retenerLaEspera(page, request, usuarioFinal, lote.id);
  await page.getByRole('button', { name: 'Analizar la muestra' }).click();

  // Mientras la muestra 2 espera: su espera arriba y, debajo, el resultado actual con el aviso de que cambiará.
  await expect(page).toHaveURL(new RegExp(`/lotes/${lote.id}$`));
  const tituloDeLaEspera = page.getByRole('heading', { level: 2, name: 'Análisis de la muestra 2' });
  await expect(tituloDeLaEspera).toBeVisible();
  await expect(page.getByRole('progressbar', { name: 'Avance del análisis' })).toBeVisible();
  const nota = page.getByRole('note');
  await expect(nota).toHaveText('Este resultado cambiará cuando termine la muestra 2: sumará los granos de las dos.');
  const resumen = page.getByRole('region', { name: 'Resumen del resultado' });
  await expect(cifra(page, '90,6')).toBeVisible();
  await expect(page.getByRole('region', { name: 'Sugerencia' })).toHaveCount(0);
  expect((await tituloDeLaEspera.boundingBox()).y).toBeLessThan((await nota.boundingBox()).y);
  expect((await nota.boundingBox()).y).toBeLessThan((await resumen.boundingBox()).y);

  await espera.soltar();

  // Al terminar, el resultado de la suma, con el foco en el título.
  await expect(cifra(page, '88,3')).toBeVisible({ timeout: 30_000 });
  await expect(page.getByRole('heading', { level: 1, name: `Resultado del lote ${lote.codigo}` })).toBeFocused();
  await expect(page.getByRole('status').filter({ hasText: 'Listo' }))
    .toHaveText(`Listo: este es el resultado del lote ${lote.codigo}.`);
  await expect(resumen.getByText('Corriente', { exact: true })).toBeVisible();
  await expect(resumen.getByText('172 de 200 marcados como sanos')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Análisis de la muestra 2' })).toHaveCount(0);
  await expect(page.getByRole('note')).toHaveCount(0);
  await expect(page.getByRole('region', { name: 'Sugerencia' })).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'Analizar otra muestra' })).toHaveCount(0);
  // Cada muestra con su título y sus cifras (RF-14 C3), y sus granos con el número de la muestra en el nombre.
  const fotos = page.getByRole('region', { name: 'Fotos marcadas' });
  await expect(fotos.getByRole('heading', { level: 3 })).toHaveText(['Muestra 1', 'Muestra 2']);
  await expect(fotos.getByText('87 de 100 marcados como sanos', { exact: true })).toBeVisible();
  await expect(fotos.getByText('85 de 100 marcados como sanos', { exact: true })).toBeVisible();
  await expect(page.getByRole('group', { name: 'Granos de la foto 1 de la muestra 1' })
    .getByRole('button', { name: /^Grano \d+: no sano/ })).toHaveCount(6);
  await expect(page.getByRole('group', { name: 'Granos de la foto 1 de la muestra 2' })
    .getByRole('button', { name: /^Grano \d+: no sano/ })).toHaveCount(8);
  const api = await request.get(`/api/v1/lotes/${lote.id}/resultado`,
    { headers: { Authorization: `Bearer ${await tokenDe(request, usuarioFinal)}` } });
  expect((await api.json()).lote).toEqual(expect.objectContaining({
    porcentajeSanos: 88.3, margen: 5.5, totalGranos: 200, granosSanos: 172, categoria: 'CORRIENTE',
    sugerirOtraMuestra: false, limiteSugerencia: null, categoriaDelLimite: null }));
});

test('RF-14 · la administradora ve el aviso de la sugerencia con su límite, sin «Analizar otra muestra»: la segunda muestra la analiza un usuario final (de punta a punta)', async ({ page, request }) => {
  const { empresa, usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const lote = await loteAnalizado(request, usuarioFinal, [106, 107]);
  await iniciarSesion(page, empresa.administrador);

  await page.goto(`/lotes/${lote.id}`);

  const sugerencia = page.getByRole('region', { name: 'Sugerencia' });
  await expect(sugerencia).toContainText('El resultado quedó a 0,6 puntos del límite de Estándar (90 %).');
  await expect(sugerencia).toContainText('Un usuario final de tu empresa puede analizar una segunda muestra del mismo '
    + 'lote para confirmarlo.');
  await expect(sugerencia.getByRole('link')).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'Analizar otra muestra' })).toHaveCount(0);
});

test('RF-12 · C8 · un lote sin sugerencia, con 100 de 100 sanos, no ofrece otra muestra: el resultado no tiene el aviso, la página de las fotos no recibe ninguna y la API responde 409 a otra foto y a otro análisis (de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  // Sin no sanos: 100,0 %, Premium o Especial, a 4 puntos de 96.
  const lote = await loteAnalizado(request, usuarioFinal, [100, 100]);
  await iniciarSesion(page, usuarioFinal);

  await page.goto(`/lotes/${lote.id}`);

  await expect(cifra(page, '100,0')).toBeVisible();
  await expect(page.getByRole('region', { name: 'Sugerencia' })).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'Analizar otra muestra' })).toHaveCount(0);
  await page.goto(`/lotes/${lote.id}/fotos`);
  await expect(page.getByText('Este lote ya no está pendiente de análisis: sus fotos no se pueden cambiar.'))
    .toBeVisible();
  await expect(page.getByRole('heading', { level: 1, name: 'Fotos de la muestra', exact: true })).toBeVisible();
  await expect(page.getByLabel(/^Archivo de la foto/)).toHaveCount(0);
  const autorizacion = { Authorization: `Bearer ${await tokenDe(request, usuarioFinal)}` };
  const foto = await request.post(`/api/v1/lotes/${lote.id}/fotos/1/subida`,
    { data: { tipo: 'image/png', tamanoBytes: 1_000 }, headers: autorizacion });
  expect(foto.status()).toBe(409);
  expect((await foto.json()).detail).toBe(SIN_OTRA_MUESTRA);
  const analisis = await request.post(`/api/v1/lotes/${lote.id}/analisis`, { headers: autorizacion });
  expect(analisis.status()).toBe(409);
  expect((await analisis.json()).detail).toBe(LOTE_ANALIZADO);
});

test('RF-12 · C8 · un lote con 2 muestras no admite una tercera ni sugiere otra, aunque la suma quede a 0,6 puntos de 90: el resultado no tiene el aviso y la API responde 409 a otra foto y a otro análisis (de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  // 87 + 87 de 200: 90,6 %, Estándar, con el margen más chico de las dos muestras.
  const lote = await loteConDosMuestras(request, usuarioFinal, [106, 107], [106, 107]);
  await iniciarSesion(page, usuarioFinal);

  await page.goto(`/lotes/${lote.id}`);

  await expect(cifra(page, '90,6')).toBeVisible();
  await expect(page.getByText('Rango probable: entre 85,3 y 95,9 %')).toBeVisible();
  await expect(page.getByRole('region', { name: 'Fotos marcadas' }).getByRole('heading', { level: 3 }))
    .toHaveText(['Muestra 1', 'Muestra 2']);
  await expect(page.getByRole('region', { name: 'Sugerencia' })).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'Analizar otra muestra' })).toHaveCount(0);
  const autorizacion = { Authorization: `Bearer ${await tokenDe(request, usuarioFinal)}` };
  const foto = await request.post(`/api/v1/lotes/${lote.id}/fotos/1/subida`,
    { data: { tipo: 'image/png', tamanoBytes: 1_000 }, headers: autorizacion });
  expect(foto.status()).toBe(409);
  expect((await foto.json()).detail).toBe(SIN_OTRA_MUESTRA);
  const analisis = await request.post(`/api/v1/lotes/${lote.id}/analisis`, { headers: autorizacion });
  expect(analisis.status()).toBe(409);
  expect((await analisis.json()).detail).toBe(LOTE_ANALIZADO);
});
