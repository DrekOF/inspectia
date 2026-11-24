/*
 * Nombre: fotos.spec.js
 * Descripción: Pruebas de punta a punta de las fotos de la muestra (RF-05, RF-14 C5, RNF-03, RNF-05, D-41): el botón
 *              de cada casilla abre el selector, en el celular con la cámara trasera; la foto sube por el enlace
 *              firmado, pasa por la app hasta el backend y se ve; los rechazos y el límite de 10 MB; el reemplazo;
 *              retomar el lote desde el historial; y nadie de otra empresa ni sin firma abre las fotos. Las fotos se
 *              hacen en la prueba (RNF-08). El estado del lote se busca en su fila: la lista «Estado» de los filtros
 *              del historial también dice «Pendiente de análisis».
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { randomUUID } from 'node:crypto';
import {
  casillaDeFoto, crearEmpresaConUsuarioFinal, elegirFoto, expect, iniciarSesion, loteDelHistorial, registrarLotes,
  seVeLaImagen, subirFotosPorLaApi, test, tokenDe,
} from './ayudas.js';
import { jpegDelNavegador, PDF, png } from './imagenes.js';

const FORMATO_NO_PERMITIDO = 'La foto debe ser JPG o PNG: elige una foto en uno de esos formatos.';
const DEMASIADO_GRANDE =
  'La foto pesa más de 10 MB: tómala de nuevo con la resolución normal de la cámara o elige otra.';

/**
 * Una empresa nueva con su usuario final y un lote pendiente: cada prueba tiene los suyos, porque corren a la vez
 * sobre la misma base.
 *
 * @param {import('@playwright/test').APIRequestContext} api el cliente de la API de Playwright
 * @returns {Promise<{ empresa: object, usuarioFinal: object, lote: object }>} la empresa, el usuario y el lote
 */
async function loteNuevo(api) {
  const { empresa, usuarioFinal } = await crearEmpresaConUsuarioFinal(api);
  const [lote] = await registrarLotes(api, usuarioFinal, [{ codigo: `F-${randomUUID().slice(0, 8)}` }]);
  return { empresa, usuarioFinal, lote };
}

/**
 * Un PNG listo para el selector de archivos.
 *
 * @param {string} nombre el nombre del archivo
 * @param {object} [opciones] las opciones de png(), como su tamaño exacto en bytes
 * @returns {{ name: string, mimeType: string, buffer: Buffer }} el archivo
 */
function archivoPng(nombre, opciones) {
  return { name: nombre, mimeType: 'image/png', buffer: png(opciones) };
}

test('RF-05 · C1 · el botón de una casilla vacía abre el selector de archivos, en el celular con la cámara trasera, y la foto elegida se sube, se ve y sigue al recargar (de punta a punta)', async ({ page, request, isMobile }) => {
  const { usuarioFinal, lote } = await loteNuevo(request);
  await iniciarSesion(page, usuarioFinal);
  await page.goto(`/lotes/${lote.id}/fotos`);
  const entrada = page.getByLabel('Archivo de la foto 1');
  if (isMobile) {
    await expect(entrada).toHaveAttribute('capture', 'environment');
  } else {
    await expect(entrada).not.toHaveAttribute('capture');
  }
  const primera = casillaDeFoto(page, 1);
  await expect(primera.getByRole('button', { name: isMobile ? 'Tomar foto' : 'Elegir archivo' })).toBeVisible();

  const selector = await elegirFoto(page, 1, { name: 'granos.jpg', mimeType: 'image/jpeg',
    buffer: await jpegDelNavegador(page) });

  expect(selector.isMultiple()).toBe(false);
  await expect(primera.getByText('Cargada', { exact: true })).toBeVisible();
  await seVeLaImagen(primera.getByRole('img', { name: 'Foto 1 de la muestra' }));
  await expect(primera.getByRole('button', { name: 'Reemplazar' })).toBeVisible();
  await page.reload();
  await seVeLaImagen(casillaDeFoto(page, 1).getByRole('img', { name: 'Foto 1 de la muestra' }));
});

test('RNF-05 · C2 · en el celular, con los motores de Chrome de Android y de Safari de iPhone, cada casilla pide la cámara trasera para JPG o PNG (de punta a punta)', async ({ page, request, isMobile }) => {
  test.skip(!isMobile, 'La cámara es del celular; en el PC, el selector elige archivos (RF-05 C1)');
  const { usuarioFinal, lote } = await loteNuevo(request);
  await iniciarSesion(page, usuarioFinal);
  await page.goto(`/lotes/${lote.id}/fotos`);

  for (const numero of [1, 2, 3]) {
    await expect(casillaDeFoto(page, numero).getByRole('button', { name: 'Tomar foto' })).toBeVisible();
    const entrada = page.getByLabel(`Archivo de la foto ${numero}`);
    await expect(entrada).toHaveAttribute('capture', 'environment');
    await expect(entrada).toHaveAttribute('accept', 'image/jpeg,image/png,.jpg,.jpeg,.png');
  }
});

test('RF-05 · C2 · con 3 fotos subidas la página no ofrece una cuarta, y la API rechaza la foto 4 (de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal, lote } = await loteNuevo(request);
  await subirFotosPorLaApi(request, usuarioFinal, lote.id,
    [1, 2, 3].map((numero) => ({ numero, archivo: png({ color: [40 * numero, 63, 34] }) })));
  await iniciarSesion(page, usuarioFinal);

  await page.goto(`/lotes/${lote.id}/fotos`);

  for (const numero of [1, 2, 3]) {
    await expect(casillaDeFoto(page, numero).getByText('Cargada', { exact: true })).toBeVisible();
  }
  await expect(page.getByRole('group', { name: /^Foto \d/ })).toHaveCount(3);
  await expect(page.getByLabel('Archivo de la foto 4')).toHaveCount(0);
  const cuarta = await request.post(`/api/v1/lotes/${lote.id}/fotos/4/subida`, {
    data: { tipo: 'image/png', tamanoBytes: 2048 },
    headers: { Authorization: `Bearer ${await tokenDe(request, usuarioFinal)}` },
  });
  expect(cuarta.status()).toBe(400);
});

test('RF-05 · C3 · un PDF y una foto de 10.485.761 bytes se rechazan con su mensaje, y una de 10.485.760 se sube (prueba de límite, de punta a punta)', async ({ page, request }) => {
  // La foto de 10 MB pasa por la app compilada y por el backend, que las pruebas del backend no recorren.
  test.slow();
  const { usuarioFinal, lote } = await loteNuevo(request);
  await iniciarSesion(page, usuarioFinal);
  await page.goto(`/lotes/${lote.id}/fotos`);
  const primera = casillaDeFoto(page, 1);

  await page.getByLabel('Archivo de la foto 1').setInputFiles({ name: 'informe.pdf', mimeType: 'application/pdf',
    buffer: PDF });
  await expect(primera.getByRole('alert')).toHaveText(FORMATO_NO_PERMITIDO);
  await page.getByLabel('Archivo de la foto 1').setInputFiles(archivoPng('grande.png', { bytes: 10_485_761 }));
  await expect(primera.getByRole('alert')).toHaveText(DEMASIADO_GRANDE);
  await page.getByLabel('Archivo de la foto 2').setInputFiles(archivoPng('justa.png', { bytes: 10_485_760 }));

  const segunda = casillaDeFoto(page, 2);
  await expect(segunda.getByText('Cargada', { exact: true })).toBeVisible({ timeout: 30_000 });
  await seVeLaImagen(segunda.getByRole('img', { name: 'Foto 2 de la muestra' }));
  await expect(primera.getByRole('img')).toHaveCount(0);
});

test('RF-05 · C5 · al reemplazar la foto 2, la página muestra la nueva y el enlace de la anterior ya no la abre: el archivo se borró (de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal, lote } = await loteNuevo(request);
  await subirFotosPorLaApi(request, usuarioFinal, lote.id, [{ numero: 2, archivo: png() }]);
  await iniciarSesion(page, usuarioFinal);
  await page.goto(`/lotes/${lote.id}/fotos`);
  const imagen = casillaDeFoto(page, 2).getByRole('img', { name: 'Foto 2 de la muestra' });
  await seVeLaImagen(imagen);
  const anterior = await imagen.getAttribute('src');

  await elegirFoto(page, 2, archivoPng('nueva.png', { color: [46, 107, 52] }));

  await expect(imagen).not.toHaveAttribute('src', anterior);
  await seVeLaImagen(imagen);
  expect((await request.get(anterior)).status()).toBe(404);
});

test('RF-14 · C5 · el usuario final sube una foto, va al historial, pulsa «Continuar» y vuelve a verla, con las casillas que faltan (de punta a punta)', async ({ page, request, isMobile }) => {
  const { usuarioFinal, lote } = await loteNuevo(request);
  await iniciarSesion(page, usuarioFinal);
  await page.goto(`/lotes/${lote.id}/fotos`);
  await page.getByLabel('Archivo de la foto 1').setInputFiles(archivoPng('granos.png'));
  await expect(casillaDeFoto(page, 1).getByText('Cargada', { exact: true })).toBeVisible();

  await page.goto('/historial');
  await page.getByRole('link', { name: `Continuar el lote ${lote.codigo}` }).click();

  await expect(page).toHaveURL(new RegExp(`/lotes/${lote.id}/fotos$`));
  await seVeLaImagen(casillaDeFoto(page, 1).getByRole('img', { name: 'Foto 1 de la muestra' }));
  await expect(casillaDeFoto(page, 2).getByRole('button', { name: isMobile ? 'Tomar foto' : 'Elegir archivo' }))
    .toBeVisible();
});

test('RF-14 · la administradora de la empresa ve el lote pendiente en el historial sin «Continuar», y en la dirección de sus fotos ve «Sin permiso» (de punta a punta)', async ({ page, request }) => {
  const { empresa, lote } = await loteNuevo(request);
  await iniciarSesion(page, empresa.administrador);

  await page.goto('/historial');

  await expect(page.getByText(lote.codigo, { exact: true })).toBeVisible();
  await expect(loteDelHistorial(page, lote.codigo).getByText('Pendiente de análisis')).toBeVisible();
  await expect(page.getByRole('link', { name: /^Continuar/ })).toHaveCount(0);
  await page.goto(`/lotes/${lote.id}/fotos`);
  await expect(page.getByRole('heading', { name: 'Sin permiso' })).toBeVisible();
});

test('RNF-03 · C1 · un usuario final de otra empresa que abre la dirección de las fotos de un lote ajeno ve que no existe, y la API le responde 404 (de punta a punta)', async ({ page, request }) => {
  const { usuarioFinal, lote } = await loteNuevo(request);
  await subirFotosPorLaApi(request, usuarioFinal, lote.id, [{ numero: 1, archivo: png() }]);
  const otra = await crearEmpresaConUsuarioFinal(request);
  await iniciarSesion(page, otra.usuarioFinal);

  await page.goto(`/lotes/${lote.id}/fotos`);

  await expect(page.getByRole('alert')).toHaveText('No existe un registro con ese identificador.');
  await expect(page.getByRole('img', { name: 'Foto 1 de la muestra' })).toHaveCount(0);
  const ajenas = await request.get(`/api/v1/lotes/${lote.id}/fotos`,
    { headers: { Authorization: `Bearer ${await tokenDe(request, otra.usuarioFinal)}` } });
  expect(ajenas.status()).toBe(404);
});

test('RNF-03 · C3 · una foto solo se abre con el enlace temporal que el backend firmó para la empresa dueña: con la firma cambiada o sin ella, responde 403 (de punta a punta)', async ({ request }) => {
  const { usuarioFinal, lote } = await loteNuevo(request);
  await subirFotosPorLaApi(request, usuarioFinal, lote.id, [{ numero: 1, archivo: png() }]);
  const lista = await request.get(`/api/v1/lotes/${lote.id}/fotos`,
    { headers: { Authorization: `Bearer ${await tokenDe(request, usuarioFinal)}` } });
  const [foto] = (await lista.json()).fotos;
  const [ruta, consulta] = foto.vista.url.split('?');
  const firma = new URLSearchParams(consulta).get('firma');
  const cambiada = new URLSearchParams(consulta);
  cambiada.set('firma', `${firma.startsWith('A') ? 'B' : 'A'}${firma.slice(1)}`);

  const propia = await request.get(foto.vista.url);

  expect(propia.status()).toBe(200);
  expect(propia.headers()['content-type']).toMatch(/^image\/png/);
  expect((await request.get(`${ruta}?${cambiada}`)).status()).toBe(403);
  expect((await request.get(ruta)).status()).toBe(403);
});
