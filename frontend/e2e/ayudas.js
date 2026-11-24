/*
 * Nombre: ayudas.js (e2e)
 * Descripción: Ayudas de las pruebas de punta a punta con Playwright: los datos del entorno aislado, la persona
 *              de cada rol, crear usuarios y empresas propios por la API para las pruebas que cambian datos,
 *              iniciar y cerrar sesión, buscar una fila en la lista de una pantalla generada, una empresa propia
 *              con su usuario final y sus lotes, para las pruebas de los lotes (RF-04, RF-14), subir fotos de la
 *              muestra en la pantalla o por la API (RF-05), analizar un lote por la API (RF-10), también su segunda
 *              muestra (RF-14 C6), crear y desactivar usuarios finales de una empresa propia (RF-03 C6), filtrar el
 *              historial y encontrar un lote en él (RF-14 C2), y retener la espera de la segunda muestra para mirarla
 *              (D-25).
 * Fecha de creación: 20/10/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.7
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
 * @returns {Promise<{ empresa: object,
 *   usuarioFinal: { id: string, nombre: string, correo: string, contrasena: string } }>} la empresa y su usuario final
 */
export async function crearEmpresaConUsuarioFinal(api) {
  const empresa = await crearEmpresa(api);
  const usuarioFinal = { nombre: 'Marta Ruiz', correo: `marta.${randomUUID().slice(0, 8)}@cacaoandino.test`,
    contrasena: CONTRASENA, rol: 'USUARIO_FINAL' };
  const respuesta = await api.post('/api/v1/usuarios', {
    data: usuarioFinal, headers: { Authorization: `Bearer ${await tokenDe(api, empresa.administrador)}` },
  });
  expect(respuesta.status()).toBe(201);
  return { empresa, usuarioFinal: { ...usuarioFinal, id: (await respuesta.json()).id } };
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
 * Envía a analizar la muestra de un lote por la API, como «Analizar la muestra», y espera a que termine (RF-10). El
 * avance es el de la muestra en curso: se espera el de esa muestra, así la 1 terminada no pasa por la 2.
 *
 * @param {import('@playwright/test').APIRequestContext} api el cliente de la API de Playwright
 * @param {{ correo: string, contrasena: string }} persona el usuario final
 * @param {string} loteId el lote, con 2 o 3 fotos ya subidas
 * @param {{ muestra?: number }} [opciones] el número de la muestra que se envía; por defecto, la 1
 */
export async function analizarPorLaApi(api, persona, loteId, { muestra = 1 } = {}) {
  const autorizacion = { Authorization: `Bearer ${await tokenDe(api, persona)}` };
  const envio = await api.post(`/api/v1/lotes/${loteId}/analisis`, { headers: autorizacion });
  expect(envio.status(), `enviar la muestra ${muestra}`).toBe(202);
  await expect.poll(async () => {
    const avance = await (await api.get(`/api/v1/lotes/${loteId}/analisis`, { headers: autorizacion })).json();
    return { muestra: avance.muestra, estado: avance.estado };
  }, { message: `el análisis de la muestra ${muestra} termina`, timeout: 30_000 })
    .toEqual({ muestra, estado: 'TERMINADA' });
}

/**
 * Las fotos de una muestra para el clasificador falso: una de 100 px de alto por cada ancho, sin granos dudosos.
 *
 * @param {number[]} anchos el ancho de cada foto
 * @returns {Array<{ numero: number, archivo: Buffer }>} las fotos, numeradas desde 1
 */
function fotosDeAnchos(anchos) {
  return anchos.map((ancho, indice) => ({ numero: indice + 1, archivo: png({ ancho, alto: 100 }) }));
}

/**
 * Un lote analizado por la API: lo registra, le sube una foto de 100 px de alto por cada ancho y lo analiza. El
 * clasificador falso da 50 granos por foto, y son no sanos tantos como el ancho módulo 50: con 106 y 107, 87 de 100
 * sanos, que corregido da 90,6 % (Estándar, con sugerencia).
 *
 * @param {import('@playwright/test').APIRequestContext} api el cliente de la API de Playwright
 * @param {{ correo: string, contrasena: string }} persona el usuario final
 * @param {number[]} [anchos] el ancho de cada foto
 * @param {{ codigo?: string }} [opciones] el código del lote; por defecto, uno nuevo
 * @returns {Promise<object>} el lote, como lo responde la API al registrarlo
 */
export async function loteAnalizado(api, persona, anchos = [106, 107],
  { codigo = `R-${randomUUID().slice(0, 8)}` } = {}) {
  const [lote] = await registrarLotes(api, persona, [{ codigo }]);
  await subirFotosPorLaApi(api, persona, lote.id, fotosDeAnchos(anchos));
  await analizarPorLaApi(api, persona, lote.id);
  return lote;
}

/**
 * Analiza por la API la segunda muestra de un lote cuyo resultado la sugiere (D-25): la primera foto confirmada crea
 * la muestra 2, y al terminar el lote queda con la suma de las dos.
 *
 * @param {import('@playwright/test').APIRequestContext} api el cliente de la API de Playwright
 * @param {{ correo: string, contrasena: string }} persona el usuario final
 * @param {string} loteId el lote, analizado y con la sugerencia de otra muestra
 * @param {number[]} anchos el ancho de cada foto de la muestra 2
 */
export async function segundaMuestraPorLaApi(api, persona, loteId, anchos) {
  await subirFotosPorLaApi(api, persona, loteId, fotosDeAnchos(anchos));
  await analizarPorLaApi(api, persona, loteId, { muestra: 2 });
}

/**
 * Un lote con dos muestras analizadas por la API. Con las de siempre, 106 y 107 en la 1 y 108 y 107 en la 2, suma
 * 172 de 200 sanos: 88,3 % corregido, Corriente, sin otra sugerencia.
 *
 * @param {import('@playwright/test').APIRequestContext} api el cliente de la API de Playwright
 * @param {{ correo: string, contrasena: string }} persona el usuario final
 * @param {number[]} [primera] el ancho de cada foto de la muestra 1, que debe sugerir otra muestra
 * @param {number[]} [segunda] el ancho de cada foto de la muestra 2
 * @returns {Promise<object>} el lote, como lo responde la API al registrarlo
 */
export async function loteConDosMuestras(api, persona, primera = [106, 107], segunda = [108, 107]) {
  const lote = await loteAnalizado(api, persona, primera);
  await segundaMuestraPorLaApi(api, persona, lote.id, segunda);
  return lote;
}

/**
 * Retiene en la página la muestra en curso de un lote como si esperara en la cola, para mirar su espera. El
 * clasificador falso del entorno aislado responde enseguida y la espera dura un instante: mientras la prueba la
 * mira, la página recibe el resultado que el backend tenía antes de enviar la muestra, el real, con la muestra en
 * curso en espera, y ese mismo avance. Lo que la página manda, como el envío de la muestra, llega al backend. Al
 * soltarla, la página vuelve a recibir lo que responde el backend.
 *
 * @param {import('@playwright/test').Page} page la página
 * @param {import('@playwright/test').APIRequestContext} api el cliente de la API de Playwright
 * @param {{ correo: string, contrasena: string }} persona quien la mira
 * @param {string} loteId el lote, con la muestra en curso pendiente y sus fotos subidas
 * @returns {Promise<{ soltar: () => Promise<void> }>} cómo soltarla
 */
export async function retenerLaEspera(page, api, persona, loteId) {
  const autorizacion = { Authorization: `Bearer ${await tokenDe(api, persona)}` };
  const respuesta = await api.get(`/api/v1/lotes/${loteId}/resultado`, { headers: autorizacion });
  expect(respuesta.status(), 'el resultado antes de enviar la muestra').toBe(200);
  const antes = await respuesta.json();
  const numero = Math.max(...antes.muestras.map((muestra) => muestra.numero));
  const fechaEnvio = new Date().toISOString();
  const resultado = {
    ...antes,
    muestras: antes.muestras.map((muestra) => (muestra.numero === numero
      ? { ...muestra, estado: 'EN_ESPERA', fechaEnvio } : muestra)),
  };
  const avance = { muestra: numero, estado: 'EN_ESPERA', fechaEnvio, fechaInicio: null, fechaFin: null, fallo: false };
  const rutas = [
    [`**/api/v1/lotes/${loteId}/resultado`, (ruta) => ruta.fulfill({ json: resultado })],
    [`**/api/v1/lotes/${loteId}/analisis`,
      (ruta) => (ruta.request().method() === 'GET' ? ruta.fulfill({ json: avance }) : ruta.fallback())],
  ];
  for (const [direccion, manejador] of rutas) {
    await page.route(direccion, manejador);
  }
  return {
    soltar: async () => {
      for (const [direccion, manejador] of rutas) {
        await page.unroute(direccion, manejador);
      }
    },
  };
}

/**
 * Un lote del historial: su fila de la tabla, en el PC, o su tarjeta, en el celular.
 *
 * @param {import('@playwright/test').Page} page la página, en el historial
 * @param {string} codigo el código del lote
 * @returns {import('@playwright/test').Locator} la fila o la tarjeta
 */
export function loteDelHistorial(page, codigo) {
  return page.locator('table tbody tr, ul[aria-label="Lotes de la empresa"] > li')
    .filter({ has: page.getByText(codigo, { exact: true }) });
}

/**
 * El formulario de los filtros del historial (RF-14 C2). En el celular está detrás del botón «Filtros»: lo abre si
 * está cerrado.
 *
 * @param {import('@playwright/test').Page} page la página, en el historial
 * @param {boolean} esCelular si la página se ve en tamaño de celular
 * @returns {Promise<import('@playwright/test').Locator>} el formulario
 */
export async function abrirFiltros(page, esCelular) {
  if (esCelular) {
    const boton = page.getByRole('button', { name: 'Filtros', exact: true });
    if ((await boton.getAttribute('aria-expanded')) !== 'true') {
      await boton.click();
    }
    await expect(boton).toHaveAttribute('aria-expanded', 'true');
  }
  const formulario = page.getByRole('form', { name: 'Filtros del historial' });
  await expect(formulario).toBeVisible();
  return formulario;
}

/**
 * Escribe los filtros del historial y pulsa «Aplicar filtros». Los que no recibe quedan vacíos, en «Todos» o
 * «Todas»: así cada llamada pide exactamente esos.
 *
 * @param {import('@playwright/test').Locator} formulario el formulario de los filtros
 * @param {{ desde?: string, hasta?: string, estado?: string, categoria?: string, codigo?: string,
 *   usuario?: string }} filtros las fechas (aaaa-mm-dd), el código como se escribe, y el estado, la categoría y el
 *   usuario como se ven en sus listas, como «Analizado»
 */
export async function filtrarElHistorial(formulario, filtros) {
  const { desde = '', hasta = '', estado, categoria, codigo = '', usuario } = filtros;
  /**
   * Elige una opción de una lista por su texto, o la primera, «Todos» o «Todas», si no hay texto.
   *
   * @param {string} etiqueta la etiqueta de la lista
   * @param {string|undefined} texto el texto de la opción
   * @returns {Promise<string[]>} los valores elegidos
   */
  const elegir = (etiqueta, texto) => formulario.getByLabel(etiqueta, { exact: true })
    .selectOption(texto ? { label: texto } : { index: 0 });
  await formulario.getByLabel('Resultado desde', { exact: true }).fill(desde);
  await formulario.getByLabel('Hasta', { exact: true }).fill(hasta);
  await elegir('Estado', estado);
  await elegir('Categoría', categoria);
  await formulario.getByLabel('Código del lote', { exact: true }).fill(codigo);
  await elegir('Usuario', usuario);
  await formulario.getByRole('button', { name: 'Aplicar filtros' }).click();
}

/**
 * Espera que el historial muestre solo esos lotes, de los que conoce la prueba, y que anuncie cuántos encontró.
 *
 * @param {import('@playwright/test').Page} page la página, en el historial
 * @param {string[]} todos los códigos de los lotes de la empresa
 * @param {string[]} esperados los que deben verse
 */
export async function seVenSoloLosLotes(page, todos, esperados) {
  const total = esperados.length === 1 ? '1 lote encontrado' : `${esperados.length} lotes encontrados`;
  await expect(page.getByRole('status').filter({ hasText: /encontrados?$/ })).toHaveText(total);
  for (const codigo of todos) {
    await expect(page.getByText(codigo, { exact: true }), `el lote ${codigo}`)
      .toHaveCount(esperados.includes(codigo) ? 1 : 0);
  }
}

/**
 * Crea un usuario final en una empresa propia, con su administrador: para las pruebas con varias personas que
 * registran lotes en la misma empresa (RF-14 C2).
 *
 * @param {import('@playwright/test').APIRequestContext} api el cliente de la API de Playwright
 * @param {{ administrador: { correo: string, contrasena: string } }} empresa la empresa, con su administrador
 * @param {string} nombre su nombre, como «Pedro Salas»
 * @returns {Promise<{ id: string, nombre: string, correo: string, contrasena: string }>} el usuario
 */
export async function crearUsuarioFinalEn(api, empresa, nombre) {
  const usuario = nombre.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().replace(/\s+/g, '.');
  const persona = { nombre, correo: `${usuario}.${randomUUID().slice(0, 8)}@cacaoandino.test`, contrasena: CONTRASENA,
    rol: 'USUARIO_FINAL' };
  const respuesta = await api.post('/api/v1/usuarios', {
    data: persona, headers: { Authorization: `Bearer ${await tokenDe(api, empresa.administrador)}` },
  });
  expect(respuesta.status(), `crear a ${nombre}`).toBe(201);
  return { ...persona, id: (await respuesta.json()).id };
}

/**
 * Desactiva un usuario de una empresa propia, con su administrador (RF-03).
 *
 * @param {import('@playwright/test').APIRequestContext} api el cliente de la API de Playwright
 * @param {{ administrador: { correo: string, contrasena: string } }} empresa la empresa, con su administrador
 * @param {string} id el usuario
 */
export async function desactivarUsuarioEn(api, empresa, id) {
  const respuesta = await api.post(`/api/v1/usuarios/${id}/desactivar`, {
    headers: { Authorization: `Bearer ${await tokenDe(api, empresa.administrador)}` },
  });
  expect(respuesta.status()).toBe(200);
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
