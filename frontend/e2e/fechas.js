/*
 * Nombre: fechas.js (e2e)
 * Descripción: Días del calendario de Colombia contados desde hoy, para las pruebas de punta a punta y las de
 *              Selenium de los lotes (RF-04): la fecha de recepción de los lotes que no prueban la fecha y los
 *              límites de la fecha de recepción. Corren con el reloj real, así que las fechas no pueden ser fijas.
 *              Se calculan aparte del código de la app, para que las pruebas no copien su fórmula.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */

/** «Hoy» es el día en Colombia, como en la app y en el backend. */
const ZONA = 'America/Bogota';

/**
 * Hoy en Colombia, en el formato de un campo de fecha (aaaa-mm-dd).
 *
 * @returns {string} la fecha
 */
function hoy() {
  return new Intl.DateTimeFormat('en-CA', { timeZone: ZONA }).format(new Date());
}

/**
 * Un día de Colombia contado desde hoy, en el formato de un campo de fecha (aaaa-mm-dd).
 *
 * @param {number} [dias] los días que se suman; negativos para el pasado
 * @returns {string} la fecha
 */
export function diaEnColombia(dias = 0) {
  const fecha = new Date(`${hoy()}T12:00:00Z`);
  fecha.setUTCDate(fecha.getUTCDate() + dias);
  return fecha.toISOString().slice(0, 10);
}

/**
 * Hace un año en Colombia: el mismo día del año anterior o, si hoy es 29 de febrero, el 28 (RF-04).
 *
 * @returns {string} la fecha (aaaa-mm-dd)
 */
export function haceUnAnioEnColombia() {
  const [anio, mes, dia] = hoy().split('-').map(Number);
  const fecha = new Date(Date.UTC(anio - 1, mes - 1, dia, 12));
  // Un 29 de febrero del año anterior no existe: Date pasa al 1 de marzo, y se vuelve al último día de febrero.
  if (fecha.getUTCMonth() !== mes - 1) {
    fecha.setUTCDate(0);
  }
  return fecha.toISOString().slice(0, 10);
}

/**
 * El día anterior a una fecha.
 *
 * @param {string} fecha la fecha (aaaa-mm-dd)
 * @returns {string} el día anterior (aaaa-mm-dd)
 */
export function diaAnterior(fecha) {
  const dia = new Date(`${fecha}T12:00:00Z`);
  dia.setUTCDate(dia.getUTCDate() - 1);
  return dia.toISOString().slice(0, 10);
}
