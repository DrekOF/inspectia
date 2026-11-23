/*
 * Nombre: lotes.js (api)
 * Descripción: Las peticiones de los lotes al backend: registrar un lote, el historial por páginas, el detalle de
 *              un lote y las sugerencias de finca o proveedor (RF-04, RF-14).
 * Fecha de creación: 22/11/2026
 * Actualización: 22/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */

/** Lotes por página del historial (RF-14). */
export const LOTES_POR_PAGINA = 20;

/**
 * @typedef {object} Lote un lote como lo responde el backend
 * @property {string} id su identificador
 * @property {string} codigo su código
 * @property {string} fechaRecepcion la fecha de recepción (aaaa-mm-dd)
 * @property {string} origen la finca o el proveedor
 * @property {string|null} observaciones las observaciones
 * @property {'PENDIENTE'|'ANALIZADO'} estado su estado
 * @property {{ id: string, nombre: string }} usuario quién lo registró
 * @property {number|null} porcentajeSanos el porcentaje de granos sanos, con 1 decimal; null mientras está pendiente
 * @property {string|null} categoria la categoría probable, como ESTANDAR; null mientras está pendiente
 * @property {string|null} fechaResultado la fecha del resultado (ISO 8601); null mientras está pendiente
 * @property {string} fechaCreacion la fecha de registro (ISO 8601)
 */

/**
 * Registra un lote, pendiente de análisis (POST /api/v1/lotes). La empresa y el usuario salen de la sesión.
 *
 * @param {import('./cliente.js').Pedir} pedir la función del cliente de la API
 * @param {{ codigo: string, fechaRecepcion: string, origen: string, observaciones: string|null }} datos los datos
 *   del lote
 * @returns {Promise<Lote>} el lote registrado
 */
export function registrarLote(pedir, datos) {
  return pedir('POST', '/lotes', datos);
}

/**
 * Una página del historial de lotes de la empresa (GET /api/v1/lotes), del último movimiento al más antiguo.
 *
 * @param {import('./cliente.js').Pedir} pedir la función del cliente de la API
 * @param {number} pagina el número de la página, desde 0
 * @returns {Promise<{ contenido: Lote[], pagina: number, tamano: number, totalElementos: number,
 *   totalPaginas: number }>} la página
 */
export function historialDeLotes(pedir, pagina) {
  return pedir('GET', `/lotes?pagina=${pagina}&tamano=${LOTES_POR_PAGINA}`);
}

/**
 * Un lote de la empresa (GET /api/v1/lotes/{id}). Si no existe o es de otra empresa, la API responde 404.
 *
 * @param {import('./cliente.js').Pedir} pedir la función del cliente de la API
 * @param {string} id el lote
 * @returns {Promise<Lote>} el lote
 */
export function verLote(pedir, id) {
  return pedir('GET', `/lotes/${encodeURIComponent(id)}`);
}

/**
 * Las fincas y los proveedores que ya usó la empresa y contienen el texto (GET /api/v1/lotes/origenes), sin
 * distinguir mayúsculas ni tildes, en orden alfabético y como máximo 10.
 *
 * @param {import('./cliente.js').Pedir} pedir la función del cliente de la API
 * @param {string} texto lo que el usuario lleva escrito
 * @returns {Promise<string[]>} las sugerencias
 */
export function origenesDeLotes(pedir, texto) {
  return pedir('GET', `/lotes/origenes?texto=${encodeURIComponent(texto)}`);
}
