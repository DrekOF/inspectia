/*
 * Nombre: almacen.test.js
 * Descripción: Pruebas de la sesión guardada en el navegador (D-28, D-31): sobrevive a recargar la página,
 *              no se usa vencida y cerrar sesión la borra (RF-01).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { describe, expect, it } from 'vitest';
import { USUARIOS } from '../pruebas/ayudas.jsx';
import { borrarSesion, CLAVE_SESION, guardarSesion, leerSesion } from './almacen.js';

const AHORA = new Date('2026-10-20T15:00:00Z');
const sesion = (vence) => ({ token: 'token-de-prueba', vence, usuario: USUARIOS.ADMIN_EMPRESA });

describe('sesión guardada', () => {
  it('RF-01 · la sesión queda guardada en el navegador y sobrevive a recargar la página (D-31)', () => {
    guardarSesion(sesion('2026-10-20T23:00:00Z'));

    expect(leerSesion(AHORA)).toEqual(sesion('2026-10-20T23:00:00Z'));
    expect(localStorage.getItem(CLAVE_SESION)).not.toBeNull();
  });

  it.each([['ya venció', '2026-10-20T14:59:59Z'], ['vence en este instante', '2026-10-20T15:00:00Z']])(
    'RF-01 · C6 · una sesión vencida no se usa: al leerla se borra y hay que iniciar sesión de nuevo: %s',
    (_, vence) => {
      guardarSesion(sesion(vence));

      expect(leerSesion(AHORA)).toBeNull();
      expect(localStorage.getItem(CLAVE_SESION)).toBeNull();
    },
  );

  it('RF-01 · una sesión guardada que no se puede leer se borra', () => {
    localStorage.setItem(CLAVE_SESION, '{no es JSON');

    expect(leerSesion(AHORA)).toBeNull();
    expect(localStorage.getItem(CLAVE_SESION)).toBeNull();
  });

  it('RF-01 · cerrar sesión borra el token del navegador (D-28)', () => {
    guardarSesion(sesion('2026-10-20T23:00:00Z'));

    borrarSesion();

    expect(leerSesion(AHORA)).toBeNull();
  });
});
