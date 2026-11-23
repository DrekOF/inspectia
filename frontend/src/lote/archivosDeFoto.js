/*
 * Nombre: archivosDeFoto.js
 * Descripción: La revisión de una foto en el navegador, antes de pedir el enlace para subirla (RF-05 C3): JPG o PNG,
 *              reconocidos por sus primeros bytes como los reconoce el backend, y de hasta 10 MB.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */

/** Lo más que pesa una foto: 10 MB, 10.485.760 bytes (RF-05, foto.json). */
export const MAXIMO_DE_BYTES = 10_485_760;

/** Lo que ofrece el selector de archivos: JPG y PNG, por su tipo y por su extensión (RF-05 C1). */
export const ACEPTA = 'image/jpeg,image/png,.jpg,.jpeg,.png';

/** Los formatos que acepta la app, con sus primeros bytes, los mismos que revisa el backend (FormatoDeFoto). */
const FORMATOS = [
  { tipo: 'image/jpeg', comienzo: [0xFF, 0xD8, 0xFF] },
  { tipo: 'image/png', comienzo: [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A] },
];

/** Cuántos primeros bytes hacen falta para reconocer cualquier formato. */
const BYTES_DEL_COMIENZO = Math.max(...FORMATOS.map(({ comienzo }) => comienzo.length));

/**
 * Revisa una foto antes de subirla. El formato sale de sus primeros bytes y no del nombre ni del tipo que dice el
 * navegador, que algunos Android dejan vacío. Va antes que el tamaño: a un archivo que no es una foto no le sirve
 * pesar menos.
 *
 * @param {Blob} archivo el archivo elegido
 * @returns {Promise<{ tipo: string } | { error: string }>} el tipo que se firma, como image/jpeg, o la clave del
 *   mensaje que dice qué hacer
 */
export async function revisarArchivo(archivo) {
  const primeros = new Uint8Array(await archivo.slice(0, BYTES_DEL_COMIENZO).arrayBuffer());
  const formato = FORMATOS.find(({ comienzo }) => comienzo.every((byte, posicion) => primeros[posicion] === byte));
  if (!formato) {
    return { error: 'foto.formato.no_permitido' };
  }
  if (archivo.size > MAXIMO_DE_BYTES) {
    return { error: 'foto.tamano_bytes.demasiado_grande' };
  }
  return { tipo: formato.tipo };
}
