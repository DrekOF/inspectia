/*
 * Nombre: playwright.config.js
 * Descripción: Configuración de las pruebas de punta a punta con Playwright (RP-14): corren contra el
 *              entorno aislado (backend, base y app compilada) en PC y en celular, con los motores de Chrome,
 *              Firefox y Safari (RNF-05).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { defineConfig, devices } from '@playwright/test';

/** Dirección de la app compilada que sirve el entorno aislado (e2e/entorno). */
const APP = process.env.INSPECTIA_E2E_URL ?? 'http://localhost:4173';

export default defineConfig({
  testDir: './e2e',
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
  projects: [
    { name: 'pc-chrome', use: { ...devices['Desktop Chrome'], viewport: { width: 1280, height: 800 } } },
    { name: 'pc-firefox', use: { ...devices['Desktop Firefox'], viewport: { width: 1280, height: 800 } } },
    { name: 'pc-safari', use: { ...devices['Desktop Safari'], viewport: { width: 1280, height: 800 } } },
    { name: 'celular-chrome', use: { ...devices['Pixel 7'] } },
    { name: 'celular-safari', use: { ...devices['iPhone 13'] } },
  ],
});
