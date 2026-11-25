/*
 * Nombre: lotes.js (api)
 * Descripción: Las peticiones de los lotes al backend: registrar un lote, el historial por páginas con sus filtros,
 *              quienes registraron lotes de la empresa, para el filtro «Usuario», el detalle de un lote y las
 *              sugerencias de finca o proveedor (RF-04, RF-14).
 * Fecha de creación: 22/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */

/** Lotes por página del historial (RF-14). */
export const LOTES_POR_PAGINA = 20;

/** Los filtros del historial, en el orden en que van en la petición: los mismos nombres de la API (RF-14 C2). */
export const NOMBRES_DE_LOS_FILTROS = ['desde', 'hasta', 'estado', 'categoria', 'codigo', 'usuario'];

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
 * @property {boolean|null} sugerirOtraMuestra si su resultado sugiere analizar otra muestra (D-24, D-25); null
 *   mientras está pendiente
 * @property {string|null} fechaResultado la fecha del resultado (ISO 8601); null mientras está pendiente
 * @property {string} fechaCreacion la fecha de registro (ISO 8601)
 */

/**
 * @typedef {object} FiltrosDelHistorial los filtros del historial (RF-14 C2); un filtro vacío no filtra
 * @property {string} desde el primer día de la fecha del resultado, en Colombia (aaaa-mm-dd)
 * @property {string} hasta el último día de la fecha del resultado, en Colombia (aaaa-mm-dd), incluido
 * @property {string} estado el estado, PENDIENTE o ANALIZADO
 * @property {string} categoria la categoría probable, como ESTANDAR
 * @property {string} codigo un texto que el código contiene, sin distinguir mayúsculas
 * @property {string} usuario el identificador de quien registró el lote
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
 * Una página del historial de lotes de la empresa (GET /api/v1/lotes), del último movimiento al más antiguo, con los
 * filtros que tengan valor: los vacíos no se mandan.
 *
 * @param {import('./cliente.js').Pedir} pedir la función del cliente de la API
 * @param {number} pagina el número de la página, desde 0
 * @param {Partial<FiltrosDelHistorial>} [filtros] los filtros
 * @returns {Promise<{ contenido: Lote[], pagina: number, tamano: number, totalElementos: number,
 *   totalPaginas: number }>} la página
 */
export function historialDeLotes(pedir, pagina, filtros = {}) {
  const parametros = new URLSearchParams({ pagina: String(pagina), tamano: String(LOTES_POR_PAGINA) });
  for (const nombre of NOMBRES_DE_LOS_FILTROS) {
    if (filtros[nombre]) {
      parametros.set(nombre, filtros[nombre]);
    }
  }
  return pedir('GET', `/lotes?${parametros}`);
}

/**
 * Quienes registraron lotes de la empresa (GET /api/v1/lotes/registradores), también los desactivados, para el
 * filtro «Usuario» del historial: solo su identificador y su nombre, sin correos, ordenados por nombre.
 *
 * @param {import('./cliente.js').Pedir} pedir la función del cliente de la API
 * @returns {Promise<{ id: string, nombre: string }[]>} las personas
 */
export function registradoresDeLotes(pedir) {
  return pedir('GET', '/lotes/registradores');
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
