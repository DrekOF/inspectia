/*
 * Nombre: reglas.js
 * Descripción: Reglas de los datos que se revisan antes de enviarlos, iguales a las del backend: un dato en
 *              blanco y los límites de una contraseña nueva (RF-18 C3 y C4).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */

/**
 * Límites de una contraseña nueva, los mismos del backend: de 8 a 64 caracteres y como máximo 72 bytes,
 * porque BCrypt solo usa 72 (RF-18).
 */
export const CONTRASENA = { minimo: 8, maximo: 64, maximoBytes: 72 };

const codificador = new TextEncoder();

/**
 * Los bytes que ocupa un texto en UTF-8, como los cuenta BCrypt: 1 por cada letra sin tilde y 2 por cada
 * letra con tilde o eñe.
 *
 * @param {string} texto el texto
 * @returns {number} los bytes
 */
export function bytesUtf8(texto) {
  return codificador.encode(texto).length;
}

/**
 * Si un texto está en blanco: vacío o solo con espacios y saltos de línea. Es la misma regla de `@NotBlank`
 * en el backend, que quita los caracteres hasta el espacio (U+0020).
 *
 * @param {string} texto el texto
 * @returns {boolean} si está en blanco
 */
export function enBlanco(texto) {
  return [...texto].every((letra) => letra.codePointAt(0) <= 0x20);
}

/**
 * Qué reglas cumple una contraseña nueva, para mostrarlas mientras se escribe (RF-18 C3 y C4).
 *
 * @param {string} contrasena la contraseña nueva
 * @returns {{ largo: boolean, bytes: boolean, espacios: boolean }} si cumple el largo, los bytes y si tiene
 *   algo más que espacios
 */
export function reglasDeContrasena(contrasena) {
  return {
    largo: contrasena.length >= CONTRASENA.minimo && contrasena.length <= CONTRASENA.maximo,
    bytes: bytesUtf8(contrasena) <= CONTRASENA.maximoBytes,
    espacios: !enBlanco(contrasena),
  };
}
