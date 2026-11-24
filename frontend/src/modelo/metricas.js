/*
 * Nombre: metricas.js
 * Descripción: Lo que comparten las partes de la pantalla «Modelo de IA» (RF-17): las métricas de la tabla
 *              comparativa en su orden, el texto de una métrica, el modelo en uso y el total de granos del conjunto de
 *              prueba, que sale de la matriz de confusión (D-48).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { textoDelDecimal } from '../crud/decimales.js';

/** Lo que muestra una métrica que el modelo no tiene, como las del clasificador falso. */
export const SIN_DATO = '—';

/**
 * Las métricas de la tabla comparativa, en su orden: el dato de la API y la clave de su título, que es la etiqueta de
 * la columna de la tabla modelo (specs/entidades/modelo.json). Las precisiones y los recall de cada clase van juntos.
 */
export const METRICAS = [
  ['f1Macro', 'modelo.f1_macro.etiqueta'],
  ['precisionMacro', 'modelo.precision_macro.etiqueta'],
  ['recallMacro', 'modelo.recall_macro.etiqueta'],
  ['precisionSano', 'modelo.precision_sano.etiqueta'],
  ['recallSano', 'modelo.recall_sano.etiqueta'],
  ['precisionNoSano', 'modelo.precision_no_sano.etiqueta'],
  ['recallNoSano', 'modelo.recall_no_sano.etiqueta'],
  ['tiempoPorGranoMs', 'modelo.tiempo_por_grano_ms.etiqueta'],
];

/**
 * El texto de una métrica: con 2 decimales, como la guarda la base, en el formato del idioma, o «—» si el modelo no la
 * tiene.
 *
 * @param {number|null|undefined} valor la métrica
 * @param {string} region la región del idioma, como es-CO
 * @returns {string} el texto, como 73,50
 */
export function textoDeMetrica(valor, region) {
  return valor === null || valor === undefined ? SIN_DATO : textoDelDecimal(valor, 2, region);
}

/**
 * El modelo en uso, si hay uno.
 *
 * @param {import('../api/modelo.js').ModeloMedido[]} modelos los modelos
 * @returns {import('../api/modelo.js').ModeloMedido|null} el modelo en uso, o null
 */
export function modeloEnUso(modelos) {
  return modelos.find((modelo) => modelo.enUso) ?? null;
}

/**
 * Cuántos granos tiene el conjunto de prueba: la suma de la matriz de confusión.
 *
 * @param {import('../api/modelo.js').MatrizDeConfusion|null} matriz la matriz
 * @returns {number|null} el total, o null sin matriz
 */
export function granosDePrueba(matriz) {
  if (!matriz) {
    return null;
  }
  return matriz.sanosComoSanos + matriz.sanosComoNoSanos + matriz.noSanosComoSanos + matriz.noSanosComoNoSanos;
}
