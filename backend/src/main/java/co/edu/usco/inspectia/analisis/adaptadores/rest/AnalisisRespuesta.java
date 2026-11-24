/*
 * Nombre: AnalisisRespuesta.java
 * Descripción: El avance del análisis de la muestra en curso de un lote, como lo responde la API (RNF-01 C4,
 *              RNF-02 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.rest;

import co.edu.usco.inspectia.analisis.aplicacion.AvanceDelAnalisis;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * El avance del análisis de la muestra en curso (RNF-01 C4, RNF-02 C3). No trae la posición en la cola, que es de
 * todas las empresas (RNF-03).
 *
 * @param muestra el número de la muestra en curso, o vacío si el lote todavía no tiene muestra
 * @param estado {@code PENDIENTE}, {@code EN_ESPERA}, {@code EN_PROCESO} o {@code TERMINADA}, o vacío
 * @param fechaEnvio cuándo se envió a analizar, o vacía
 * @param fechaInicio cuándo empezó el análisis, o vacía
 * @param fechaFin cuándo terminó, o vacía
 * @param fallo si el último envío volvió sin resultado
 */
@Schema(description = "El avance del análisis de la muestra en curso del lote (RNF-01 C4, RNF-02 C3).")
public record AnalisisRespuesta(
        @Schema(description = "El número de la muestra en curso, la de número más alto; vacío si el lote todavía no "
                + "tiene muestra.", example = "1") Integer muestra,
        @Schema(description = "El estado de la muestra: PENDIENTE mientras se toman las fotos, EN_ESPERA en la cola, "
                + "EN_PROCESO mientras se analiza y TERMINADA con el resultado; vacío si no hay muestra.",
                allowableValues = {"PENDIENTE", "EN_ESPERA", "EN_PROCESO", "TERMINADA"}, example = "EN_ESPERA")
        String estado,
        @Schema(description = "Cuándo se envió a analizar; vacía si nunca se envió.",
                example = "2026-11-23T15:00:00Z") Instant fechaEnvio,
        @Schema(description = "Cuándo empezó el análisis; vacía mientras espera en la cola.") Instant fechaInicio,
        @Schema(description = "Cuándo terminó el análisis; vacía mientras no termine.") Instant fechaFin,
        @Schema(description = "Verdadero si el último envío volvió sin resultado: la muestra quedó PENDIENTE, con su "
                + "fecha de envío, y se puede enviar de nuevo.", example = "false") boolean fallo) {

    /**
     * La respuesta de un avance.
     *
     * @param avance el avance
     * @return la respuesta
     */
    static AnalisisRespuesta de(AvanceDelAnalisis avance) {
        return new AnalisisRespuesta(avance.muestra(), avance.estado() == null ? null : avance.estado().name(),
                avance.fechaEnvio(), avance.fechaInicio(), avance.fechaFin(), avance.fallo());
    }
}
