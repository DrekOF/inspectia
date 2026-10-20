/*
 * Nombre: i18n.test.js
 * Descripción: Pruebas de los textos de la app (RP-11): los propios del frontend existen en español y en
 *              inglés, y los compartidos con el backend salen de sus archivos .properties, con sus marcadores.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { afterEach, describe, expect, it } from 'vitest';
import en from './en.js';
import es from './es.js';
import i18n from './i18n.js';

afterEach(async () => {
  await i18n.changeLanguage('es');
});

describe('textos de la app', () => {
  it('RP-11 · cada texto propio del frontend existe en español y en inglés, y ninguno está vacío', () => {
    expect(Object.keys(en).sort()).toEqual(Object.keys(es).sort());
    for (const textos of [es, en]) {
      expect(Object.entries(textos).filter(([, texto]) => !texto?.trim())).toEqual([]);
    }
  });

  it('RP-11 · la app está en español si la persona no eligió otro idioma', () => {
    expect(i18n.language).toBe('es');
    expect(i18n.t('iniciarSesion.titulo')).toBe('Iniciar sesión');
  });

  it.each([
    ['es', 'error.credenciales_incorrectas', 'Correo o contraseña incorrectos'],
    ['en', 'error.credenciales_incorrectas', 'Incorrect email or password'],
    ['es', 'usuario.contrasena_hash.longitud_min', 'La contraseña debe tener al menos 8 caracteres.'],
    ['es', 'usuario.rol.ADMIN_PLATAFORMA', 'Administrador de la plataforma'],
    ['en', 'rango_recomendacion.etiqueta.plural', 'Recommendation ranges'],
  ])('RP-11 · los textos compartidos con el backend salen de sus archivos .properties: %s %s', async (idioma, clave, texto) => {
    await i18n.changeLanguage(idioma);

    expect(i18n.t(clave)).toBe(texto);
  });

  it('RP-11 · los marcadores del backend, como {0} y {1}, se reemplazan igual que en el backend', () => {
    expect(i18n.t('error.ayuda_bloqueo', { 0: 5, 1: 15 })).toBe('Si fallas 5 veces seguidas, la cuenta se bloquea 15 minutos.');
    expect(i18n.t('validacion.longitud_max', { max: 300 })).toBe('Puede tener como máximo 300 caracteres.');
  });
});
