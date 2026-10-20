/*
 * Nombre: esquemas.test.js
 * Descripción: Pruebas de las reglas de los formularios de Iniciar sesión y Cambiar contraseña (Yup, RP-07):
 *              los mismos límites y mensajes que el backend (RF-01 y RF-18).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { describe, expect, it } from 'vitest';
import i18n from '../i18n/i18n.js';
import { esquemaCambioDeContrasena, esquemaInicioDeSesion } from './esquemas.js';
import { bytesUtf8 } from './reglas.js';

/**
 * El primer error de un campo, o null si cumple.
 *
 * @param {import('yup').ObjectSchema} esquema el esquema del formulario
 * @param {string} campo el campo que se revisa
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

describe('cambiar contraseña', () => {
  const esquema = () => esquemaCambioDeContrasena(i18n.t);
  const conNueva = (nueva) => ({ contrasenaActual: 'Clave-Actual-2026', contrasenaNueva: nueva, contrasenaRepetida: nueva });

  it.each([
    ['7 caracteres', 'a'.repeat(7), 'La contraseña debe tener al menos 8 caracteres.'],
    ['8 caracteres', 'a'.repeat(8), null],
    ['64 caracteres', 'a'.repeat(64), null],
    ['65 caracteres', 'a'.repeat(65), 'La contraseña puede tener como máximo 64 caracteres.'],
  ])('RF-18 · C3 · la contraseña nueva acepta de 8 a 64 caracteres: con 7 o con 65 se rechaza (prueba de límite): %s',
    async (_, nueva, mensaje) => {
      expect(await errorDe(esquema(), 'contrasenaNueva', conNueva(nueva))).toBe(mensaje);
    });

  it.each([
    ['64 caracteres con 8 eñes (72 bytes)', 'ñ'.repeat(8) + 'a'.repeat(56), null],
    ['64 caracteres con 9 eñes (73 bytes)', 'ñ'.repeat(9) + 'a'.repeat(55),
      'La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes'],
  ])('RF-18 · C4 · la contraseña nueva ocupa como máximo 72 bytes, contando 2 por cada eñe (prueba de límite): %s',
    async (_, nueva, mensaje) => {
      expect(await errorDe(esquema(), 'contrasenaNueva', conNueva(nueva))).toBe(mensaje);
    });

  it('RF-18 · una contraseña nueva de solo espacios se rechaza con «Este dato es obligatorio.», como en el backend', async () => {
    expect(await errorDe(esquema(), 'contrasenaNueva', conNueva(' '.repeat(8)))).toBe('Este dato es obligatorio.');
  });

  it('RF-18 · la contraseña actual es obligatoria', async () => {
    expect(await errorDe(esquema(), 'contrasenaActual', { ...conNueva('Clave-Nueva-2026'), contrasenaActual: '' }))
      .toBe('Este dato es obligatorio.');
  });

  it('RF-18 · la contraseña repetida tiene que ser igual a la nueva', async () => {
    const datos = { ...conNueva('Clave-Nueva-2026'), contrasenaRepetida: 'Clave-Nueva-2025' };

    expect(await errorDe(esquema(), 'contrasenaRepetida', datos)).toBe('No coincide con la contraseña nueva.');
  });

  it('RF-18 · los mensajes salen en el idioma de la app', async () => {
    await i18n.changeLanguage('en');
    try {
      expect(await errorDe(esquema(), 'contrasenaNueva', conNueva('a'.repeat(7))))
        .toBe('The password must have at least 8 characters.');
    } finally {
      await i18n.changeLanguage('es');
    }
  });
});

describe('iniciar sesión', () => {
  it.each([['correo'], ['contrasena']])('RF-01 · el correo y la contraseña son obligatorios: %s', async (campo) => {
    const datos = { correo: 'ana.perez@cacaosdelhuila.co', contrasena: 'Clave-Segura-2026', [campo]: '   ' };

    expect(await errorDe(esquemaInicioDeSesion(i18n.t), campo, datos)).toBe('Este dato es obligatorio.');
  });
});

describe('bytes en UTF-8', () => {
  it.each([['a', 1], ['ñ', 2], ['á', 2], ['€', 3], ['', 0]])(
    'RF-18 · cada letra con tilde y cada eñe ocupan 2 bytes, como los cuenta BCrypt: «%s» ocupa %i',
    (texto, bytes) => {
      expect(bytesUtf8(texto)).toBe(bytes);
    },
  );
});
