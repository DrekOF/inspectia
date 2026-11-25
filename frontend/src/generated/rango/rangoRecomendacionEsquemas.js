/*
 * Nombre: rangoRecomendacionEsquemas.js
 * Descripción: Validaciones de Yup de las filas de rango_recomendacion: las del JSON, con los mensajes
 *              de la API (RF-13).
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.11)
 * Versión: 1.11
 * Entidades: rango_recomendacion 1.3
 */
import { array, number, object, string } from 'yup';
import { lleno, maximoDecimales, numeroDecimal, recortar } from '../../validacion/reglas.js';

/**
 * Las validaciones de las filas de rangos de recomendación: las de la API en cada fila, con sus mensajes (RF-13).
 * Antes de validar, los textos se normalizan como en la API y los números se leen como se escriben, con coma o con
 * punto. Las reglas de todo el bloque, como el orden de los límites, no van aquí: se programan a mano.
 *
 * @param {(clave: string, valores?: object) => string} t la función de traducción de i18next
 * @returns {import('yup').ObjectSchema} el esquema
 */
export function esquemaDelBloque(t) {
  return object({
    filas: array().of(object({
      limiteInferior: number()
        .transform(numeroDecimal)
        .typeError(t('validacion.numero'))
        .test('obligatorio', t('validacion.obligatorio'), lleno)
        .max(100, t('validacion.maximo', { value: 100 }))
        .min(0, t('validacion.minimo', { value: 0 }))
        .test('decimales', t('validacion.decimales_uno'), maximoDecimales(1)),
      recomendacion: string()
        .transform(recortar)
        .test('obligatorio', t('validacion.obligatorio'), lleno)
        .max(300, t('validacion.longitud_max', { max: 300 })),
    })),
  });
}
