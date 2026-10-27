/*
 * Nombre: esquemas.test.js
 * Descripción: Pruebas de las reglas de los formularios de Iniciar sesión y Cambiar contraseña (Yup, RP-07):
 *              los mismos límites y mensajes que el backend (RF-01 y RF-18), la contraseña de Iniciar sesión
 *              sin límites (RF-01 C8) y las reglas que se ven mientras se escribe la nueva. La contraseña
 *              nueva se revisa como la de un usuario, con sus validaciones y sus límites generados (RP-23).
 * Fecha de creación: 20/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { describe, expect, it } from 'vitest';
import { esquemaParaCrear, LIMITES } from '../generated/usuario/usuarioEsquemas.js';
import i18n from '../i18n/i18n.js';
import { esquemaCambioDeContrasena, esquemaInicioDeSesion } from './esquemas.js';
import { bytesUtf8, enBlanco, reglasDeContrasena } from './reglas.js';

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

/** Los valores del borde de una contraseña nueva: los de los criterios y los de las pruebas generadas. */
const BORDES_DE_CONTRASENA = [
  ['vacía', ''],
  ['8 espacios', ' '.repeat(8)],
  ['7 letras', 'a'.repeat(7)],
  ['8 letras', 'a'.repeat(8)],
  ['64 letras', 'a'.repeat(64)],
  ['65 letras', 'a'.repeat(65)],
  ['64 caracteres con 8 eñes (72 bytes)', 'ñ'.repeat(8) + 'a'.repeat(56)],
  ['64 caracteres con 9 eñes (73 bytes)', 'ñ'.repeat(9) + 'a'.repeat(55)],
  ['37 eñes (74 bytes)', 'ñ'.repeat(37)],
  ['65 eñes, que se pasan del largo y de los bytes', 'ñ'.repeat(65)],
];

/**
 * El error de la contraseña de un usuario nuevo con las validaciones generadas de su JSON (RP-23), o null si cumple.
 *
 * @param {string} contrasena la contraseña
 * @returns {Promise<string|null>} el mensaje
 */
const errorGenerado = (contrasena) => errorDe(esquemaParaCrear(i18n.t), 'contrasena', { contrasena });

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

  it('RF-18 · C4 · con 65 eñes, que se pasan del largo y de los bytes, la contraseña nueva da el error de los bytes, como la API', async () => {
    expect(await errorDe(esquema(), 'contrasenaNueva', conNueva('ñ'.repeat(65))))
      .toBe('La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes');
  });

  it.each(BORDES_DE_CONTRASENA)('RP-23 · la contraseña nueva se revisa con las validaciones generadas de la contraseña de un usuario, en el orden de la API: el mismo error que al crearlo: %s',
    async (_, nueva) => {
      expect(await errorDe(esquema(), 'contrasenaNueva', conNueva(nueva))).toBe(await errorGenerado(nueva));
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

  it('RF-01 · C8 · la contraseña para iniciar sesión no tiene límite de largo ni de bytes: la que no sirve la rechaza el backend', async () => {
    const datos = { correo: 'ana.perez@cacaosdelhuila.co', contrasena: `Clave-Segura-2026-${'ñ'.repeat(50)}` };

    expect(await errorDe(esquemaInicioDeSesion(i18n.t), 'contrasena', datos)).toBeNull();
  });
});

describe('reglas de la contraseña nueva', () => {
  it.each([
    ['vacía', '', { largo: false, bytes: true, espacios: false }],
    ['7 letras', 'a'.repeat(7), { largo: false, bytes: true, espacios: true }],
    ['8 letras', 'a'.repeat(8), { largo: true, bytes: true, espacios: true }],
    ['8 espacios', ' '.repeat(8), { largo: true, bytes: true, espacios: false }],
    ['64 caracteres con 8 eñes (72 bytes)', 'ñ'.repeat(8) + 'a'.repeat(56), { largo: true, bytes: true, espacios: true }],
    ['64 caracteres con 9 eñes (73 bytes)', 'ñ'.repeat(9) + 'a'.repeat(55), { largo: true, bytes: false, espacios: true }],
    ['65 letras', 'a'.repeat(65), { largo: false, bytes: true, espacios: true }],
  ])('RF-18 · las reglas que se ven mientras se escribe dicen cuáles se cumplen: %s', (_, contrasena, reglas) => {
    expect(reglasDeContrasena(contrasena, LIMITES.contrasena)).toEqual(reglas);
  });

  it('RP-23 · las reglas de la contraseña nueva siguen los límites que les llegan del JSON de usuario: con otros, cambian', () => {
    const limites = { minimo: 10, maximo: 12, maximoBytes: 13 };

    expect(reglasDeContrasena('a'.repeat(9), limites).largo).toBe(false);
    expect(reglasDeContrasena('a'.repeat(13), limites).largo).toBe(false);
    expect(reglasDeContrasena('ñ'.repeat(7), limites).bytes).toBe(false);
    expect(reglasDeContrasena('a'.repeat(12), limites)).toEqual({ largo: true, bytes: true, espacios: true });
  });

  it.each(BORDES_DE_CONTRASENA)('RP-23 · las reglas que se ven mientras se escribe aceptan lo mismo que las validaciones generadas de la contraseña de un usuario: %s',
    async (_, contrasena) => {
      const cumpleTodas = Object.values(reglasDeContrasena(contrasena, LIMITES.contrasena)).every(Boolean);

      expect(cumpleTodas).toBe(await errorGenerado(contrasena) === null);
    });

  it.each([['', true], ['  \t\n', true], ['a', false], [' a ', false], ['\u00a0', false]])(
    'RF-18 · un dato está en blanco con las mismas reglas de @NotBlank en el backend: «%s» → %s',
    (texto, enBlancoEsperado) => {
      expect(enBlanco(texto)).toBe(enBlancoEsperado);
    },
  );
});

describe('bytes en UTF-8', () => {
  it.each([['a', 1], ['ñ', 2], ['á', 2], ['€', 3], ['', 0]])(
    'RF-18 · cada letra con tilde y cada eñe ocupan 2 bytes, como los cuenta BCrypt: «%s» ocupa %i',
    (texto, bytes) => {
      expect(bytesUtf8(texto)).toBe(bytes);
    },
  );
});
