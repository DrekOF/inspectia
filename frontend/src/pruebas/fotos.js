/*
 * Nombre: fotos.js (pruebas)
 * Descripción: Fotos de prueba y respuestas simuladas de la API de las fotos de un lote, también las de la segunda
 *              muestra, y de sus enlaces firmados, para las pruebas de la página de las fotos (RF-05, RF-14 C5 y C6,
 *              D-25, D-41). Como el backend, el enlace se pide para una muestra, su llave la lleva, y el enlace y la
 *              confirmación responden 409 si esa muestra ya no es la que recibe las fotos.
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { File as FileDeNode } from 'node:buffer';
import { http, HttpResponse } from 'msw';
import { problema, servidor } from './servidor.js';

/** El mensaje del backend para un enlace con otra firma, como el de un Content-Type distinto del firmado (D-41). */
export const ENLACE_NO_VALIDO = 'El enlace de la foto venció o no es válido: vuelve a intentarlo.';

/**
 * El mensaje del backend cuando la muestra que espera la página ya no es la que recibe las fotos (foto.muestra_cambio,
 * D-25).
 */
export const MUESTRA_CAMBIO =
  'El lote cambió mientras tenías abierta esta página: recárgala para ver qué muestra recibe las fotos.';

/** La muestra de un lote que todavía no tiene fotos. */
export const SIN_FOTOS = { muestra: null, estado: null, fotos: [] };

/** Los primeros bytes de cada tipo de archivo de prueba. */
const COMIENZOS = {
  'image/jpeg': [0xFF, 0xD8, 0xFF, 0xE0],
  'image/png': [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A],
  'application/pdf': [0x25, 0x50, 0x44, 0x46, 0x2D],
};

/**
 * Un archivo de prueba: empieza con los bytes de su tipo y mide lo que se pide. Nunca sale del dataset (RNF-08).
 * Es un File de Node y no el de jsdom, porque el fetch de Node no acepta el de jsdom como cuerpo; en el navegador,
 * el File que entrega el selector sí sirve.
 *
 * @param {string} nombre el nombre del archivo, como granos.jpg
 * @param {string} tipo el tipo que dice el navegador, como image/jpeg
 * @param {number} [tamano] cuántos bytes mide
 * @param {string} [comoSi] el tipo de sus bytes, si es otro que el que dice: para un archivo que miente
 * @returns {File} el archivo
 */
export function archivo(nombre, tipo, tamano = 64, comoSi = tipo) {
  const bytes = new Uint8Array(tamano);
  bytes.set((COMIENZOS[comoSi] ?? []).slice(0, tamano));
  return /** @type {File} */ (/** @type {unknown} */ (new FileDeNode([bytes], nombre, { type: tipo })));
}

/**
 * Un archivo de prueba que tarda en dejar leer sus primeros bytes, como uno de OneDrive que todavía está en la nube,
 * hasta que la prueba lo suelta. Se sube como cualquier otro.
 *
 * @param {string} nombre el nombre del archivo
 * @param {string} tipo su tipo y el de sus bytes
 * @param {number} [tamano] cuántos bytes mide
 * @returns {{ archivo: File, soltar: () => void }} el archivo y cómo dejar que se lea
 */
export function archivoLento(nombre, tipo, tamano = 64) {
  let soltar;
  const lectura = new Promise((resolver) => { soltar = resolver; });
  const lento = archivo(nombre, tipo, tamano);
  const cortar = lento.slice.bind(lento);
  lento.slice = (...limites) => {
    const parte = cortar(...limites);
    return { arrayBuffer: () => lectura.then(() => parte.arrayBuffer()) };
  };
  return { archivo: lento, soltar };
}

/**
 * Una foto cargada, como la responde la API.
 *
 * @param {number} numero su número, de 1 a 3
 * @param {{ formato?: string, url?: string }} [opciones] su formato y su enlace de vista
 * @returns {object} la foto
 */
export function fotoCargada(numero, { formato = 'JPG', url = `/api/v1/archivos/e/l/foto-${numero}.jpg?vence=1&firma=v` } = {}) {
  return { numero, formato, tamanoBytes: 2048, estado: 'CARGADA', vista: { url, vence: '2026-11-23T15:15:00Z' } };
}

/**
 * Responde las fotos de la muestra en curso de un lote y anota cuántas veces se pidieron. Sin fotos responde que el
 * lote no tiene muestra en curso, como el de la segunda muestra que todavía no se crea (D-25).
 *
 * @param {string} loteId el lote
 * @param {object[]|((vez: number) => object[])} fotos las fotos, como las de {@link fotoCargada}, o las de cada vez
 *   que se piden, desde 1
 * @param {{ muestra?: number, estado?: string }} [opciones] el número y el estado de la muestra en curso, como la 2
 *   pendiente de la segunda muestra
 * @returns {{ veces: number }} cuántas veces se pidieron
 */
export function conFotos(loteId, fotos, { muestra = 1, estado = 'PENDIENTE' } = {}) {
  const pedidas = { veces: 0 };
  servidor.use(http.get(`/api/v1/lotes/${loteId}/fotos`, () => {
    pedidas.veces += 1;
    const deEstaVez = typeof fotos === 'function' ? fotos(pedidas.veces) : fotos;
    return HttpResponse.json(deEstaVez.length === 0 ? SIN_FOTOS : { muestra, estado, fotos: deEstaVez });
  }));
  return pedidas;
}

/**
 * El número de la muestra que dice una llave de foto, como la del backend: e/l/m2-….jpg dice la 2.
 *
 * @param {string} llave la llave
 * @returns {number|null} el número de la muestra, o null si la llave no lo dice
 */
function muestraDeLaLlave(llave) {
  const partes = /\/m([12])-[^/]+$/.exec(llave ?? '');
  return partes ? Number(partes[1]) : null;
}

/**
 * El error de la muestra que pide un enlace, como lo valida el backend: obligatoria y de 1 a 2.
 *
 * @param {unknown} muestra la muestra que llegó en el cuerpo
 * @returns {string|null} el mensaje del error, o null si sirve
 */
function errorDeLaMuestra(muestra) {
  if (muestra === undefined || muestra === null) {
    return 'Este dato es obligatorio.';
  }
  if (!Number.isInteger(muestra) || muestra < 1) {
    return 'Debe ser mayor o igual a 1.';
  }
  return muestra > 2 ? 'Debe ser menor o igual a 2.' : null;
}

/**
 * Responde la subida de las fotos de un lote, como el backend con la carpeta local (D-41): el enlace, el PUT al
 * enlace y la confirmación. Como el backend, el enlace exige la muestra que espera la página, 1 o 2 (si falta o es
 * otra cosa, 400 en «muestra»), y su llave la lleva; si no es la muestra que recibe las fotos, el enlace y la
 * confirmación responden 409 (foto.muestra_cambio, D-25). El PUT responde 403 si su Content-Type no es el que se
 * firmó. Anota lo que llegó a cada uno.
 *
 * @param {string} loteId el lote
 * @param {{ base?: string, espera?: Promise<unknown>, muestra?: number }} [opciones] dónde están los enlaces:
 *   /api/v1/archivos, o el dominio de S3; lo que espera el PUT antes de responder, para ver la página mientras sube; y
 *   la muestra que recibe las fotos, 1 si no se dice
 * @returns {{ pedidas: object[], subidas: object[], confirmadas: object[], muestra: number }} lo que llegó: lo que se
 *   pidió, cada PUT, con su Authorization, su Content-Type, su Accept-Language, sus bytes y su dirección, y cada
 *   confirmación; y la muestra que recibe las fotos, que la prueba puede cambiar, como si otro usuario cambiara el lote
 */
export function conSubidas(loteId, { base = '/api/v1/archivos', espera, muestra = 1 } = {}) {
  const anotado = { pedidas: [], subidas: [], confirmadas: [], muestra };
  const firmados = new Map();
  servidor.use(
    http.post(`/api/v1/lotes/${loteId}/fotos/:numero/subida`, async ({ request, params }) => {
      const cuerpo = await request.json();
      anotado.pedidas.push({ numero: Number(params.numero), ...cuerpo });
      const rechazo = errorDeLaMuestra(cuerpo.muestra);
      if (rechazo) {
        return problema(400, 'Revisa los datos marcados.', { errores: [{ campo: 'muestra', mensaje: rechazo }] });
      }
      if (cuerpo.muestra !== anotado.muestra) {
        return problema(409, MUESTRA_CAMBIO);
      }
      const extension = cuerpo.tipo === 'image/png' ? 'png' : 'jpg';
      const llave = `e/${loteId}/m${cuerpo.muestra}-foto-${params.numero}-${anotado.pedidas.length}.${extension}`;
      firmados.set(llave, cuerpo.tipo);
      return HttpResponse.json({
        llave, url: `${base}/${llave}?vence=1&firma=s`, metodo: 'PUT', encabezados: { 'Content-Type': cuerpo.tipo },
        vence: '2026-11-23T15:10:00Z',
      }, { status: 201 });
    }),
    http.put(`${base}/*`, async ({ request }) => {
      await espera;
      const tipo = request.headers.get('Content-Type');
      anotado.subidas.push({
        autorizacion: request.headers.get('Authorization'), tipo, idioma: request.headers.get('Accept-Language'),
        bytes: (await request.arrayBuffer()).byteLength, url: request.url,
      });
      const llave = /\/(e\/.+)$/.exec(new URL(request.url).pathname)?.[1];
      if (firmados.get(llave) !== tipo) {
        return problema(403, ENLACE_NO_VALIDO);
      }
      return new HttpResponse(null, { status: 200 });
    }),
    http.put(`/api/v1/lotes/${loteId}/fotos/:numero`, async ({ request, params }) => {
      const { llave } = await request.json();
      const numero = Number(params.numero);
      anotado.confirmadas.push({ numero, llave });
      if (muestraDeLaLlave(llave) !== anotado.muestra) {
        return problema(409, MUESTRA_CAMBIO);
      }
      return HttpResponse.json(fotoCargada(numero, {
        formato: llave.endsWith('.png') ? 'PNG' : 'JPG', url: `${base}/${llave}?vence=2&firma=v`,
      }));
    }),
  );
  return anotado;
}
