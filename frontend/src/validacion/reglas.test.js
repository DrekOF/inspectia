/*
 * Nombre: reglas.test.js
 * Descripción: Pruebas de las funciones que usan las validaciones de Yup generadas (RP-07, RP-23): recortar y
 *              pasar a minúsculas como lo hace la API antes de validar, saber si un dato está lleno como lo
 *              sabe @NotBlank, si uno opcional llega sin estar en blanco como lo revisa @NoEnBlanco, el texto
 *              vacío como ausente, si un texto cabe en sus bytes como lo revisa @MaximoBytes, leer un número
 *              escrito con coma o con punto y contar sus decimales como @MaximoDecimales (RF-13).
 * Fecha de creación: 26/10/2026
 * Actualización: 28/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
import { describe, expect, it } from 'vitest';
import {
  cabeEnBytes, lleno, maximoDecimales, minusculas, noEnBlanco, numeroDecimal, recortar, vacioComoAusente,
} from './reglas.js';

describe('recortar', () => {
  it.each([
    ['espacios', '  Neiva  ', 'Neiva'],
    ['tabulación y salto de línea', '\tNeiva\n', 'Neiva'],
    ['espacios de otros anchos', '\u2003Neiva\u3000', 'Neiva'],
    ['espacios de adentro', ' San Vicente de Chucurí ', 'San Vicente de Chucurí'],
    ['espacio duro', '\u00a0Neiva\u00a0', '\u00a0Neiva\u00a0'],
    ['espacio duro angosto', '\u202fNeiva\u2007', '\u202fNeiva\u2007'],
    ['retorno de carro y avance de página', '\rNeiva\u000c', 'Neiva'],
    ['separadores de archivo y de unidad', '\u001cNeiva\u001f', 'Neiva'],
    ['separadores de línea y de párrafo', '\u2028Neiva\u2029', 'Neiva'],
    ['controles que no son espacios', '\u0008Neiva\u001b', '\u0008Neiva\u001b'],
  ])('RP-07 · recortar quita los mismos espacios del inicio y del final que String.strip() en la API: %s',
    (_, texto, recortado) => {
      expect(recortar(texto)).toBe(recortado);
    });

  it('RP-07 · recortar deja igual lo que no es un texto', () => {
    expect(recortar(undefined)).toBeUndefined();
    expect(recortar(null)).toBeNull();
  });
});

describe('minúsculas', () => {
  it('RP-07 · minusculas pasa un texto a minúsculas, también las letras con tilde y la eñe, como la API', () => {
    expect(minusculas('ANA.PÉREZ@CACAOSDELHUILA.CO')).toBe('ana.pérez@cacaosdelhuila.co');
    expect(minusculas('MUÑOZ')).toBe('muñoz');
  });

  it('RP-07 · minusculas deja igual lo que no es un texto', () => {
    expect(minusculas(undefined)).toBeUndefined();
  });
});

describe('lleno', () => {
  it.each([
    ['vacío', '', false],
    ['solo espacios y saltos de línea', '  \t\n', false],
    ['sin valor', undefined, false],
    ['nulo', null, false],
    ['con letras', 'Neiva', true],
    ['un espacio duro', '\u00a0', true],
    ['un número, aunque sea cero', 0, true],
  ])('RP-07 · un dato está lleno con la misma regla de @NotBlank en la API: %s', (_, valor, estaLleno) => {
    expect(lleno(valor)).toBe(estaLleno);
  });
});

describe('no en blanco', () => {
  it.each([
    ['sin valor: es opcional', undefined, true],
    ['nulo', null, true],
    ['vacío', '', false],
    ['solo espacios', ' '.repeat(8), false],
    ['con letras', 'Clave-Segura-2026', true],
  ])('RP-07 · un dato opcional que llega no puede estar en blanco, como lo revisa @NoEnBlanco en la API: %s',
    (_, valor, cumple) => {
      expect(noEnBlanco(valor)).toBe(cumple);
    });
});

describe('vacío como ausente', () => {
  it('RP-07 · un texto vacío cuenta como ausente, como el dato que no llega a la API; lo demás queda igual', () => {
    expect(vacioComoAusente('')).toBeUndefined();
    expect(vacioComoAusente('  ')).toBe('  ');
    expect(vacioComoAusente('USUARIO_FINAL')).toBe('USUARIO_FINAL');
    expect(vacioComoAusente(null)).toBeNull();
  });
});

describe('cabe en bytes', () => {
  it.each([
    ['36 eñes, 72 bytes', 'ñ'.repeat(36), true],
    ['37 eñes, 74 bytes', 'ñ'.repeat(37), false],
    ['72 letras sin tilde', 'a'.repeat(72), true],
    ['sin valor: lo revisa obligatorio', undefined, true],
  ])('RP-07 · cabeEnBytes cuenta los bytes en UTF-8, como @MaximoBytes en la API: %s', (_, valor, cabe) => {
    expect(cabeEnBytes(72)(valor)).toBe(cabe);
  });

  it('RP-07 · cabeEnBytes deja pasar el dato que falta aunque el máximo sea pequeño: lo revisa obligatorio', () => {
    expect(cabeEnBytes(2)(undefined)).toBe(true);
    expect(cabeEnBytes(2)(null)).toBe(true);
  });
});

describe('número decimal', () => {
  /**
   * Lee un texto como lo hace Yup con numeroDecimal: Yup le pasa primero su propio número, que aquí no importa, y
   * después el texto escrito.
   *
   * @param {string} texto lo escrito en el campo
   * @returns {number|undefined} el número, NaN si no lo es, o undefined si el campo está vacío
   */
  const leer = (texto) => numeroDecimal(Number.NaN, texto);

  it.each([
    ['con coma', '90,5', 90.5],
    ['con punto', '90.5', 90.5],
    ['sin decimales', '96', 96],
    ['con espacios alrededor', ' 90,5 ', 90.5],
    ['negativo', '-0,55', -0.55],
    ['con un cero al final', '90,50', 90.5],
  ])('RP-07 · numeroDecimal lee un número escrito con coma o con punto: %s', (_, texto, numero) => {
    expect(leer(texto)).toBe(numero);
  });

  it.each([['vacío', ''], ['solo espacios', '   ']])(
    'RP-07 · numeroDecimal deja sin valor un campo %s: es el dato que falta, que revisa obligatorio', (_, texto) => {
      expect(leer(texto)).toBeUndefined();
    });

  it.each([
    ['letras', 'noventa'],
    ['dos separadores', '1.000,5'],
    ['un separador sin decimales', '90,'],
    ['un separador sin enteros', ',5'],
    ['un espacio en medio', '9 0'],
    ['notación científica', '1e3'],
    ['un signo más', '+5'],
  ])('RP-07 · numeroDecimal no lee como número lo que no se escribe así en el formulario: %s', (_, texto) => {
    expect(leer(texto)).toBeNaN();
  });

  it('RP-07 · numeroDecimal deja un número como llegó, y sin valor lo que no llegó', () => {
    expect(numeroDecimal(90.5, 90.5)).toBe(90.5);
    expect(numeroDecimal(undefined, undefined)).toBeUndefined();
    expect(numeroDecimal(null, null)).toBeUndefined();
  });
});

describe('máximo de decimales', () => {
  it.each([
    [1, 90.5, true],
    [1, 90, true],
    [1, Number('90.50'), true],
    [1, 0.05, false],
    [1, 100.55, false],
    [2, 0.05, true],
    [2, 0.055, false],
    [1, 1e-7, false],
  ])('RP-07 · maximoDecimales(%i) cuenta los decimales sin los ceros del final, como @MaximoDecimales en la API: %s',
    (escala, valor, cumple) => {
      expect(maximoDecimales(escala)(valor)).toBe(cumple);
    });

  it('RP-07 · maximoDecimales deja pasar el dato que falta y lo que no es un número: los revisan obligatorio y el tipo', () => {
    expect(maximoDecimales(1)(undefined)).toBe(true);
    expect(maximoDecimales(1)(null)).toBe(true);
    expect(maximoDecimales(1)(Number.NaN)).toBe(true);
  });
});
