/*
 * Nombre: flujos.test.js
 * Descripción: Pruebas con Selenium de los flujos principales (RP-14): entrar con el menú del rol, los datos
 *              errados, cambiar la contraseña y registrar una empresa, en Chrome y en Edge, en tamaño de PC y de
 *              celular (RNF-05). Si una falla, guarda su captura y la consola del navegador en reports/selenium.
 * Fecha de creación: 20/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { afterAll, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import {
  abrirNavegador, APP, boton, Browser, By, campo, campoDelGrupo, CELULAR, CONTRASENA, crearUsuario, datos,
  descartarConsola, desactivarUsuario, empresaNueva, entrar, esperar, esperarTexto, guardarFalla,
} from './ayudas.js';

const COMBINACIONES = [
  ['Chrome', Browser.CHROME, 'PC', null],
  ['Chrome', Browser.CHROME, 'celular', CELULAR],
  ['Edge', Browser.EDGE, 'PC', null],
  ['Edge', Browser.EDGE, 'celular', CELULAR],
];

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
});
