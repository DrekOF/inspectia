/*
 * Nombre: empresaEsquemas.test.js
 * Descripción: Pruebas generadas de las validaciones de empresa: los mismos casos de límite que la
 *              prueba generada de la API.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.7)
 * Versión: 1.7
 * Entidades: empresa 1.3, usuario 1.6
 */
import { describe, expect, it } from 'vitest';
import i18n from '../../i18n/i18n.js';
import { esquemaParaCrear, esquemaParaEditar } from './empresaEsquemas.js';

/** Datos válidos para registrar: los ejemplos del JSON, los mismos de la prueba generada de la API. */
const DATOS_PARA_CREAR = { nombre: 'Cacaos del Huila', nit: '900123456-8', ciudad: 'Neiva', administrador: { nombre: 'Ana Pérez', correo: 'ana.perez@example.com', contrasena: 'Prueba-Segura-2026' } };

/** Datos válidos para editar: los ejemplos del JSON. */
const DATOS_PARA_EDITAR = { nombre: 'Cacaos del Huila', nit: '900123456-8', ciudad: 'Neiva' };

/** Lo que rechaza cada validación del JSON al registrar, con el mensaje en español de la API. El formato del correo no está: lo valida solo el backend. */
const CASOS_AL_CREAR = [
  ['nombre', '', 'Este dato es obligatorio.'],
  ['nombre', 'x'.repeat(121), 'Puede tener como máximo 120 caracteres.'],
  ['nit', '', 'Este dato es obligatorio.'],
  ['nit', '?', 'Escribe el NIT sin puntos y con el dígito de verificación, por ejemplo 900123456-8.'],
  ['ciudad', '', 'Este dato es obligatorio.'],
  ['ciudad', 'x'.repeat(81), 'Puede tener como máximo 80 caracteres.'],
  ['administrador.nombre', '', 'Este dato es obligatorio.'],
  ['administrador.nombre', 'x'.repeat(121), 'Puede tener como máximo 120 caracteres.'],
  ['administrador.correo', '', 'Este dato es obligatorio.'],
  ['administrador.correo', correoDeLargo(255), 'Puede tener como máximo 254 caracteres.'],
  ['administrador.contrasena', '', 'Este dato es obligatorio.'],
  ['administrador.contrasena', ' '.repeat(8), 'Este dato es obligatorio.'],
  ['administrador.contrasena', 'x'.repeat(65), 'La contraseña puede tener como máximo 64 caracteres.'],
  ['administrador.contrasena', 'x'.repeat(7), 'La contraseña debe tener al menos 8 caracteres.'],
  ['administrador.contrasena', 'ñ'.repeat(37), 'La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes'],
];

/** Los valores del borde que acepta cada validación del JSON al registrar, como la API. */
const BORDES_AL_CREAR = [
  ['nombre', 'x'.repeat(120)],
  ['ciudad', 'x'.repeat(80)],
  ['administrador.nombre', 'x'.repeat(120)],
  ['administrador.correo', correoDeLargo(254)],
  ['administrador.contrasena', 'x'.repeat(64)],
  ['administrador.contrasena', 'x'.repeat(8)],
  ['administrador.contrasena', 'ñ'.repeat(36)],
];

/** Lo que rechaza cada validación del JSON al editar, con el mensaje en español de la API. */
const CASOS_AL_EDITAR = [
  ['nombre', '', 'Este dato es obligatorio.'],
  ['nombre', 'x'.repeat(121), 'Puede tener como máximo 120 caracteres.'],
  ['nit', '', 'Este dato es obligatorio.'],
  ['nit', '?', 'Escribe el NIT sin puntos y con el dígito de verificación, por ejemplo 900123456-8.'],
  ['ciudad', '', 'Este dato es obligatorio.'],
  ['ciudad', 'x'.repeat(81), 'Puede tener como máximo 80 caracteres.'],
];

/** Los valores del borde que acepta cada validación del JSON al editar, como la API. */
const BORDES_AL_EDITAR = [
  ['nombre', 'x'.repeat(120)],
  ['ciudad', 'x'.repeat(80)],
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

describe('validaciones del formulario de empresa (generadas)', () => {
  it('RF-02 · los ejemplos del JSON de empresa cumplen las validaciones del formulario', async () => {
    expect(await esquemaParaCrear(i18n.t).isValid(DATOS_PARA_CREAR)).toBe(true);
    expect(await esquemaParaEditar(i18n.t).isValid(DATOS_PARA_EDITAR)).toBe(true);
  });

  it.each(CASOS_AL_CREAR)('RF-02 · cada validación del JSON de empresa rechaza al registrar lo mismo que la API, con su mensaje: %s',
    async (campo, valor, mensaje) => {
      expect(await errorDe(esquemaParaCrear(i18n.t), campo, conValor(DATOS_PARA_CREAR, campo, valor))).toBe(mensaje);
    });

  it.each(BORDES_AL_CREAR)('RF-02 · cada validación del JSON de empresa acepta al registrar su valor del borde, como la API: %s',
    async (campo, valor) => {
      expect(await errorDe(esquemaParaCrear(i18n.t), campo, conValor(DATOS_PARA_CREAR, campo, valor))).toBeNull();
    });

  it.each(CASOS_AL_EDITAR)('RF-02 · cada validación del JSON de empresa rechaza al editar lo mismo que la API, con su mensaje: %s',
    async (campo, valor, mensaje) => {
      expect(await errorDe(esquemaParaEditar(i18n.t), campo, conValor(DATOS_PARA_EDITAR, campo, valor))).toBe(mensaje);
    });

  it.each(BORDES_AL_EDITAR)('RF-02 · cada validación del JSON de empresa acepta al editar su valor del borde, como la API: %s',
    async (campo, valor) => {
      expect(await errorDe(esquemaParaEditar(i18n.t), campo, conValor(DATOS_PARA_EDITAR, campo, valor))).toBeNull();
    });

  it('RF-02 · los mensajes de las validaciones de empresa salen en el idioma de la app', async () => {
    await i18n.changeLanguage('en');
    try {
      expect(await errorDe(esquemaParaCrear(i18n.t), 'nombre',
        conValor(DATOS_PARA_CREAR, 'nombre', ''))).toBe('This field is required.');
    } finally {
      await i18n.changeLanguage('es');
    }
  });
});
