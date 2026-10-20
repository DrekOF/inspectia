/*
 * Nombre: stryker.config.mjs
 * Descripción: Configuración de las pruebas de mutación del frontend con Stryker (RP-12): cambia la lógica de
 *              la sesión, el cliente de la API, las validaciones y el menú, y revisa que las pruebas unitarias
 *              lo noten.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */

/** @type {import('@stryker-mutator/api/core').PartialStrykerOptions} */
export default {
  testRunner: 'vitest',
  vitest: { configFile: 'vite.config.js' },
  mutate: [
    'src/api/**/*.js',
    'src/sesion/**/*.js',
    'src/validacion/**/*.js',
    'src/navegacion/menu.js',
    'herramientas/**/*.js',
    '!**/*.test.js',
  ],
  reporters: ['clear-text', 'progress', 'html'],
  htmlReporter: { fileName: 'reports/mutacion/index.html' },
  thresholds: { high: 85, low: 70, break: null },
};
