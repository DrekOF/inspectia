/*
 * Nombre: modelo.spec.js
 * Descripción: Pruebas de punta a punta de la pantalla «Modelo de IA» (RF-17): las métricas de cada modelo, en la
 *              tabla del PC o en las tarjetas del celular, coinciden con las que guarda la tabla de modelos y responde la
 *              API (C1); la confianza promedio de cada empresa sale solo de sus análisis (C2), y el usuario final no ve la
 *              pantalla ni la API (C3). El entorno de punta a punta usa el clasificador falso: su modelo es el único
 *              registrado, solo con sus recall y sin estar en uso. Da 50 granos por foto, con confianza 90,00, salvo
 *              los primeros «alto módulo 50», que van con 55,00.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { randomUUID } from 'node:crypto';
import {
  abrirMenu, analizarPorLaApi, cerrarSesion, crearEmpresaConUsuarioFinal, datos, expect, iniciarSesion, registrarLotes,
  subirFotosPorLaApi, test, tokenDe,
} from './ayudas.js';
import { png } from './imagenes.js';

/** La pantalla en la app y su API. */
const RUTA = '/modelo';
const API = '/api/v1/modelo';

/** El nombre de la tabla del PC y de la lista del celular con las métricas de los modelos. */
const METRICAS = 'Métricas de los modelos';

/** Las métricas de un modelo, en el orden de la tabla. */
const COLUMNAS = ['f1Macro', 'precisionMacro', 'recallMacro', 'precisionSano', 'recallSano', 'precisionNoSano',
  'recallNoSano', 'tiempoPorGranoMs'];

/**
 * Una métrica como la muestra la pantalla en español: con 2 decimales y coma, o «—» si el modelo no la tiene.
 *
 * @param {number|null} valor la métrica, como la responde la API
 * @returns {string} el texto
 */
function textoDe(valor) {
  return valor === null ? '—' : new Intl.NumberFormat('es-CO', {
    minimumFractionDigits: 2, maximumFractionDigits: 2, useGrouping: false,
  }).format(valor);
}

/**
 * La fila de un modelo en la tabla del PC, o su tarjeta en el celular.
 *
 * @param {import('@playwright/test').Page} page la página, en «Modelo de IA»
 * @param {boolean} esCelular si la página se ve en tamaño de celular
 * @param {string} nombre el nombre del modelo
 * @returns {import('@playwright/test').Locator} la fila o la tarjeta
 */
function modeloEnLaPantalla(page, esCelular, nombre) {
  if (esCelular) {
    return page.getByRole('list', { name: METRICAS }).getByRole('listitem').filter({ hasText: nombre });
  }
  return page.getByRole('table', { name: METRICAS }).getByRole('row')
    .filter({ has: page.getByRole('rowheader', { name: new RegExp(`^${nombre.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}`) }) });
}

/**
 * Un lote analizado por la API con dos fotos de 106 y 107 px de ancho y el alto dado: cada foto tiene «alto módulo
 * 50» granos con confianza 55,00 y los demás, de 50, con 90,00.
 *
 * @param {import('@playwright/test').APIRequestContext} api el cliente de la API de Playwright
 * @param {{ correo: string, contrasena: string }} persona el usuario final
 * @param {number} alto el alto de las dos fotos
 */
async function loteAnalizadoConAlto(api, persona, alto) {
  const [lote] = await registrarLotes(api, persona, [{ codigo: `M-${randomUUID().slice(0, 8)}` }]);
  await subirFotosPorLaApi(api, persona, lote.id, [106, 107].map((ancho, indice) => ({
    numero: indice + 1, archivo: png({ ancho, alto }),
  })));
  await analizarPorLaApi(api, persona, lote.id);
}

/**
 * Abre «Modelo de IA» desde el menú, como la administradora de la empresa, y espera las métricas.
 *
 * @param {import('@playwright/test').Page} page la página
 * @param {boolean} esCelular si la página se ve en tamaño de celular
 * @param {{ nombre: string, correo: string, contrasena: string }} administrador quién entra
 */
async function abrirElModelo(page, esCelular, administrador) {
  await iniciarSesion(page, administrador);
  const menu = await abrirMenu(page, esCelular);
  await menu.getByRole('link', { name: 'Modelo de IA' }).click();
  await expect(page.getByRole('heading', { name: 'Modelo de IA', level: 1 })).toBeVisible();
  await expect(page.getByRole(esCelular ? 'list' : 'table', { name: METRICAS })).toBeVisible();
}

test('RF-17 · C1 · las métricas de cada modelo en la pantalla coinciden con las que guarda la tabla de modelos y responde la API, y el modelo en uso se marca, si hay uno (de punta a punta)', async ({ page, request, isMobile }) => {
  const respuesta = await request.get(API, {
    headers: { Authorization: `Bearer ${await tokenDe(request, datos.adminEmpresa)}` },
  });
  expect(respuesta.status()).toBe(200);
  const { modelos } = await respuesta.json();
  // Con el clasificador falso, su modelo está registrado, solo con sus recall.
  expect(modelos.map(({ nombre }) => nombre)).toContain('clasificador-falso');

  await abrirElModelo(page, isMobile, datos.adminEmpresa);

  for (const modelo of modelos) {
    const enLaPantalla = modeloEnLaPantalla(page, isMobile, modelo.nombre);
    await expect(enLaPantalla, modelo.nombre).toHaveCount(1);
    await expect(enLaPantalla).toContainText(`Versión ${modelo.version}`);
    for (const columna of COLUMNAS) {
      await expect(enLaPantalla, `${modelo.nombre}: ${columna}`).toContainText(textoDe(modelo[columna]));
    }
    await expect(enLaPantalla.getByText('En uso', { exact: true }), `${modelo.nombre} en uso`)
      .toHaveCount(modelo.enUso ? 1 : 0);
  }
  const enUso = page.getByRole('region', { name: 'Modelo en uso' });
  const elegido = modelos.find((modelo) => modelo.enUso);
  await expect(enUso).toContainText(elegido ? elegido.nombre : 'Ningún modelo está en uso.');
});

test('RF-17 · C2 · la confianza promedio de cada empresa usa solo los granos de sus análisis: 88,60 % en una y 90,00 % en la otra (de punta a punta)', async ({ page, request, isMobile }) => {
  test.slow();
  const conDudosos = await crearEmpresaConUsuarioFinal(request);
  const sinDudosos = await crearEmpresaConUsuarioFinal(request);
  // 2 granos de 50 con 55,00 en cada foto: (96 × 90 + 4 × 55) / 100 = 88,60.
  await loteAnalizadoConAlto(request, conDudosos.usuarioFinal, 102);
  // Todos con 90,00.
  await loteAnalizadoConAlto(request, sinDudosos.usuarioFinal, 100);

  await abrirElModelo(page, isMobile, conDudosos.empresa.administrador);
  const confianza = page.getByRole('region', { name: 'Confianza promedio de tus análisis' });
  await expect(confianza).toContainText('88,60 %');
  await expect(confianza)
    .toContainText(`De 100 granos en 1 muestra analizada de ${conDudosos.empresa.nombre}.`);

  await cerrarSesion(page);
  await abrirElModelo(page, isMobile, sinDudosos.empresa.administrador);
  await expect(confianza).toContainText('90,00 %');
  await expect(confianza)
    .toContainText(`De 100 granos en 1 muestra analizada de ${sinDudosos.empresa.nombre}.`);
});

test('RF-17 · C3 · un usuario final no puede entrar a «Modelo de IA»: no está en su menú, su dirección le dice que no tiene permiso y la API le responde 403 (de punta a punta)', async ({ page, request, isMobile, usuarioFinal }) => {
  await iniciarSesion(page, usuarioFinal);
  const menu = await abrirMenu(page, isMobile);
  await expect(menu.getByRole('link', { name: 'Inicio' })).toBeVisible();
  await expect(menu.getByRole('link', { name: 'Modelo de IA' })).toHaveCount(0);

  await page.goto(RUTA);

  await expect(page.getByText('No tienes permiso para esta operación.')).toBeVisible();
  await expect(page.getByRole('table')).toHaveCount(0);
  const conSuToken = await request.get(API, {
    headers: { Authorization: `Bearer ${await tokenDe(request, usuarioFinal)}` },
  });
  expect(conSuToken.status()).toBe(403);
  expect((await request.get(API)).status()).toBe(401);
});
