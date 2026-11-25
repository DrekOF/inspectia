/*
 * Nombre: revision.js (lote)
 * Descripción: Lo que la página decide sin dibujar nada sobre la revisión de las fotos por el servicio de IA (D-46):
 *              la lista de números de las fotos en el idioma de la app, el máximo de dudosos con el formato del idioma,
 *              cuántos granos suman las fotos revisadas (RF-07 C3), qué fotos hay que repetir (RF-06, RF-07 C2, RF-09
 *              C1), por qué volvió la muestra sin resultado (D-44) y el máximo de dudosos del aviso de confianza baja
 *              (RF-09 C4 y C6).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { textoDelDecimal } from '../crud/decimales.js';

/** Los granos que pide una muestra entre todas sus fotos para calcular el resultado (RF-07 C3, D-23). */
export const MINIMO_DE_GRANOS = 100;

/** Las repeticiones por granos dudosos que permite una muestra (RF-09, restricción 3). */
export const MAXIMO_DE_REPETICIONES = 2;

/**
 * Los números de las fotos, juntos como en el idioma: «1 y 2», «1, 2 y 3», «1 and 2».
 *
 * @param {number[]} numeros los números
 * @param {string} region la región del idioma, como es-CO
 * @returns {string} el texto
 */
export function listaDeNumeros(numeros, region) {
  return new Intl.ListFormat(region, { style: 'long', type: 'conjunction' }).format(numeros.map(String));
}

/**
 * Un porcentaje guardado con 1 decimal, sin decimales si es entero («10») y con uno si no («10,5» o «10.5»).
 *
 * @param {number} valor el porcentaje
 * @param {string} region la región del idioma, como es-CO
 * @returns {string} el texto
 */
export function textoDelPorcentaje(valor, region) {
  const decimas = Math.round(valor * 10);
  return textoDelDecimal(decimas / 10, decimas % 10 === 0 ? 0 : 1, region);
}

/**
 * Cuántos granos encontró la revisión entre todas las fotos (RF-07 C3). Solo si todas tienen su conteo: una foto
 * «Cargada» todavía no lo tiene (D-46).
 *
 * @param {import('../api/fotos.js').Foto[]} fotos las fotos de la muestra
 * @returns {number|null} el total, o null si no hay fotos o alguna no tiene conteo
 */
export function totalDeGranos(fotos) {
  if (fotos.length === 0 || fotos.some((foto) => foto.granosEncontrados === null || foto.granosEncontrados === undefined)) {
    return null;
  }
  return fotos.reduce((suma, foto) => suma + foto.granosEncontrados, 0);
}

/**
 * Los números de las fotos que hay que repetir, de menor a mayor.
 *
 * @param {import('../api/fotos.js').Foto[]} fotos las fotos de la muestra
 * @returns {number[]} los números
 */
export function numerosPorRepetir(fotos) {
  return fotos.filter((foto) => foto.estado === 'POR_REPETIR').map((foto) => foto.numero).sort((uno, otro) => uno - otro);
}

/**
 * @typedef {object} Devolucion por qué volvió una muestra de la cola sin resultado (D-44)
 * @property {'POCOS_GRANOS'|'SIN_RESPUESTA_DE_LA_IA'|null} motivoDevolucion el motivo de la muestra, o null si la
 *   devolvió una foto o fue otra falla
 * @property {number} repeticiones cuántas fotos pidió repetir RF-09 por granos dudosos, de 0 a 2
 * @property {{ numero: number, motivo: string }[]} fotosPorRepetir las fotos que hay que repetir, con su motivo
 */

/**
 * Por qué volvió la muestra en curso: lo que dijo el último avance consultado o, si no se consultó, como al recargar
 * la página, lo que trajo la muestra del resultado. Un campo que no llegó, como con el clasificador falso, va vacío.
 *
 * @param {object|null} muestra la muestra en curso, del resultado
 * @param {object|null} avance el último avance consultado, si lo hay
 * @returns {Devolucion} la devolución
 */
export function devolucionDeLaMuestra(muestra, avance) {
  const fuente = avance ?? muestra ?? {};
  return {
    motivoDevolucion: fuente.motivoDevolucion ?? null,
    repeticiones: fuente.repeticiones ?? 0,
    fotosPorRepetir: fuente.fotosPorRepetir ?? [],
  };
}

/**
 * El máximo de dudosos del aviso de confianza baja: el de la primera muestra que lo tiene (RF-09 C6).
 *
 * @param {object[]} muestras las muestras del lote
 * @returns {number|null} el porcentaje, o null si ninguna lo tiene o no trae su máximo
 */
export function porcentajeDelAviso(muestras) {
  const conAviso = muestras.find((muestra) => muestra.avisoConfianzaBaja === true);
  return conAviso?.porcentajeMaxDudosos ?? null;
}
