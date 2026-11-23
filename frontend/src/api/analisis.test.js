/*
 * Nombre: analisis.test.js (api)
 * Descripción: Pruebas de las peticiones del análisis de un lote (RF-05 C2, RNF-01 C4, RF-10): el envío de la muestra,
 *              su avance y el resultado van a sus rutas de la API, con el id del lote codificado.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { describe, expect, it, vi } from 'vitest';
import { avanceDelAnalisis, enviarMuestra, resultadoDelLote } from './analisis.js';

const LOTE = '0199f0a0-0000-7000-8000-000000000143';

describe('Peticiones del análisis', () => {
  it('RF-10 · el envío de la muestra, su avance y el resultado van a sus rutas de la API; el envío no lleva cuerpo', async () => {
    const pedir = vi.fn().mockResolvedValue({});

    await enviarMuestra(pedir, LOTE);
    await avanceDelAnalisis(pedir, LOTE);
    await resultadoDelLote(pedir, LOTE);

    expect(pedir.mock.calls).toEqual([
      ['POST', `/lotes/${LOTE}/analisis`],
      ['GET', `/lotes/${LOTE}/analisis`],
      ['GET', `/lotes/${LOTE}/resultado`],
    ]);
  });

  it('RNF-03 · un id con caracteres raros va codificado: no puede pedir otra ruta de la API', async () => {
    const pedir = vi.fn().mockResolvedValue({});

    await resultadoDelLote(pedir, '../usuarios?x=1');

    expect(pedir).toHaveBeenCalledWith('GET', '/lotes/..%2Fusuarios%3Fx%3D1/resultado');
  });
});
