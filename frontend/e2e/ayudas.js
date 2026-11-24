/*
 * Nombre: ayudas.js (e2e)
 * Descripción: Ayudas de las pruebas de punta a punta con Playwright: los datos del entorno aislado, la persona
 *              de cada rol, crear usuarios y empresas propios por la API para las pruebas que cambian datos,
 *              iniciar y cerrar sesión, buscar una fila en la lista de una pantalla generada, una empresa propia
 *              con su usuario final y sus lotes, para las pruebas de los lotes (RF-04, RF-14), subir fotos de la
 *              muestra en la pantalla o por la API (RF-05), y analizar un lote por la API (RF-10).
 * Fecha de creación: 20/10/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.6
 */
import { randomUUID } from 'node:crypto';
import { test as base, expect } from '@playwright/test';
import { datos, empresaNueva } from './entorno/datos.js';
import { diaEnColombia } from './fechas.js';
import { png } from './imagenes.js';

/*
 * Los datos del entorno aislado que arma e2e/entorno (backend, base y usuarios de cada corrida): el
 * administrador de la plataforma, una empresa con su administrador y su usuario final, y un usuario de una
 * empresa desactivada. Las contraseñas se crean en cada corrida y nunca quedan en el repositorio. Con
 * empresaNueva, cada prueba arma los datos de su propia empresa.
 */
export { datos, empresaNueva, expect };

/** Contraseña de los usuarios que crean las pruebas. */
export const CONTRASENA = 'Clave-Segura-2026';

/**
 * La persona de prueba de un rol, para las pruebas que solo miran, como los permisos de una pantalla generada:
 * el administrador de la plataforma, el de la empresa de prueba o su usuario final.
 *
 * @param {string} rol ADMIN_PLATAFORMA, ADMIN_EMPRESA o USUARIO_FINAL
 * @returns {{ nombre: string, correo: string, contrasena: string }} la persona
 */
export function personaDelRol(rol) {
  const persona = {
    ADMIN_PLATAFORMA: datos.plataforma, ADMIN_EMPRESA: datos.adminEmpresa, USUARIO_FINAL: datos.usuarioFinal,
  }[rol];
  if (!persona) {
    throw new Error(`El entorno aislado no tiene una persona con el rol ${rol}`);
  }
  return persona;
}

/**
 * El token de una persona, pedido directamente a la API.
 *
 * @param {import('@playwright/test').APIRequestContext} api el cliente de la API de Playwright
 * @param {{ correo: string, contrasena: string }} persona quién inicia sesión
 * @returns {Promise<string>} el token
 */
export async function tokenDe(api, persona) {
  const respuesta = await api.post('/api/v1/sesion', { data: { correo: persona.correo, contrasena: persona.contrasena } });
  expect(respuesta.status()).toBe(200);
  return (await respuesta.json()).token;
}

/**
 * Crea un usuario nuevo en la empresa de prueba, con el administrador de empresa: así cada prueba que
 * cambia datos (contraseña o bloqueo) tiene el suyo y no choca con las demás.
 *
 * @param {import('@playwright/test').APIRequestContext} api el cliente de la API de Playwright
 * @param {string} [rol] el rol: USUARIO_FINAL o ADMIN_EMPRESA
 * @param {string} [contrasena] su contraseña; por defecto, CONTRASENA
 * @returns {Promise<{ id: string, nombre: string, correo: string, contrasena: string }>} el usuario
 */
export async function crearUsuario(api, rol = 'USUARIO_FINAL', contrasena = CONTRASENA) {
  const persona = { nombre: 'Luis Gómez', correo: `luis.${randomUUID().slice(0, 8)}@cacaosdelhuila.test`,
    contrasena, rol };
  const respuesta = await api.post('/api/v1/usuarios', {
    data: persona, headers: { Authorization: `Bearer ${await tokenDe(api, datos.adminEmpresa)}` },
  });
  expect(respuesta.status()).toBe(201);
  return { ...persona, id: (await respuesta.json()).id };
}

/**
 * Desactiva un usuario de la empresa de prueba, con el administrador de empresa.
 *
 * @param {import('@playwright/test').APIRequestContext} api el cliente de la API de Playwright
 * @param {string} id el usuario
 */
export async function desactivarUsuario(api, id) {
  const respuesta = await api.post(`/api/v1/usuarios/${id}/desactivar`, {
    headers: { Authorization: `Bearer ${await tokenDe(api, datos.adminEmpresa)}` },
  });
  expect(respuesta.status()).toBe(200);
}

/**
 * Registra una empresa nueva por la API, con el administrador de la plataforma: para las pruebas que la
 * desactivan, la reactivan o restablecen la contraseña de su administrador.
 *
 * @param {import('@playwright/test').APIRequestContext} api el cliente de la API de Playwright
 * @returns {Promise<object>} la empresa, con su id y su primer administrador
 */
export async function crearEmpresa(api) {
  const empresa = empresaNueva(CONTRASENA);
  const respuesta = await api.post('/api/v1/empresas', {
    data: empresa, headers: { Authorization: `Bearer ${await tokenDe(api, datos.plataforma)}` },
  });
  expect(respuesta.status()).toBe(201);
  return { ...empresa, id: (await respuesta.json()).id };
}

/**
 * Una fecha de recepción de hace una semana en Colombia, para los lotes que no prueban la fecha. Es relativa a
 * hoy: una fecha fija pasaría a ser de hace más de un año, y la app la rechazaría (RF-04).
 */
export const RECEPCION = diaEnColombia(-7);

/**
 * Una empresa nueva con un usuario final, creados por la API: así cada prueba de los lotes tiene los suyos y no ve
 * los de las demás, que corren a la vez sobre la misma base.
 *
 * @param {import('@playwright/test').APIRequestContext} api el cliente de la API de Playwright
 * @returns {Promise<{ empresa: object, usuarioFinal: { nombre: string, correo: string, contrasena: string } }>}
 *   la empresa y su usuario final
 */
export async function crearEmpresaConUsuarioFinal(api) {
  const empresa = await crearEmpresa(api);
  const usuarioFinal = { nombre: 'Marta Ruiz', correo: `marta.${randomUUID().slice(0, 8)}@cacaoandino.test`,
    contrasena: CONTRASENA, rol: 'USUARIO_FINAL' };
  const respuesta = await api.post('/api/v1/usuarios', {
    data: usuarioFinal, headers: { Authorization: `Bearer ${await tokenDe(api, empresa.administrador)}` },
  });
  expect(respuesta.status()).toBe(201);
  return { empresa, usuarioFinal };
}

/**
 * Registra lotes por la API con un usuario final, uno detrás de otro (RF-04).
 *
 * @param {import('@playwright/test').APIRequestContext} api el cliente de la API de Playwright
 * @param {{ correo: string, contrasena: string }} persona el usuario final
 * @param {Array<{ codigo: string, origen?: string }>} lotes los lotes
 * @returns {Promise<object[]>} los lotes registrados, como los responde la API
 */
export async function registrarLotes(api, persona, lotes) {
  const token = await tokenDe(api, persona);
  const registrados = [];
  for (const { codigo, origen = 'Finca La Esperanza' } of lotes) {
    const respuesta = await api.post('/api/v1/lotes', {
      data: { codigo, fechaRecepcion: RECEPCION, origen, observaciones: null },
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(respuesta.status(), `registrar ${codigo}`).toBe(201);
    registrados.push(await respuesta.json());
  }
  return registrados;
}

/**
 * Sube fotos de la muestra de un lote por la API, como la página (D-41): pide el enlace, sube el archivo al enlace
 * sin el token y lo confirma.
 *
 * @param {import('@playwright/test').APIRequestContext} api el cliente de la API de Playwright
 * @param {{ correo: string, contrasena: string }} persona el usuario final
 * @param {string} loteId el lote
 * @param {Array<{ numero: number, archivo: Buffer, tipo?: string }>} fotos el número de cada foto, sus bytes y su
 *   tipo; por defecto, image/png
 */
export async function subirFotosPorLaApi(api, persona, loteId, fotos) {
  const autorizacion = { Authorization: `Bearer ${await tokenDe(api, persona)}` };
  for (const { numero, archivo, tipo = 'image/png' } of fotos) {
    const pedido = await api.post(`/api/v1/lotes/${loteId}/fotos/${numero}/subida`,
      { data: { tipo, tamanoBytes: archivo.length }, headers: autorizacion });
    expect(pedido.status(), `enlace de la foto ${numero}`).toBe(201);
    const enlace = await pedido.json();
    const subida = await api.fetch(enlace.url, { method: enlace.metodo, headers: enlace.encabezados, data: archivo });
    expect(subida.ok(), `subida de la foto ${numero}`).toBe(true);
    const confirmada = await api.put(`/api/v1/lotes/${loteId}/fotos/${numero}`,
      { data: { llave: enlace.llave }, headers: autorizacion });
    expect(confirmada.status(), `confirmación de la foto ${numero}`).toBe(200);
  }
}

/**
 * Envía a analizar la muestra de un lote por la API, como «Analizar la muestra», y espera a que termine (RF-10).
 *
 * @param {import('@playwright/test').APIRequestContext} api el cliente de la API de Playwright
 * @param {{ correo: string, contrasena: string }} persona el usuario final
 * @param {string} loteId el lote, con 2 o 3 fotos ya subidas
 */
export async function analizarPorLaApi(api, persona, loteId) {
  const autorizacion = { Authorization: `Bearer ${await tokenDe(api, persona)}` };
  const envio = await api.post(`/api/v1/lotes/${loteId}/analisis`, { headers: autorizacion });
  expect(envio.status(), 'enviar la muestra').toBe(202);
  await expect.poll(async () => {
    const avance = await api.get(`/api/v1/lotes/${loteId}/analisis`, { headers: autorizacion });
    return (await avance.json()).estado;
  }, { message: 'el análisis termina', timeout: 30_000 }).toBe('TERMINADA');
}

/**
 * Un lote analizado por la API: lo registra, le sube una foto de 100 px de alto por cada ancho y lo analiza. El
 * clasificador falso da 50 granos por foto, y son no sanos tantos como el ancho módulo 50: con 106 y 107, 87 de 100
 * sanos, que corregido da 90,6 % (Estándar, con sugerencia).
 *
 * @param {import('@playwright/test').APIRequestContext} api el cliente de la API de Playwright
 * @param {{ correo: string, contrasena: string }} persona el usuario final
 * @param {number[]} [anchos] el ancho de cada foto
 * @returns {Promise<object>} el lote, como lo responde la API al registrarlo
 */
export async function loteAnalizado(api, persona, anchos = [106, 107]) {
  const [lote] = await registrarLotes(api, persona, [{ codigo: `R-${randomUUID().slice(0, 8)}` }]);
  await subirFotosPorLaApi(api, persona, lote.id,
    anchos.map((ancho, indice) => ({ numero: indice + 1, archivo: png({ ancho, alto: 100 }) })));
  await analizarPorLaApi(api, persona, lote.id);
  return lote;
}

/**
 * La casilla de una foto en la página de las fotos de la muestra.
 *
 * @param {import('@playwright/test').Page} page la página
 * @param {number} numero el número de la foto, de 1 a 3
 * @returns {import('@playwright/test').Locator} la casilla
 */
export function casillaDeFoto(page, numero) {
  return page.getByRole('group', { name: numero === 3 ? 'Foto 3 (opcional)' : `Foto ${numero}`, exact: true });
}

/**
 * Elige una foto como una persona: pulsa el botón de su casilla («Elegir archivo», «Tomar foto» o «Reemplazar»),
 * que abre el selector de archivos, y elige el archivo.
 *
 * @param {import('@playwright/test').Page} page la página, en las fotos de la muestra
 * @param {number} numero el número de la foto
 * @param {{ name: string, mimeType: string, buffer: Buffer }} archivo el archivo
 * @returns {Promise<import('@playwright/test').FileChooser>} el selector que se abrió
 */
export async function elegirFoto(page, numero, archivo) {
  const boton = casillaDeFoto(page, numero).getByRole('button', { name: /^(Elegir archivo|Tomar foto|Reemplazar)$/ });
  const [selector] = await Promise.all([page.waitForEvent('filechooser'), boton.click()]);
  await selector.setFiles(archivo);
  return selector;
}

/**
 * Espera que una imagen se vea entera: que el navegador la haya cargado y tenga ancho. Así se sabe que su enlace pasa
 * por la app hasta el backend y que el archivo es una imagen.
 *
 * @param {import('@playwright/test').Locator} imagen la imagen
 */
export async function seVeLaImagen(imagen) {
  await expect(imagen).toBeVisible();
  await expect.poll(() => imagen.evaluate((elemento) => (elemento.complete ? elemento.naturalWidth : 0)))
    .toBeGreaterThan(0);
}

/**
 * Desactiva una empresa por la API, con el administrador de la plataforma.
 *
 * @param {import('@playwright/test').APIRequestContext} api el cliente de la API de Playwright
 * @param {string} id la empresa
 */
export async function desactivarEmpresa(api, id) {
  const respuesta = await api.post(`/api/v1/empresas/${id}/desactivar`, {
    headers: { Authorization: `Bearer ${await tokenDe(api, datos.plataforma)}` },
  });
  expect(respuesta.status()).toBe(200);
}

/**
 * Las pruebas con un usuario final propio, que se crea antes de cada prueba y se desactiva al terminar:
 * así no chocan con las de otros navegadores y la empresa de prueba no llega a los 25 activos (RF-03).
 */
export const test = base.extend({
  usuarioFinal: async ({ request }, usar) => {
    const persona = await crearUsuario(request);
    await usar(persona);
    await desactivarUsuario(request, persona.id);
  },
});

/**
 * Escribe el correo y la contraseña en Iniciar sesión y pulsa el botón.
 *
 * @param {import('@playwright/test').Page} page la página
 * @param {{ correo: string, contrasena: string }} persona quién entra
 */
export async function escribirYEntrar(page, persona) {
  await page.getByLabel('Correo', { exact: true }).fill(persona.correo);
  await page.getByLabel('Contraseña', { exact: true }).fill(persona.contrasena);
  await page.getByRole('button', { name: 'Iniciar sesión' }).click();
}

/**
 * Inicia sesión desde la pantalla Iniciar sesión y espera el saludo del inicio.
 *
 * @param {import('@playwright/test').Page} page la página
 * @param {{ nombre: string, correo: string, contrasena: string }} persona quién entra
 */
export async function iniciarSesion(page, persona) {
  await page.goto('/iniciar-sesion');
  await escribirYEntrar(page, persona);
  await expect(page.getByRole('heading', { name: `Hola, ${persona.nombre.split(' ')[0]}` })).toBeVisible();
}

/**
 * El menú principal. En el celular está detrás del botón «Abrir el menú»: lo abre primero.
 *
 * @param {import('@playwright/test').Page} page la página
 * @param {boolean} esCelular si la prueba corre en tamaño de celular
 * @returns {Promise<import('@playwright/test').Locator>} el menú
 */
export async function abrirMenu(page, esCelular) {
  if (esCelular) {
    await page.getByRole('button', { name: 'Abrir el menú' }).click();
  }
  const menu = page.getByRole('navigation', { name: 'Menú principal' });
  await expect(menu).toBeVisible();
  return menu;
}

/**
 * Cierra la sesión desde «Mi cuenta» y espera Iniciar sesión, para entrar con otra persona.
 *
 * @param {import('@playwright/test').Page} page la página
 */
export async function cerrarSesion(page) {
  await page.getByRole('button', { name: /^Mi cuenta:/ }).click();
  await page.getByRole('menuitem', { name: 'Cerrar sesión' }).click();
  await expect(page.getByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
}

/**
 * Un botón de una fila de la lista de una pantalla generada, como «Desactivar Cacao Andino 1a2b3c4d». Las
 * páginas son de 20 registros: si no está en la que se ve, pasa a la siguiente hasta encontrarlo. Al pasar de
 * página, el rango («21–40 de 45») cambia enseguida, pero las filas siguen siendo las de la página anterior
 * hasta que responde la API: por eso, antes de buscar otra vez, espera que cambie la primera fila. Si no,
 * puede encontrar el botón en filas viejas o llegar a la última página antes de que carguen sus filas.
 *
 * @param {import('@playwright/test').Page} page la página, con la lista ya cargada
 * @param {string} nombre el nombre accesible del botón
 * @returns {Promise<import('@playwright/test').Locator>} el botón
 */
export async function botonDeLaFila(page, nombre) {
  const boton = page.getByRole('button', { name: nombre, exact: true });
  const siguiente = page.getByRole('button', { name: 'Ir a la página siguiente' });
  // La primera fila de la tabla, en el PC, o la primera tarjeta, en el celular: de la lista con nombre, la de la
  // pantalla, y no de otra lista de la página, como la del panel de administradores.
  const primera = page.locator('table tbody tr, ul[role="list"][aria-label] > li').first();
  while (!(await boton.isVisible())) {
    await expect(siguiente, `«${nombre}» no está en ninguna página de la lista`).toBeEnabled();
    const antes = await primera.textContent();
    await siguiente.click();
    await expect(primera).not.toHaveText(antes);
  }
  return boton;
}
