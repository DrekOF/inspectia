/*
 * Nombre: usuarioEsquemas.test.js
 * Descripción: Pruebas generadas de las validaciones de usuario: los mismos casos de límite que la
 *              prueba generada de la API.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.9)
 * Versión: 1.9
 * Entidades: usuario 1.8
 */
import { describe, expect, it } from 'vitest';
import i18n from '../../i18n/i18n.js';
import { esquemaParaCrear, esquemaParaEditar } from './usuarioEsquemas.js';

/** Datos válidos para registrar: los ejemplos del JSON, los mismos de la prueba generada de la API. */
const DATOS_PARA_CREAR = { nombre: 'Ana Pérez', correo: 'ana.perez@example.com', contrasena: 'Prueba-Segura-2026', rol: 'USUARIO_FINAL' };

/** Datos válidos para editar: los ejemplos del JSON. */
const DATOS_PARA_EDITAR = { nombre: 'Ana Pérez', correo: 'ana.perez@example.com', contrasena: 'Prueba-Segura-2026', rol: 'USUARIO_FINAL' };

/** Lo que rechaza cada validación del JSON al registrar, con el mensaje en español de la API. El formato del correo no está: lo valida solo el backend. */
const CASOS_AL_CREAR = [
  ['nombre', '', 'Este dato es obligatorio.'],
  ['nombre', 'x'.repeat(121), 'Puede tener como máximo 120 caracteres.'],
  ['correo', '', 'Este dato es obligatorio.'],
  ['correo', correoDeLargo(255), 'Puede tener como máximo 254 caracteres.'],
  ['contrasena', '', 'Este dato es obligatorio.'],
  ['contrasena', ' '.repeat(8), 'Este dato es obligatorio.'],
  ['contrasena', 'x'.repeat(65), 'La contraseña puede tener como máximo 64 caracteres.'],
  ['contrasena', 'x'.repeat(7), 'La contraseña debe tener al menos 8 caracteres.'],
  ['contrasena', 'ñ'.repeat(37), 'La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes'],
  ['contrasena', 'ñ'.repeat(65), 'La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes'],
  ['rol', '', 'Este dato es obligatorio.'],
];

/** Los valores del borde que acepta cada validación del JSON al registrar, como la API. */
const BORDES_AL_CREAR = [
  ['nombre', 'x'.repeat(120)],
  ['correo', correoDeLargo(254)],
  ['contrasena', 'x'.repeat(64)],
  ['contrasena', 'x'.repeat(8)],
  ['contrasena', 'ñ'.repeat(36)],
];

/** Lo que rechaza cada validación del JSON al editar, con el mensaje en español de la API. */
const CASOS_AL_EDITAR = [
  ['nombre', '', 'Este dato es obligatorio.'],
  ['nombre', 'x'.repeat(121), 'Puede tener como máximo 120 caracteres.'],
  ['correo', '', 'Este dato es obligatorio.'],
  ['correo', correoDeLargo(255), 'Puede tener como máximo 254 caracteres.'],
  ['contrasena', ' '.repeat(8), 'Este dato es obligatorio.'],
  ['contrasena', 'x'.repeat(65), 'La contraseña puede tener como máximo 64 caracteres.'],
  ['contrasena', 'x'.repeat(7), 'La contraseña debe tener al menos 8 caracteres.'],
  ['contrasena', 'ñ'.repeat(37), 'La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes'],
  ['contrasena', 'ñ'.repeat(65), 'La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes'],
  ['rol', '', 'Este dato es obligatorio.'],
];

/** Los valores del borde que acepta cada validación del JSON al editar, como la API. */
const BORDES_AL_EDITAR = [
  ['nombre', 'x'.repeat(120)],
  ['correo', correoDeLargo(254)],
  ['contrasena', 'x'.repeat(64)],
  ['contrasena', 'x'.repeat(8)],
  ['contrasena', 'ñ'.repeat(36)],
];

/**
 * Un correo válido de un largo dado: 64 letras antes de la arroba y partes del dominio de hasta 63, como en la
 * prueba generada de la API.
 *
 * @param {number} largo los caracteres del correo
 * @returns {string} el correo
 */
function correoDeLargo(largo) {
  const local = 'a'.repeat(64);
  let dominio = 'co';
  while (local.length + 1 + dominio.length < largo) {
    const falta = largo - local.length - 1 - dominio.length - 1;
    dominio = `${'b'.repeat(Math.max(1, Math.min(63, falta)))}.${dominio}`;
  }
  return `${local}@${dominio}`;
}

/**
 * Los datos con un campo cambiado: administrador.correo cambia el correo dentro de administrador.
 *
 * @param {object} datos los datos del formulario
 * @param {string} campo el campo, con los nombres de la API
 * @param {string|number} valor el valor nuevo
 * @returns {object} una copia de los datos con el campo cambiado
 */
function conValor(datos, campo, valor) {
  const copia = structuredClone(datos);
  const partes = campo.split('.');
  const ultimo = partes.pop();
  partes.reduce((objeto, parte) => objeto[parte], copia)[ultimo] = valor;
  return copia;
}

/**
 * El primer error de un campo, o null si cumple sus validaciones.
 *
 * @param {import('yup').ObjectSchema} esquema el esquema del formulario
 * @param {string} campo el campo
 * @param {object} datos todos los datos del formulario
 * @returns {Promise<string|null>} el mensaje
 */
async function errorDe(esquema, campo, datos) {
  try {
    await esquema.validateAt(campo, datos);
    return null;
  } catch (error) {
    return error.message;
  }
}

describe('validaciones del formulario de usuario (generadas)', () => {
  it('RF-03 · los ejemplos del JSON de usuario cumplen las validaciones del formulario', async () => {
    expect(await esquemaParaCrear(i18n.t).isValid(DATOS_PARA_CREAR)).toBe(true);
    expect(await esquemaParaEditar(i18n.t).isValid(DATOS_PARA_EDITAR)).toBe(true);
  });

  it.each(CASOS_AL_CREAR)('RF-03 · cada validación del JSON de usuario rechaza al registrar lo mismo que la API, con su mensaje: %s',
    async (campo, valor, mensaje) => {
      expect(await errorDe(esquemaParaCrear(i18n.t), campo, conValor(DATOS_PARA_CREAR, campo, valor))).toBe(mensaje);
    });

  it.each(BORDES_AL_CREAR)('RF-03 · cada validación del JSON de usuario acepta al registrar su valor del borde, como la API: %s',
    async (campo, valor) => {
      expect(await errorDe(esquemaParaCrear(i18n.t), campo, conValor(DATOS_PARA_CREAR, campo, valor))).toBeNull();
    });

  it.each(CASOS_AL_EDITAR)('RF-03 · cada validación del JSON de usuario rechaza al editar lo mismo que la API, con su mensaje: %s',
    async (campo, valor, mensaje) => {
      expect(await errorDe(esquemaParaEditar(i18n.t), campo, conValor(DATOS_PARA_EDITAR, campo, valor))).toBe(mensaje);
    });

  it.each(BORDES_AL_EDITAR)('RF-03 · cada validación del JSON de usuario acepta al editar su valor del borde, como la API: %s',
    async (campo, valor) => {
      expect(await errorDe(esquemaParaEditar(i18n.t), campo, conValor(DATOS_PARA_EDITAR, campo, valor))).toBeNull();
    });

  it('RF-03 · los mensajes de las validaciones de usuario salen en el idioma de la app', async () => {
    await i18n.changeLanguage('en');
    try {
      expect(await errorDe(esquemaParaCrear(i18n.t), 'nombre',
        conValor(DATOS_PARA_CREAR, 'nombre', ''))).toBe('This field is required.');
    } finally {
      await i18n.changeLanguage('es');
    }
  });
});
