/*
 * Nombre: preparacion.js
 * Descripción: Preparación de las pruebas unitarias y de componentes (Vitest con jsdom): comparaciones de
 *              jest-dom, la API simulada con MSW y lo que jsdom no trae, como matchMedia.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterAll, afterEach, beforeAll, vi } from 'vitest';
import { servidor } from './servidor.js';

/*
 * jsdom no calcula estilos ni tamaños de pantalla: matchMedia responde siempre «no», así las pruebas ven
 * la versión de PC salvo que una pida otra con anchoDePantalla().
 */
let anchoActual = 1280;

/**
 * Simula el ancho de la ventana para las consultas de medios de Material UI (useMediaQuery).
 *
 * @param {number} ancho el ancho en píxeles, como 390 para un celular
 */
export function anchoDePantalla(ancho) {
  anchoActual = ancho;
}

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn((consulta) => {
    const minimo = /min-width:\s*(\d+(?:\.\d+)?)px/.exec(consulta);
    const maximo = /max-width:\s*(\d+(?:\.\d+)?)px/.exec(consulta);
    const cumple = (!minimo || anchoActual >= Number(minimo[1])) && (!maximo || anchoActual <= Number(maximo[1]));
    return {
      matches: cumple && !consulta.includes('prefers-color-scheme: dark'),
      media: consulta,
      onchange: null,
      addListener: () => {},
      removeListener: () => {},
      addEventListener: () => {},
      removeEventListener: () => {},
      dispatchEvent: () => false,
    };
  }),
});

beforeAll(() => servidor.listen({ onUnhandledRequest: 'error' }));

afterEach(() => {
  cleanup();
  servidor.resetHandlers();
  localStorage.clear();
  sessionStorage.clear();
  anchoActual = 1280;
  window.history.replaceState({}, '', '/');
});

afterAll(() => servidor.close());
