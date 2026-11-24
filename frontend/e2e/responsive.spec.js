/*
 * Nombre: responsive.spec.js
 * Descripción: Pruebas de punta a punta del diseño para PC y celular (RNF-05): ninguna pantalla se desplaza a
 *              los lados desde 360 px de ancho, también las de los lotes (RF-04, RF-14), con el historial en 7
 *              páginas, las fotos de la muestra con una foto subida y un error a la vista (RF-05) y el resultado de un
 *              lote (RF-10, RF-11), el historial con los filtros abiertos, un error y el chip «Sugiere otra muestra»
 *              (RF-14 C2) y el resultado con la sugerencia, con la espera de la muestra 2 y con dos muestras (RF-14
 *              C6), y «Modelo de IA» (RF-17); las pruebas corren en tamaño de PC y de celular.
 * Fecha de creación: 20/10/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.6
 */
import { randomUUID } from 'node:crypto';
import {
  abrirFiltros, casillaDeFoto, crearEmpresaConUsuarioFinal, datos, expect, filtrarElHistorial, iniciarSesion,
  loteAnalizado, loteConDosMuestras, loteDelHistorial, registrarLotes, retenerLaEspera, seVeLaImagen,
  subirFotosPorLaApi, test,
} from './ayudas.js';
import { diaEnColombia } from './fechas.js';
import { png } from './imagenes.js';

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
  for (const ruta of ['/cambiar-contrasena', '/acerca-de', '/estadisticas', '/historial', '/modelo']) {
    await page.goto(ruta);
    await expect(page.getByRole('main')).toBeVisible();
    await expect(page.getByRole('progressbar')).toHaveCount(0);
    await sinDesplazamientoLateral(page, ruta);
  }
});

test('RNF-05 · C1 · las pantallas de los lotes no se desplazan a los lados desde 360 px de ancho, tampoco el historial con 7 páginas, las fotos con un error a la vista ni el resultado', async ({ page, request }) => {
  // Registrar 121 lotes por la API toma unos segundos más que las demás pruebas.
  test.slow();
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const prefijo = `R${randomUUID().slice(0, 6)}`;
  // Con 121 lotes hay 7 páginas: en la primera, la fila de los botones de las páginas es la más ancha.
  const lotes = await registrarLotes(request, usuarioFinal,
    Array.from({ length: 121 }, (_, indice) => ({ codigo: `${prefijo}-${indice + 1}` })));
  const ultimo = lotes.at(-1);
  await subirFotosPorLaApi(request, usuarioFinal, ultimo.id, [{ numero: 1, archivo: png() }]);
  const analizado = await loteAnalizado(request, usuarioFinal, [106, 107]);
  await page.setViewportSize({ width: 360, height: 740 });
  await iniciarSesion(page, usuarioFinal);

  await page.goto('/lotes/nuevo');
  await expect(page.getByRole('heading', { name: 'Nuevo lote' })).toBeVisible();
  await sinDesplazamientoLateral(page, 'Nuevo lote');
  await page.goto('/historial');
  await expect(page.getByText(ultimo.codigo, { exact: true })).toBeVisible();
  // Más el analizado: 122 lotes, todavía en 7 páginas.
  await expect(page.getByText('122 lotes · 20 por página')).toBeVisible();
  await sinDesplazamientoLateral(page, 'Historial, con 7 páginas');
  await page.goto(`/lotes/${ultimo.id}/fotos`);
  await expect(page.getByRole('region', { name: 'Guía de captura' })).toBeVisible();
  await seVeLaImagen(casillaDeFoto(page, 1).getByRole('img', { name: 'Foto 1 de la muestra' }));
  // El error más largo, en la casilla más angosta: la 2, en una de las 2 columnas del celular.
  await page.getByLabel('Archivo de la foto 2').setInputFiles({ name: 'grande.png', mimeType: 'image/png',
    buffer: png({ bytes: 10_485_761 }) });
  await expect(casillaDeFoto(page, 2).getByRole('alert')).toBeVisible();
  await sinDesplazamientoLateral(page, 'Fotos de la muestra, con una foto subida y otra rechazada');
  await page.goto(`/lotes/${analizado.id}`);
  await expect(page.getByRole('group', { name: 'Granos de la foto 1' }).getByRole('button')).toHaveCount(50);
  await sinDesplazamientoLateral(page, 'Resultado del lote');
});

test('RNF-05 · C1 · a 360 px no se desplazan a los lados el historial con los filtros abiertos, un error y el chip «Sugiere otra muestra», ni el resultado con la sugerencia, con la espera de la muestra 2 y con dos muestras', async ({ page, request }) => {
  // Cuatro análisis.
  test.slow();
  const { usuarioFinal } = await crearEmpresaConUsuarioFinal(request);
  const conSugerencia = await loteAnalizado(request, usuarioFinal, [106, 107]);
  // La muestra 2 con sus fotos subidas, sin enviar: la prueba la retiene en la cola para mirar su espera.
  const enEspera = await loteAnalizado(request, usuarioFinal, [106, 107]);
  await subirFotosPorLaApi(request, usuarioFinal, enEspera.id, [
    { numero: 1, archivo: png({ ancho: 108, alto: 100 }) }, { numero: 2, archivo: png({ ancho: 107, alto: 100 }) },
  ], { muestra: 2 });
  const dosMuestras = await loteConDosMuestras(request, usuarioFinal);
  await page.setViewportSize({ width: 360, height: 740 });
  await iniciarSesion(page, usuarioFinal);

  await page.goto('/historial');
  await expect(loteDelHistorial(page, conSugerencia.codigo).getByText('Sugiere otra muestra', { exact: true }))
    .toBeVisible();
  // A 360 px, los filtros van detrás del botón «Filtros», también en el proyecto de PC.
  const formulario = await abrirFiltros(page, true);
  await sinDesplazamientoLateral(page, 'Historial con los filtros abiertos y el chip');
  // Con todos los campos llenos y el error más largo, bajo «Hasta».
  await filtrarElHistorial(formulario, {
    desde: diaEnColombia(0), hasta: diaEnColombia(-1), estado: 'Pendiente de análisis', categoria: 'Premium o Especial',
    codigo: 'L-0142', usuario: 'Marta Ruiz',
  });
  await expect(formulario.getByText('La fecha «Hasta» no puede ser anterior a «Resultado desde».')).toBeVisible();
  await sinDesplazamientoLateral(page, 'Historial con un error en los filtros');
  await page.goto(`/lotes/${conSugerencia.id}`);
  await expect(page.getByRole('region', { name: 'Sugerencia' }).getByRole('link', { name: 'Analizar otra muestra' }))
    .toBeVisible();
  await sinDesplazamientoLateral(page, 'Resultado con la sugerencia de otra muestra');
  const espera = await retenerLaEspera(page, request, usuarioFinal, enEspera.id);
  await page.goto(`/lotes/${enEspera.id}`);
  await expect(page.getByRole('note')).toBeVisible();
  await expect(page.getByRole('group', { name: 'Granos de la foto 1 de la muestra 1' }).getByRole('button'))
    .toHaveCount(50);
  await sinDesplazamientoLateral(page, 'Resultado con la espera de la muestra 2');
  await espera.soltar();
  await page.goto(`/lotes/${dosMuestras.id}`);
  await expect(page.getByRole('group', { name: 'Granos de la foto 1 de la muestra 2' }).getByRole('button'))
    .toHaveCount(50);
  await sinDesplazamientoLateral(page, 'Resultado con dos muestras');
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
