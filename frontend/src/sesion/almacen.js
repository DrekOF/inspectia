/*
 * Nombre: almacen.js
 * Descripción: La sesión guardada en el navegador (D-31): el token, cuándo vence y el usuario, en
 *              localStorage, para que sobreviva a recargar la página y se comparta entre pestañas. Una sesión
 *              vencida no se usa (RF-01 C6) y cerrar sesión la borra (D-28).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */

/** Clave del navegador donde queda la sesión. */
export const CLAVE_SESION = 'inspectia.sesion';

/**
 * @typedef {object} Sesion la sesión abierta
 * @property {string} token el token, para Authorization: Bearer
 * @property {string} vence cuándo vence (ISO 8601)
 * @property {import('../api/sesion.js').UsuarioDeSesion} usuario quién entró
 */

/**
 * Guarda la sesión en el navegador.
 *
 * @param {Sesion} sesion la sesión
 */
export function guardarSesion({ token, vence, usuario }) {
  localStorage.setItem(CLAVE_SESION, JSON.stringify({ token, vence, usuario }));
}

/** Borra la sesión del navegador. */
export function borrarSesion() {
  localStorage.removeItem(CLAVE_SESION);
}

/**
 * La sesión guardada, si sigue vigente. Si venció o no se puede leer, la borra.
 *
 * @param {Date} [ahora] el momento de la consulta
 * @returns {Sesion|null} la sesión, o null si no hay una vigente
 */
export function leerSesion(ahora = new Date()) {
  try {
    const sesion = JSON.parse(localStorage.getItem(CLAVE_SESION));
    if (sesion?.token && sesion.usuario && new Date(sesion.vence) > ahora) {
      return sesion;
    }
  } catch {
    // Una sesión que no es JSON se trata como vencida.
  }
  borrarSesion();
  return null;
}
