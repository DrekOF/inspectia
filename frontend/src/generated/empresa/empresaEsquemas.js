/*
 * Nombre: empresaEsquemas.js
 * Descripción: Validaciones de Yup del formulario de empresa: las del JSON, con los mensajes de la API
 *              (RF-02).
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.9)
 * Versión: 1.9
 * Entidades: empresa 1.3, usuario 1.8
 */
import { object, string } from 'yup';
import { cabeEnBytes, lleno, minusculas, recortar } from '../../validacion/reglas.js';

/**
 * Los límites de cada campo, los del JSON: para las páginas escritas a mano que validan lo mismo, como Cambiar
 * contraseña (RF-18), sin copiarlos.
 */
export const LIMITES = {
  nombre: { maximo: 120 },
  nit: { maximo: 15 },
  ciudad: { maximo: 80 },
};

/**
 * Las validaciones del formulario para registrar empresa: las de la API, con sus mensajes (RF-02).
 * Antes de validar, los textos se normalizan como en la API.
 *
 * @param {(clave: string, valores?: object) => string} t la función de traducción de i18next
 * @returns {import('yup').ObjectSchema} el esquema
 */
export function esquemaParaCrear(t) {
  return object({
    nombre: string()
      .transform(recortar)
      .test('obligatorio', t('validacion.obligatorio'), lleno)
      .max(120, t('validacion.longitud_max', { max: 120 })),
    nit: string()
      .transform(recortar)
      .test('obligatorio', t('validacion.obligatorio'), lleno)
      .matches(/^[0-9]{6,10}-[0-9]$/, t('empresa.nit.patron'))
      .max(15, t('validacion.longitud_max', { max: 15 })),
    ciudad: string()
      .transform(recortar)
      .test('obligatorio', t('validacion.obligatorio'), lleno)
      .max(80, t('validacion.longitud_max', { max: 80 })),
    administrador: object({
      nombre: string()
        .transform(recortar)
        .test('obligatorio', t('validacion.obligatorio'), lleno)
        .max(120, t('validacion.longitud_max', { max: 120 })),
      correo: string()
        .transform(recortar)
        .transform(minusculas)
        .test('obligatorio', t('validacion.obligatorio'), lleno)
        .max(254, t('validacion.longitud_max', { max: 254 })),
      contrasena: string()
        .test('obligatorio', t('validacion.obligatorio'), lleno)
        .test('bytes_max', t('usuario.contrasena_hash.bytes_max'), cabeEnBytes(72))
        .max(64, t('usuario.contrasena_hash.longitud_max'))
        .min(8, t('usuario.contrasena_hash.longitud_min')),
    }),
  });
}

/**
 * Las validaciones del formulario para editar empresa: las de la API, con sus mensajes (RF-02).
 *
 * @param {(clave: string, valores?: object) => string} t la función de traducción de i18next
 * @returns {import('yup').ObjectSchema} el esquema
 */
export function esquemaParaEditar(t) {
  return object({
    nombre: string()
      .transform(recortar)
      .test('obligatorio', t('validacion.obligatorio'), lleno)
      .max(120, t('validacion.longitud_max', { max: 120 })),
    nit: string()
      .transform(recortar)
      .test('obligatorio', t('validacion.obligatorio'), lleno)
      .matches(/^[0-9]{6,10}-[0-9]$/, t('empresa.nit.patron'))
      .max(15, t('validacion.longitud_max', { max: 15 })),
    ciudad: string()
      .transform(recortar)
      .test('obligatorio', t('validacion.obligatorio'), lleno)
      .max(80, t('validacion.longitud_max', { max: 80 })),
  });
}
