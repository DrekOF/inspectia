/*
 * Nombre: metricas.test.js
 * Descripción: Pruebas unitarias de lo que comparten las partes de la pantalla «Modelo de IA» (RF-17): el texto de una
 *              métrica, el modelo en uso y los granos del conjunto de prueba, que salen de la matriz de confusión.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { describe, expect, it } from 'vitest';
import { EFFICIENTNET, FALSO, RESNET } from '../pruebas/modelo.js';
import { granosDePrueba, modeloEnUso, textoDeMetrica } from './metricas.js';

describe('métricas del modelo', () => {
  it('RF-17 · C1 · una métrica va con 2 decimales, como la guarda la base, en el formato de cada idioma', () => {
    expect(textoDeMetrica(73.5, 'es-CO')).toBe('73,50');
    expect(textoDeMetrica(72.31, 'en-US')).toBe('72.31');
    expect(textoDeMetrica(0, 'es-CO')).toBe('0,00');
  });

  it('RF-17 · C1 · una métrica que el modelo no tiene, como las del clasificador falso, va con «—»', () => {
    expect(textoDeMetrica(null, 'es-CO')).toBe('—');
    expect(textoDeMetrica(undefined, 'es-CO')).toBe('—');
  });

  it('RF-17 · el modelo en uso es el que la API marca, o ninguno', () => {
    expect(modeloEnUso([RESNET, EFFICIENTNET, FALSO])).toBe(EFFICIENTNET);
    expect(modeloEnUso([RESNET, FALSO])).toBeNull();
    expect(modeloEnUso([])).toBeNull();
  });

  it('RF-17 · los granos del conjunto de prueba son la suma de la matriz de confusión: 334 con EfficientNet-B0', () => {
    expect(granosDePrueba(EFFICIENTNET.matriz)).toBe(334);
    expect(granosDePrueba({ sanosComoSanos: 1, sanosComoNoSanos: 2, noSanosComoSanos: 3, noSanosComoNoSanos: 4 }))
      .toBe(10);
    expect(granosDePrueba(null)).toBeNull();
  });
});
