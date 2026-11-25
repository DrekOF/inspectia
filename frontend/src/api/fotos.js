/*
 * Nombre: fotos.js (api)
 * Descripción: Las peticiones de las fotos de la muestra de un lote (RF-05, RF-14 C5): la lista de las fotos, el
 *              enlace para subir una, que dice para qué muestra se pide (D-25), la subida al enlace y la confirmación
 *              (D-41). La subida no usa el cliente de la API: va directo al enlace firmado y sin el token de la sesión,
 *              como irá a S3.
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { ErrorDeApi } from './cliente.js';

/**
 * @typedef {object} EnlaceTemporal un enlace firmado que vence, para subir o ver una foto (D-41)
 * @property {string} url la dirección: relativa a la página con la carpeta local, o la de S3
 * @property {string} [metodo] el método HTTP; para subir, PUT
 * @property {Record<string, string>} [encabezados] los encabezados que pide la firma, como Content-Type
 * @property {string} vence cuándo vence (ISO 8601)
 */

/**
 * @typedef {object} Foto una foto de la muestra, como la responde el backend
 * @property {number} numero su número, de 1 a 3
 * @property {'JPG'|'PNG'} formato su formato
 * @property {number} tamanoBytes cuánto pesa
 * @property {'CARGADA'|'APROBADA'|'POR_REPETIR'} estado su estado; antes de la revisión de la IA, CARGADA
 * @property {EnlaceTemporal} vista el enlace para verla
 */

/**
 * @typedef {object} FotosDeLaMuestra la muestra en curso de un lote y sus fotos
 * @property {number|null} muestra el número de la muestra, o null si todavía no tiene fotos
 * @property {string|null} estado el estado de la muestra, o null si todavía no tiene fotos
 * @property {Foto[]} fotos las fotos ya subidas
 */

/**
 * Las fotos de la muestra en curso de un lote (GET /api/v1/lotes/{id}/fotos), con enlaces nuevos para verlas.
 *
 * @param {import('./cliente.js').Pedir} pedir la función del cliente de la API
 * @param {string} loteId el lote
 * @returns {Promise<FotosDeLaMuestra>} las fotos
 */
export function fotosDelLote(pedir, loteId) {
  return pedir('GET', `/lotes/${encodeURIComponent(loteId)}/fotos`);
}

/**
 * Pide el enlace para subir una foto (POST /api/v1/lotes/{id}/fotos/{numero}/subida). El backend revisa el lote, el
 * número, la muestra, el tipo y el tamaño antes de firmarlo. La muestra es la que la página espera que reciba la foto:
 * si el lote cambió y ahora la recibe otra, como cuando la muestra 1 terminó mientras la página seguía abierta, el
 * backend responde 409 y no da el enlace (D-25). La llave del enlace lleva esa muestra, y la confirmación la revisa.
 *
 * @param {import('./cliente.js').Pedir} pedir la función del cliente de la API
 * @param {string} loteId el lote
 * @param {number} numero el número de la foto, de 1 a 3
 * @param {{ tipo: string, tamanoBytes: number, muestra: number }} archivo el tipo de la foto, como image/jpeg, cuánto
 *   pesa y el número de la muestra que la recibe, 1 o 2
 * @returns {Promise<EnlaceTemporal & { llave: string }>} el enlace, con la llave con la que se confirma
 */
export function pedirSubida(pedir, loteId, numero, { tipo, tamanoBytes, muestra }) {
  return pedir('POST', `/lotes/${encodeURIComponent(loteId)}/fotos/${numero}/subida`, { tipo, tamanoBytes, muestra });
}

/**
 * Confirma una foto ya subida (PUT /api/v1/lotes/{id}/fotos/{numero}): el backend revisa el archivo y la guarda;
 * si reemplaza otra, la anterior se borra (RF-05 C5). Si la muestra de la llave ya no es la que recibe las fotos,
 * responde 409 y no guarda nada (D-25).
 *
 * @param {import('./cliente.js').Pedir} pedir la función del cliente de la API
 * @param {string} loteId el lote
 * @param {number} numero el número de la foto, de 1 a 3
 * @param {string} llave la llave del enlace con el que se subió
 * @returns {Promise<Foto>} la foto guardada
 */
export function confirmarFoto(pedir, loteId, numero, llave) {
  return pedir('PUT', `/lotes/${encodeURIComponent(loteId)}/fotos/${numero}`, { llave });
}

/**
 * El mensaje de una respuesta con error, si viene en JSON (application/problem+json). S3 responde en XML: entonces
 * no hay mensaje.
 *
 * @param {Response} respuesta la respuesta
 * @returns {Promise<string|null>} el mensaje
 */
async function detalleDe(respuesta) {
  try {
    return JSON.parse(await respuesta.text())?.detail ?? null;
  } catch {
    return null;
  }
}

/**
 * Sube una foto a su enlace firmado, con el método y los encabezados de la firma (D-41): el Content-Type va siempre
 * el firmado, aunque el navegador diga otro tipo o ninguno. No lleva el token de la sesión, que S3 rechazaría junto a
 * un enlace firmado, y un 401 o un 403 del enlace no cierra la sesión. Sí lleva el idioma de la app, para que los
 * mensajes del backend lleguen en él.
 *
 * @param {EnlaceTemporal} enlace el enlace de subida
 * @param {Blob} archivo la foto
 * @param {string} [idioma] el idioma de la app, como «es», para Accept-Language
 * @returns {Promise<void>} se cumple cuando la foto llegó
 * @throws {ErrorDeApi} si no llegó: con estado 0 si no hubo conexión
 */
export async function subirArchivo(enlace, archivo, idioma) {
  let respuesta;
  try {
    respuesta = await fetch(new URL(enlace.url, window.location.origin), {
      method: enlace.metodo ?? 'PUT',
      headers: { ...(idioma ? { 'Accept-Language': idioma } : {}), ...enlace.encabezados },
      body: archivo,
    });
  } catch {
    throw new ErrorDeApi({ estado: 0 });
  }
  if (!respuesta.ok) {
    throw new ErrorDeApi({ estado: respuesta.status, detalle: await detalleDe(respuesta) });
  }
}
