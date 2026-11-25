/*
 * Nombre: estadisticas.js (api)
 * Descripción: La petición de las estadísticas de la empresa al backend (RF-16): lotes analizados, promedio de granos
 *              sanos, lotes por categoría, promedio por mes y por finca o proveedor en un periodo.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */

/** Los campos del periodo, con los mismos nombres de la API. */
export const CAMPOS_DEL_PERIODO = ['desde', 'hasta'];

/**
 * @typedef {object} Estadisticas las estadísticas como las responde el backend (RF-16)
 * @property {string|null} desde el primer día del periodo (aaaa-mm-dd)
 * @property {string|null} hasta el último día del periodo (aaaa-mm-dd)
 * @property {boolean} sinLotes si no hay lotes analizados en el periodo (RF-16 C2)
 * @property {number} lotesAnalizados cuántos lotes analizados hay
 * @property {number|null} promedioSanos el promedio de granos sanos, con 1 decimal; null sin lotes
 * @property {Array<{ categoria: string, lotes: number }>} porCategoria las 4 categorías, en el orden de los rangos
 * @property {Array<{ mes: string, lotes: number, promedioSanos: number|null }>} porMes cada mes (aaaa-mm), del
 *   primero con lotes al último; uno sin lotes va sin promedio
 * @property {Array<{ origen: string, lotes: number, promedioSanos: number }>} porOrigen cada finca o proveedor, de
 *   mayor a menor promedio
 */

/**
 * Las estadísticas de la empresa de la sesión en un periodo (GET /api/v1/estadisticas). Solo las pide el
 * administrador de empresa (RF-16 C3).
 *
 * @param {import('./cliente.js').Pedir} pedir la función del cliente de la API
 * @param {{ desde: string, hasta: string }} periodo el primer y el último día (aaaa-mm-dd), incluidos
 * @returns {Promise<Estadisticas>} las estadísticas
 */
export function estadisticasDeLaEmpresa(pedir, periodo) {
  const parametros = new URLSearchParams();
  for (const nombre of CAMPOS_DEL_PERIODO) {
    if (periodo[nombre]) {
      parametros.set(nombre, periodo[nombre]);
    }
  }
  const consulta = parametros.toString();
  return pedir('GET', `/estadisticas${consulta ? `?${consulta}` : ''}`);
}
