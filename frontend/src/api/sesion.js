/*
 * Nombre: sesion.js (api)
 * Descripción: Las peticiones de la sesión al backend (D-28): iniciar sesión, consultar la sesión actual y
 *              cambiar la propia contraseña (RF-01 y RF-18).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */

/**
 * @typedef {object} UsuarioDeSesion el usuario como lo responde el backend
 * @property {string} id su identificador
 * @property {string} nombre su nombre
 * @property {string} correo su correo
 * @property {'USUARIO_FINAL'|'ADMIN_EMPRESA'|'ADMIN_PLATAFORMA'} rol su rol
 * @property {{ id: string, nombre: string }|null} empresa su empresa; null para el administrador de la plataforma
 */

/**
 * @typedef {object} RespuestaDeSesion lo que responde el backend al iniciar sesión o al cambiar la contraseña
 * @property {string} token el token, para Authorization: Bearer
 * @property {string} tipo el tipo del token: Bearer
 * @property {string} vence cuándo vence (ISO 8601), a las 8 horas de iniciar sesión (RNF-04)
 * @property {UsuarioDeSesion} usuario quién entró
 */

/**
 * Inicia sesión con el correo y la contraseña (POST /api/v1/sesion).
 *
 * @param {import('./cliente.js').Pedir} pedir la función del cliente de la API
 * @param {{ correo: string, contrasena: string }} datos el correo y la contraseña, tal como se escribieron
 * @returns {Promise<RespuestaDeSesion>} el token y el usuario
 */
export function iniciarSesion(pedir, datos) {
  return pedir('POST', '/sesion', datos);
}

/**
 * La sesión actual (GET /api/v1/sesion): el usuario con el rol que tiene ahora y cuándo vence la sesión.
 *
 * @param {import('./cliente.js').Pedir} pedir la función del cliente de la API
 * @returns {Promise<{ vence: string, usuario: UsuarioDeSesion }>} la sesión
 */
export function sesionActual(pedir) {
  return pedir('GET', '/sesion');
}

/**
 * Cambia la propia contraseña (PUT /api/v1/sesion/contrasena). Las otras sesiones dejan de servir y esta
 * sigue con el token nuevo de la respuesta, que vence a la misma hora (RF-18 C5).
 *
 * @param {import('./cliente.js').Pedir} pedir la función del cliente de la API
 * @param {{ contrasenaActual: string, contrasenaNueva: string }} datos la contraseña actual y la nueva
 * @returns {Promise<RespuestaDeSesion>} el token nuevo y el usuario
 */
export function cambiarContrasena(pedir, datos) {
  return pedir('PUT', '/sesion/contrasena', datos);
}
