/*
 * Nombre: fechas.js (lote)
 * Descripción: Las fechas de los lotes en la hora de Colombia: el día de hoy, que no puede pasar la fecha de
 *              recepción (RF-04 C4); el de hace un año, la fecha de recepción más antigua que se acepta (RF-04,
 *              restricción 4), y la fecha del resultado, como la muestra el historial (RF-14).
 * Fecha de creación: 22/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
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
 * Hace un año: el mismo día del año anterior, la fecha de recepción más antigua que se acepta (RF-04, restricción
 * 4). Si el día es 29 de febrero, es el 28 de febrero del año anterior, como en el backend (LocalDate.minusYears).
 *
 * @param {string} dia el día (aaaa-mm-dd), como hoy en Colombia
 * @returns {string} el día de hace un año (aaaa-mm-dd)
 */
export function haceUnAnio(dia) {
  const [anio, mes, numero] = dia.split('-').map(Number);
  // El día 0 del mes siguiente es el último del mes: así el 29 de febrero pasa al 28 en un año que no es bisiesto.
  const ultimoDelMes = new Date(Date.UTC(anio - 1, mes, 0)).getUTCDate();
  const dosDigitos = (valor) => String(valor).padStart(2, '0');
  return `${String(anio - 1).padStart(4, '0')}-${dosDigitos(mes)}-${dosDigitos(Math.min(numero, ultimoDelMes))}`;
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
