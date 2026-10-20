/*
 * Nombre: playwright.config.js
 * Descripción: Configuración de las pruebas de punta a punta con Playwright (RP-14): corren contra el
 *              entorno aislado (backend, base y app compilada), que enciende e2e/entorno/global.js, en PC y
 *              en celular, con los motores de Chrome, Firefox y Safari (RNF-05).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { defineConfig, devices } from '@playwright/test';

/**
 * Dirección de la app compilada que sirve el entorno aislado. La pone e2e/entorno/global.js antes de abrir
 * los navegadores, porque cada corrida usa un puerto libre.
 */
const APP = process.env.INSPECTIA_E2E_URL ?? 'http://localhost:4173';

/**
 * Los navegadores: en PC, los motores de Chrome, Firefox y Safari; en celular, Chrome y Safari. Con
 * INSPECTIA_E2E_SIN_FIREFOX=1 se salta Firefox en los equipos donde no abre, como el de Playwright 1.64.1 en
 * Windows 11 25H2 (ver docs/bitacora.md); en CI siempre corre.
 */
const PROYECTOS = [
  { name: 'pc-chrome', use: { ...devices['Desktop Chrome'], viewport: { width: 1280, height: 800 } } },
  { name: 'pc-firefox', use: { ...devices['Desktop Firefox'], viewport: { width: 1280, height: 800 } } },
  { name: 'pc-safari', use: { ...devices['Desktop Safari'], viewport: { width: 1280, height: 800 } } },
  { name: 'celular-chrome', use: { ...devices['Pixel 7'] } },
  { name: 'celular-safari', use: { ...devices['iPhone 13'] } },
].filter(({ name }) => !(name === 'pc-firefox' && process.env.INSPECTIA_E2E_SIN_FIREFOX === '1'));

export default defineConfig({
  testDir: './e2e',
  globalSetup: './e2e/entorno/global.js',
  outputDir: './test-results',
  fullyParallel: true,
  forbidOnly: true,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: APP,
    locale: 'es-CO',
    timezoneId: 'America/Bogota',
    trace: 'retain-on-failure',
  },
  projects: PROYECTOS,
});
