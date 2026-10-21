/*
 * Nombre: ayudas.js (selenium)
 * Descripción: Ayudas de las pruebas con Selenium (RP-14): abrir Chrome o Edge en tamaño de PC o de celular,
 *              buscar campos por su etiqueta y botones por su texto, y crear usuarios propios por la API.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { randomUUID } from 'node:crypto';
import { Browser, Builder, By, until } from 'selenium-webdriver';
import chrome from 'selenium-webdriver/chrome.js';
import edge from 'selenium-webdriver/edge.js';
import { datos } from '../e2e/entorno/datos.js';

/** Dirección de la app compilada que sirve el entorno aislado (e2e/entorno). */
export const APP = process.env.INSPECTIA_E2E_URL ?? 'http://localhost:4173';

/* Los datos del entorno aislado: los mismos de las pruebas de Playwright (e2e/entorno/datos.js). */
export { datos };

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
 * El token del administrador de la empresa de prueba.
 *
 * @returns {Promise<string>} el token
 */
async function tokenDelAdministrador() {
  const respuesta = await pedir('POST', '/sesion', { correo: datos.adminEmpresa.correo, contrasena: datos.adminEmpresa.contrasena });
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
  const respuesta = await pedir('POST', '/usuarios', persona, await tokenDelAdministrador());
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
  await pedir('POST', `/usuarios/${id}/desactivar`, null, await tokenDelAdministrador());
}

export { Browser, By };
