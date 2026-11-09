/*
 * Nombre: flujos.test.js
 * Descripción: Pruebas con Selenium de los flujos principales (RP-14): entrar con el menú del rol, los datos
 *              errados, cambiar la contraseña, registrar una empresa, crear un usuario que entra y guardar unos
 *              rangos propios y restaurar los de D-10, en Chrome y en Edge, en tamaño de PC y de celular
 *              (RNF-05). Con INSPECTIA_E2E_SIN_EDGE=1 salta Edge, como en la CI. Si una falla, guarda su captura y
 *              la consola del navegador en reports/selenium.
 * Fecha de creación: 20/10/2026
 * Actualización: 09/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.4
 */
import { randomUUID } from 'node:crypto';
import { afterAll, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import {
  abrirNavegador, APP, boton, Browser, By, campo, campoDelGrupo, CELULAR, CONTRASENA, crearEmpresa, crearUsuario,
  datos, descartarConsola, desactivarUsuario, empresaNueva, entrar, escribirEn, esperar, esperarTexto, guardarFalla,
} from './ayudas.js';

/**
 * Los navegadores, cada uno en tamaño de PC y de celular. Con INSPECTIA_E2E_SIN_EDGE=1 se salta Edge, como en la CI:
 * en Ubuntu 24.04, AppArmor no deja que Edge cree el sandbox y se cierra al abrir. Edge se sigue probando en Windows
 * (ver docs/bitacora.md).
 */
const COMBINACIONES = [
  ['Chrome', Browser.CHROME, 'PC', null],
  ['Chrome', Browser.CHROME, 'celular', CELULAR],
  ['Edge', Browser.EDGE, 'PC', null],
  ['Edge', Browser.EDGE, 'celular', CELULAR],
].filter(([, navegador]) => !(navegador === Browser.EDGE && process.env.INSPECTIA_E2E_SIN_EDGE === '1'));

describe.each(COMBINACIONES)('%s en %s', (_, navegador, __, celular) => {
  let driver;

  beforeAll(async () => {
    driver = await abrirNavegador(navegador, celular);
  });

  afterAll(async () => {
    await driver?.quit();
  });

  // Si la prueba falla, su captura y la consola del navegador quedan en reports/selenium, para explicar las
  // fallas intermitentes.
  beforeEach(async ({ onTestFailed }) => {
    await descartarConsola(driver);
    onTestFailed(async ({ task }) => {
      const guardados = await guardarFalla(driver, task);
      console.warn(`Falló «${task.name}». Captura y consola: ${guardados.join(' y ')}`);
    });
  });

  /**
   * El menú principal; en el celular, después de pulsar «Abrir el menú».
   *
   * @returns {Promise<import('selenium-webdriver').WebElement>} el menú
   */
  async function menu() {
    if (celular) {
      await (await boton(driver, 'Abrir el menú')).click();
    }
    return esperar(driver, By.css('nav[aria-label="Menú principal"]'));
  }

  it('RF-01 · C1 · el administrador de empresa entra y ve el menú de su rol (Selenium)', async () => {
    await entrar(driver, datos.adminEmpresa);
    await esperarTexto(driver, 'Hola, Ana');

    const enlaces = await (await menu()).findElements(By.css('a'));
    const textos = await Promise.all(enlaces.map((enlace) => enlace.getText()));
    expect(textos).toEqual(['Inicio', 'Historial', 'Usuarios', 'Rangos de recomendación', 'Estadísticas', 'Modelo de IA',
      'Cambiar contraseña', 'Acerca de']);
    await driver.executeScript('localStorage.clear()');
  });

  it('RF-01 · C2 · con datos errados se ve «Correo o contraseña incorrectos» (Selenium)', async () => {
    await entrar(driver, { correo: 'nadie@cacaosdelhuila.test', contrasena: 'no-es-la-clave' });

    const aviso = await esperar(driver, By.css('[role="alert"]'));
    expect(await aviso.getText()).toContain('Correo o contraseña incorrectos');
  });

  it('RF-18 · C1 · se cambia la contraseña y la nueva inicia sesión (Selenium)', async () => {
    const luis = await crearUsuario();
    const nueva = 'Granos del Huila 2026';
    try {
      await entrar(driver, luis);
      await esperarTexto(driver, 'Hola, Luis');
      await driver.get(`${APP}/cambiar-contrasena`);
      await (await campo(driver, 'Contraseña actual')).sendKeys(luis.contrasena);
      await (await campo(driver, 'Contraseña nueva')).sendKeys(nueva);
      await (await campo(driver, 'Repite la contraseña nueva')).sendKeys(nueva);
      await (await boton(driver, 'Guardar')).click();
      await esperarTexto(driver, 'Tu contraseña cambió.');

      await driver.executeScript('localStorage.clear()');
      await entrar(driver, { ...luis, contrasena: nueva });
      await esperarTexto(driver, 'Hola, Luis');
    } finally {
      await driver.executeScript('localStorage.clear()');
      await desactivarUsuario(luis.id);
    }
  });

  it('RF-02 · C1 · el administrador de la plataforma registra una empresa y su primer administrador en la pantalla (Selenium)', async () => {
    const empresa = empresaNueva(CONTRASENA);
    try {
      await entrar(driver, datos.plataforma);
      await esperarTexto(driver, 'Hola, Andrés');
      await driver.get(`${APP}/empresas`);
      await (await boton(driver, 'Nueva empresa')).click();
      await (await campoDelGrupo(driver, 'Empresa', 'Nombre')).sendKeys(empresa.nombre);
      await (await campoDelGrupo(driver, 'Empresa', 'NIT')).sendKeys(empresa.nit);
      await (await campoDelGrupo(driver, 'Empresa', 'Ciudad')).sendKeys(empresa.ciudad);
      await (await campoDelGrupo(driver, 'Primer administrador', 'Nombre')).sendKeys(empresa.administrador.nombre);
      await (await campoDelGrupo(driver, 'Primer administrador', 'Correo')).sendKeys(empresa.administrador.correo);
      await (await campoDelGrupo(driver, 'Primer administrador', 'Contraseña inicial')).sendKeys(empresa.administrador.contrasena);
      await (await boton(driver, 'Registrar empresa')).click();

      await esperarTexto(driver, `Se guardó ${empresa.nombre}.`);
    } finally {
      await driver.executeScript('localStorage.clear()');
    }
  });

  it('RF-03 · C1 · el administrador de empresa crea un usuario en la pantalla y ese usuario inicia sesión (Selenium)', async () => {
    // En una empresa propia, para no llenar los 25 activos de la empresa de prueba.
    const empresa = await crearEmpresa();
    const pedro = { nombre: 'Pedro Salas', correo: `pedro.salas.${randomUUID().slice(0, 8)}@cacaoandino.test`,
      contrasena: CONTRASENA };
    try {
      await entrar(driver, empresa.administrador);
      await esperarTexto(driver, 'Hola, Laura');
      await driver.get(`${APP}/usuarios`);
      await (await boton(driver, 'Nuevo usuario')).click();
      await (await campo(driver, 'Nombre')).sendKeys(pedro.nombre);
      await (await campo(driver, 'Correo')).sendKeys(pedro.correo);
      await (await campo(driver, 'Contraseña inicial')).sendKeys(pedro.contrasena);
      await (await boton(driver, 'Crear usuario')).click();
      await esperarTexto(driver, `Se guardaron los datos de ${pedro.nombre}.`);

      await driver.executeScript('localStorage.clear()');
      await entrar(driver, pedro);
      await esperarTexto(driver, 'Hola, Pedro');
    } finally {
      await driver.executeScript('localStorage.clear()');
    }
  });

  it('RF-13 · C4 · el administrador de empresa guarda unos rangos propios y «Restaurar valores por defecto» los devuelve a los de D-10 (Selenium)', async () => {
    // En una empresa propia: guardar cambia los rangos de la empresa.
    const empresa = await crearEmpresa();
    const limite = 'Límite inferior (% de granos sanos)';
    /**
     * El valor de un campo de una categoría, después de cargar la pantalla.
     *
     * @param {string} categoria el nombre de la categoría
     * @param {string} [etiqueta] la etiqueta del campo; por defecto, el límite inferior
     * @returns {Promise<string>} el valor
     */
    const valorDe = async (categoria, etiqueta = limite) => (await campoDelGrupo(driver, categoria, etiqueta)).getAttribute('value');
    try {
      await entrar(driver, empresa.administrador);
      await esperarTexto(driver, 'Hola, Laura');
      await driver.get(`${APP}/rangos-recomendacion`);
      await escribirEn(await campoDelGrupo(driver, 'Premium o Especial', limite), '97,5');
      await escribirEn(await campoDelGrupo(driver, 'Premium o Especial', 'Recomendación'), 'Para el comprador de exportación.');
      await (await boton(driver, 'Guardar cambios')).click();
      await esperarTexto(driver, 'Se guardaron los rangos.');
      await driver.navigate().refresh();
      expect(await valorDe('Premium o Especial')).toBe('97,5');
      expect(await valorDe('Premium o Especial', 'Recomendación')).toBe('Para el comprador de exportación.');

      await (await boton(driver, 'Restaurar valores por defecto')).click();
      await esperarTexto(driver, 'Se pierden los límites y los textos propios de la empresa.');
      await (await boton(driver, 'Restaurar')).click();
      await esperarTexto(driver, 'Se restauraron los valores por defecto.');
      await driver.navigate().refresh();
      const limites = [];
      for (const categoria of ['Premium o Especial', 'Estándar', 'Corriente', 'Fuera de norma']) {
        limites.push(await valorDe(categoria));
      }
      expect(limites).toEqual(['96,0', '90,0', '82,0', '0,0']);
      expect(await valorDe('Premium o Especial', 'Recomendación')).toBe('Apto para mercado especial o de exportación.');
    } finally {
      await driver.executeScript('localStorage.clear()');
    }
  });
});
