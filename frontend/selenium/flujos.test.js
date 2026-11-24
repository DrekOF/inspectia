/*
 * Nombre: flujos.test.js
 * Descripción: Pruebas con Selenium de los flujos principales (RP-14): entrar con el menú del rol, los datos
 *              errados, cambiar la contraseña, registrar una empresa, crear un usuario que entra, guardar unos
 *              rangos propios y restaurar los de D-10, registrar un lote que pasa a las fotos y queda pendiente
 *              en el historial, subir dos fotos y volver a ellas con «Continuar», analizar la muestra y ver su
 *              resultado, y analizar la segunda muestra desde el aviso de la sugerencia y ver la suma de las dos, en
 *              Chrome y en Edge, en tamaño de PC y de celular (RNF-05). Con INSPECTIA_E2E_SIN_EDGE=1 salta Edge, como
 *              en la CI. Cada prueba empieza sin sesión. Si una falla, guarda su captura y la consola del navegador en
 *              reports/selenium.
 * Fecha de creación: 20/10/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.8
 */
import { randomUUID } from 'node:crypto';
import { rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { afterAll, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { Key } from 'selenium-webdriver';
import {
  abrirNavegador, analizarPorLaApi, APP, archivoDeLaFoto, boton, Browser, By, campo, campoDelGrupo, CELULAR, CONTRASENA, crearEmpresa,
  crearUsuario, datos, descartarConsola, desactivarUsuario, empresaNueva, entrar, escribirEn, esperar, esperarQueEntre,
  esperarTexto, guardarFalla, registrarLote, sinSesion,
} from './ayudas.js';
import { diaEnColombia } from '../e2e/fechas.js';
import { png } from '../e2e/imagenes.js';

/**
 * Escribe una fecha (aaaa-mm-dd) en un campo de fecha. Con sendKeys, el formato depende del idioma del navegador;
 * así se pone el valor con el setter del navegador y se avisa con un evento input, que React sí recibe.
 *
 * @param {import('selenium-webdriver').WebDriver} driver el navegador
 * @param {import('selenium-webdriver').WebElement} elemento el campo de fecha
 * @param {string} fecha la fecha
 */
async function escribirFecha(driver, elemento, fecha) {
  await driver.executeScript(`
    const [campoDeFecha, valor] = arguments;
    Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set.call(campoDeFecha, valor);
    campoDeFecha.dispatchEvent(new Event('input', { bubbles: true }));`, elemento, fecha);
}

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
    await sinSesion(driver);
    await descartarConsola(driver);
    onTestFailed(async ({ task }) => {
      const guardados = await guardarFalla(driver, task);
      console.warn(`Falló «${task.name}». Captura y consola: ${guardados.join(' y ')}`);
    });
  });

  /**
   * El menú principal; en el celular, después de pulsar «Abrir el menú» y de que termine de entrar.
   *
   * @returns {Promise<import('selenium-webdriver').WebElement>} el menú
   */
  async function menu() {
    if (celular) {
      await (await boton(driver, 'Abrir el menú')).click();
    }
    const nav = await esperar(driver, By.css('nav[aria-label="Menú principal"]'));
    await esperarQueEntre(driver, nav);
    return nav;
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

  it('RF-04 · C1 · el usuario final registra un lote en la pantalla, pasa a las fotos y lo ve pendiente en el historial (Selenium)', async () => {
    const luis = await crearUsuario();
    const codigo = `S-${randomUUID().slice(0, 8)}`;
    try {
      await entrar(driver, luis);
      await esperarTexto(driver, 'Hola, Luis');
      await driver.get(`${APP}/lotes/nuevo`);
      await (await campo(driver, 'Código del lote')).sendKeys(codigo);
      await escribirFecha(driver, await campo(driver, 'Fecha de recepción'), diaEnColombia(-7));
      const origen = await campo(driver, 'Finca o proveedor');
      await origen.sendKeys('Finca La Esperanza');
      // Cierra las sugerencias de la finca, para que no tapen el botón.
      await origen.sendKeys(Key.ESCAPE);
      await (await boton(driver, 'Continuar a las fotos')).click();
      await esperarTexto(driver, `Lote ${codigo} · Finca La Esperanza.`);
      await esperarTexto(driver, 'Guía de captura');

      await driver.get(`${APP}/historial`);
      // El estado se busca en la fila del lote (o en su tarjeta, en el celular): la empresa de prueba tiene más
      // lotes pendientes, de las otras combinaciones y de corridas anteriores.
      await esperar(driver, By.xpath(`//*[self::tr or self::li][.//*[normalize-space()='${codigo}']]`
        + "//*[normalize-space()='Pendiente de análisis']"));
    } finally {
      await driver.executeScript('localStorage.clear()');
      await desactivarUsuario(luis.id);
    }
  });

  it('RF-14 · C5 · el usuario final sube dos fotos, las ve cargadas, y desde «Continuar» del historial vuelve a ellas (Selenium)', async () => {
    const luis = await crearUsuario();
    const lote = await registrarLote(luis, `S-${randomUUID().slice(0, 8)}`, diaEnColombia(-7));
    // Selenium elige archivos por su ruta: las fotos se escriben en la carpeta temporal y se borran al terminar.
    const fotos = [1, 2].map((numero) => join(tmpdir(), `inspectia-selenium-${randomUUID().slice(0, 8)}-${numero}.png`));
    fotos.forEach((ruta, indice) => writeFileSync(ruta, png({ color: [107, 63 + 40 * indice, 34] })));
    /**
     * Espera que la foto se vea entera, cargada desde su enlace.
     *
     * @param {number} numero el número de la foto
     */
    const seVe = async (numero) => {
      const imagen = await esperar(driver, By.css(`img[alt="Foto ${numero} de la muestra"]`));
      await driver.wait(() => driver.executeScript('return arguments[0].complete && arguments[0].naturalWidth > 0',
        imagen), 15_000);
    };
    try {
      await entrar(driver, luis);
      await esperarTexto(driver, 'Hola, Luis');
      await driver.get(`${APP}/lotes/${lote.id}/fotos`);
      for (const [indice, ruta] of fotos.entries()) {
        await (await archivoDeLaFoto(driver, indice + 1)).sendKeys(ruta);
        await seVe(indice + 1);
      }

      await driver.get(`${APP}/historial`);
      await (await boton(driver, `Continuar el lote ${lote.codigo}`)).click();

      await seVe(1);
      await seVe(2);
      expect(await driver.getCurrentUrl()).toBe(`${APP}/lotes/${lote.id}/fotos`);
    } finally {
      fotos.forEach((ruta) => rmSync(ruta, { force: true }));
      await driver.executeScript('localStorage.clear()');
      await desactivarUsuario(luis.id);
    }
  });

  it('RF-10 · el usuario final sube dos fotos, pulsa «Analizar la muestra» y ve el resultado: 87 de 100 sanos dan 90,6 % (Selenium)', async () => {
    const luis = await crearUsuario();
    const lote = await registrarLote(luis, `S-${randomUUID().slice(0, 8)}`, diaEnColombia(-7));
    // El clasificador falso marca como no sanos tantos granos como el ancho de la foto módulo 50: 6 y 7 de 100.
    const fotos = [106, 107].map((ancho) => {
      const ruta = join(tmpdir(), `inspectia-selenium-${randomUUID().slice(0, 8)}-${ancho}.png`);
      writeFileSync(ruta, png({ ancho, alto: 100 }));
      return ruta;
    });
    try {
      await entrar(driver, luis);
      await esperarTexto(driver, 'Hola, Luis');
      await driver.get(`${APP}/lotes/${lote.id}/fotos`);
      for (const [indice, ruta] of fotos.entries()) {
        await (await archivoDeLaFoto(driver, indice + 1)).sendKeys(ruta);
        await esperar(driver, By.css(`img[alt="Foto ${indice + 1} de la muestra"]`));
      }

      await (await boton(driver, 'Analizar la muestra')).click();

      // El título de la página, no el de la pestaña del navegador, que dice lo mismo y no se ve.
      await esperar(driver, By.xpath(`//h1[normalize-space()='Resultado del lote ${lote.codigo}']`));
      // La cifra grande lleva un espacio sin corte antes de «%», que normalize-space de XPath no cambia: se busca sin él.
      await esperarTexto(driver, '90,6');
      await esperarTexto(driver, '87 de 100 marcados como sanos');
      await esperarTexto(driver, 'Rango probable: entre 83,1 y 98,1 %');
      expect(await driver.getCurrentUrl()).toBe(`${APP}/lotes/${lote.id}`);
    } finally {
      fotos.forEach((ruta) => rmSync(ruta, { force: true }));
      await driver.executeScript('localStorage.clear()');
      await desactivarUsuario(luis.id);
    }
  });

  it('RF-14 · C6 · el usuario final pulsa «Analizar otra muestra» en el resultado que la sugiere, sube dos fotos de la muestra 2, la analiza y ve la suma de las dos: 172 de 200 dan 88,3 % (Selenium)', async () => {
    const luis = await crearUsuario();
    const lote = await registrarLote(luis, `S-${randomUUID().slice(0, 8)}`, diaEnColombia(-7));
    // La muestra 1, por la API: 87 de 100 sanos, 90,6 %, a 0,6 puntos de 90. La 2, en la pantalla: 8 + 7 no sanos.
    await analizarPorLaApi(luis, lote.id, [106, 107]);
    const fotos = [108, 107].map((ancho) => {
      const ruta = join(tmpdir(), `inspectia-selenium-${randomUUID().slice(0, 8)}-${ancho}.png`);
      writeFileSync(ruta, png({ ancho, alto: 100 }));
      return ruta;
    });
    try {
      await entrar(driver, luis);
      await esperarTexto(driver, 'Hola, Luis');
      await driver.get(`${APP}/lotes/${lote.id}`);
      await esperarTexto(driver, 'El resultado quedó a 0,6 puntos del límite de Estándar (90 %).');
      await (await boton(driver, 'Analizar otra muestra')).click();

      await esperar(driver, By.xpath("//h1[normalize-space()='Fotos de la muestra 2']"));
      for (const [indice, ruta] of fotos.entries()) {
        await (await archivoDeLaFoto(driver, indice + 1)).sendKeys(ruta);
        await esperar(driver, By.css(`img[alt="Foto ${indice + 1} de la muestra"]`));
      }
      await (await boton(driver, 'Analizar la muestra')).click();

      await esperar(driver, By.xpath(`//h1[normalize-space()='Resultado del lote ${lote.codigo}']`));
      // La cifra grande lleva un espacio sin corte antes de «%», que normalize-space de XPath no cambia: se busca sin él.
      await esperarTexto(driver, '88,3');
      await esperarTexto(driver, '172 de 200 marcados como sanos');
      await esperar(driver, By.xpath("//h3[normalize-space()='Muestra 1']"));
      await esperar(driver, By.xpath("//h3[normalize-space()='Muestra 2']"));
      await esperarTexto(driver, '87 de 100 marcados como sanos');
      await esperarTexto(driver, '85 de 100 marcados como sanos');
      expect(await driver.findElements(By.xpath("//a[normalize-space()='Analizar otra muestra']"))).toHaveLength(0);
      expect(await driver.getCurrentUrl()).toBe(`${APP}/lotes/${lote.id}`);
    } finally {
      fotos.forEach((ruta) => rmSync(ruta, { force: true }));
      await driver.executeScript('localStorage.clear()');
      await desactivarUsuario(luis.id);
    }
  });
});
