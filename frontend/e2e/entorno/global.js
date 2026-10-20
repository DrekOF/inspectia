/*
 * Nombre: global.js (e2e/entorno)
 * Descripción: Preparación global de las pruebas de punta a punta, la misma para Playwright y para Selenium
 *              (RP-14): enciende el entorno aislado antes de las pruebas y lo apaga al terminar. Si
 *              INSPECTIA_E2E_URL ya trae la dirección de un entorno encendido con «npm run e2e:entorno», lo
 *              usa tal cual.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { encenderEntorno } from './entorno.js';

/**
 * Enciende el entorno aislado y pasa su dirección a las pruebas en INSPECTIA_E2E_URL.
 *
 * @returns {Promise<(() => Promise<void>)|undefined>} la función que lo apaga al terminar, o nada si el entorno
 *   ya estaba encendido
 */
export default async function prepararPruebas() {
  if (process.env.INSPECTIA_E2E_URL) {
    return undefined;
  }
  const { app, apagar } = await encenderEntorno();
  process.env.INSPECTIA_E2E_URL = app;
  return apagar;
}
