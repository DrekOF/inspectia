/*
 * Nombre: usuarioApi.js
 * Descripción: Cliente de la API de usuario en /api/v1/usuarios (RF-03).
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.9)
 * Versión: 1.9
 * Entidades: usuario 1.8
 */

/** La ruta de usuario en la API, después de /api/v1. */
const RUTA = '/usuarios';

/**
 * @typedef {object} Pagina una página del listado, como la responde la API
 * @property {object[]} contenido los registros de la página
 * @property {number} pagina el número de la página, desde 0
 * @property {number} tamano las filas por página
 * @property {number} totalElementos cuántos registros hay en total
 * @property {number} totalPaginas cuántas páginas hay
 */

/**
 * Lista usuarios por páginas (RF-03).
 *
 * @param {import('../../api/cliente.js').Pedir} pedir la función para pedir a la API
 * @param {object} [filtro] qué página pedir
 * @param {number} [filtro.pagina] el número de la página, desde 0
 * @param {number} [filtro.tamano] las filas por página, de 1 a 100
 * @param {boolean} [filtro.activo] solo los activos (true) o solo los desactivados (false); sin él, todos (D-33)
 * @returns {Promise<Pagina>} la página
 */
export function listar(pedir, { pagina = 0, tamano = 20, activo } = {}) {
  const parametros = new URLSearchParams({ pagina: String(pagina), tamano: String(tamano) });
  if (activo !== undefined) {
    parametros.set('activo', String(activo));
  }
  return pedir('GET', `${RUTA}?${parametros}`);
}

/**
 * Un registro de usuario por su identificador (RF-03).
 *
 * @param {import('../../api/cliente.js').Pedir} pedir la función para pedir a la API
 * @param {string} id el identificador
 * @returns {Promise<object>} el registro
 */
export function ver(pedir, id) {
  return pedir('GET', `${RUTA}/${encodeURIComponent(id)}`);
}

/**
 * Registra usuario (RF-03).
 *
 * @param {import('../../api/cliente.js').Pedir} pedir la función para pedir a la API
 * @param {object} datos los datos del formulario, con los nombres de la API
 * @returns {Promise<object>} el registro nuevo
 */
export function crear(pedir, datos) {
  return pedir('POST', RUTA, datos);
}

/**
 * Edita usuario (RF-03).
 *
 * @param {import('../../api/cliente.js').Pedir} pedir la función para pedir a la API
 * @param {string} id el identificador
 * @param {object} datos los datos del formulario, con los nombres de la API
 * @returns {Promise<object>} el registro como quedó
 */
export function editar(pedir, id, datos) {
  return pedir('PUT', `${RUTA}/${encodeURIComponent(id)}`, datos);
}

/**
 * Desactiva usuario: no se borra nada (RF-03).
 *
 * @param {import('../../api/cliente.js').Pedir} pedir la función para pedir a la API
 * @param {string} id el identificador
 * @returns {Promise<object>} el registro como quedó
 */
export function desactivar(pedir, id) {
  return pedir('POST', `${RUTA}/${encodeURIComponent(id)}/desactivar`);
}

/**
 * Reactiva usuario (RF-03).
 *
 * @param {import('../../api/cliente.js').Pedir} pedir la función para pedir a la API
 * @param {string} id el identificador
 * @returns {Promise<object>} el registro como quedó
 */
export function reactivar(pedir, id) {
  return pedir('POST', `${RUTA}/${encodeURIComponent(id)}/reactivar`);
}
