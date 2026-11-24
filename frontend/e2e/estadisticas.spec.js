/*
 * Nombre: estadisticas.spec.js
 * Descripción: Pruebas de punta a punta de la pantalla Estadísticas (RF-16): las cifras coinciden con los lotes del
 *              historial en el mismo periodo (C1), un periodo sin lotes muestra un mensaje en lugar de gráficas vacías
 *              (C2), el usuario final no entra (C3) y «Paraíso» y «Paraiso» cuentan como la misma finca (C4), cada
 *              prueba con una empresa propia y sus lotes analizados por la API. También que la pantalla con datos no se desplaza a los
 *              lados desde 360 px y no tiene fallas de accesibilidad WCAG AA. Las fotos se hacen en la prueba (RNF-08).
 *              Si la preparación cruza la medianoche de Colombia, la prueba de las cifras se salta: sus lotes no
 *              tendrían la misma fecha del resultado.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import AxeBuilder from '@axe-core/playwright';
import {
  abrirMenu, crearEmpresaConUsuarioFinal, expect, iniciarSesion, loteAnalizado, registrarLotes, test, tokenDe,
} from './ayudas.js';
import { diaEnColombia } from './fechas.js';

/** Las pautas que revisa axe, las mismas de accesibilidad.spec.js (RNF-06 C4). */
const PAUTAS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'];

/**
 * Un porcentaje como lo muestra la pantalla en español, como «90,6 %».
 *
 * @param {number} decimas el porcentaje en décimas, como 906
 * @returns {string} el texto
 */
function porcentaje(decimas) {
  return `${Math.floor(decimas / 10)},${decimas % 10} %`;
}

/**
 * El promedio de unos porcentajes con 1 decimal, redondeado a 1 decimal con la mitad hacia arriba, como el backend,
 * en décimas: así no hay errores de los números de coma flotante.
 *
 * @param {number[]} valores los porcentajes, como 90.6
 * @returns {number} el promedio en décimas
 */
function promedioEnDecimas(valores) {
  const suma = valores.reduce((total, valor) => total + Math.round(valor * 10), 0);
  return Math.floor((2 * suma + valores.length) / (2 * valores.length));
}

/**
 * Las celdas de cada fila de una tabla, sin la de los títulos.
 *
 * @param {import('@playwright/test').Page} page la página
 * @param {string} nombre el nombre de la tabla
 * @returns {Promise<string[][]>} el texto de cada celda, por fila
 */
async function filasDe(page, nombre) {
  const filas = page.getByRole('table', { name: nombre }).getByRole('row');
  await expect(filas.first()).toBeVisible();
  const todas = await filas.evaluateAll((elementos) => elementos.slice(1)
    .map((fila) => Array.from(fila.querySelectorAll('td')).map((celda) => celda.textContent)));
  return todas;
}

test('RF-16 · C1 · las cifras coinciden con los lotes del historial en el mismo periodo: lotes analizados, promedio, lotes por categoría y promedio por finca, sin el lote pendiente, y con datos la pantalla no se desplaza a los lados desde 360 px ni tiene fallas de accesibilidad (de punta a punta)', async ({ page, request, isMobile }) => {
  // Cuatro análisis por la API.
  test.slow();
  const { empresa, usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const hoy = diaEnColombia(0);
  // Con los rangos por defecto: 106 y 107 dan 90,6 % (Estándar), 103 y 104, 100,0 % (Premium), 107 y 107, 88,3 %
  // (Corriente) y 108 y 109, 81,6 % (Fuera de norma).
  await loteAnalizado(request, usuarioFinal, [106, 107], { origen: 'Paraíso' });
  await loteAnalizado(request, usuarioFinal, [103, 104], { origen: 'Paraiso' });
  await loteAnalizado(request, usuarioFinal, [107, 107], { origen: 'La Esperanza' });
  await loteAnalizado(request, usuarioFinal, [108, 109], { origen: 'la esperanza' });
  // Un lote pendiente no cuenta: todavía no tiene resultado.
  await registrarLotes(request, usuarioFinal, [{ codigo: `P-${Date.now().toString(36)}`, origen: 'Paraíso' }]);
  test.skip(diaEnColombia(0) !== hoy, 'La preparación cruzó la medianoche de Colombia: los lotes no tienen la misma '
    + 'fecha del resultado. Vuelve a correr la prueba.');

  // El historial de la empresa en el mismo periodo, con los lotes analizados, por la API.
  const token = await tokenDe(request, empresa.administrador);
  const respuesta = await request.get(`/api/v1/lotes?tamano=100&estado=ANALIZADO&desde=${hoy}&hasta=${hoy}`,
    { headers: { Authorization: `Bearer ${token}` } });
  expect(respuesta.status()).toBe(200);
  const historial = await respuesta.json();
  expect(historial.totalElementos).toBe(4);
  const porcentajes = historial.contenido.map((lote) => lote.porcentajeSanos);
  const deLaFinca = (patron) => historial.contenido.filter((lote) => patron.test(lote.origen))
    .map((lote) => lote.porcentajeSanos);

  await iniciarSesion(page, empresa.administrador);
  const menu = await abrirMenu(page, isMobile);
  await menu.getByRole('link', { name: 'Estadísticas' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Estadísticas' })).toBeVisible();
  const periodo = page.getByRole('form', { name: 'Periodo de las estadísticas' });
  await periodo.getByLabel('Desde', { exact: true }).fill(hoy);
  await periodo.getByLabel('Hasta', { exact: true }).fill(hoy);
  await periodo.getByRole('button', { name: 'Ver estadísticas' }).click();
  await expect(page).toHaveURL(new RegExp(`desde=${hoy}&hasta=${hoy}`));

  // C1: los lotes analizados, el promedio y los lotes por categoría coinciden con el historial.
  await expect(page.getByRole('status')).toHaveText('4 lotes analizados en el periodo');
  await expect(page.getByRole('definition').first()).toHaveText(String(historial.totalElementos));
  await expect(page.getByRole('definition').nth(1)).toHaveText(porcentaje(promedioEnDecimas(porcentajes)));
  const categorias = await filasDe(page, 'Lotes por categoría');
  for (const [categoria, nombre] of [['PREMIUM', 'Premium o Especial'], ['ESTANDAR', 'Estándar'],
    ['CORRIENTE', 'Corriente'], ['FUERA_DE_NORMA', 'Fuera de norma']]) {
    const enElHistorial = historial.contenido.filter((lote) => lote.categoria === categoria).length;
    expect(categorias.find(([texto]) => texto === nombre), nombre).toEqual([nombre, String(enElHistorial), '25,0 %']);
  }

  // Cada finca una vez, con sus 2 lotes (C4); la forma que se ve es la del resultado más reciente.
  const fincas = await filasDe(page, 'Promedio por finca o proveedor');
  expect(fincas).toHaveLength(2);
  expect(fincas).toContainEqual(['Paraiso', porcentaje(promedioEnDecimas(deLaFinca(/^para[ií]so$/i))), '2']);
  expect(fincas).toContainEqual(['la esperanza', porcentaje(promedioEnDecimas(deLaFinca(/^la esperanza$/i))), '2']);
  await expect(page.getByRole('table', { name: 'Evolución del promedio por mes' }).getByRole('row')).toHaveCount(2);

  // Con datos, la pantalla no se desplaza a los lados desde 360 px y no tiene fallas de accesibilidad (RNF-05, RNF-06).
  await page.setViewportSize({ width: 360, height: 740 });
  await expect(page.getByTestId('grafica-por-mes')).toBeAttached();
  const { contenido, ventana } = await page.evaluate(() => ({
    contenido: document.documentElement.scrollWidth, ventana: document.documentElement.clientWidth }));
  expect(contenido).toBeLessThanOrEqual(ventana);
  const revision = await new AxeBuilder({ page }).withTags(PAUTAS).analyze();
  expect(revision.violations.map((falla) => `${falla.id}: ${falla.help}`)).toEqual([]);
});

test('RF-16 · C4 · «Paraíso» y «Paraiso» cuentan como la misma finca en el promedio por finca o proveedor (de punta a punta)', async ({ page, request }) => {
  test.slow();
  const { empresa, usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  await loteAnalizado(request, usuarioFinal, [106, 107], { origen: 'Paraíso' });
  await loteAnalizado(request, usuarioFinal, [103, 104], { origen: 'Paraiso' });
  await iniciarSesion(page, empresa.administrador);

  // El periodo por defecto, los últimos 12 meses, trae los lotes de hoy.
  await page.goto('/estadisticas');

  const fincas = await filasDe(page, 'Promedio por finca o proveedor');
  expect(fincas).toHaveLength(1);
  expect(fincas[0][0]).toBe('Paraiso');
  expect(fincas[0][2]).toBe('2');
});

test('RF-16 · C2 · sin lotes analizados en el periodo, la pantalla muestra un mensaje en lugar de gráficas vacías (de punta a punta)', async ({ page, request }) => {
  const { empresa, usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  // Un lote pendiente no tiene resultado: no cuenta.
  await registrarLotes(request, usuarioFinal, [{ codigo: `P-${Date.now().toString(36)}` }]);
  await iniciarSesion(page, empresa.administrador);

  await page.goto('/estadisticas');

  await expect(page.getByText('No hay lotes analizados en este periodo. Elige otro periodo para ver sus estadísticas.'))
    .toBeVisible();
  await expect(page.getByRole('status')).toHaveText('0 lotes analizados en el periodo');
  await expect(page.getByRole('table')).toHaveCount(0);
  await expect(page.getByTestId('grafica-por-mes')).toHaveCount(0);
  await expect(page.getByRole('form', { name: 'Periodo de las estadísticas' })).toBeVisible();
});

test('RF-16 · C3 · un usuario final no puede entrar a las estadísticas: no las tiene en su menú, la dirección le dice que no tiene permiso y la API le responde 403 (de punta a punta)', async ({ page, request, isMobile }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  await iniciarSesion(page, usuarioFinal);

  const menu = await abrirMenu(page, isMobile);
  await expect(menu.getByRole('link', { name: 'Historial' })).toBeVisible();
  await expect(menu.getByRole('link', { name: 'Estadísticas' })).toHaveCount(0);
  await page.goto('/estadisticas');
  await expect(page.getByText('No tienes permiso para esta operación.')).toBeVisible();
  const respuesta = await request.get('/api/v1/estadisticas',
    { headers: { Authorization: `Bearer ${await tokenDe(request, usuarioFinal)}` } });
  expect(respuesta.status()).toBe(403);
});
