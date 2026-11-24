/*
 * Nombre: modelo.js (api)
 * Descripción: La petición de la pantalla «Modelo de IA» al backend: las métricas de prueba de los modelos, cuál está
 *              en uso y la confianza promedio de los análisis de la empresa (RF-17).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */

/**
 * @typedef {object} MatrizDeConfusion la matriz de confusión del conjunto de prueba, en granos (D-48)
 * @property {number} sanosComoSanos sanos clasificados como sanos
 * @property {number} sanosComoNoSanos sanos clasificados como no sanos
 * @property {number} noSanosComoSanos no sanos clasificados como sanos
 * @property {number} noSanosComoNoSanos no sanos clasificados como no sanos
 */

/**
 * @typedef {object} ModeloMedido un modelo con sus métricas del conjunto de prueba, en porcentaje; las que no tiene,
 *   como las del clasificador falso de desarrollo, vienen en null
 * @property {string} id su identificador
 * @property {string} nombre su nombre, como EfficientNet-B0
 * @property {string} version su versión
 * @property {'CNN'|'TRANSFORMER'|null} tipo su tipo
 * @property {boolean} enUso si es el modelo en uso
 * @property {number|null} f1Macro el F1-score macro
 * @property {number|null} precisionMacro la precisión macro
 * @property {number|null} recallMacro el recall macro
 * @property {number} recallSano el recall de sano
 * @property {number} recallNoSano el recall de no sano
 * @property {number|null} precisionSano la precisión de sano
 * @property {number|null} precisionNoSano la precisión de no sano
 * @property {MatrizDeConfusion|null} matriz la matriz de confusión
 * @property {number|null} tiempoPorGranoMs el tiempo por grano, en milisegundos
 */

/**
 * @typedef {object} ConfianzaPromedio la confianza promedio de los análisis de la empresa (RF-17 C2)
 * @property {number|null} promedio la confianza, en porcentaje y con 2 decimales; null si no hay análisis
 * @property {number} granos cuántos granos la dan
 * @property {number} muestras de cuántas muestras analizadas
 */

/**
 * Las métricas de los modelos y la confianza promedio de la empresa de la sesión (GET /api/v1/modelo). Solo para el
 * administrador de empresa: a los demás roles, la API responde 403.
 *
 * @param {import('./cliente.js').Pedir} pedir la función del cliente de la API
 * @returns {Promise<{ modelos: ModeloMedido[], confianza: ConfianzaPromedio }>} los modelos, del mayor F1 macro al
 *   menor, y la confianza promedio
 */
export function metricasDelModelo(pedir) {
  return pedir('GET', '/modelo');
}
