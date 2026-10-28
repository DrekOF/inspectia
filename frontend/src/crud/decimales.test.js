/*
 * Nombre: decimales.test.js
 * Descripción: Pruebas de cómo se muestra un decimal en un formulario (RP-11): con la escala de su columna y el
 *              separador del idioma de la app, coma en español y punto en inglés, sin separar los miles.
 * Fecha de creación: 28/10/2026
 * Actualización: 28/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { describe, expect, it } from 'vitest';
import { textoDelDecimal } from './decimales.js';

describe('texto de un decimal', () => {
  it.each([
    [96, 1, 'es', '96,0'],
    [95.9, 1, 'en', '95.9'],
    [0, 1, 'es', '0,0'],
    [90.5, 2, 'es', '90,50'],
    [1000.5, 1, 'es', '1000,5'],
    [1000.5, 1, 'en', '1000.5'],
  ])('RP-11 · un decimal se muestra con su escala y el separador del idioma, sin separar los miles: %s con escala %i en %s',
    (valor, escala, idioma, texto) => {
      expect(textoDelDecimal(valor, escala, idioma)).toBe(texto);
    });

  it('RP-11 · un decimal que no llegó se muestra vacío', () => {
    expect(textoDelDecimal(null, 1, 'es')).toBe('');
    expect(textoDelDecimal(undefined, 1, 'en')).toBe('');
  });
});
