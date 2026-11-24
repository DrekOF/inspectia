/*
 * Nombre: ConfianzaPromedioRespuesta.java
 * Descripción: La confianza promedio de los análisis de la empresa de la sesión, como la responde la API del modelo de
 *              IA (RF-17 C2, RNF-03).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.estadistica.modelo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * La confianza promedio de los granos de los análisis de la empresa (RF-17 C2): solo de su empresa (RNF-03).
 *
 * @param promedio la confianza promedio (%), con 2 decimales, o vacía si la empresa no tiene análisis
 * @param granos cuántos granos la dan
 * @param muestras de cuántas muestras analizadas son esos granos
 */
@Schema(description = "Confianza promedio de los granos de los análisis de la empresa (RF-17 C2).")
public record ConfianzaPromedioRespuesta(
        @Schema(description = "Confianza promedio (%), con 2 decimales; vacía si la empresa no tiene análisis.",
                example = "88.60") BigDecimal promedio,
        @Schema(description = "Granos que dan el promedio", example = "200") long granos,
        @Schema(description = "Muestras analizadas de esos granos", example = "2") long muestras) {

    /**
     * La confianza como la responde la API.
     *
     * @param fila la fila de la base
     * @return la confianza
     */
    static ConfianzaPromedioRespuesta de(ConfianzaDeLaEmpresa fila) {
        return new ConfianzaPromedioRespuesta(fila.getPromedio(), valor(fila.getGranos()), valor(fila.getMuestras()));
    }

    private static long valor(Long conteo) {
        return conteo == null ? 0 : conteo;
    }
}
