/*
 * Nombre: MatrizDeConfusionRespuesta.java
 * Descripción: La matriz de confusión de un modelo en el conjunto de prueba, en granos, como la responde la API del
 *              modelo de IA (RF-17, RNF-07 C2, D-48).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.estadistica.modelo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * La matriz de confusión del conjunto de prueba (D-48): cuántos granos de cada clase real clasificó el modelo en cada
 * clase. Su suma es el total del conjunto de prueba (334 granos en la comparación de Julián, D-14).
 *
 * @param sanosComoSanos sanos clasificados como sanos
 * @param sanosComoNoSanos sanos clasificados como no sanos
 * @param noSanosComoSanos no sanos clasificados como sanos
 * @param noSanosComoNoSanos no sanos clasificados como no sanos
 */
@Schema(description = "Matriz de confusión del conjunto de prueba, en granos (D-48).")
public record MatrizDeConfusionRespuesta(
        @Schema(description = "Sanos clasificados como sanos", example = "238") Integer sanosComoSanos,
        @Schema(description = "Sanos clasificados como no sanos", example = "23") Integer sanosComoNoSanos,
        @Schema(description = "No sanos clasificados como sanos", example = "34") Integer noSanosComoSanos,
        @Schema(description = "No sanos clasificados como no sanos", example = "39") Integer noSanosComoNoSanos) {

    /**
     * La matriz de un modelo, si tiene las cuatro cifras.
     *
     * @param modelo el modelo
     * @return la matriz, o {@code null} si al modelo le falta alguna, como al del clasificador falso
     */
    static MatrizDeConfusionRespuesta de(ModeloComparado modelo) {
        if (modelo.getSanosComoSanos() == null || modelo.getSanosComoNoSanos() == null
                || modelo.getNoSanosComoSanos() == null || modelo.getNoSanosComoNoSanos() == null) {
            return null;
        }
        return new MatrizDeConfusionRespuesta(modelo.getSanosComoSanos(), modelo.getSanosComoNoSanos(),
                modelo.getNoSanosComoSanos(), modelo.getNoSanosComoNoSanos());
    }
}
