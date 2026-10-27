/*
 * Nombre: ayudas.js (selenium)
 * Descripción: Ayudas de las pruebas con Selenium (RP-14): abrir Chrome o Edge en tamaño de PC o de celular,
 *              buscar campos por su etiqueta, también dentro de un grupo, y botones por su texto, crear
 *              usuarios y empresas propios por la API y, si una prueba falla, guardar una captura de la
 *              pantalla y la consola del navegador en reports/selenium, que no se sube al repositorio.
 * Fecha de creación: 20/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
import { randomUUID } from 'node:crypto';
import { mkdirSync, writeFileSync } from 'node:fs';
import { EOL } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { Browser, Builder, By, logging, until } from 'selenium-webdriver';
import chrome from 'selenium-webdriver/chrome.js';
import edge from 'selenium-webdriver/edge.js';
import { datos, empresaNueva } from '../e2e/entorno/datos.js';

/** Dirección de la app compilada que sirve el entorno aislado (e2e/entorno). */
export const APP = process.env.INSPECTIA_E2E_URL ?? 'http://localhost:4173';

/* Los datos del entorno aislado y los de una empresa nueva: los mismos de las pruebas de Playwright
 * (e2e/entorno/datos.js). */
export { datos, empresaNueva };

/** Contraseña de los usuarios que crean las pruebas. */
export const CONTRASENA = 'Clave-Segura-2026';

/** Tiempo máximo para esperar un elemento, en milisegundos. */
const ESPERA = 15_000;

/** Tamaño de celular para la emulación de Chrome y Edge: el del prototipo (390 px). */
export const CELULAR = { width: 390, height: 844, pixelRatio: 3, touch: true };

/**
 * Abre un navegador real con Selenium Manager, que descarga su controlador.
 *
 * @param {string} navegador Browser.CHROME o Browser.EDGE
 * @param {object|null} celular las medidas del celular, o null para el tamaño de PC (1280 x 800)
 * @returns {Promise<import('selenium-webdriver').WebDriver>} el navegador
 */
export async function abrirNavegador(navegador, celular) {
  const opciones = navegador === Browser.EDGE ? new edge.Options() : new chrome.Options();
  opciones.addArguments('--lang=es-CO');
  // La consola del navegador, para guardarla si una prueba falla. Edge la pide con su propio prefijo.
  const consola = new logging.Preferences();
  consola.setLevel(logging.Type.BROWSER, logging.Level.ALL);
  opciones.setLoggingPrefs(consola);
  if (navegador === Browser.EDGE) {
    opciones.set('ms:loggingPrefs', consola.toJSON());
  }
  if (!process.env.INSPECTIA_E2E_VER) {
    opciones.addArguments('--headless=new');
  }
  if (celular) {
    opciones.setMobileEmulation({ deviceMetrics: celular });
  } else {
    opciones.addArguments('--window-size=1280,800');
  }
  const constructor = new Builder().forBrowser(navegador);
  return (navegador === Browser.EDGE ? constructor.setEdgeOptions(opciones) : constructor.setChromeOptions(opciones)).build();
}

/**
 * Un elemento que se espera hasta que aparezca y se vea.
 *
 * @param {import('selenium-webdriver').WebDriver} driver el navegador
 * @param {import('selenium-webdriver').By} selector cómo encontrarlo
 * @returns {Promise<import('selenium-webdriver').WebElement>} el elemento
 */
export async function esperar(driver, selector) {
  const elemento = await driver.wait(until.elementLocated(selector), ESPERA);
  await driver.wait(until.elementIsVisible(elemento), ESPERA);
  return elemento;
}

/**
 * Un campo del formulario por el texto de su etiqueta, como «Correo».
 *
 * @param {import('selenium-webdriver').WebDriver} driver el navegador
 * @param {string} etiqueta el texto de la etiqueta
 * @returns {Promise<import('selenium-webdriver').WebElement>} el campo
 */
export async function campo(driver, etiqueta) {
  const label = await esperar(driver, By.xpath(`//label[normalize-space()='${etiqueta}']`));
  return esperar(driver, By.id(await label.getAttribute('for')));
}

/**
 * Un campo de un grupo del formulario por el texto de su etiqueta, como «Nombre» en «Primer administrador»,
 * cuando hay otro campo con la misma etiqueta en otro grupo.
 *
 * @param {import('selenium-webdriver').WebDriver} driver el navegador
 * @param {string} grupo el título del grupo (la leyenda de su fieldset)
 * @param {string} etiqueta el texto de la etiqueta
 * @returns {Promise<import('selenium-webdriver').WebElement>} el campo
 */
export async function campoDelGrupo(driver, grupo, etiqueta) {
  const label = await esperar(driver,
    By.xpath(`//fieldset[legend[normalize-space()='${grupo}']]//label[normalize-space()='${etiqueta}']`));
  return esperar(driver, By.id(await label.getAttribute('for')));
}

/**
 * Un botón o un enlace por su texto o su nombre accesible (aria-label).
 *
 * @param {import('selenium-webdriver').WebDriver} driver el navegador
 * @param {string} texto el texto o el aria-label
 * @returns {Promise<import('selenium-webdriver').WebElement>} el botón
 */
export function boton(driver, texto) {
  return esperar(driver, By.xpath(`//*[(self::button or self::a or @role='menuitem')
    and (normalize-space()='${texto}' or @aria-label='${texto}')]`));
}

/**
 * Espera que un texto aparezca en la página.
 *
 * @param {import('selenium-webdriver').WebDriver} driver el navegador
 * @param {string} texto el texto, o parte de él
 * @returns {Promise<import('selenium-webdriver').WebElement>} el elemento que lo tiene
 */
export function esperarTexto(driver, texto) {
  return esperar(driver, By.xpath(`//*[contains(normalize-space(), '${texto}') and not(*[contains(normalize-space(), '${texto}')])]`));
}

/**
 * Abre Iniciar sesión, escribe el correo y la contraseña y pulsa el botón.
 *
 * @param {import('selenium-webdriver').WebDriver} driver el navegador
 * @param {{ correo: string, contrasena: string }} persona quién entra
 */
export async function entrar(driver, persona) {
  await driver.get(`${APP}/iniciar-sesion`);
  await (await campo(driver, 'Correo')).sendKeys(persona.correo);
  await (await campo(driver, 'Contraseña')).sendKeys(persona.contrasena);
  await (await boton(driver, 'Iniciar sesión')).click();
}

/**
 * Pide a la API, por la app compilada, con el token de una persona.
 *
 * @param {string} metodo el método HTTP
 * @param {string} ruta la ruta, como /usuarios
 * @param {object} [datosDeLaPeticion] el cuerpo
 * @param {string} [token] el token
 * @returns {Promise<Response>} la respuesta
 */
function pedir(metodo, ruta, datosDeLaPeticion, token) {
  return fetch(`${APP}/api/v1${ruta}`, {
    method: metodo,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: datosDeLaPeticion ? JSON.stringify(datosDeLaPeticion) : undefined,
  });
}

/**
 * El token de una persona del entorno, como el administrador de la empresa de prueba.
 *
 * @param {{ correo: string, contrasena: string }} persona quién inicia sesión
 * @returns {Promise<string>} el token
 */
async function tokenDe(persona) {
  const respuesta = await pedir('POST', '/sesion', { correo: persona.correo, contrasena: persona.contrasena });
  return (await respuesta.json()).token;
}

/**
 * Crea un usuario final propio en la empresa de prueba, para las pruebas que cambian datos.
 *
 * @returns {Promise<{ id: string, nombre: string, correo: string, contrasena: string }>} el usuario
 */
export async function crearUsuario() {
  const persona = { nombre: 'Luis Gómez', correo: `luis.${randomUUID().slice(0, 8)}@cacaosdelhuila.test`,
    contrasena: CONTRASENA, rol: 'USUARIO_FINAL' };
  const respuesta = await pedir('POST', '/usuarios', persona, await tokenDe(datos.adminEmpresa));
  if (respuesta.status !== 201) {
    throw new Error(`No se pudo crear el usuario de prueba: ${respuesta.status}`);
  }
  return { ...persona, id: (await respuesta.json()).id };
}

/**
 * Desactiva un usuario de la empresa de prueba al terminar la prueba.
 *
 * @param {string} id el usuario
 */
export async function desactivarUsuario(id) {
  await pedir('POST', `/usuarios/${id}/desactivar`, null, await tokenDe(datos.adminEmpresa));
}

/**
 * Registra una empresa nueva por la API, con el administrador de la plataforma: para las pruebas que crean
 * usuarios en la pantalla sin llenar los 25 activos de la empresa de prueba (RF-03).
 *
 * @returns {Promise<object>} la empresa, con su id y su primer administrador
 */
export async function crearEmpresa() {
  const empresa = empresaNueva(CONTRASENA);
  const respuesta = await pedir('POST', '/empresas', empresa, await tokenDe(datos.plataforma));
  if (respuesta.status !== 201) {
    throw new Error(`No se pudo registrar la empresa de prueba: ${respuesta.status}`);
  }
  return { ...empresa, id: (await respuesta.json()).id };
}

/**
 * Dónde quedan las capturas y las consolas de las pruebas que fallan. Está en .gitignore, y no es test-results
 * porque Playwright vacía esa carpeta al empezar cada corrida.
 */
export const CARPETA_DE_FALLAS = fileURLToPath(new URL('../reports/selenium/', import.meta.url));

/**
 * Descarta lo que ya tiene la consola del navegador, para que lo que se guarde de una prueba que falla sea solo
 * de esa prueba.
 *
 * @param {import('selenium-webdriver').WebDriver|undefined} driver el navegador, si alcanzó a abrirse
 */
export async function descartarConsola(driver) {
  try {
    await driver?.manage().logs().get(logging.Type.BROWSER);
  } catch {
    // Si el navegador no entrega su consola, guardarFalla lo anota.
  }
}

/**
 * El nombre de los archivos de una prueba que falló: la hora, el navegador y la prueba, sin los caracteres que
 * Windows no acepta en un nombre de archivo.
 *
 * @param {{ name: string, suite?: { name: string } }} prueba la prueba de Vitest
 * @returns {string} el nombre, sin extensión
 */
function nombreDeLaFalla(prueba) {
  const hora = new Date().toISOString().replace(/[:.]/g, '-');
  const texto = `${prueba.suite?.name ?? ''} ${prueba.name}`.replace(/[^a-zA-Z0-9áéíóúüñÁÉÍÓÚÜÑ ·_-]/g, '-');
  return `${hora} ${texto}`.slice(0, 110).trim();
}

/**
 * Guarda una captura de la pantalla (.png) y la consola del navegador (.consola.log) de una prueba que falló, en
 * CARPETA_DE_FALLAS: así una falla intermitente deja con qué explicarla. La consola empieza con la prueba, su
 * error, la dirección en la que estaba el navegador y la hora.
 *
 * @param {import('selenium-webdriver').WebDriver|undefined} driver el navegador, si alcanzó a abrirse
 * @param {{ name: string, suite?: { name: string }, result?: { errors?: Array<{ message: string }> } }} prueba la
 *   prueba de Vitest que falló
 * @returns {Promise<string[]>} los archivos guardados
 */
export async function guardarFalla(driver, prueba) {
  if (!driver) {
    return [];
  }
  mkdirSync(CARPETA_DE_FALLAS, { recursive: true });
  const base = join(CARPETA_DE_FALLAS, nombreDeLaFalla(prueba));
  const guardados = [];
  const lineas = [
    `Prueba: ${prueba.suite?.name ?? ''} › ${prueba.name}`,
    `Error: ${prueba.result?.errors?.[0]?.message ?? 'sin mensaje'}`,
    `Dirección: ${await driver.getCurrentUrl().catch((error) => `no se pudo leer (${error.message})`)}`,
    `Hora: ${new Date().toISOString()}`,
    '',
    'Consola del navegador:',
  ];
  try {
    writeFileSync(`${base}.png`, await driver.takeScreenshot(), 'base64');
    guardados.push(`${base}.png`);
  } catch (error) {
    lineas.splice(4, 0, `Captura: no se pudo tomar (${error.message})`);
  }
  try {
    const entradas = await driver.manage().logs().get(logging.Type.BROWSER);
    lineas.push(...(entradas.length > 0
      ? entradas.map((entrada) => `[${new Date(entrada.timestamp).toISOString()}] ${entrada.level.name} ${entrada.message}`)
      : ['(vacía)']));
  } catch (error) {
    lineas.push(`(no se pudo leer: ${error.message})`);
  }
  writeFileSync(`${base}.consola.log`, lineas.join(EOL) + EOL);
  guardados.push(`${base}.consola.log`);
  return guardados;
}

export { Browser, By };
