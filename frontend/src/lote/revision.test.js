/*
 * Nombre: revision.test.js (lote)
 * Descripción: Pruebas de lo que la página decide sin dibujar nada sobre la revisión de las fotos por el servicio de
 *              IA: la lista de números en el idioma de la app, el porcentaje sin decimales si es entero, el total de
 *              granos de las fotos revisadas (RF-07 C3), las fotos por repetir (RF-06, RF-07 C2, RF-09 C1), por qué
 *              volvió la muestra sin resultado, del avance o de la muestra (D-44), y el máximo de dudosos del aviso de
 *              confianza baja (RF-09 C4 y C6).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { describe, expect, it } from 'vitest';
import { fotoCargada, fotoRevisada } from '../pruebas/fotos.js';
import { avance, resultadoPendiente } from '../pruebas/resultados.js';
import {
  devolucionDeLaMuestra, listaDeNumeros, numerosPorRepetir, porcentajeDelAviso, textoDelPorcentaje, totalDeGranos,
} from './revision.js';

describe('Textos de la revisión', () => {
  it('RNF-06 · C3 · los números de las fotos se juntan como en el idioma: «1 y 2», «1, 2 y 3» y «1 and 2»', () => {
    expect(listaDeNumeros([2], 'es-CO')).toBe('2');
    expect(listaDeNumeros([1, 2], 'es-CO')).toBe('1 y 2');
    expect(listaDeNumeros([1, 2, 3], 'es-CO')).toBe('1, 2 y 3');
    expect(listaDeNumeros([1, 2], 'en-US')).toBe('1 and 2');
  });

  it('RF-09 · C4 · el máximo de dudosos va sin decimales si es entero, «10», y con uno si no, «10,5» o «10.5»', () => {
    expect(textoDelPorcentaje(10, 'es-CO')).toBe('10');
    expect(textoDelPorcentaje(10.5, 'es-CO')).toBe('10,5');
    expect(textoDelPorcentaje(10.5, 'en-US')).toBe('10.5');
  });
});

describe('Granos y fotos por repetir', () => {
  it('RF-07 · C3 · el total suma los granos que encontró la revisión en cada foto, también en una por repetir', () => {
    expect(totalDeGranos([fotoRevisada(1, { granosEncontrados: 52 }),
      fotoRevisada(2, { estado: 'POR_REPETIR', motivo: 'BORROSA', granosEncontrados: 47 })])).toBe(99);
  });

  it('RF-07 · C3 · sin fotos, o con una foto sin conteo, como una «Cargada», no hay total', () => {
    expect(totalDeGranos([])).toBeNull();
    expect(totalDeGranos([fotoRevisada(1), fotoCargada(2)])).toBeNull();
    expect(totalDeGranos([fotoRevisada(1), { ...fotoRevisada(2), granosEncontrados: null }])).toBeNull();
  });

  it('RF-06 · C1 · las fotos por repetir van por su número, de menor a mayor, sin las aprobadas ni las cargadas', () => {
    expect(numerosPorRepetir([
      fotoRevisada(3, { estado: 'POR_REPETIR', motivo: 'POCA_LUZ' }), fotoCargada(2),
      fotoRevisada(1, { estado: 'POR_REPETIR', motivo: 'BORROSA' }), fotoRevisada(4),
    ])).toEqual([1, 3]);
    expect(numerosPorRepetir([fotoCargada(1), fotoRevisada(2)])).toEqual([]);
  });
});

describe('Por qué volvió la muestra', () => {
  it('D-44 · con un avance consultado, el motivo, las fotos por repetir y las repeticiones salen del avance', () => {
    const { muestras: [muestra] } = resultadoPendiente('EN_PROCESO');
    const consultado = avance('PENDIENTE', {
      fallo: true, repeticiones: 1, motivoDevolucion: null, fotosPorRepetir: [{ numero: 2, motivo: 'GRANOS_DUDOSOS' }],
    });

    expect(devolucionDeLaMuestra(muestra, consultado)).toEqual({
      motivoDevolucion: null, repeticiones: 1, fotosPorRepetir: [{ numero: 2, motivo: 'GRANOS_DUDOSOS' }],
    });
  });

  it('D-44 · sin avance, al recargar la página, salen de la muestra del resultado', () => {
    const { muestras: [muestra] } = resultadoPendiente('PENDIENTE', {
      fechaEnvio: '2026-11-23T16:42:00Z', devolucion: { motivoDevolucion: 'POCOS_GRANOS', repeticiones: 0, fotosPorRepetir: [] },
    });

    expect(devolucionDeLaMuestra(muestra, null))
      .toEqual({ motivoDevolucion: 'POCOS_GRANOS', repeticiones: 0, fotosPorRepetir: [] });
  });

  it('D-44 · sin los campos nuevos, como con el clasificador falso, todo va vacío y la página muestra el mensaje general', () => {
    const { muestras: [muestra] } = resultadoPendiente('PENDIENTE', { fechaEnvio: '2026-11-23T16:42:00Z' });

    expect(devolucionDeLaMuestra(muestra, avance('PENDIENTE', { fallo: true })))
      .toEqual({ motivoDevolucion: null, repeticiones: 0, fotosPorRepetir: [] });
    expect(devolucionDeLaMuestra(null, null)).toEqual({ motivoDevolucion: null, repeticiones: 0, fotosPorRepetir: [] });
  });
});

describe('Aviso de confianza baja', () => {
  it('RF-09 · C6 · el máximo de dudosos del aviso es el de la primera muestra que lo tiene', () => {
    expect(porcentajeDelAviso([
      { numero: 1, avisoConfianzaBaja: false, porcentajeMaxDudosos: 8 },
      { numero: 2, avisoConfianzaBaja: true, porcentajeMaxDudosos: 10 },
    ])).toBe(10);
  });

  it('RF-09 · C4 · sin una muestra con el aviso, o sin su máximo, no hay porcentaje', () => {
    expect(porcentajeDelAviso([{ numero: 1, avisoConfianzaBaja: false, porcentajeMaxDudosos: 10 }])).toBeNull();
    expect(porcentajeDelAviso([{ numero: 1, avisoConfianzaBaja: true }])).toBeNull();
    expect(porcentajeDelAviso([])).toBeNull();
  });
});
