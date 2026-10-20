/*
 * Nombre: textos-del-backend.test.js
 * Descripción: Pruebas de la lectura de los .properties del backend para los textos del frontend (RP-11):
 *              comentarios, escapes, caracteres en UTF-8 y la sintaxis de Java, las mismas claves en español y
 *              en inglés, y el plugin de Vite que los entrega a la app.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { tmpdir } from 'node:os';
import { basename } from 'node:path';
import { describe, expect, it } from 'vitest';
import { leerProperties, pluginTextosDelBackend, raizDelRepositorio, textosDelBackend } from './textos-del-backend.js';

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

  it('RP-11 · se leen como en Java las claves con escapes, el espacio como separador, la sangría, una clave sin valor y una barra al final del archivo', () => {
    const texto = [
      'con\\:dos\\ puntos=valor',
      'separada por espacio',
      '   con.sangria : valor',
      'solo.clave',
      'al.final=termina en barra\\',
    ].join('\n');

    expect(leerProperties(texto)).toEqual({
      'con:dos puntos': 'valor',
      separada: 'por espacio',
      'con.sangria': 'valor',
      'solo.clave': '',
      'al.final': 'termina en barra',
    });
  });

  it('RP-11 · los textos del backend tienen las mismas claves en español y en inglés', () => {
    const { es, en } = textosDelBackend();

    expect(Object.keys(es).length).toBeGreaterThan(100);
    expect(Object.keys(en).sort()).toEqual(Object.keys(es).sort());
    expect(es['error.credenciales_incorrectas']).toBe('Correo o contraseña incorrectos');
    expect(en['validacion.obligatorio']).toBe('This field is required.');
  });

  it('RP-11 · el plugin de Vite entrega los textos como el módulo virtual:textos-del-backend y vigila los cuatro .properties', () => {
    const plugin = pluginTextosDelBackend();
    const vigilados = [];
    const id = plugin.resolveId('virtual:textos-del-backend');

    const codigo = plugin.load.call({ addWatchFile: (archivo) => vigilados.push(archivo) }, id);

    expect(plugin.resolveId('./otro.js')).toBeNull();
    expect(plugin.load.call({ addWatchFile: () => {} }, './otro.js')).toBeNull();
    expect(vigilados.map((archivo) => basename(archivo)).sort())
      .toEqual(['generado.properties', 'generado_en.properties', 'mensajes.properties', 'mensajes_en.properties']);
    expect(codigo.startsWith('export default ')).toBe(true);
    expect(JSON.parse(codigo.slice('export default '.length, -1))).toEqual(textosDelBackend());
  });

  it('RP-11 · si no encuentra los .properties del backend, lo dice en vez de dejar la app sin textos', () => {
    expect(() => raizDelRepositorio(tmpdir())).toThrow('No se encontraron los textos del backend');
  });
});
