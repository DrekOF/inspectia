/*
 * Nombre: tema.test.js
 * Descripción: Pruebas del tema de Material UI: el texto de ejemplo de los campos va en el color del texto
 *              secundario y sin transparencia, el que tokens.test.js mide con 4,5:1 sobre los campos (RNF-06 C4).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { describe, expect, it } from 'vitest';
import { tema } from './tema.js';

describe('Tema', () => {
  it('RNF-06 · C4 · el texto de ejemplo de los campos va en el color del texto secundario y sin transparencia, en cada navegador', () => {
    const estilo = tema.components.MuiOutlinedInput.styleOverrides.input({ theme: tema });

    for (const selector of ['&::placeholder', '&::-webkit-input-placeholder', '&::-moz-placeholder']) {
      expect(estilo[selector]).toEqual({ color: tema.vars.palette.text.secondary, opacity: 1 });
    }
  });
});
