/*
 * Nombre: granos.js (lote)
 * Descripción: Los granos de una foto marcada (RF-11): en orden de lectura, de arriba abajo y de izquierda a derecha,
 *              que es el orden en que se numeran y se recorren con las flechas del teclado; dónde va la marca de cada
 *              uno, en porcentaje del tamaño de la foto, y a qué grano lleva cada tecla (RNF-06 C4).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */

/** Las flechas y cuántos granos avanza cada una: las dos de adelante van al siguiente y las de atrás, al anterior. */
const PASOS_DE_LAS_FLECHAS = { ArrowRight: 1, ArrowDown: 1, ArrowLeft: -1, ArrowUp: -1 };

/**
 * El centro vertical de un grano.
 *
 * @param {import('../api/analisis.js').GranoMarcado} grano el grano
 * @returns {number} el centro, en píxeles de la foto
 */
function centroVertical(grano) {
  return grano.y + grano.alto / 2;
}

/**
 * El centro horizontal de un grano.
 *
 * @param {import('../api/analisis.js').GranoMarcado} grano el grano
 * @returns {number} el centro, en píxeles de la foto
 */
function centroHorizontal(grano) {
  return grano.x + grano.ancho / 2;
}

/**
 * Los granos en orden de lectura: por filas, de arriba abajo, y en cada fila de izquierda a derecha. Una fila empieza
 * con el grano más alto que falta y se queda con los que tienen el centro por encima del borde de abajo de ese grano,
 * así los granos de una fila que no están alineados del todo no saltan a otra.
 *
 * @param {import('../api/analisis.js').GranoMarcado[]} granos los granos, en cualquier orden
 * @returns {import('../api/analisis.js').GranoMarcado[]} los mismos granos en orden de lectura, en una lista nueva
 */
export function enOrdenDeLectura(granos) {
  const deArribaAbajo = [...granos].sort((uno, otro) => centroVertical(uno) - centroVertical(otro));
  const filas = [];
  for (const grano of deArribaAbajo) {
    const fila = filas.at(-1);
    if (fila && centroVertical(grano) <= fila.borde) {
      fila.granos.push(grano);
    } else {
      filas.push({ borde: grano.y + grano.alto, granos: [grano] });
    }
  }
  return filas.flatMap((fila) => fila.granos.sort((uno, otro) => centroHorizontal(uno) - centroHorizontal(otro)));
}

/**
 * Un tramo de un grano, recortado al tamaño de la foto, en porcentaje.
 *
 * @param {number} inicio dónde empieza, en píxeles
 * @param {number} largo cuánto mide, en píxeles
 * @param {number} total cuánto mide la foto, en píxeles
 * @returns {[number, number]} dónde empieza y cuánto mide, en porcentaje de la foto
 */
function tramoEnPorcentaje(inicio, largo, total) {
  const desde = Math.min(Math.max(inicio, 0), total);
  const hasta = Math.min(Math.max(inicio + largo, desde), total);
  return [(desde / total) * 100, ((hasta - desde) / total) * 100];
}

/**
 * Dónde va la marca de un grano sobre la foto entera, en porcentaje de su tamaño original: así la marca sigue al grano
 * aunque la foto se vea más pequeña, como en el celular. Lo que se salga de la foto se recorta.
 *
 * @param {import('../api/analisis.js').GranoMarcado} grano el grano, en píxeles de la foto original
 * @param {{ ancho: number, alto: number }} foto el tamaño original de la foto, en píxeles
 * @returns {{ left: number, top: number, width: number, height: number }} la posición y el tamaño, en porcentaje
 */
export function posicionEnPorcentaje(grano, foto) {
  const [left, width] = tramoEnPorcentaje(grano.x, grano.ancho, foto.ancho);
  const [top, height] = tramoEnPorcentaje(grano.y, grano.alto, foto.alto);
  return { left, top, width, height };
}

/**
 * A qué grano lleva una tecla, en orden de lectura: las flechas pasan al siguiente o al anterior sin dar la vuelta,
 * Inicio va al primero y Fin al último.
 *
 * @param {number} indice el grano que tiene el foco, desde 0
 * @param {string} tecla la tecla, como ArrowRight (KeyboardEvent.key)
 * @param {number} total cuántos granos tiene la foto
 * @returns {number|null} el grano al que lleva, desde 0, o null si la tecla no mueve el foco
 */
export function indiceSiguiente(indice, tecla, total) {
  if (total === 0) {
    return null;
  }
  if (tecla === 'Home') {
    return 0;
  }
  if (tecla === 'End') {
    return total - 1;
  }
  if (!Object.hasOwn(PASOS_DE_LAS_FLECHAS, tecla)) {
    return null;
  }
  return Math.min(Math.max(indice + PASOS_DE_LAS_FLECHAS[tecla], 0), total - 1);
}
