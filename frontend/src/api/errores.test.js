/*
 * Nombre: errores.test.js
 * Descripción: Pruebas del texto que muestra la app para un error de la API (RNF-06 C3): el mensaje del
 *              backend o, si no hubo conexión o el servidor no dio un mensaje, qué pasó y qué hacer; y de
 *              los errores de cada campo que se marcan en el formulario (RF-18).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { afterEach, describe, expect, it } from 'vitest';
import i18n from '../i18n/i18n.js';
import { ErrorDeApi } from './cliente.js';
import { erroresPorCampo, textoDelError } from './errores.js';

const SIN_CONEXION = 'No se pudo conectar con el servidor. Revisa tu conexión a internet e intenta de nuevo.';
const INESPERADO = 'Ocurrió un error inesperado. Intenta de nuevo más tarde.';

afterEach(async () => {
  await i18n.changeLanguage('es');
});

describe('texto de un error', () => {
  it('RNF-06 · C3 · el mensaje del backend se muestra tal cual, porque ya dice qué pasó y qué hacer', () => {
    const error = new ErrorDeApi({ estado: 401, detalle: 'Correo o contraseña incorrectos' });

    expect(textoDelError(error, i18n.t)).toBe('Correo o contraseña incorrectos');
  });

  it('RNF-06 · C3 · sin conexión dice que no se pudo conectar con el servidor y que revise la conexión', () => {
    expect(textoDelError(new ErrorDeApi({ estado: 0 }), i18n.t)).toBe(SIN_CONEXION);
  });

  it('RNF-06 · C3 · una falla sin mensaje del backend dice que fue inesperada y que intente más tarde', () => {
    expect(textoDelError(new ErrorDeApi({ estado: 502 }), i18n.t)).toBe(INESPERADO);
  });

  it('RNF-06 · C3 · un error que no viene de la API también dice qué hacer', () => {
    expect(textoDelError(new TypeError('Failed to fetch'), i18n.t)).toBe(INESPERADO);
  });

  it('RP-11 · sin conexión, el aviso sale en el idioma de la app', async () => {
    await i18n.changeLanguage('en');

    expect(textoDelError(new ErrorDeApi({ estado: 0 }), i18n.t))
      .toBe('Could not connect to the server. Check your internet connection and try again.');
  });
});

describe('errores por campo', () => {
  const CAMPOS = ['contrasenaActual', 'contrasenaNueva'];
  const AYUDA = 'Si fallas 5 veces seguidas, la cuenta se bloquea 15 minutos.';

  it('RF-18 · C2 · el error de un campo del formulario va en ese campo, con la ayuda del backend', () => {
    const error = new ErrorDeApi({ estado: 400, ayuda: AYUDA,
      errores: [{ campo: 'contrasenaActual', mensaje: 'La contraseña actual no es correcta.' }] });

    expect(erroresPorCampo(error, CAMPOS)).toEqual({ contrasenaActual: `La contraseña actual no es correcta. ${AYUDA}` });
  });

  it('RF-18 · con varios campos marcados, la ayuda va solo con el primero del formulario', () => {
    const error = new ErrorDeApi({ estado: 400, ayuda: AYUDA, errores: [
      { campo: 'contrasenaNueva', mensaje: 'La contraseña debe tener al menos 8 caracteres.' },
      { campo: 'contrasenaActual', mensaje: 'Este dato es obligatorio.' },
    ] });

    expect(erroresPorCampo(error, CAMPOS)).toEqual({
      contrasenaActual: `Este dato es obligatorio. ${AYUDA}`,
      contrasenaNueva: 'La contraseña debe tener al menos 8 caracteres.',
    });
  });

  it('RF-18 · sin ayuda del backend, cada campo lleva solo su mensaje', () => {
    const error = new ErrorDeApi({ estado: 400, errores: [{ campo: 'contrasenaNueva', mensaje: 'Este dato es obligatorio.' }] });

    expect(erroresPorCampo(error, CAMPOS)).toEqual({ contrasenaNueva: 'Este dato es obligatorio.' });
  });

  it.each([
    ['un campo que no está en el formulario', new ErrorDeApi({ estado: 400, errores: [{ campo: 'otro', mensaje: 'x' }] })],
    ['un error sin campos, como el bloqueo', new ErrorDeApi({ estado: 429, detalle: 'Tu cuenta está bloqueada' })],
    ['un error que no viene de la API', new TypeError('Failed to fetch')],
  ])('RF-18 · %s no marca ningún campo: se muestra como aviso', (_, error) => {
    expect(erroresPorCampo(error, CAMPOS)).toBeNull();
  });
});
