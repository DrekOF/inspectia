/*
 * Nombre: usuarioEsquemas.js
 * Descripción: Validaciones de Yup del formulario de usuario: las del JSON, con los mensajes de la API
 *              (RF-03).
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.11)
 * Versión: 1.11
 * Entidades: usuario 1.8
 */
import { object, string } from 'yup';
import { cabeEnBytes, lleno, minusculas, noEnBlanco, recortar, vacioComoAusente } from '../../validacion/reglas.js';

/**
 * Los límites de cada campo, los del JSON: para las páginas escritas a mano que validan lo mismo, como Cambiar
 * contraseña (RF-18), sin copiarlos.
 */
export const LIMITES = {
  nombre: { maximo: 120 },
  correo: { maximo: 254 },
  contrasena: { minimo: 8, maximo: 64, maximoBytes: 72 },
};

/**
 * Las validaciones del formulario para registrar usuario: las de la API, con sus mensajes (RF-03).
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
    rol: string()
      .transform(vacioComoAusente)
      .test('obligatorio', t('validacion.obligatorio'), lleno)
      .oneOf(['USUARIO_FINAL', 'ADMIN_EMPRESA'], t('usuario.rol.valores_en_formulario')),
  });
}

/**
 * Las validaciones del formulario para editar usuario: las de la API, con sus mensajes (RF-03).
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
    correo: string()
      .transform(recortar)
      .transform(minusculas)
      .test('obligatorio', t('validacion.obligatorio'), lleno)
      .max(254, t('validacion.longitud_max', { max: 254 })),
    contrasena: string()
      .transform(vacioComoAusente)
      .test('obligatorio', t('validacion.obligatorio'), noEnBlanco)
      .test('bytes_max', t('usuario.contrasena_hash.bytes_max'), cabeEnBytes(72))
      .max(64, t('usuario.contrasena_hash.longitud_max'))
      .min(8, t('usuario.contrasena_hash.longitud_min')),
    rol: string()
      .transform(vacioComoAusente)
      .test('obligatorio', t('validacion.obligatorio'), lleno)
      .oneOf(['USUARIO_FINAL', 'ADMIN_EMPRESA'], t('usuario.rol.valores_en_formulario')),
  });
}
