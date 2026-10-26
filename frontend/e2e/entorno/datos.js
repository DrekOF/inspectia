/*
 * Nombre: datos.js (e2e/entorno)
 * Descripción: Los datos del entorno aislado (e2e/.entorno/datos.json) para las pruebas de Playwright y de
 *              Selenium: la dirección de la app y las personas de prueba con sus contraseñas de esta corrida.
 *              Si el archivo no existe, la primera prueba que los use falla con un mensaje que dice cómo
 *              encender el entorno; listar las pruebas («playwright test --list») funciona sin él. También
 *              arma los datos de una empresa nueva, distintos en cada prueba, con un NIT válido.
 * Fecha de creación: 20/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { randomInt, randomUUID } from 'node:crypto';
import { existsSync, readFileSync } from 'node:fs';

/** El archivo que escribe e2e/entorno/entorno.js al encenderse y borra al apagarse. */
const ARCHIVO = new URL('../.entorno/datos.json', import.meta.url);

/** Qué hacer si no hay datos: el entorno no está encendido. */
export const SIN_ENTORNO = 'No hay datos del entorno aislado de las pruebas (e2e/.entorno/datos.json): el entorno '
  + 'no está encendido. «npm run e2e:playwright» y «npm run e2e:selenium» lo encienden solos; si INSPECTIA_E2E_URL '
  + 'está definida, se usa el que ya está encendido: enciéndelo con «npm run e2e:entorno» o quita esa variable.';

/**
 * Si el entorno dejó sus datos.
 *
 * @returns {boolean} si existe e2e/.entorno/datos.json
 */
export function hayDatos() {
  return existsSync(ARCHIVO);
}

let leidos = null;

/**
 * Los datos del entorno, leídos la primera vez que una prueba los pide.
 *
 * @returns {object} los datos
 */
function leer() {
  if (!leidos) {
    if (!hayDatos()) {
      throw new Error(SIN_ENTORNO);
    }
    leidos = JSON.parse(readFileSync(ARCHIVO, 'utf8'));
  }
  return leidos;
}

/**
 * Los datos del entorno: la app (app), el administrador de la plataforma (plataforma), la empresa de prueba
 * (empresa) con su administrador (adminEmpresa) y un usuario final (usuarioFinal), y una empresa desactivada con
 * su usuario (empresaDesactivada.usuario). Se leen al usarlos, no al importar este archivo.
 */
export const datos = new Proxy({}, {
  get(_, nombre) {
    // Quien revisa si es una promesa o lo muestra en la consola no necesita los datos.
    if (typeof nombre === 'symbol' || nombre === 'then') {
      return undefined;
    }
    return leer()[nombre];
  },
});

/** Los pesos del dígito de verificación del NIT (DIAN), como en DigitoVerificacionNit del backend. */
const PESOS_DEL_NIT = [3, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47, 53, 59, 67, 71];

/**
 * Un NIT nuevo, de 9 números y con su dígito de verificación, para que cada prueba registre su propia empresa.
 *
 * @returns {string} el NIT, como 912345678-3
 */
function nitNuevo() {
  const numero = String(randomInt(800_000_000, 1_000_000_000));
  const suma = [...numero].reverse().reduce((total, digito, i) => total + Number(digito) * PESOS_DEL_NIT[i], 0);
  const residuo = suma % 11;
  return `${numero}-${residuo <= 1 ? residuo : 11 - residuo}`;
}

/**
 * Los datos de una empresa nueva y de su primer administrador, distintos en cada prueba: así las pruebas que la
 * registran, la desactivan o restablecen la contraseña de su administrador no chocan entre navegadores ni con
 * otra corrida sobre el mismo entorno.
 *
 * @param {string} contrasena la contraseña inicial del administrador
 * @returns {{ nombre: string, nit: string, ciudad: string,
 *   administrador: { nombre: string, correo: string, contrasena: string } }} la empresa
 */
export function empresaNueva(contrasena) {
  const marca = randomUUID().slice(0, 8);
  return {
    nombre: `Cacao Andino ${marca}`,
    nit: nitNuevo(),
    ciudad: 'Bucaramanga',
    administrador: { nombre: 'Laura Gómez', correo: `laura.${marca}@cacaoandino.test`, contrasena },
  };
}
