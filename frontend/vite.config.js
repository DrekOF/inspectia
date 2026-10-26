/*
 * Nombre: vite.config.js
 * Descripción: Configuración de Vite (servidor de desarrollo y compilación) y de Vitest (pruebas
 *              unitarias y de componentes) del frontend de InspectIA. Los textos que comparte con el
 *              backend llegan por el plugin de herramientas/textos-del-backend.js (RP-11).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';
import { pluginTextosDelBackend } from './herramientas/textos-del-backend.js';

/**
 * Dirección del backend en desarrollo. Vite le pasa las peticiones a `/api`, así el navegador ve un
 * solo origen y el backend no necesita CORS (D-31). Se cambia con INSPECTIA_API_URL si el backend
 * corre en otro puerto.
 */
const API = process.env.INSPECTIA_API_URL ?? 'http://localhost:8080';

export default defineConfig({
  plugins: [react(), pluginTextosDelBackend()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: API, changeOrigin: false },
    },
  },
  preview: {
    port: 4173,
    proxy: {
      '/api': { target: API, changeOrigin: false },
    },
  },
  build: {
    rolldownOptions: {
      output: {
        // Las bibliotecas van aparte del código de la app: cambian poco, así el navegador las guarda entre
        // versiones, y ningún archivo pasa de 500 kB.
        codeSplitting: {
          groups: [
            { name: 'react', test: /node_modules[\\/](react|react-dom|react-router|scheduler)[\\/]/ },
            { name: 'material-ui', test: /node_modules[\\/](@mui|@emotion|stylis)[\\/]/ },
            { name: 'bibliotecas', test: /node_modules[\\/]/ },
          ],
        },
      },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/pruebas/preparacion.js'],
    include: ['src/**/*.test.{js,jsx}', 'herramientas/**/*.test.js'],
    restoreMocks: true,
    coverage: {
      include: ['src/**/*.{js,jsx}', 'herramientas/**/*.js'],
      exclude: ['src/**/*.test.{js,jsx}', 'src/pruebas/**', 'src/main.jsx'],
    },
  },
});
