/*
 * Nombre: imagenes.js (e2e)
 * Descripción: Archivos de prueba hechos en la prueba, sin nada del dataset ni fotos pesadas en el repositorio
 *              (RNF-08): un PNG de un color, de un tamaño exacto si hace falta, un PDF de una línea y, en el navegador,
 *              un JPEG dibujado en un canvas. Los usan las pruebas de las fotos con Playwright y con Selenium (RF-05).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { crc32, deflateSync } from 'node:zlib';

/** Los primeros 8 bytes de todo PNG. */
const FIRMA_PNG = Buffer.from([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]);

/** Lo que mide un trozo de PNG además de sus datos: su largo, su tipo y su CRC. */
const MARCO_DEL_TROZO = 12;

/**
 * Un trozo de un PNG: su largo, su tipo, sus datos y el CRC del tipo y los datos.
 *
 * @param {string} tipo el tipo, como IHDR
 * @param {Buffer} datos los datos
 * @returns {Buffer} el trozo
 */
function trozo(tipo, datos) {
  const largo = Buffer.alloc(4);
  largo.writeUInt32BE(datos.length);
  const tipoYDatos = Buffer.concat([Buffer.from(tipo, 'latin1'), datos]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(tipoYDatos));
  return Buffer.concat([largo, tipoYDatos, crc]);
}

/**
 * Un PNG de un color. Con bytes, mide exactamente eso: lleva un trozo de texto de relleno, que los visores ignoran,
 * para las pruebas del límite de 10 MB (RF-05 C3).
 *
 * @param {{ ancho?: number, alto?: number, color?: number[], bytes?: number }} [opciones] su ancho y su alto en
 *   píxeles, su color (rojo, verde y azul) y cuántos bytes mide
 * @returns {Buffer} el PNG
 */
export function png({ ancho = 96, alto = 72, color = [107, 63, 34], bytes } = {}) {
  const cabecera = Buffer.alloc(13);
  cabecera.writeUInt32BE(ancho, 0);
  cabecera.writeUInt32BE(alto, 4);
  // 8 bits por canal, en color (RGB), sin entrelazado.
  cabecera.set([8, 2, 0, 0, 0], 8);
  // Cada fila empieza con el filtro 0 (ninguno).
  const fila = Buffer.alloc(1 + ancho * 3);
  for (let x = 0; x < ancho; x += 1) {
    fila.set(color, 1 + x * 3);
  }
  const pixeles = deflateSync(Buffer.concat(Array.from({ length: alto }, () => fila)));
  const inicio = Buffer.concat([FIRMA_PNG, trozo('IHDR', cabecera), trozo('IDAT', pixeles)]);
  const fin = trozo('IEND', Buffer.alloc(0));
  if (bytes === undefined) {
    return Buffer.concat([inicio, fin]);
  }
  const clave = Buffer.from('Comment\0', 'latin1');
  const relleno = bytes - inicio.length - fin.length - MARCO_DEL_TROZO - clave.length;
  if (relleno < 0) {
    throw new Error(`Un PNG de ${ancho} × ${alto} no cabe en ${bytes} bytes`);
  }
  return Buffer.concat([inicio, trozo('tEXt', Buffer.concat([clave, Buffer.alloc(relleno, 'x')])), fin]);
}

/** Un PDF de una línea: no es una foto (RF-05 C3). */
export const PDF = Buffer.from('%PDF-1.4\n1 0 obj << /Type /Catalog >> endobj\ntrailer << /Root 1 0 R >>\n%%EOF\n',
  'latin1');

/**
 * Un JPEG chico dibujado en el navegador con un canvas, como lo entregaría la cámara de un celular.
 *
 * @param {import('@playwright/test').Page} page la página, en cualquier pantalla de la app
 * @returns {Promise<Buffer>} el JPEG
 */
export async function jpegDelNavegador(page) {
  const bytes = await page.evaluate(async () => {
    const lienzo = document.createElement('canvas');
    lienzo.width = 96;
    lienzo.height = 72;
    const contexto = lienzo.getContext('2d');
    contexto.fillStyle = 'rgb(107, 63, 34)';
    contexto.fillRect(0, 0, 96, 72);
    contexto.fillStyle = 'rgb(250, 246, 241)';
    contexto.fillRect(24, 18, 48, 36);
    const blob = await new Promise((resolver) => { lienzo.toBlob(resolver, 'image/jpeg', 0.9); });
    return Array.from(new Uint8Array(await blob.arrayBuffer()));
  });
  return Buffer.from(bytes);
}
