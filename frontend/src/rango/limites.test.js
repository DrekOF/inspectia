/*
 * Nombre: limites.test.js
 * Descripción: Pruebas de lo escrito a mano para los rangos (RF-13): qué límites no bajan en orden, con los mismos
 *              bordes que las pruebas del backend (el empate, 96,0 y 95,9, y Corriente en 0,1 y en 0); qué
 *              porcentajes abarca cada categoría, para el resumen «Así quedan los rangos»; y la regla del orden que
 *              usa la pantalla antes de enviar.
 * Fecha de creación: 28/10/2026
 * Actualización: 28/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { describe, expect, it } from 'vitest';
import { intervalosDeLosRangos, limitesFueraDeOrden, reglaDelOrden } from './limites.js';

/** Las categorías, en el orden de su lista: de la mejor a la peor. */
const CATEGORIAS = ['PREMIUM', 'ESTANDAR', 'CORRIENTE', 'FUERA_DE_NORMA'];

/**
 * Las filas de los rangos con unos límites, en el orden de las categorías.
 *
 * @param {Array<number|undefined>} limites los límites
 * @returns {object[]} las filas
 */
const filasCon = (limites) => limites.map((limiteInferior, posicion) => ({ categoria: CATEGORIAS[posicion], limiteInferior }));

describe('límites fuera de orden', () => {
  it.each([
    ['los de D-10', [96, 90, 82, 0], []],
    ['Estándar 97 % y Premium 96 %, el ejemplo de RF-13 C1', [96, 97, 82, 0], [0, 1]],
    ['un empate entre Premium y Estándar', [96, 96, 82, 0], [0, 1]],
    ['Estándar en 95,9, justo debajo de Premium', [96, 95.9, 82, 0], []],
    ['Corriente en 0,1, justo encima de Fuera de norma', [96, 90, 0.1, 0], []],
    ['Corriente en 0, igual a Fuera de norma', [96, 90, 0, 0], [2, 3]],
    ['dos pares seguidos que no bajan', [80, 90, 95, 0], [0, 1, 2]],
    ['un límite que no es un número no se compara', [96, Number.NaN, 97, 0], []],
    ['un límite vacío tampoco', [96, undefined, 82, 0], []],
  ])('RF-13 · C1 · los límites bajan estrictamente, como los revisa el backend: %s', (_, limites, fuera) => {
    expect(limitesFueraDeOrden(limites)).toEqual(fuera);
  });
});

describe('intervalos de los rangos', () => {
  it('RF-13 · con los límites de D-10, cada categoría abarca desde su límite hasta un decimal antes del de la categoría de arriba', () => {
    expect(intervalosDeLosRangos([96, 90, 82, 0], 1)).toEqual([
      { tipo: 'desde', limite: 96 },
      { tipo: 'entre', desde: 90, hasta: 95.9 },
      { tipo: 'entre', desde: 82, hasta: 89.9 },
      { tipo: 'menosDe', limite: 82 },
    ]);
  });

  it('RF-13 · las categorías de un par que no baja, o las que dependen de un límite que no es un número, dicen que hay que revisar el límite', () => {
    expect(intervalosDeLosRangos([96, 97, 82, 0], 1)).toEqual([
      { tipo: 'revisar' }, { tipo: 'revisar' }, { tipo: 'entre', desde: 82, hasta: 96.9 }, { tipo: 'menosDe', limite: 82 },
    ]);
    expect(intervalosDeLosRangos([96, 90, 0, 0], 1)).toEqual([
      { tipo: 'desde', limite: 96 }, { tipo: 'entre', desde: 90, hasta: 95.9 }, { tipo: 'revisar' }, { tipo: 'revisar' },
    ]);
    expect(intervalosDeLosRangos([96, Number.NaN, 82, 0], 1)).toEqual([
      { tipo: 'desde', limite: 96 }, { tipo: 'revisar' }, { tipo: 'revisar' }, { tipo: 'menosDe', limite: 82 },
    ]);
    expect(intervalosDeLosRangos([Number.NaN, 90, 82, 0], 1)).toEqual([
      { tipo: 'revisar' }, { tipo: 'revisar' }, { tipo: 'entre', desde: 82, hasta: 89.9 }, { tipo: 'menosDe', limite: 82 },
    ]);
    expect(intervalosDeLosRangos([96, 90, Number.NaN, 0], 1)).toEqual([
      { tipo: 'desde', limite: 96 }, { tipo: 'entre', desde: 90, hasta: 95.9 }, { tipo: 'revisar' }, { tipo: 'revisar' },
    ]);
  });

  it('RF-13 · el final de cada categoría queda un paso de la escala antes del límite de arriba, sin errores de redondeo', () => {
    expect(intervalosDeLosRangos([96, 90.1, 0.3, 0], 1)[1]).toEqual({ tipo: 'entre', desde: 90.1, hasta: 95.9 });
    expect(intervalosDeLosRangos([0.3, 0.2, 0.1, 0], 1)[2]).toEqual({ tipo: 'entre', desde: 0.1, hasta: 0.1 });
    expect(intervalosDeLosRangos([96, 90, 82, 0], 2)[1]).toEqual({ tipo: 'entre', desde: 90, hasta: 95.99 });
  });
});

describe('regla del orden', () => {
  it('RF-13 · C1 · con los límites en orden, la regla deja guardar', () => {
    expect(reglaDelOrden(filasCon([96, 90, 82, 0]))).toBeNull();
  });

  it('RF-13 · C1 · la regla del orden da el mensaje de la regla del JSON y los límites de cada par que no baja', () => {
    expect(reglaDelOrden(filasCon([96, 97, 82, 0]))).toEqual({
      mensaje: 'rango_recomendacion.reglas.limites_en_orden',
      campos: ['filas[0].limiteInferior', 'filas[1].limiteInferior'],
    });
    expect(reglaDelOrden(filasCon([96, 90, 0, 0])).campos).toEqual(['filas[2].limiteInferior', 'filas[3].limiteInferior']);
  });
});
