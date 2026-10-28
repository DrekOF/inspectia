/*
 * Nombre: rangoRecomendacionEsquemas.test.js
 * Descripción: Pruebas generadas de las validaciones de las filas de rango_recomendacion: los mismos
 *              casos de límite que la prueba generada de la API.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.10)
 * Versión: 1.10
 * Entidades: rango_recomendacion 1.3
 */
import { describe, expect, it } from 'vitest';
import i18n from '../../i18n/i18n.js';
import { esquemaDelBloque } from './rangoRecomendacionEsquemas.js';

/** Las filas válidas: las de los valores por defecto del JSON, como las muestra el formulario. */
const FILAS = [
  { categoria: 'PREMIUM', limiteInferior: '96,0', recomendacion: 'Apto para mercado especial o de exportación.' },
  { categoria: 'ESTANDAR', limiteInferior: '90,0', recomendacion: 'Apto para venta estándar. Retirar a mano los granos defectuosos podría subirlo de categoría.' },
  { categoria: 'CORRIENTE', limiteInferior: '82,0', recomendacion: 'Retirar los granos defectuosos antes de vender, y revisar la fermentación y el secado.' },
  { categoria: 'FUERA_DE_NORMA', limiteInferior: '0,0', recomendacion: 'No venderlo como cacao de calidad: hacer una selección completa y revisar todo el proceso después de la cosecha.' },
];

/** Lo que rechaza cada validación del JSON en una fila, con el mensaje en español de la API. */
const CASOS = [
  ['limiteInferior', '', 'Este dato es obligatorio.'],
  ['limiteInferior', '-1', 'Debe ser mayor o igual a 0.'],
  ['limiteInferior', '101', 'Debe ser menor o igual a 100.'],
  ['limiteInferior', '0,05', 'Usa como máximo 1 decimal.'],
  ['limiteInferior', '100,55', 'Debe ser menor o igual a 100.'],
  ['limiteInferior', '-0,55', 'Debe ser mayor o igual a 0.'],
  ['recomendacion', '', 'Este dato es obligatorio.'],
  ['recomendacion', 'x'.repeat(301), 'Puede tener como máximo 300 caracteres.'],
];

/**
 * Los valores del borde que acepta cada validación del JSON en una fila. En la API, un valor del borde puede no
 * cumplir una regla del bloque escrita a mano, como el orden de los límites: por eso solo los revisa esta prueba.
 */
const BORDES = [
  ['limiteInferior', '0'],
  ['limiteInferior', '100'],
  ['recomendacion', 'x'.repeat(300)],
];

/**
 * El primer error de un campo de la primera fila, o null si cumple sus validaciones.
 *
 * @param {string} campo el campo, con el nombre de la API
 * @param {string} valor lo que se escribe en él
 * @returns {Promise<string|null>} el mensaje
 */
async function errorDe(campo, valor) {
  const datos = { filas: FILAS.map((fila, posicion) => (posicion === 0 ? { ...fila, [campo]: valor } : fila)) };
  try {
    await esquemaDelBloque(i18n.t).validateAt(`filas[0].${campo}`, datos);
    return null;
  } catch (error) {
    return error.message;
  }
}

describe('validaciones de las filas de rango_recomendacion (generadas)', () => {
  it('RF-13 · los valores por defecto de rango_recomendacion, como los muestra el formulario, cumplen las validaciones', async () => {
    expect(await esquemaDelBloque(i18n.t).isValid({ filas: FILAS })).toBe(true);
  });

  it.each(CASOS)('RF-13 · cada validación del JSON de rango_recomendacion rechaza en una fila lo mismo que la API, con su mensaje: %s',
    async (campo, valor, mensaje) => {
      expect(await errorDe(campo, valor)).toBe(mensaje);
    });

  it.each(BORDES)('RF-13 · cada validación del JSON de rango_recomendacion acepta en una fila su valor del borde: %s',
    async (campo, valor) => {
      expect(await errorDe(campo, valor)).toBeNull();
    });

  it('RF-13 · limiteInferior se escribe con coma o con punto, y lo que no es un número tiene su mensaje', async () => {
    const esquema = esquemaDelBloque(i18n.t);
    const fila = (valor) => ({ filas: [{ ...FILAS[0], limiteInferior: valor }] });

    expect(esquema.cast(fila('1,5')).filas[0].limiteInferior).toBe(1.5);
    expect(esquema.cast(fila('1.5')).filas[0].limiteInferior).toBe(1.5);
    expect(await errorDe('limiteInferior', 'abc')).toBe('Escribe un número, como 90,5.');
  });

  it('RF-13 · los mensajes de las validaciones de rango_recomendacion salen en el idioma de la app', async () => {
    await i18n.changeLanguage('en');
    try {
      expect(await errorDe('limiteInferior', '')).toBe('This field is required.');
    } finally {
      await i18n.changeLanguage('es');
    }
  });
});
