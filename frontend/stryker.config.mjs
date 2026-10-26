/*
 * Nombre: stryker.config.mjs
 * Descripción: Configuración de las pruebas de mutación del frontend con Stryker (RP-12): cambia la lógica de
 *              la sesión, el cliente de la API, las validaciones, el menú, las pantallas de registros, la API y
 *              las validaciones generadas y las herramientas, y revisa que las pruebas unitarias lo noten.
 * Fecha de creación: 20/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
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
    'src/crud/**/*.js',
    'src/empresa/**/*.js',
    'src/generated/**/*Api.js',
    'src/generated/**/*Esquemas.js',
    'herramientas/**/*.js',
    '!**/*.test.js',
  ],
  // Lo que no hace falta para las pruebas unitarias no se copia a su carpeta de trabajo (.stryker-tmp), como
  // el entorno de punta a punta, que guarda una copia compilada del backend.
  ignorePatterns: ['e2e/.entorno', 'test-results', 'playwright-report', 'dist'],
  // Con un proceso por procesador, las pruebas de componentes se vuelven lentas y Stryker corta por tiempo
  // mutantes que no dejan un bucle infinito: los cuenta como detectados y el puntaje sube sin razón. Con la
  // mitad de los procesadores y 15 s de margen, solo se cortan los bucles infinitos de verdad.
  concurrency: '50%',
  timeoutMS: 15000,
  // Los mutantes estáticos (valores que se calculan al cargar un módulo, como las rutas del menú) no se evalúan
  // bien con Vitest: el resultado depende de si el módulo ya estaba cargado, no de las pruebas. La ruta de Inicio
  // «sobrevivía» aunque su prueba falla al cambiarla a mano. Se dejan fuera del puntaje.
  ignoreStatic: true,
  reporters: ['clear-text', 'progress', 'html'],
  htmlReporter: { fileName: 'reports/mutacion/index.html' },
  thresholds: { high: 85, low: 70, break: null },
};
