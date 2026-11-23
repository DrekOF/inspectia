/*
 * Nombre: esquemaDeLote.js
 * Descripción: Esquema de Yup del formulario Nuevo lote (RF-04, RP-07), con los mismos límites y mensajes que el
 *              backend: el código, la fecha de recepción no posterior a hoy en Colombia ni de hace más de un año,
 *              la finca o el proveedor y las observaciones, opcionales.
 * Fecha de creación: 22/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { object, string } from 'yup';
import { enBlanco, recortar } from '../validacion/reglas.js';
import { haceUnAnio } from './fechas.js';

/** Las longitudes de la tabla lote (specs/entidades/lote.json), las mismas que valida el backend. */
export const LIMITES_DEL_LOTE = { codigo: 30, origen: 120, observaciones: 500 };

/**
 * Un texto con su largo máximo, contado sin los espacios del inicio y del final, como en el backend; y, si es
 * obligatorio, que no quede en blanco.
 *
 * @param {(clave: string, valores?: object) => string} t la función de traducción de i18next
 * @param {{ obligatorio: boolean, maximo: number }} reglas si es obligatorio y su largo máximo
 * @returns {import('yup').StringSchema} el esquema del campo
 */
function texto(t, { obligatorio, maximo }) {
  const largo = string().default('')
    .test('largo', () => t('validacion.longitud_max', { max: maximo }), (valor) => recortar(valor ?? '').length <= maximo);
  return obligatorio
    ? largo.test('obligatorio', () => t('validacion.obligatorio'), (valor) => !enBlanco(valor ?? ''))
    : largo;
}

/**
 * Esquema de Nuevo lote.
 *
 * @param {(clave: string, valores?: object) => string} t la función de traducción de i18next
 * @param {string} hoy el día de hoy en Colombia (aaaa-mm-dd): la fecha de recepción no puede ser posterior, ni
 *   de hace más de un año
 * @returns {import('yup').ObjectSchema} el esquema del formulario
 */
export function esquemaDeLote(t, hoy) {
  const primeraFecha = haceUnAnio(hoy);
  return object({
    codigo: texto(t, { obligatorio: true, maximo: LIMITES_DEL_LOTE.codigo }),
    fechaRecepcion: string().default('')
      .test('obligatorio', () => t('validacion.obligatorio'), (valor) => !enBlanco(valor ?? ''))
      .test('noPosteriorAHoy', () => t('lote.fecha_recepcion.no_posterior_a_hoy'),
        (valor) => enBlanco(valor ?? '') || valor <= hoy)
      .test('noAnteriorAUnAnio', () => t('lote.fecha_recepcion.no_anterior_a_un_anio'),
        (valor) => enBlanco(valor ?? '') || valor >= primeraFecha),
    origen: texto(t, { obligatorio: true, maximo: LIMITES_DEL_LOTE.origen }),
    observaciones: texto(t, { obligatorio: false, maximo: LIMITES_DEL_LOTE.observaciones }),
  });
}
