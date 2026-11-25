/*
 * Nombre: usabilidad.spec.js
 * Descripción: Prueba de punta a punta de RNF-06 C2: un usuario final registra un lote, sube las fotos de la muestra y
 *              ve el resultado en 3 pantallas como máximo (Nuevo lote, Fotos de la muestra y Resultado del lote). Las
 *              fotos se hacen en la prueba (RNF-08): el clasificador falso da 50 granos por foto y tantos no sanos como
 *              su ancho módulo 50.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { randomUUID } from 'node:crypto';
import {
  abrirMenu, casillaDeFoto, crearEmpresaConUsuarioFinal, elegirFoto, expect, iniciarSesion, RECEPCION, test,
} from './ayudas.js';
import { png } from './imagenes.js';

/** Lo máximo que puede tomar registrar un lote, subir las fotos y ver el resultado (RNF-06 C2). */
const MAXIMO_DE_PANTALLAS = 3;

/** Un identificador de la base (UUID) en una ruta, como el del lote. */
const UUID = /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/g;

/**
 * La pantalla que se ve: su ruta, con el identificador del lote como «:id», y su título (h1).
 *
 * @param {import('@playwright/test').Page} page la página
 * @returns {Promise<{ ruta: string, titulo: string }>} la pantalla
 */
async function pantalla(page) {
  const titulo = page.getByRole('heading', { level: 1 });
  await expect(titulo).toBeVisible();
  return { ruta: new URL(page.url()).pathname.replace(UUID, ':id'), titulo: (await titulo.textContent()).trim() };
}

/*
 * Qué cuenta como pantalla: cada ruta distinta de la app por la que pasa el usuario, desde que elige «Nuevo lote» en el
 * menú hasta que ve el porcentaje del resultado. Se cuentan las rutas a las que navega la página (también las que la
 * app cambia sola, como al pasar a las fotos) y, en cada paso, la que se ve con su título. La espera del análisis no es
 * otra pantalla: se muestra dentro del resultado, en la misma ruta. Los avisos, los selectores de archivos y los menús
 * tampoco, porque no cambian de pantalla.
 */
test('RNF-06 · C2 · registrar un lote, subir las fotos y ver el resultado toma 3 pantallas: Nuevo lote, Fotos de la muestra y Resultado del lote (de punta a punta)', async ({ page, request, isMobile }) => {
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const codigo = `U-${randomUUID().slice(0, 8)}`;
  await iniciarSesion(page, usuarioFinal);
  const navegadas = [];
  page.on('framenavigated', (marco) => {
    if (marco === page.mainFrame()) {
      navegadas.push(new URL(marco.url()).pathname.replace(UUID, ':id'));
    }
  });
  const vistas = [];

  await (await abrirMenu(page, isMobile)).getByRole('link', { name: 'Nuevo lote' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Nuevo lote' })).toBeVisible();
  vistas.push(await pantalla(page));
  await page.getByLabel('Código del lote').fill(codigo);
  await page.getByLabel('Fecha de recepción').fill(RECEPCION);
  await page.getByLabel('Finca o proveedor').fill('Finca La Esperanza');
  await page.getByLabel('Finca o proveedor').press('Escape');
  await page.getByRole('button', { name: 'Continuar a las fotos' }).click();

  await expect(page.getByRole('heading', { level: 1, name: 'Fotos de la muestra' })).toBeVisible();
  vistas.push(await pantalla(page));
  for (const [numero, ancho] of [[1, 106], [2, 107]]) {
    await elegirFoto(page, numero, { name: `muestra-${numero}.png`, mimeType: 'image/png',
      buffer: png({ ancho, alto: 100 }) });
    await expect(casillaDeFoto(page, numero).getByText('Cargada', { exact: true })).toBeVisible();
  }
  await page.getByRole('button', { name: 'Analizar la muestra' }).click();

  await expect(page.getByRole('heading', { level: 1, name: `Resultado del lote ${codigo}` })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Resumen del resultado' }).getByText('90,6 %', { exact: true }))
    .toBeVisible({ timeout: 30_000 });
  vistas.push(await pantalla(page));

  expect(vistas).toEqual([
    { ruta: '/lotes/nuevo', titulo: 'Nuevo lote' },
    { ruta: '/lotes/:id/fotos', titulo: 'Fotos de la muestra' },
    { ruta: '/lotes/:id', titulo: `Resultado del lote ${codigo}` },
  ]);
  const rutas = [...new Set([...navegadas, ...vistas.map((vista) => vista.ruta)])];
  expect(rutas, 'las rutas por las que pasó el usuario').toEqual(['/lotes/nuevo', '/lotes/:id/fotos', '/lotes/:id']);
  expect(rutas.length).toBeLessThanOrEqual(MAXIMO_DE_PANTALLAS);
});
