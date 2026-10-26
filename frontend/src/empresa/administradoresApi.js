/*
 * Nombre: administradoresApi.js
 * Descripción: Cliente de la API de los administradores de una empresa (RF-02 C12), escrita a mano en el backend
 *              (usuario/administradores): listarlos y restablecer la contraseña de uno.
 * Fecha de creación: 26/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */

/**
 * @typedef {object} Administrador un administrador de empresa, como lo responde la API
 * @property {string} id su identificador
 * @property {string} nombre su nombre
 * @property {string} correo su correo
 * @property {boolean} activo si puede iniciar sesión
 */

/**
 * La ruta de los administradores de una empresa, después de /api/v1.
 *
 * @param {string} empresaId la empresa
 * @returns {string} la ruta
 */
const rutaDe = (empresaId) => `/empresas/${encodeURIComponent(empresaId)}/administradores`;

/**
 * Los administradores de una empresa.
 *
 * @param {import('../api/cliente.js').Pedir} pedir la función para pedir a la API
 * @param {string} empresaId la empresa
 * @returns {Promise<Administrador[]>} sus administradores
 */
export function listarAdministradores(pedir, empresaId) {
  return pedir('GET', rutaDe(empresaId));
}

/**
 * Restablece la contraseña de un administrador: se cierran sus sesiones abiertas y se le quita el bloqueo (D-30).
 *
 * @param {import('../api/cliente.js').Pedir} pedir la función para pedir a la API
 * @param {string} empresaId su empresa
 * @param {string} usuarioId el administrador
 * @param {string} contrasena la contraseña nueva
 * @returns {Promise<null>} nada: la API responde 204
 */
export function restablecerContrasena(pedir, empresaId, usuarioId, contrasena) {
  return pedir('PUT', `${rutaDe(empresaId)}/${encodeURIComponent(usuarioId)}/contrasena`, { contrasena });
}
