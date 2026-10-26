/*
 * Nombre: reglas.test.js
 * Descripción: Pruebas de las funciones que usan las validaciones de Yup generadas (RP-07, RP-23): recortar y
 *              pasar a minúsculas como lo hace la API antes de validar, saber si un dato está lleno como lo
 *              sabe @NotBlank y si un texto cabe en sus bytes como lo revisa @MaximoBytes.
 * Fecha de creación: 26/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { describe, expect, it } from 'vitest';
import { cabeEnBytes, lleno, minusculas, recortar } from './reglas.js';

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
