/*
 * Nombre: MetricasDelModelo.java
 * Descripción: Un modelo de la comparación de Julián con todas las métricas que guarda la tabla modelo, como las
 *              responde el servicio de IA en GET /modelos (D-18, D-48, RF-17, RNF-07).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.math.BigDecimal;

/**
 * Un modelo de la comparación (D-18) con sus métricas del conjunto de prueba (D-48). Los porcentajes van de 0 a 100 con
 * 2 decimales y la matriz de confusión, en granos. Al arrancar con el servicio de IA, el servidor registra los 5.
 *
 * @param nombre el nombre, como {@code EfficientNet-B0}
 * @param version la versión, como {@code 1.0}
 * @param tipo {@code CNN} o {@code TRANSFORMER} (RP-25)
 * @param f1Macro el F1 macro
 * @param precisionMacro la precisión macro
 * @param recallMacro el recall macro
 * @param recallSano el recall de sano, con que se corrige el porcentaje (D-39)
 * @param recallNoSano el recall de no sano (D-39)
 * @param precisionSano la precisión de sano
 * @param precisionNoSano la precisión de no sano
 * @param sanosComoSanos sanos clasificados como sanos
 * @param sanosComoNoSanos sanos clasificados como no sanos
 * @param noSanosComoSanos no sanos clasificados como sanos
 * @param noSanosComoNoSanos no sanos clasificados como no sanos
 * @param tiempoPorGranoMs el tiempo por grano, en milisegundos
 */
public record MetricasDelModelo(String nombre, String version, String tipo, BigDecimal f1Macro,
        BigDecimal precisionMacro, BigDecimal recallMacro, BigDecimal recallSano, BigDecimal recallNoSano,
        BigDecimal precisionSano, BigDecimal precisionNoSano, int sanosComoSanos, int sanosComoNoSanos,
        int noSanosComoSanos, int noSanosComoNoSanos, BigDecimal tiempoPorGranoMs) {
}
