/*
 * Nombre: decimales.js
 * Descripción: Cómo se muestra un decimal en un formulario generado (RP-11, RF-13): con la escala de su columna y el
 *              separador del idioma de la app, coma en español y punto en inglés. Sin separar los miles, para que el
 *              texto se pueda editar y volver a leer como número.
 * Fecha de creación: 28/10/2026
 * Actualización: 28/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */

/**
 * El texto de un decimal para un campo: con los decimales de su escala y el separador del idioma, sin separar los
 * miles, como 96,0 en español y 96.0 en inglés.
 *
 * @param {number|null|undefined} valor el número, como lo responde la API
 * @param {number} escala los decimales de su columna
 * @param {string} idioma el idioma o su región, como es o es-CO
 * @returns {string} el texto, o vacío si el número no llegó
 */
export function textoDelDecimal(valor, escala, idioma) {
  if (valor === null || valor === undefined) {
    return '';
  }
  return new Intl.NumberFormat(idioma, {
    minimumFractionDigits: escala,
    maximumFractionDigits: escala,
    useGrouping: false,
  }).format(valor);
}
