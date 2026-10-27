/*
 * Nombre: reglas.js
 * Descripción: Reglas de los datos que se revisan antes de enviarlos, iguales a las del backend: un dato en
 *              blanco y las reglas de una contraseña nueva, con los límites del JSON de usuario (RF-18 C3 y
 *              C4), y lo que usan las validaciones de Yup de las pantallas generadas: recortar y pasar a
 *              minúsculas como la API antes de validar, un dato lleno, uno opcional que no llega en blanco, el
 *              texto vacío como ausente y el máximo de bytes (RP-07, RP-23).
 * Fecha de creación: 20/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */

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
 * @param {{ minimo: number, maximo: number, maximoBytes: number }} limites sus límites, los del JSON de usuario
 *   (LIMITES.contrasena, generado): de 8 a 64 caracteres y como máximo 72 bytes, porque BCrypt solo usa 72
 * @returns {{ largo: boolean, bytes: boolean, espacios: boolean }} si cumple el largo, los bytes y si tiene
 *   algo más que espacios
 */
export function reglasDeContrasena(contrasena, limites) {
  return {
    largo: contrasena.length >= limites.minimo && contrasena.length <= limites.maximo,
    bytes: bytesUtf8(contrasena) <= limites.maximoBytes,
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
 * Si un dato opcional que llega no está en blanco, como `@NoEnBlanco` en la API, que responde lo mismo que
 * `@NotBlank`. Si falta, lo deja pasar: es opcional.
 *
 * @param {unknown} valor el valor del campo
 * @returns {boolean} si falta o no está en blanco
 */
export function noEnBlanco(valor) {
  return valor === undefined || valor === null || lleno(valor);
}

/**
 * Un texto vacío cuenta como ausente, como el dato que no llega a la API. Así no se revisa un dato opcional vacío,
 * como la contraseña al editar, y una lista obligatoria vacía da el error de obligatorio y no el de sus valores:
 * Yup revisa oneOf antes que las demás pruebas, y lo ausente lo deja pasar.
 *
 * @param {unknown} valor el valor del campo
 * @returns {unknown} undefined si es un texto vacío, o el valor como llegó
 */
export function vacioComoAusente(valor) {
  return valor === '' ? undefined : valor;
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
