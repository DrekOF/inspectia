/*
 * Nombre: rangoRecomendacionApi.js
 * Descripción: Cliente de la API de rango_recomendacion en /api/v1/rangos-recomendacion, que se edita
 *              en bloque (RF-13).
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.11)
 * Versión: 1.11
 * Entidades: rango_recomendacion 1.3
 */

/** La ruta de rango_recomendacion en la API, después de /api/v1. */
const RUTA = '/rangos-recomendacion';

/**
 * Las filas de rangos de recomendación de la empresa de la sesión, una por cada categoría, en el orden de su lista
 * (RF-13).
 *
 * @param {import('../../api/cliente.js').Pedir} pedir la función para pedir a la API
 * @returns {Promise<object[]>} las filas
 */
export function listar(pedir) {
  return pedir('GET', RUTA);
}

/**
 * Edita todas las filas juntas (RF-13): si una no cumple, la API no guarda ninguna.
 *
 * @param {import('../../api/cliente.js').Pedir} pedir la función para pedir a la API
 * @param {object[]} filas las filas, con los nombres de la API
 * @returns {Promise<object[]>} las filas como quedaron
 */
export function editar(pedir, filas) {
  return pedir('PUT', RUTA, { filas });
}

/**
 * Vuelve a los valores por defecto (RF-13): los mismos que recibe cada empresa nueva.
 *
 * @param {import('../../api/cliente.js').Pedir} pedir la función para pedir a la API
 * @returns {Promise<object[]>} las filas como quedaron
 */
export function restaurarPorDefecto(pedir) {
  return pedir('POST', `${RUTA}/restaurar-por-defecto`);
}
