/*
 * Nombre: claseInicial.test.js
 * Descripción: Pruebas de la clase del tema con que arranca la página (RP-08): la que eligió la persona o la del
 *              sistema, como la decide Material UI, también sin acceso al almacenamiento del navegador.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { describe, expect, it } from 'vitest';
import { CLAVES_DEL_TEMA, esquemaInicial } from './claseInicial.js';

/**
 * Un almacenamiento del navegador con el modo elegido.
 *
 * @param {string|null} modo el modo guardado: light, dark, system o ninguno
 * @returns {{ getItem: (clave: string) => string|null }} el almacenamiento
 */
const almacenCon = (modo) => ({ getItem: (clave) => (clave === CLAVES_DEL_TEMA.modo ? modo : null) });

/**
 * Una ventana con el tema del sistema.
 *
 * @param {boolean} oscuro si el sistema está en oscuro
 * @returns {{ matchMedia: (consulta: string) => { matches: boolean } }} la ventana
 */
const sistema = (oscuro) => ({ matchMedia: (consulta) => ({ matches: oscuro && consulta.includes('dark') }) });

describe('Clase inicial del tema', () => {
  it('RP-08 · con un tema elegido, la app arranca con ese, aunque el sistema esté en el otro', () => {
    expect(esquemaInicial(almacenCon('dark'), sistema(false))).toBe('dark');
    expect(esquemaInicial(almacenCon('light'), sistema(true))).toBe('light');
  });

  it('RP-08 · con el tema del sistema, o sin elegir todavía, arranca con el del sistema', () => {
    expect(esquemaInicial(almacenCon('system'), sistema(true))).toBe('dark');
    expect(esquemaInicial(almacenCon(null), sistema(true))).toBe('dark');
    expect(esquemaInicial(almacenCon(null), sistema(false))).toBe('light');
  });

  it('RP-08 · sin acceso al almacenamiento del navegador, arranca con el tema del sistema', () => {
    const bloqueado = { getItem: () => { throw new Error('Acceso denegado'); } };

    expect(esquemaInicial(bloqueado, sistema(true))).toBe('dark');
  });
});
