/*
 * Nombre: EstadoDeLote.java
 * Descripción: Estados de un lote: pendiente de análisis o analizado (RF-04, RF-12, RF-14).
 * Fecha de creación: 10/11/2026
 * Actualización: 10/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

/** Estado de un lote. Los valores son los de la columna {@code lote.estado} (specs/entidades/lote.json). */
public enum EstadoDeLote {

    /** Registrado y sin resultado: se le toman las fotos y se analiza (RF-04, RF-05). */
    PENDIENTE,

    /** Con resultado: porcentaje, categoría y recomendación (RF-12). */
    ANALIZADO
}
