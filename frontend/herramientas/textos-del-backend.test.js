/*
 * Nombre: textos-del-backend.test.js
 * Descripción: Pruebas de la lectura de los .properties del backend para los textos del frontend (RP-11):
 *              comentarios, escapes y caracteres en UTF-8, y las mismas claves en español y en inglés.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { describe, expect, it } from 'vitest';
import { leerProperties, textosDelBackend } from './textos-del-backend.js';

describe('textos del backend', () => {
  it('RP-11 · se leen los .properties del backend: comentarios, escapes y letras con tilde en UTF-8', () => {
    const texto = [
      '# Nombre: generado.properties',
      '! otro comentario',
      '',
      'validacion.obligatorio=Este dato es obligatorio.',
      'error.ayuda_bloqueo = Si fallas {0} veces seguidas, la cuenta se bloquea {1} minutos.',
      'con.escapes=línea uno\\nlínea dos, una barra \\\\ y \\u00f1',
      'empieza.con.espacio=\\  espacio al inicio',
      'larga=una línea \\',
      '    que sigue',
    ].join('\r\n');

    expect(leerProperties(texto)).toEqual({
      'validacion.obligatorio': 'Este dato es obligatorio.',
      'error.ayuda_bloqueo': 'Si fallas {0} veces seguidas, la cuenta se bloquea {1} minutos.',
      'con.escapes': 'línea uno\nlínea dos, una barra \\ y ñ',
      'empieza.con.espacio': '  espacio al inicio',
      larga: 'una línea que sigue',
    });
  });

  it('RP-11 · los textos del backend tienen las mismas claves en español y en inglés', () => {
    const { es, en } = textosDelBackend();

    expect(Object.keys(es).length).toBeGreaterThan(100);
    expect(Object.keys(en).sort()).toEqual(Object.keys(es).sort());
    expect(es['error.credenciales_incorrectas']).toBe('Correo o contraseña incorrectos');
    expect(en['validacion.obligatorio']).toBe('This field is required.');
  });
});
