/*
 * Nombre: otra-tematica.test.js
 * Descripción: Pruebas de lo que calcula la demostración de otra temática (RP-23): el alcance con producto, lo que
 *              escribió el generador listado por capa y el enlace a node_modules en la copia del frontend, que en
 *              Windows es un junction. Encender la copia (Docker, Maven y la página) se prueba corriendo la
 *              demostración.
 * Fecha de creación: 28/10/2026
 * Actualización: 28/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { lstatSync, mkdirSync, mkdtempSync, readFileSync, realpathSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { afterEach, describe, expect, it } from 'vitest';
import { alcanceConProducto, enlazarModulos, porCapa, tipoDeEnlace } from './otra-tematica.js';

describe('alcance de la demostración', () => {
  it('RP-23 · el alcance de la demostración agrega producto al final de la API y de las pantallas, y deja lo demás igual', () => {
    const alcance = { _header: { version: '1.5' }, api: ['empresa', 'usuario'], pantallas: ['empresa'] };

    expect(alcanceConProducto(alcance)).toEqual({
      _header: { version: '1.5' }, api: ['empresa', 'usuario', 'producto'], pantallas: ['empresa', 'producto'],
    });
    expect(alcance).toEqual({ _header: { version: '1.5' }, api: ['empresa', 'usuario'], pantallas: ['empresa'] });
  });
});

describe('lo generado, por capa', () => {
  it('RP-23 · lo que escribió el generador para producto se lista por capa, en orden: de la base a las pruebas de la pantalla', () => {
    const escritos = [
      'frontend/src/generated/producto/ProductoPantalla.jsx',
      'backend/src/generated/java/co/edu/usco/inspectia/producto/ProductoControlador.java',
      'backend/src/generated/resources/db/migration/V1__esquema_inicial.sql',
      'frontend/e2e/generated/producto.spec.js',
      'backend/src/test-generated/java/co/edu/usco/inspectia/producto/ProductoApiGeneradaIT.java',
      'backend/src/generated/resources/i18n/generado_en.properties',
      'frontend/src/generated/producto/productoEsquemas.test.js',
      'frontend/src/generated/textos.js',
      'backend/src/generated/java/co/edu/usco/inspectia/producto/Producto.java',
      'frontend/src/generated/pantallas.js',
      'frontend/src/generated/producto/ProductoPantalla.test.jsx',
    ];

    expect(porCapa(escritos)).toEqual([
      { capa: 'Base de datos', archivos: ['backend/src/generated/resources/db/migration/V1__esquema_inicial.sql'] },
      { capa: 'API del backend', archivos: [
        'backend/src/generated/java/co/edu/usco/inspectia/producto/Producto.java',
        'backend/src/generated/java/co/edu/usco/inspectia/producto/ProductoControlador.java',
      ] },
      { capa: 'Pruebas de la API', archivos: [
        'backend/src/test-generated/java/co/edu/usco/inspectia/producto/ProductoApiGeneradaIT.java',
      ] },
      { capa: 'Textos, en español y en inglés', archivos: [
        'backend/src/generated/resources/i18n/generado_en.properties', 'frontend/src/generated/textos.js',
      ] },
      { capa: 'Pantalla', archivos: [
        'frontend/src/generated/pantallas.js', 'frontend/src/generated/producto/ProductoPantalla.jsx',
      ] },
      { capa: 'Pruebas de la pantalla', archivos: [
        'frontend/e2e/generated/producto.spec.js', 'frontend/src/generated/producto/ProductoPantalla.test.jsx',
        'frontend/src/generated/producto/productoEsquemas.test.js',
      ] },
    ]);
  });

  it('RP-23 · una capa en la que el generador no escribió nada no se lista', () => {
    expect(porCapa(['frontend/src/generated/textos.js'])).toEqual([
      { capa: 'Textos, en español y en inglés', archivos: ['frontend/src/generated/textos.js'] },
    ]);
  });
});

describe('tipo de enlace', () => {
  it('RP-23 · en Windows el enlace a node_modules es un junction, que no pide permisos de administrador; en los demás, un enlace simbólico', () => {
    expect(tipoDeEnlace('win32')).toBe('junction');
    expect(tipoDeEnlace('linux')).toBe('dir');
    expect(tipoDeEnlace('darwin')).toBe('dir');
  });
});

describe('enlace a node_modules', () => {
  let base;

  afterEach(() => {
    rmSync(base, { recursive: true, force: true });
  });

  it('RP-23 · la copia del frontend usa el node_modules del original por un enlace, sin copiarlo; si ya lo tiene, lo deja', () => {
    base = mkdtempSync(join(tmpdir(), 'demostracion-'));
    const frontend = join(base, 'frontend');
    const copia = join(base, 'copia');
    mkdirSync(join(frontend, 'node_modules', 'vite'), { recursive: true });
    writeFileSync(join(frontend, 'node_modules', 'vite', 'package.json'), '{}');
    mkdirSync(copia);

    enlazarModulos(frontend, copia);
    enlazarModulos(frontend, copia);

    const enlace = join(copia, 'node_modules');
    expect(lstatSync(enlace).isSymbolicLink()).toBe(true);
    expect(realpathSync(enlace)).toBe(realpathSync(join(frontend, 'node_modules')));
    expect(readFileSync(join(enlace, 'vite', 'package.json'), 'utf8')).toBe('{}');
  });
});
