/*
 * Nombre: vitest.selenium.config.js
 * Descripción: Configuración de Vitest para las pruebas con Selenium (RP-14): corren en Node contra el entorno
 *              aislado, que enciende e2e/entorno/global.js, una a la vez y con más tiempo, porque abren
 *              navegadores reales.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    environment: 'node',
    include: ['selenium/**/*.test.js'],
    globalSetup: ['./e2e/entorno/global.js'],
    testTimeout: 120_000,
    hookTimeout: 120_000,
    fileParallelism: false,
  },
});
