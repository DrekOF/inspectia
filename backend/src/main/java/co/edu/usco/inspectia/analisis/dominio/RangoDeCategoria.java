/*
 * Nombre: RangoDeCategoria.java
 * Descripción: Un rango de recomendación de la empresa: su categoría, desde qué porcentaje aplica y qué recomienda
 *              (RF-12, RF-13, D-10).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Un rango de recomendación de la empresa (RF-12, RF-13). La categoría es el código que guarda la base, como
 * {@code PREMIUM} o {@code FUERA_DE_NORMA}, y no el enum generado: así las reglas no dependen de la temática (RP-23) y
 * ordenan los rangos por su límite, no por el orden de una lista.
 *
 * @param categoria el código de la categoría
 * @param limiteInferior desde qué porcentaje de granos sanos aplica, de 0 a 100; el de Fuera de norma es 0
 * @param recomendacion el texto que escribió la empresa, que el lote copia tal cual (RF-13)
 */
public record RangoDeCategoria(String categoria, BigDecimal limiteInferior, String recomendacion) {

    /**
     * Revisa el rango.
     *
     * @throws IllegalArgumentException si falta un dato o el límite no va de 0 a 100
     */
    public RangoDeCategoria {
        Objects.requireNonNull(categoria, "categoria");
        Objects.requireNonNull(limiteInferior, "limiteInferior");
        Objects.requireNonNull(recomendacion, "recomendacion");
        if (categoria.isBlank()) {
            throw new IllegalArgumentException("El rango no tiene categoría");
        }
        if (limiteInferior.signum() < 0 || limiteInferior.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("El límite de " + categoria + " debe ir de 0 a 100, y es "
                    + limiteInferior);
        }
    }
}
