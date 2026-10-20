/*
 * Nombre: menu.test.js
 * Descripción: Pruebas del menú según el rol (RF-01 C1, RNF-04): las entradas y las rutas del prototipo para
 *              cada rol, y «Mi cuenta» para todos.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { describe, expect, it } from 'vitest';
import i18n from '../i18n/i18n.js';
import { MENU_DE_CUENTA, menuPrincipal } from './menu.js';

/**
 * Las entradas de un menú como las ve la persona: su texto en español y su ruta.
 *
 * @param {Array<{ etiqueta: string, ruta?: string }>} entradas las entradas del menú
 * @returns {Array<[string, string|undefined]>} texto y ruta de cada una
 */
const comoSeVen = (entradas) => entradas.map((e) => [i18n.t(e.etiqueta), e.ruta]);

describe('menú según el rol', () => {
  it.each([
    ['USUARIO_FINAL', [['Inicio', '/'], ['Nuevo lote', '/lotes/nuevo'], ['Historial', '/historial']]],
    ['ADMIN_EMPRESA', [['Inicio', '/'], ['Historial', '/historial'], ['Usuarios', '/usuarios'],
      ['Rangos de recomendación', '/rangos-recomendacion'], ['Estadísticas', '/estadisticas'], ['Modelo de IA', '/modelo']]],
    ['ADMIN_PLATAFORMA', [['Inicio', '/'], ['Empresas', '/empresas']]],
  ])('RF-01 · C1 · cada rol ve su menú, sin las funciones de los otros roles: %s', (rol, entradas) => {
    expect(comoSeVen(menuPrincipal(rol))).toEqual(entradas);
  });

  it('RF-01 · un rol que la app no conoce no ve funciones, solo «Mi cuenta»', () => {
    expect(menuPrincipal('OTRO')).toEqual([]);
  });

  it('RF-01 · «Mi cuenta» tiene Cambiar contraseña, Acerca de y Cerrar sesión, para todos los roles', () => {
    expect(comoSeVen(MENU_DE_CUENTA)).toEqual([
      ['Cambiar contraseña', '/cambiar-contrasena'], ['Acerca de', '/acerca-de'], ['Cerrar sesión', undefined]]);
  });
});
