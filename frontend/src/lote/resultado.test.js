/*
 * Nombre: resultado.test.js (lote)
 * Descripción: Pruebas de lo que la página del resultado decide sin dibujar nada: el rango probable recortado a
 *              0–100 %, con un extremo recortado sin decimales (D-43), contado en décimas para no arrastrar errores de
 *              coma flotante; la muestra en curso, y qué mostrar de un lote según su estado y el de su muestra
 *              (RNF-01 C4, RNF-02 C3), también con la segunda muestra en espera, sin enviar, devuelta o terminada
 *              (D-25, RF-14 C6).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { describe, expect, it } from 'vitest';
import {
  avance, resultadoAnalizado, resultadoConDosMuestras, resultadoConSegundaMuestra, resultadoPendiente,
} from '../pruebas/resultados.js';
import { muestraEnCurso, rangoProbable, textosDelRango, vistaDelLote } from './resultado.js';

describe('Rango probable del porcentaje', () => {
  it('D-43 · el rango es el porcentaje guardado más y menos el margen guardado: 90,6 con 7,5 da de 83,1 a 98,1, sin errores de coma flotante', () => {
    expect(rangoProbable(90.6, 7.5)).toEqual({
      desde: { valor: 83.1, recortado: false }, hasta: { valor: 98.1, recortado: false },
    });
    expect(rangoProbable(0.3, 0.1)).toEqual({ desde: { valor: 0.2, recortado: false }, hasta: { valor: 0.4, recortado: false } });
  });

  it('D-43 · con 100,0 y margen 5,7 el rango se recorta arriba, y con 0,0 y margen 11,0 se recorta abajo (prueba de límite)', () => {
    expect(rangoProbable(100, 5.7)).toEqual({ desde: { valor: 94.3, recortado: false }, hasta: { valor: 100, recortado: true } });
    expect(rangoProbable(0, 11)).toEqual({ desde: { valor: 0, recortado: true }, hasta: { valor: 11, recortado: false } });
  });

  it('D-43 · un rango que llega justo a 0 o a 100 no se recorta: conserva su decimal (prueba de límite)', () => {
    expect(rangoProbable(7.5, 7.5)).toEqual({ desde: { valor: 0, recortado: false }, hasta: { valor: 15, recortado: false } });
    expect(rangoProbable(94.3, 5.7)).toEqual({ desde: { valor: 88.6, recortado: false }, hasta: { valor: 100, recortado: false } });
    expect(rangoProbable(94.4, 5.7).hasta).toEqual({ valor: 100, recortado: true });
  });

  it('D-43 · un extremo recortado se escribe sin decimales y los demás con uno, con coma en español y punto en inglés', () => {
    expect(textosDelRango(100, 5.7, 'es-CO')).toEqual({ desde: '94,3', hasta: '100' });
    expect(textosDelRango(0, 11, 'es-CO')).toEqual({ desde: '0', hasta: '11,0' });
    expect(textosDelRango(7.5, 7.5, 'es-CO')).toEqual({ desde: '0,0', hasta: '15,0' });
    expect(textosDelRango(90.6, 7.5, 'en-US')).toEqual({ desde: '83.1', hasta: '98.1' });
  });
});

describe('Qué muestra la página de un lote', () => {
  it('RNF-02 · C3 · la muestra en curso es la de número más alto; sin muestras, no hay', () => {
    expect(muestraEnCurso([{ numero: 2 }, { numero: 1 }])).toEqual({ numero: 2 });
    expect(muestraEnCurso([{ numero: 1 }, { numero: 2 }])).toEqual({ numero: 2 });
    expect(muestraEnCurso([])).toBeNull();
  });

  it('RF-14 · C3 · un lote analizado muestra su resultado', () => {
    expect(vistaDelLote(resultadoAnalizado(), null)).toBe('resultado');
    expect(vistaDelLote(resultadoAnalizado(), avance('EN_PROCESO'))).toBe('resultado');
  });

  it('RNF-02 · C3 · con la muestra en espera o en proceso, la página muestra la espera, según el resultado o según el último avance', () => {
    expect(vistaDelLote(resultadoPendiente('EN_ESPERA'), null)).toBe('espera');
    expect(vistaDelLote(resultadoPendiente('EN_PROCESO'), null)).toBe('espera');
    expect(vistaDelLote(resultadoPendiente('EN_ESPERA'), avance('EN_PROCESO'))).toBe('espera');
  });

  it('RNF-01 · C4 · cuando el avance dice que terminó, la página espera el resultado', () => {
    expect(vistaDelLote(resultadoPendiente('EN_PROCESO'), avance('TERMINADA'))).toBe('cargando');
  });

  it('RNF-01 · una muestra que volvió a quedar pendiente después de enviarse es un análisis que no terminó', () => {
    expect(vistaDelLote(resultadoPendiente('PENDIENTE', { fechaEnvio: '2026-11-23T16:42:00Z' }), null)).toBe('fallo');
    expect(vistaDelLote(resultadoPendiente('EN_PROCESO'), avance('PENDIENTE', { fallo: true }))).toBe('fallo');
  });

  it('RF-14 · C5 · un lote pendiente sin muestra, o con su muestra sin enviar, todavía no tiene resultado', () => {
    expect(vistaDelLote(resultadoPendiente(null), null)).toBe('sinResultado');
    expect(vistaDelLote(resultadoPendiente('PENDIENTE'), null)).toBe('sinResultado');
    expect(vistaDelLote(resultadoPendiente('EN_ESPERA'), avance('PENDIENTE'))).toBe('sinResultado');
  });

  it('RF-14 · con el lote analizado y la muestra 2 en espera o en proceso, la página muestra la espera con el resultado actual, según el resultado o según el último avance', () => {
    expect(vistaDelLote(resultadoConSegundaMuestra('EN_ESPERA'), null)).toBe('esperaConResultado');
    expect(vistaDelLote(resultadoConSegundaMuestra('EN_PROCESO'), null)).toBe('esperaConResultado');
    expect(vistaDelLote(resultadoConSegundaMuestra('EN_ESPERA'), avance('EN_PROCESO', { muestra: 2 })))
      .toBe('esperaConResultado');
  });

  it('RF-14 · con la muestra 2 todavía sin enviar, o con las dos muestras terminadas, la página muestra el resultado', () => {
    expect(vistaDelLote(resultadoConSegundaMuestra('PENDIENTE'), null)).toBe('resultado');
    expect(vistaDelLote(resultadoConDosMuestras(), null)).toBe('resultado');
  });

  it('RNF-06 · C3 · una muestra 2 que volvió a quedar pendiente después de enviarse muestra el resultado actual con el aviso de que no terminó', () => {
    expect(vistaDelLote(resultadoConSegundaMuestra('PENDIENTE', { fechaEnvio: '2026-11-23T17:10:00Z' }), null))
      .toBe('resultadoConFallo');
    expect(vistaDelLote(resultadoConSegundaMuestra('EN_PROCESO'), avance('PENDIENTE', { fallo: true, muestra: 2 })))
      .toBe('resultadoConFallo');
  });
});
