/*
 * Nombre: fechas.js (lote)
 * Descripción: Las fechas de los lotes en la hora de Colombia: el día de hoy, que no puede pasar la fecha de
 *              recepción (RF-04 C4), y la fecha del resultado, como la muestra el historial (RF-14).
 * Fecha de creación: 22/11/2026
 * Actualización: 22/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */

/** «Hoy» es el día en Colombia, como en el backend («Fechas» en specs/modelo-datos.md). */
export const ZONA_DE_COLOMBIA = 'America/Bogota';

/**
 * El día de hoy en Colombia, en el formato de un campo de fecha (aaaa-mm-dd), aunque el navegador esté en otra
 * zona horaria.
 *
 * @param {Date} [ahora] el momento; por defecto, ahora
 * @returns {string} la fecha
 */
export function hoyEnColombia(ahora = new Date()) {
  return new Intl.DateTimeFormat('en-CA', { timeZone: ZONA_DE_COLOMBIA }).format(ahora);
}

/**
 * Una fecha y hora del backend como día del calendario de Colombia, en el formato del idioma, como 18/10/2026.
 *
 * @param {string} fechaHora la fecha y hora (ISO 8601)
 * @param {string} region la región del idioma de la app, como es-CO
 * @returns {string} el día
 */
export function diaEnColombia(fechaHora, region) {
  return new Intl.DateTimeFormat(region, {
    day: '2-digit', month: '2-digit', year: 'numeric', timeZone: ZONA_DE_COLOMBIA,
  }).format(new Date(fechaHora));
}
