/*
 * Nombre: esquemas.js
 * Descripción: Esquemas de Yup de los formularios Iniciar sesión (RF-01) y Cambiar contraseña (RF-18) (RP-07),
 *              con los mismos límites y mensajes que el backend. Iniciar sesión solo pide que los datos no
 *              estén vacíos: el largo y los bytes de la contraseña los revisa el backend (RF-01 C8).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { object, string } from 'yup';
import { bytesUtf8, CONTRASENA, enBlanco } from './reglas.js';

/**
 * Un texto que no puede quedar en blanco.
 *
 * @param {(clave: string) => string} t la función de traducción de i18next
 * @returns {import('yup').StringSchema} el esquema del campo
 */
function obligatorio(t) {
  return string().default('').test('obligatorio', () => t('validacion.obligatorio'), (valor) => !enBlanco(valor));
}

/**
 * Esquema de Iniciar sesión: el correo y la contraseña son obligatorios.
 *
 * @param {(clave: string) => string} t la función de traducción de i18next
 * @returns {import('yup').ObjectSchema} el esquema del formulario
 */
export function esquemaInicioDeSesion(t) {
  return object({ correo: obligatorio(t), contrasena: obligatorio(t) });
}

/**
 * Esquema de Cambiar contraseña: la contraseña actual, la nueva con sus límites y la nueva repetida.
 *
 * @param {(clave: string) => string} t la función de traducción de i18next
 * @returns {import('yup').ObjectSchema} el esquema del formulario
 */
export function esquemaCambioDeContrasena(t) {
  return object({
    contrasenaActual: obligatorio(t),
    contrasenaNueva: obligatorio(t)
      .test('longitud_min', () => t('usuario.contrasena_hash.longitud_min'),
        (valor) => valor.length >= CONTRASENA.minimo)
      .test('longitud_max', () => t('usuario.contrasena_hash.longitud_max'),
        (valor) => valor.length <= CONTRASENA.maximo)
      .test('bytes_max', () => t('usuario.contrasena_hash.bytes_max'),
        (valor) => bytesUtf8(valor) <= CONTRASENA.maximoBytes),
    contrasenaRepetida: obligatorio(t)
      .test('coincide', () => t('cambiarContrasena.noCoincide'),
        (valor, contexto) => valor === contexto.parent.contrasenaNueva),
  });
}
