/*
 * Nombre: resultado.js (lote)
 * Descripción: Lo que la página del resultado decide sin dibujar nada: el rango probable del porcentaje, recortado a
 *              0–100 % (D-43), con el porcentaje y el margen guardados con el lote, sin recalcularlos (RF-11 C3); la
 *              muestra en curso, y qué mostrar del lote: su resultado, la espera del análisis (RNF-01 C4, RNF-02 C3),
 *              un análisis que no terminó o que todavía no tiene resultado.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { textoDelDecimal } from '../crud/decimales.js';

/** Los estados de una muestra que todavía no termina: en la cola o en el análisis (RNF-02 C3). */
export const EN_CURSO = ['EN_ESPERA', 'EN_PROCESO'];

/** Las décimas de un porcentaje: 100 % son 1000 décimas. */
const CIEN_POR_CIENTO = 1000;

/**
 * @typedef {object} Extremo un extremo del rango probable
 * @property {number} valor el porcentaje, con un decimal como mucho
 * @property {boolean} recortado si el rango se pasaba de 0 o de 100 % y quedó en el límite
 */

/**
 * El rango probable del porcentaje de granos sanos (D-43): el porcentaje redondeado más y menos el margen redondeado,
 * recortado a 0–100 %. Se cuenta en décimas, con enteros, para que 90,6 − 7,5 dé 83,1 y no 83,09999.
 *
 * @param {number} porcentaje el porcentaje corregido, con 1 decimal, como se guardó con el lote
 * @param {number} margen el margen, con 1 decimal, como se guardó con el lote (P-19)
 * @returns {{ desde: Extremo, hasta: Extremo }} los dos extremos
 */
export function rangoProbable(porcentaje, margen) {
  const centro = Math.round(porcentaje * 10);
  const ancho = Math.round(margen * 10);
  const desde = centro - ancho;
  const hasta = centro + ancho;
  return {
    desde: desde < 0 ? { valor: 0, recortado: true } : { valor: desde / 10, recortado: false },
    hasta: hasta > CIEN_POR_CIENTO ? { valor: 100, recortado: true } : { valor: hasta / 10, recortado: false },
  };
}

/**
 * Los textos de los dos extremos del rango probable, en el idioma de la app: un extremo recortado va sin decimales
 * («0», «100») y los demás con uno («83,1» en español, «83.1» en inglés), como dice D-43.
 *
 * @param {number} porcentaje el porcentaje corregido, con 1 decimal
 * @param {number} margen el margen, con 1 decimal
 * @param {string} region la región del idioma, como es-CO
 * @returns {{ desde: string, hasta: string }} los textos
 */
export function textosDelRango(porcentaje, margen, region) {
  const { desde, hasta } = rangoProbable(porcentaje, margen);
  const texto = (extremo) => textoDelDecimal(extremo.valor, extremo.recortado ? 0 : 1, region);
  return { desde: texto(desde), hasta: texto(hasta) };
}

/**
 * La muestra en curso de un lote: la de número más alto.
 *
 * @param {import('../api/analisis.js').MuestraDelLote[]} muestras las muestras del lote
 * @returns {import('../api/analisis.js').MuestraDelLote|null} la muestra, o null si el lote no tiene
 */
export function muestraEnCurso(muestras) {
  return muestras.reduce((mayor, muestra) => (mayor && mayor.numero > muestra.numero ? mayor : muestra), null);
}

/**
 * @typedef {'resultado'|'espera'|'cargando'|'fallo'|'sinResultado'} Vista qué muestra la página de un lote: su
 *   resultado; la espera del análisis; nada todavía, porque el análisis acaba de terminar y el resultado está
 *   llegando; que el análisis no terminó, o que el lote todavía no tiene resultado
 */

/**
 * Qué muestra la página de un lote. Un lote analizado muestra su resultado. Uno pendiente muestra el estado de su
 * muestra en curso: el último avance consultado, o el que trajo el resultado si todavía no se consultó.
 *
 * @param {import('../api/analisis.js').ResultadoDelLote} datos el resultado y el detalle del lote
 * @param {import('../api/analisis.js').AvanceDelAnalisis|null} avance el último avance consultado, si lo hay
 * @returns {Vista} la vista
 */
export function vistaDelLote(datos, avance) {
  if (datos.lote.estado === 'ANALIZADO') {
    return 'resultado';
  }
  const muestra = muestraEnCurso(datos.muestras);
  const estado = avance ? avance.estado : muestra?.estado;
  // Una muestra pendiente que ya se envió volvió de la cola sin resultado.
  const fallo = avance ? avance.fallo : estado === 'PENDIENTE' && Boolean(muestra.fechaEnvio);
  if (EN_CURSO.includes(estado)) {
    return 'espera';
  }
  if (estado === 'TERMINADA') {
    return 'cargando';
  }
  return fallo ? 'fallo' : 'sinResultado';
}
