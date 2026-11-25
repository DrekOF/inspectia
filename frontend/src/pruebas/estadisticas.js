/*
 * Nombre: estadisticas.js (pruebas)
 * Descripción: Estadísticas de ejemplo como las responde el backend (RF-16), con lotes y sin ellos, y la respuesta
 *              simulada de GET /api/v1/estadisticas, que anota los periodos pedidos, para las pruebas de la pantalla
 *              Estadísticas.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { http, HttpResponse } from 'msw';
import { servidor } from './servidor.js';

/**
 * Las estadísticas de un periodo de septiembre a noviembre de 2026: 10 lotes, octubre sin lotes, y «La Esperanza» y
 * «Paraíso» ya agrupadas por el backend, cada una una vez (RF-16 C4).
 */
export const ESTADISTICAS = {
  desde: '2026-09-01',
  hasta: '2026-11-30',
  sinLotes: false,
  lotesAnalizados: 10,
  promedioSanos: 90.4,
  porCategoria: [
    { categoria: 'PREMIUM', lotes: 4 },
    { categoria: 'ESTANDAR', lotes: 3 },
    { categoria: 'CORRIENTE', lotes: 2 },
    { categoria: 'FUERA_DE_NORMA', lotes: 1 },
  ],
  porMes: [
    { mes: '2026-09', lotes: 4, promedioSanos: 88.5 },
    { mes: '2026-10', lotes: 0, promedioSanos: null },
    { mes: '2026-11', lotes: 6, promedioSanos: 91.7 },
  ],
  porOrigen: [
    { origen: 'La Esperanza', lotes: 6, promedioSanos: 93.2 },
    { origen: 'Paraíso', lotes: 4, promedioSanos: 86.2 },
  ],
};

/** Las estadísticas de un periodo sin lotes analizados (RF-16 C2). */
export const SIN_LOTES = {
  desde: '2026-09-01',
  hasta: '2026-11-30',
  sinLotes: true,
  lotesAnalizados: 0,
  promedioSanos: null,
  porCategoria: ['PREMIUM', 'ESTANDAR', 'CORRIENTE', 'FUERA_DE_NORMA'].map((categoria) => ({ categoria, lotes: 0 })),
  porMes: [],
  porOrigen: [],
};

/**
 * Responde GET /api/v1/estadisticas y anota los periodos pedidos.
 *
 * @param {(periodo: { desde: string|null, hasta: string|null }) => Response|object} responder la respuesta según el
 *   periodo pedido: un objeto va como JSON con 200
 * @returns {Array<{ desde: string|null, hasta: string|null }>} los periodos pedidos, en orden
 */
export function conEstadisticas(responder) {
  const pedidos = [];
  servidor.use(http.get('/api/v1/estadisticas', ({ request }) => {
    const parametros = new URL(request.url).searchParams;
    const periodo = { desde: parametros.get('desde'), hasta: parametros.get('hasta') };
    pedidos.push(periodo);
    const respuesta = responder(periodo);
    return respuesta instanceof Response ? respuesta : HttpResponse.json(respuesta);
  }));
  return pedidos;
}
