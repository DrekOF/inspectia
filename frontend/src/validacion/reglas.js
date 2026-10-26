/*
 * Nombre: reglas.js
 * Descripción: Reglas de los datos que se revisan antes de enviarlos, iguales a las del backend: un dato en
 *              blanco y los límites de una contraseña nueva (RF-18 C3 y C4), y lo que usan las validaciones de
 *              Yup de las pantallas generadas: recortar y pasar a minúsculas como la API antes de validar, un
 *              dato lleno y el máximo de bytes (RP-07, RP-23).
 * Fecha de creación: 20/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
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

/** Los espacios de Unicode que no parten la línea, como el espacio duro: Java no los cuenta como espacios. */
const NO_PARTEN = new Set([0xa0, 0x2007, 0x202f]);

/**
 * Si un carácter es un espacio para Java (Character.isWhitespace), que son los que String.strip() quita en la API:
 * los separadores de Unicode, salvo los que no parten la línea, y los controles de tabulación y de salto de línea.
 *
 * @param {string} letra el carácter
 * @returns {boolean} si es un espacio
 */
function esEspacioDeJava(letra) {
  const codigo = letra.codePointAt(0);
  if ((codigo >= 0x09 && codigo <= 0x0d) || (codigo >= 0x1c && codigo <= 0x1f) || codigo === 0x2028 || codigo === 0x2029) {
    return true;
  }
  return /\p{Zs}/u.test(letra) && !NO_PARTEN.has(codigo);
}

/**
 * Quita los espacios del inicio y del final de un texto, los mismos que quita la API antes de validar
 * (`recortar` en el JSON). Lo que no es un texto queda igual.
 *
 * @param {unknown} valor el valor del campo
 * @returns {unknown} el texto recortado, o el valor como llegó
 */
export function recortar(valor) {
  if (typeof valor !== 'string') {
    return valor;
  }
  const letras = [...valor];
  let inicio = 0;
  let fin = letras.length;
  while (inicio < fin && esEspacioDeJava(letras[inicio])) {
    inicio += 1;
  }
  while (fin > inicio && esEspacioDeJava(letras[fin - 1])) {
    fin -= 1;
  }
  return letras.slice(inicio, fin).join('');
}

/**
 * Pasa un texto a minúsculas, como la API antes de validar (`minusculas` en el JSON). Lo que no es un texto queda
 * igual.
 *
 * @param {unknown} valor el valor del campo
 * @returns {unknown} el texto en minúsculas, o el valor como llegó
 */
export function minusculas(valor) {
  return typeof valor === 'string' ? valor.toLowerCase() : valor;
}

/**
 * Si un dato está lleno, con la misma regla de `@NotBlank` en la API: un texto que no está en blanco, o cualquier
 * otro valor que no falte.
 *
 * @param {unknown} valor el valor del campo
 * @returns {boolean} si está lleno
 */
export function lleno(valor) {
  if (typeof valor === 'string') {
    return !enBlanco(valor);
  }
  return valor !== undefined && valor !== null;
}

/**
 * La prueba de Yup de un máximo de bytes en UTF-8, como `@MaximoBytes` en la API. Si falta el dato, la deja pasar:
 * eso lo revisa obligatorio.
 *
 * @param {number} maximo los bytes que caben
 * @returns {(valor: unknown) => boolean} la prueba
 */
export function cabeEnBytes(maximo) {
  return (valor) => valor === undefined || valor === null || bytesUtf8(String(valor)) <= maximo;
}
