/*
 * Nombre: granos.test.js (lote)
 * Descripción: Pruebas de los granos de una foto marcada (RF-11): el orden de lectura en que se numeran y se recorren
 *              con el teclado, también con filas que no están alineadas del todo; la posición de cada marca en
 *              porcentaje de la foto, recortada a sus bordes, y a qué grano lleva cada tecla (RNF-06 C4).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { describe, expect, it } from 'vitest';
import { enOrdenDeLectura, indiceSiguiente, posicionEnPorcentaje } from './granos.js';

/**
 * Un grano de prueba, con un nombre para reconocerlo.
 *
 * @param {string} nombre su nombre
 * @param {number} x el borde izquierdo
 * @param {number} y el borde de arriba
 * @param {number} [lado] cuánto mide de ancho y de alto
 * @returns {object} el grano
 */
function grano(nombre, x, y, lado = 40) {
  return { nombre, x, y, ancho: lado, alto: lado, clase: 'SANO', dudoso: false };
}

describe('Granos de una foto marcada', () => {
  it('RF-11 · C4 · los granos van en orden de lectura: de arriba abajo por filas y en cada fila de izquierda a derecha, aunque lleguen en otro orden', () => {
    const granos = [grano('c', 200, 10), grano('d', 10, 100), grano('a', 10, 12), grano('e', 110, 105), grano('b', 110, 8)];

    expect(enOrdenDeLectura(granos).map(({ nombre }) => nombre)).toEqual(['a', 'b', 'c', 'd', 'e']);
    expect(granos.map(({ nombre }) => nombre)).toEqual(['c', 'd', 'a', 'e', 'b']);
  });

  it('RF-11 · C4 · un grano un poco más abajo que los de su fila sigue en ella, y uno con el centro bajo el borde del primero empieza otra (prueba de límite)', () => {
    // El primero de la fila va de y = 0 a y = 40: un centro en 40 sigue en la fila y uno en 41 empieza otra.
    const enLaFila = grano('enLaFila', 10, 20);
    const primero = grano('primero', 100, 0);
    const otraFila = grano('otraFila', 5, 21);

    expect(enOrdenDeLectura([primero, enLaFila]).map(({ nombre }) => nombre)).toEqual(['enLaFila', 'primero']);
    expect(enOrdenDeLectura([primero, otraFila]).map(({ nombre }) => nombre)).toEqual(['primero', 'otraFila']);
  });

  it('RF-11 · C4 · dentro de una fila, los granos van por su centro y no por su borde izquierdo, aunque midan distinto', () => {
    // Los centros: ancho en x = 20 y angosto en x = 32; pequeño en x = 20 y grande en x = 50.
    const ancho = grano('ancho', 0, 0, 40);
    const angosto = grano('angosto', 30, 18, 4);
    const grande = { ...grano('grande', 0, 100, 100), alto: 40 };
    const pequeno = grano('pequeno', 10, 110, 20);

    expect(enOrdenDeLectura([angosto, ancho, grande, pequeno]).map(({ nombre }) => nombre))
      .toEqual(['ancho', 'angosto', 'pequeno', 'grande']);
  });

  it('RF-11 · C4 · sin granos, el orden de lectura es una lista vacía', () => {
    expect(enOrdenDeLectura([])).toEqual([]);
  });

  it('RF-11 · C1 · la marca de un grano va en porcentaje del tamaño original de la foto, así sigue al grano en cualquier tamaño', () => {
    expect(posicionEnPorcentaje({ x: 250, y: 100, ancho: 50, alto: 25 }, { ancho: 1000, alto: 500 }))
      .toEqual({ left: 25, top: 20, width: 5, height: 5 });
  });

  it('RF-11 · C1 · una marca que se sale de la foto se recorta a sus bordes (prueba de límite)', () => {
    expect(posicionEnPorcentaje({ x: -20, y: 480, ancho: 60, alto: 40 }, { ancho: 1000, alto: 500 }))
      .toEqual({ left: 0, top: 96, width: 4, height: 4 });
    expect(posicionEnPorcentaje({ x: 1000, y: 0, ancho: 10, alto: 10 }, { ancho: 1000, alto: 500 }))
      .toEqual({ left: 100, top: 0, width: 0, height: 2 });
  });

  it('RNF-06 · C4 · las flechas de adelante van al grano siguiente y las de atrás al anterior, sin dar la vuelta (prueba de límite)', () => {
    expect(indiceSiguiente(3, 'ArrowRight', 10)).toBe(4);
    expect(indiceSiguiente(3, 'ArrowDown', 10)).toBe(4);
    expect(indiceSiguiente(3, 'ArrowLeft', 10)).toBe(2);
    expect(indiceSiguiente(3, 'ArrowUp', 10)).toBe(2);
    expect(indiceSiguiente(9, 'ArrowRight', 10)).toBe(9);
    expect(indiceSiguiente(0, 'ArrowLeft', 10)).toBe(0);
  });

  it('RNF-06 · C4 · Inicio va al primer grano y Fin al último; las demás teclas no mueven el foco', () => {
    expect(indiceSiguiente(5, 'Home', 10)).toBe(0);
    expect(indiceSiguiente(5, 'End', 10)).toBe(9);
    expect(indiceSiguiente(5, 'Tab', 10)).toBeNull();
    expect(indiceSiguiente(5, 'Enter', 10)).toBeNull();
    expect(indiceSiguiente(5, 'toString', 10)).toBeNull();
    expect(indiceSiguiente(0, 'ArrowRight', 0)).toBeNull();
  });
});
