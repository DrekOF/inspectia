/*
 * Nombre: Recomendacion.java
 * Descripción: La recomendación de un lote: su categoría probable, el texto de su rango, si se sugiere analizar otra
 *              muestra y, si se sugiere, de qué límite y de qué categoría es ese límite (RF-12, D-24, D-25, D-47).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.dominio;

import java.math.BigDecimal;

/**
 * La recomendación de un lote (RF-12). La categoría es probable: la oficial se confirma con la prueba de corte (D-10).
 * Cuando sugiere otra muestra, dice por qué límite, para el aviso «El resultado quedó a 0,9 puntos del límite de
 * Estándar (90 %).» (D-47); si no, el límite y su categoría van vacíos.
 *
 * @param categoria el código de la categoría probable, como {@code ESTANDAR}
 * @param texto el texto de la recomendación de su rango, tal cual lo escribió la empresa (RF-13)
 * @param sugerirOtraMuestra si se sugiere analizar otra muestra (D-24, D-25)
 * @param limiteSugerencia el límite inferior más cercano al porcentaje, tal cual lo tiene el rango, o {@code null} si
 *     no se sugiere otra muestra (D-47)
 * @param categoriaDelLimite el código de la categoría de ese límite, que puede ser otra que la del lote, o
 *     {@code null} si no se sugiere otra muestra (D-47)
 */
public record Recomendacion(String categoria, String texto, boolean sugerirOtraMuestra, BigDecimal limiteSugerencia,
        String categoriaDelLimite) {
}
