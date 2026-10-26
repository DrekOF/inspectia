/*
 * Nombre: volver.test.js
 * Descripción: Pruebas de a dónde va la persona al iniciar sesión de nuevo después de un 401: a la pantalla
 *              anterior solo si entra el mismo usuario y la ruta es interna; si no, al inicio de su rol.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { describe, expect, it } from 'vitest';
import { USUARIOS } from '../pruebas/ayudas.jsx';
import { destinoTrasIniciarSesion, rutaInterna } from './volver.js';

describe('volver tras iniciar sesión de nuevo', () => {
  it.each([
    ['/cambiar-contrasena', true],
    ['/', true],
    ['/historial?pagina=2', true],
    ['//sitio.example', false],
    ['/\\sitio.example', false],
    ['https://sitio.example/', false],
    ['cambiar-contrasena', false],
    ['', false],
    [null, false],
  ])('RF-01 · solo se vuelve a una ruta interna: empieza por «/» y no por «//» ni «/\\»: %s', (ruta, interna) => {
    expect(rutaInterna(ruta)).toBe(interna);
  });

  const ana = USUARIOS.ADMIN_EMPRESA;

  it('RF-01 · si entra el mismo usuario, vuelve a la pantalla donde estaba', () => {
    expect(destinoTrasIniciarSesion(ana, { ruta: '/cambiar-contrasena', usuarioId: ana.id })).toBe('/cambiar-contrasena');
  });

  it('RF-01 · si entra otro usuario, va al inicio de su rol, no a la pantalla del anterior', () => {
    expect(destinoTrasIniciarSesion(USUARIOS.USUARIO_FINAL, { ruta: '/usuarios', usuarioId: ana.id })).toBe('/');
  });

  it.each([['//sitio.example'], ['https://sitio.example/'], ['/iniciar-sesion']])(
    'RF-01 · con una ruta externa, o la misma de Iniciar sesión, va al inicio de su rol: %s',
    (ruta) => {
      expect(destinoTrasIniciarSesion(ana, { ruta, usuarioId: ana.id })).toBe('/');
    },
  );

  it('RF-01 · sin pantalla anterior, como al abrir la app, va al inicio de su rol', () => {
    expect(destinoTrasIniciarSesion(ana, null)).toBe('/');
  });
});
