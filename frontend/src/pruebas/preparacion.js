/*
 * Nombre: preparacion.js
 * Descripción: Preparación de las pruebas unitarias y de componentes (Vitest con jsdom): comparaciones de
 *              jest-dom, la API simulada con MSW, lo que jsdom no trae, como matchMedia, cuánto esperan findBy y
 *              waitFor, y cada prueba en español y sin nada guardado en el navegador.
 * Fecha de creación: 20/10/2026
 * Actualización: 28/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import '@testing-library/jest-dom/vitest';
import { cleanup, configure } from '@testing-library/react';
import { afterAll, afterEach, beforeAll, vi } from 'vitest';
import i18n from '../i18n/i18n.js';
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

/*
 * findBy y waitFor esperan hasta 5 s, no el segundo de Testing Library. Con la máquina ocupada, como con dos corridas
 * a la vez o con Stryker, una pantalla que se carga la primera vez que se abre (lazy) tarda más de un segundo en
 * aparecer, y la prueba fallaba sin un error de verdad. Una prueba que pasa no espera más por esto: sigue apenas
 * aparece lo que busca.
 */
configure({ asyncUtilTimeout: 5000 });

beforeAll(() => servidor.listen({ onUnhandledRequest: 'error' }));

afterEach(async () => {
  cleanup();
  servidor.resetHandlers();
  // i18next es uno solo para todas las pruebas de un archivo: cada prueba empieza en español.
  await i18n.changeLanguage('es');
  localStorage.clear();
  sessionStorage.clear();
  anchoActual = 1280;
  window.history.replaceState({}, '', '/');
});

afterAll(() => servidor.close());
