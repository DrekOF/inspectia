/*
 * Nombre: resultados.js (pruebas)
 * Descripción: Resultados de ejemplo de un lote como los responde el backend, con sus muestras, sus fotos y sus granos
 *              marcados como los da el clasificador falso, el límite cercano de la sugerencia (D-47) y la segunda
 *              muestra en curso, devuelta o terminada (D-25), y respuestas simuladas del envío de la muestra, de su
 *              avance en la cola (la secuencia EN_ESPERA, EN_PROCESO y TERMINADA) y del resultado, para las pruebas
 *              de «Analizar la muestra» y de la página del resultado (RF-05 C2, RF-10, RF-11, RF-12, RF-14 C3 y C6,
 *              RNF-01 C4, RNF-02 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { fireEvent } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { USUARIOS } from './ayudas.jsx';
import { LOTE_PENDIENTE } from './lotes.js';
import { servidor } from './servidor.js';

/** El tamaño original de las fotos de ejemplo, en píxeles: cada celda de la cuadrícula de 10 × 5 mide 100 × 100. */
export const FOTO = { ancho: 1000, alto: 500 };

/** La recomendación por defecto de Estándar (specs/entidades/rango_recomendacion.json). */
export const RECOMENDACION_ESTANDAR =
  'Apto para venta estándar. Retirar a mano los granos defectuosos podría subirlo de categoría.';

/** La recomendación por defecto de Corriente (specs/entidades/rango_recomendacion.json). */
export const RECOMENDACION_CORRIENTE =
  'Retirar los granos defectuosos antes de vender, y revisar la fermentación y el secado.';

/**
 * Los 50 granos de una foto, como los da el clasificador falso: en una cuadrícula de 10 × 5, cada uno en el centro de
 * su celda y del 60 % de su tamaño, en orden de lectura. Los primeros son los no sanos y los primeros, los dudosos.
 *
 * @param {{ noSanos?: number, dudosos?: number }} [opciones] cuántos no sanos y cuántos dudosos
 * @returns {import('../api/analisis.js').GranoMarcado[]} los granos
 */
export function granosDeFoto({ noSanos = 0, dudosos = 0 } = {}) {
  return Array.from({ length: 50 }, (_, indice) => ({
    x: (indice % 10) * 100 + 20, y: Math.floor(indice / 10) * 100 + 20, ancho: 60, alto: 60,
    clase: indice < noSanos ? 'NO_SANO' : 'SANO', dudoso: indice < dudosos,
  }));
}

/**
 * Una foto de una muestra terminada, con su enlace de vista y sus granos.
 *
 * @param {number} numero su número, de 1 a 3
 * @param {{ granos?: object[], vence?: number, muestra?: number }} [opciones] sus granos, el número del enlace, que
 *   cambia cada vez que el backend lo firma, y el de su muestra: las fotos de la segunda tienen otro enlace
 * @returns {import('../api/analisis.js').FotoMarcada} la foto
 */
export function fotoMarcada(numero, { granos = granosDeFoto(), vence = 1, muestra = 1 } = {}) {
  const carpeta = muestra === 1 ? '' : `muestra-${muestra}/`;
  return {
    numero,
    vista: { url: `/api/v1/archivos/e/l/${carpeta}foto-${numero}.jpg?vence=${vence}&firma=v`, vence: '2026-11-23T17:00:00Z' },
    granos,
  };
}

/** Los datos del lote del ejemplo, el L-0143 del prototipo. */
const DATOS_DEL_LOTE = {
  id: LOTE_PENDIENTE.id, codigo: LOTE_PENDIENTE.codigo, fechaRecepcion: LOTE_PENDIENTE.fechaRecepcion,
  origen: LOTE_PENDIENTE.origen,
};

/** Quién envió la muestra: el usuario final de ejemplo. */
const ANALIZADA_POR = { id: USUARIOS.USUARIO_FINAL.id, nombre: USUARIOS.USUARIO_FINAL.nombre };

/**
 * Una muestra terminada, con sus cifras y sus fotos marcadas.
 *
 * @param {number} numero su número, 1 o 2 (D-25)
 * @param {{ totalGranos: number, granosSanos: number, porcentajeContado: number, fotos: object[], fechaEnvio: string,
 *   fechaFin: string }} datos sus cifras, sus fotos y cuándo se envió y terminó
 * @returns {import('../api/analisis.js').MuestraDelLote} la muestra
 */
export function muestraTerminada(numero, { totalGranos, granosSanos, porcentajeContado, fotos, fechaEnvio, fechaFin }) {
  return {
    numero, estado: 'TERMINADA', analizadaPor: ANALIZADA_POR, modelo: { nombre: 'clasificador-falso', version: '1.0' },
    totalGranos, granosSanos, porcentajeContado, fechaEnvio, fechaFin, fotos,
  };
}

/**
 * El resultado de un lote analizado con una muestra, como lo responde GET /api/v1/lotes/{id}/resultado. Por defecto,
 * el ejemplo de RF-10 C8: 87 de 100 marcados como sanos dan 90,6 % corregido, con margen de 7,5, Estándar y la
 * sugerencia de otra muestra, a 0,6 puntos del límite de Estándar (90 %), que se guarda con el lote (D-47); la foto 1
 * tiene 6 no sanos y 6 dudosos, y la foto 2, 7 no sanos. Si el cambio quita la sugerencia, el lote queda sin límite,
 * como lo exige el CHECK de V1.
 *
 * @param {object} [cambios] lo que cambia del lote, como porcentajeSanos, margen o limiteSugerencia
 * @param {{ fotos?: object[], vence?: number }} [opciones] las fotos de la muestra y el número de sus enlaces
 * @returns {import('../api/analisis.js').ResultadoDelLote} el resultado
 */
export function resultadoAnalizado(cambios = {}, { fotos, vence = 1 } = {}) {
  const sinSugerencia = cambios.sugerirOtraMuestra === false ? { limiteSugerencia: null, categoriaDelLimite: null } : {};
  const lote = {
    ...DATOS_DEL_LOTE, estado: 'ANALIZADO', fechaResultado: '2026-11-23T16:42:03Z', porcentajeSanos: 90.6, margen: 7.5,
    categoria: 'ESTANDAR', recomendacion: RECOMENDACION_ESTANDAR, sugerirOtraMuestra: true, limiteSugerencia: 90.0,
    categoriaDelLimite: 'ESTANDAR', avisoConfianzaBaja: false, totalGranos: 100, granosSanos: 87, porcentajeContado: 87.0,
    ...sinSugerencia, ...cambios,
  };
  return {
    lote,
    muestras: [muestraTerminada(1, {
      totalGranos: lote.totalGranos, granosSanos: lote.granosSanos, porcentajeContado: lote.porcentajeContado,
      fechaEnvio: '2026-11-23T16:42:00Z', fechaFin: lote.fechaResultado,
      fotos: fotos ?? [
        fotoMarcada(1, { granos: granosDeFoto({ noSanos: 6, dudosos: 6 }), vence }),
        fotoMarcada(2, { granos: granosDeFoto({ noSanos: 7 }), vence }),
      ],
    })],
  };
}

/**
 * El resultado de un lote analizado que sugirió otra muestra y ya tiene la segunda, todavía sin terminar (D-25): el lote
 * sigue con el resultado de la muestra 1, 90,6 % y la sugerencia, y la muestra 2 tiene sus fotos sin granos.
 *
 * @param {'PENDIENTE'|'EN_ESPERA'|'EN_PROCESO'} estado el estado de la muestra 2
 * @param {{ fechaEnvio?: string|null }} [opciones] cuándo se envió la muestra 2; una pendiente con fecha de envío
 *   volvió de la cola sin resultado, y una sin fecha todavía se está tomando
 * @returns {import('../api/analisis.js').ResultadoDelLote} el resultado
 */
export function resultadoConSegundaMuestra(estado,
  { fechaEnvio = estado === 'PENDIENTE' ? null : '2026-11-23T17:10:00Z' } = {}) {
  const { lote, muestras } = resultadoAnalizado();
  return {
    lote,
    muestras: [...muestras, {
      numero: 2, estado, analizadaPor: ANALIZADA_POR, modelo: null, totalGranos: null, granosSanos: null,
      porcentajeContado: null, fechaEnvio, fechaFin: null,
      fotos: [fotoMarcada(1, { granos: [], muestra: 2 }), fotoMarcada(2, { granos: [], muestra: 2 })],
    }],
  };
}

/**
 * El resultado de un lote con dos muestras terminadas (D-25), recalculado con la suma de sus granos: la muestra 1 con
 * 87 de 100 sanos y la 2 con 86 de 100 (7 no sanos en cada foto) dan 173 de 200, 89,5 % corregido, con margen de 5,4,
 * Corriente y sin la sugerencia ni el límite, aunque quede a 0,5 puntos de 90.
 *
 * @returns {import('../api/analisis.js').ResultadoDelLote} el resultado
 */
export function resultadoConDosMuestras() {
  const lote = {
    ...DATOS_DEL_LOTE, estado: 'ANALIZADO', fechaResultado: '2026-11-23T17:10:03Z', porcentajeSanos: 89.5, margen: 5.4,
    categoria: 'CORRIENTE', recomendacion: RECOMENDACION_CORRIENTE, sugerirOtraMuestra: false, limiteSugerencia: null,
    categoriaDelLimite: null, avisoConfianzaBaja: false, totalGranos: 200, granosSanos: 173, porcentajeContado: 86.5,
  };
  return {
    lote,
    muestras: [
      muestraTerminada(1, {
        totalGranos: 100, granosSanos: 87, porcentajeContado: 87.0, fechaEnvio: '2026-11-23T16:42:00Z',
        fechaFin: '2026-11-23T16:42:03Z', fotos: [
          fotoMarcada(1, { granos: granosDeFoto({ noSanos: 6, dudosos: 6 }) }),
          fotoMarcada(2, { granos: granosDeFoto({ noSanos: 7 }) }),
        ],
      }),
      muestraTerminada(2, {
        totalGranos: 100, granosSanos: 86, porcentajeContado: 86.0, fechaEnvio: '2026-11-23T17:10:00Z',
        fechaFin: lote.fechaResultado, fotos: [
          fotoMarcada(1, { granos: granosDeFoto({ noSanos: 7 }), muestra: 2 }),
          fotoMarcada(2, { granos: granosDeFoto({ noSanos: 7 }), muestra: 2 }),
        ],
      }),
    ],
  };
}

/**
 * El resultado de un lote pendiente, con su muestra en el estado que se pida y sus fotos sin granos.
 *
 * @param {'PENDIENTE'|'EN_ESPERA'|'EN_PROCESO'|null} estado el estado de la muestra, o null si el lote no tiene
 * @param {{ fechaEnvio?: string|null }} [opciones] cuándo se envió la muestra; una pendiente con fecha de envío volvió
 *   de la cola sin resultado
 * @returns {import('../api/analisis.js').ResultadoDelLote} el resultado
 */
export function resultadoPendiente(estado, { fechaEnvio = estado === 'PENDIENTE' ? null : '2026-11-23T16:42:00Z' } = {}) {
  const lote = {
    ...DATOS_DEL_LOTE, estado: 'PENDIENTE', fechaResultado: null, porcentajeSanos: null, margen: null, categoria: null,
    recomendacion: null, sugerirOtraMuestra: null, limiteSugerencia: null, categoriaDelLimite: null,
    avisoConfianzaBaja: null, totalGranos: null, granosSanos: null, porcentajeContado: null,
  };
  const muestras = estado ? [{
    numero: 1, estado, analizadaPor: ANALIZADA_POR, modelo: null, totalGranos: null, granosSanos: null,
    porcentajeContado: null, fechaEnvio, fechaFin: null,
    fotos: [fotoMarcada(1, { granos: [] }), fotoMarcada(2, { granos: [] })],
  }] : [];
  return { lote, muestras };
}

/**
 * El avance del análisis como lo responde GET /api/v1/lotes/{id}/analisis.
 *
 * @param {'PENDIENTE'|'EN_ESPERA'|'EN_PROCESO'|'TERMINADA'} estado el estado de la muestra
 * @param {{ fallo?: boolean, muestra?: number }} [opciones] si volvió de la cola sin resultado y el número de la
 *   muestra en curso, como la 2 (D-25)
 * @returns {import('../api/analisis.js').AvanceDelAnalisis} el avance
 */
export function avance(estado, { fallo = false, muestra = 1 } = {}) {
  return {
    muestra, estado, fechaEnvio: '2026-11-23T16:42:00Z',
    fechaInicio: ['EN_PROCESO', 'TERMINADA'].includes(estado) ? '2026-11-23T16:42:01Z' : null,
    fechaFin: estado === 'TERMINADA' ? '2026-11-23T16:42:03Z' : null, fallo,
  };
}

/**
 * Responde el resultado de un lote y anota cuántas veces se pidió.
 *
 * @param {string} loteId el lote
 * @param {object|((vez: number) => object)} resultado el resultado, o el de cada vez que se pide, desde 1
 * @returns {{ veces: number }} cuántas veces se pidió
 */
export function conResultado(loteId, resultado) {
  const pedidas = { veces: 0 };
  servidor.use(http.get(`/api/v1/lotes/${loteId}/resultado`, () => {
    pedidas.veces += 1;
    return HttpResponse.json(typeof resultado === 'function' ? resultado(pedidas.veces) : resultado);
  }));
  return pedidas;
}

/**
 * Responde el avance del análisis de un lote: cada consulta recibe el siguiente de la lista y, al acabarse, el
 * último. Anota cuántas consultas llegaron y cuántas hubo a la vez como máximo. Con una espera, cada respuesta
 * espera a que la prueba la suelte.
 *
 * @param {string} loteId el lote
 * @param {Array<object|(() => Response)>} secuencia los avances, como los de {@link avance}, o funciones que dan una
 *   respuesta, como un error
 * @param {{ espera?: () => Promise<unknown> }} [opciones] lo que espera cada respuesta antes de salir
 * @returns {{ veces: number, enVuelo: number, maximoEnVuelo: number }} lo que llegó
 */
export function conAvance(loteId, secuencia, { espera } = {}) {
  const anotado = { veces: 0, enVuelo: 0, maximoEnVuelo: 0 };
  servidor.use(http.get(`/api/v1/lotes/${loteId}/analisis`, async () => {
    const respuesta = secuencia[Math.min(anotado.veces, secuencia.length - 1)];
    anotado.veces += 1;
    anotado.enVuelo += 1;
    anotado.maximoEnVuelo = Math.max(anotado.maximoEnVuelo, anotado.enVuelo);
    try {
      await espera?.();
    } finally {
      anotado.enVuelo -= 1;
    }
    return typeof respuesta === 'function' ? respuesta() : HttpResponse.json(respuesta);
  }));
  return anotado;
}

/**
 * Responde el envío de la muestra de un lote (POST /api/v1/lotes/{id}/analisis) con la muestra en espera, y anota
 * cuántas veces llegó.
 *
 * @param {string} loteId el lote
 * @param {{ respuesta?: () => Response, espera?: Promise<unknown> }} [opciones] otra respuesta, como un error, y lo
 *   que espera antes de responder, para ver la página mientras se envía
 * @returns {{ veces: number }} cuántas veces llegó
 */
export function conEnvio(loteId, { respuesta, espera } = {}) {
  const pedidas = { veces: 0 };
  servidor.use(http.post(`/api/v1/lotes/${loteId}/analisis`, async () => {
    pedidas.veces += 1;
    await espera;
    return respuesta ? respuesta() : HttpResponse.json(avance('EN_ESPERA'), { status: 202 });
  }));
  return pedidas;
}

/**
 * Hace como si la imagen de una foto cargara, con su tamaño original: jsdom no carga imágenes ni sabe su tamaño.
 *
 * @param {HTMLImageElement} imagen la imagen
 * @param {{ ancho: number, alto: number }} [tamano] su tamaño original, en píxeles
 */
export function cargarImagen(imagen, tamano = FOTO) {
  Object.defineProperty(imagen, 'naturalWidth', { configurable: true, value: tamano.ancho });
  Object.defineProperty(imagen, 'naturalHeight', { configurable: true, value: tamano.alto });
  fireEvent.load(imagen);
}
