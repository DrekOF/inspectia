/*
 * Nombre: Recomendacion.java
 * Descripción: La recomendación de un lote: su categoría probable, el texto de su rango y si se sugiere analizar otra
 *              muestra (RF-12, D-24, D-25).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

/**
 * La recomendación de un lote (RF-12). La categoría es probable: la oficial se confirma con la prueba de corte (D-10).
 *
 * @param categoria el código de la categoría probable, como {@code ESTANDAR}
 * @param texto el texto de la recomendación de su rango, tal cual lo escribió la empresa (RF-13)
 * @param sugerirOtraMuestra si se sugiere analizar otra muestra (D-24, D-25)
 */
public record Recomendacion(String categoria, String texto, boolean sugerirOtraMuestra) {
}
