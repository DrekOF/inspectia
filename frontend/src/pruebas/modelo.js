/*
 * Nombre: modelo.js (pruebas)
 * Descripción: Datos que comparten las pruebas de la pantalla «Modelo de IA» (RF-17): los modelos de la comparación
 *              de Julián (D-18, D-48) y el del clasificador falso, como los responde la API, y responder las métricas
 *              con la API simulada, anotando cuántas veces se pidieron y con qué periodo.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { http, HttpResponse } from 'msw';
import { problema, servidor } from './servidor.js';

/** La ruta de la API del modelo de IA. */
export const API_MODELO = '/api/v1/modelo';

/** EfficientNet-B0, el modelo en uso (P-01), con sus métricas de D-18 y D-48. */
export const EFFICIENTNET = {
  id: '0199e2a0-0000-7000-8000-0000000000e0', nombre: 'EfficientNet-B0', version: '1.0', tipo: 'CNN', enUso: true,
  f1Macro: 73.5, precisionMacro: 75.2, recallMacro: 72.31, recallSano: 91.2, recallNoSano: 53.4, precisionSano: 87.5,
  precisionNoSano: 62.9, matriz: { sanosComoSanos: 238, sanosComoNoSanos: 23, noSanosComoSanos: 34, noSanosComoNoSanos: 39 },
  tiempoPorGranoMs: 19,
};

/** ResNet-50, que no está en uso. */
export const RESNET = {
  id: '0199e2a0-0000-7000-8000-0000000000e1', nombre: 'ResNet-50', version: '1.0', tipo: 'CNN', enUso: false,
  f1Macro: 69.9, precisionMacro: 70.11, recallMacro: 69.71, recallSano: 87.4, recallNoSano: 52.1, precisionSano: 86.69,
  precisionNoSano: 53.52, matriz: { sanosComoSanos: 228, sanosComoNoSanos: 33, noSanosComoSanos: 35, noSanosComoNoSanos: 38 },
  tiempoPorGranoMs: 50.8,
};

/** Un transformer de la comparación, con datos de ejemplo. */
export const SWIN = {
  id: '0199e2a0-0000-7000-8000-0000000000e2', nombre: 'Swin-Tiny', version: '1.0', tipo: 'TRANSFORMER', enUso: false,
  f1Macro: 66.4, precisionMacro: 68.05, recallMacro: 65.9, recallSano: 90.1, recallNoSano: 41.7, precisionSano: 84.3,
  precisionNoSano: 51.8, matriz: { sanosComoSanos: 235, sanosComoNoSanos: 26, noSanosComoSanos: 43, noSanosComoNoSanos: 30 },
  tiempoPorGranoMs: 61.25,
};

/** El modelo del clasificador falso de desarrollo: solo sus recall, sin las demás métricas y sin estar en uso. */
export const FALSO = {
  id: '0199e2a0-0000-7000-8000-0000000000f0', nombre: 'clasificador-falso', version: '1.0', tipo: null, enUso: false,
  f1Macro: null, precisionMacro: null, recallMacro: null, recallSano: 91.2, recallNoSano: 53.4, precisionSano: null,
  precisionNoSano: null, matriz: null, tiempoPorGranoMs: null,
};

/** La confianza promedio de una empresa con análisis. */
export const CON_ANALISIS = { promedio: 88.6, granos: 200, muestras: 2 };

/** La de una empresa sin análisis. */
export const SIN_ANALISIS = { promedio: null, granos: 0, muestras: 0 };

/** El mensaje del error que responde la API simulada. */
export const ERROR_DE_LA_API = 'Ocurrió un error inesperado. Intenta de nuevo.';

/**
 * Responde las métricas del modelo con la API simulada, o un error, y anota cuántas veces se pidieron y los
 * parámetros de cada petición, como el periodo de la confianza.
 *
 * @param {{ modelos?: object[], confianza?: object, estado?: number, cuerpo?: object }} [respuesta] los modelos, la
 *   confianza y, para un error, su código HTTP y, si hace falta, el cuerpo del problema; por defecto, los de la
 *   comparación con EfficientNet-B0 en uso y una empresa con análisis
 * @returns {{ veces: number, consultas: Record<string, string>[] }} cuántas veces se pidieron y con qué parámetros
 */
export function conMetricas({
  modelos = [EFFICIENTNET, RESNET, SWIN, FALSO], confianza = CON_ANALISIS, estado, cuerpo = {},
} = {}) {
  const pedidas = { veces: 0, consultas: [] };
  servidor.use(http.get(API_MODELO, ({ request }) => {
    pedidas.veces += 1;
    pedidas.consultas.push(Object.fromEntries(new URL(request.url).searchParams));
    if (estado) {
      return problema(estado, ERROR_DE_LA_API, cuerpo);
    }
    return HttpResponse.json({ modelos, confianza });
  }));
  return pedidas;
}
