/*
 * Nombre: limites.js
 * Descripción: Lo escrito a mano para los límites de los rangos (RF-13): qué límites no bajan en orden, con la misma
 *              regla del backend (ReglasRangoRecomendacionImpl); qué porcentajes abarca cada categoría, para el
 *              resumen «Así quedan los rangos»; y la regla del orden que revisa la pantalla antes de enviar. La
 *              pantalla generada la recibe en sus extensiones (RP-23).
 * Fecha de creación: 28/10/2026
 * Actualización: 28/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */

/** La clave del mensaje de la regla del orden: la misma de la API, en specs/entidades/rango_recomendacion.json. */
const MENSAJE_DEL_ORDEN = 'rango_recomendacion.reglas.limites_en_orden';

/**
 * Las posiciones de los límites que no bajan en orden: los dos de cada par seguido en el que el de abajo no es menor
 * que el de arriba. Como en el backend, un empate tampoco baja: un lote con ese porcentaje no tendría una sola
 * categoría. Un límite que no es un número, como uno vacío, no se compara: eso lo dicen sus validaciones.
 *
 * @param {Array<number|undefined>} limites los límites, en el orden de las categorías, de la mejor a la peor
 * @returns {number[]} las posiciones, de menor a mayor y sin repetir
 */
export function limitesFueraDeOrden(limites) {
  const fuera = new Set();
  limites.forEach((abajo, posicion) => {
    // Con un límite que no es un número, vacío (undefined) o como «noventa» (NaN), la comparación da falso: no se
    // compara. Tampoco el primero, que no tiene otro arriba.
    if (abajo >= limites[posicion - 1]) {
      fuera.add(posicion - 1).add(posicion);
    }
  });
  return [...fuera];
}

/**
 * @typedef {object} Intervalo lo que abarca una categoría
 * @property {'desde'|'entre'|'menosDe'|'revisar'} tipo desde su límite (la primera), entre su límite y el de arriba
 *   (las del medio), menos que el límite de arriba (la última, cuyo límite es 0) o que hay que revisar sus límites
 * @property {number} [limite] el límite, en desde y en menosDe
 * @property {number} [desde] el comienzo, en entre
 * @property {number} [hasta] el final, en entre: un paso de la escala antes del límite de arriba
 */

/**
 * Lo que abarca la categoría de una posición, sin revisar si sus límites son números o bajan en orden.
 *
 * @param {Array<number|undefined>} limites los límites, en el orden de las categorías
 * @param {number} posicion la posición de la categoría
 * @param {number} pasos cuántos pasos de la escala tiene una unidad, como 10 con un decimal
 * @returns {Intervalo} lo que abarca
 */
function intervaloDe(limites, posicion, pasos) {
  const arriba = limites[posicion - 1];
  if (posicion === 0) {
    return { tipo: 'desde', limite: limites[posicion] };
  }
  if (posicion === limites.length - 1) {
    return { tipo: 'menosDe', limite: arriba };
  }
  // En pasos enteros de la escala no hay errores de redondeo: 96 - 0,1 da 95,9 y no 95,89999999999999.
  return { tipo: 'entre', desde: limites[posicion], hasta: (Math.round(arriba * pasos) - 1) / pasos };
}

/**
 * Qué porcentajes de granos sanos abarca cada categoría con unos límites, para el resumen «Así quedan los rangos»: la
 * primera, desde su límite; las del medio, desde su límite hasta un paso de la escala antes del límite de la de arriba;
 * y la última, lo que queda por debajo del límite de la de arriba. Las categorías de un par que no baja, o las que
 * dependen de un límite que no es un número, dicen que hay que revisar el límite.
 *
 * @param {Array<number|undefined>} limites los límites, en el orden de las categorías
 * @param {number} escala los decimales de los límites, como 1
 * @returns {Intervalo[]} lo que abarca cada categoría, en el mismo orden
 */
export function intervalosDeLosRangos(limites, escala) {
  const fuera = limitesFueraDeOrden(limites);
  return limites.map((_, posicion) => {
    const intervalo = intervaloDe(limites, posicion, 10 ** escala);
    const usa = intervalo.tipo === 'entre' ? [intervalo.desde, intervalo.hasta] : [intervalo.limite];
    return fuera.includes(posicion) || !usa.every(Number.isFinite) ? { tipo: 'revisar' } : intervalo;
  });
}

/**
 * La regla del orden de los límites, la misma que revisa la API, para revisarla antes de enviar (RF-13 C1). Marca los
 * dos límites de cada par que no baja; la pantalla deja sin marcar los que no se pueden cambiar, como el de Fuera de
 * norma.
 *
 * @param {object[]} filas las filas, en el orden de las categorías, con los límites leídos como números
 * @returns {{ mensaje: string, campos: string[] }|null} la clave del mensaje de la regla y los campos que marca, o null
 *   si se cumple
 */
export function reglaDelOrden(filas) {
  const fuera = limitesFueraDeOrden(filas.map(({ limiteInferior }) => limiteInferior));
  if (fuera.length === 0) {
    return null;
  }
  return { mensaje: MENSAJE_DEL_ORDEN, campos: fuera.map((posicion) => `filas[${posicion}].limiteInferior`) };
}
