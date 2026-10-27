/*
 * Nombre: esquemas.js
 * Descripción: Esquemas de Yup de los formularios Iniciar sesión (RF-01) y Cambiar contraseña (RF-18) (RP-07),
 *              con los mismos límites y mensajes que el backend. Iniciar sesión solo pide que los datos no
 *              estén vacíos: el largo y los bytes de la contraseña los revisa el backend (RF-01 C8). La
 *              contraseña nueva se revisa como la de un usuario, con sus validaciones generadas (RP-23).
 * Fecha de creación: 20/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { object, reach, string } from 'yup';
import { esquemaParaCrear } from '../generated/usuario/usuarioEsquemas.js';
import { enBlanco } from './reglas.js';

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
 * Esquema de Cambiar contraseña: la contraseña actual, la nueva y la nueva repetida. La nueva usa las validaciones
 * generadas de la contraseña de un usuario (RP-23), en el orden en que la API escoge el error: con 65 eñes, que se
 * pasan del largo y de los bytes, el de los bytes, como la API.
 *
 * @param {(clave: string) => string} t la función de traducción de i18next
 * @returns {import('yup').ObjectSchema} el esquema del formulario
 */
export function esquemaCambioDeContrasena(t) {
  return object({
    contrasenaActual: obligatorio(t),
    contrasenaNueva: reach(esquemaParaCrear(t), 'contrasena'),
    contrasenaRepetida: obligatorio(t)
      .test('coincide', () => t('cambiarContrasena.noCoincide'),
        (valor, contexto) => valor === contexto.parent.contrasenaNueva),
  });
}
