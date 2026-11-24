/*
 * Nombre: AnalisisRespuesta.java
 * Descripción: El avance del análisis de la muestra en curso de un lote, como lo responde la API (RNF-01 C4,
 *              RNF-02 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.adaptadores.rest;

import co.edu.usco.inspectia.analisis.aplicacion.AvanceDelAnalisis;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

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
 * @param motivoDevolucion por qué volvió, o vacío (D-44)
 * @param repeticiones las fotos que RF-09 pidió repetir en esta muestra
 * @param fotosPorRepetir las fotos de la muestra que están por repetir
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
                + "fecha de envío, y se puede enviar de nuevo.", example = "false") boolean fallo,
        @Schema(description = "Por qué volvió de la cola sin resultado (D-44): POCOS_GRANOS (RF-07 C3) o "
                + "SIN_RESPUESTA_DE_LA_IA (RF-08 C6). Vacío si no volvió, si la devolvió una foto (ver fotosPorRepetir) "
                + "o si fue otra falla. Se borra al enviarla de nuevo.", allowableValues = {"POCOS_GRANOS", "SIN_RESPUESTA_DE_LA_IA"},
                nullable = true, example = "SIN_RESPUESTA_DE_LA_IA") String motivoDevolucion,
        @Schema(description = "Cuántas fotos pidió repetir RF-09 por granos dudosos en esta muestra, de 0 a 2.",
                example = "0") int repeticiones,
        @Schema(description = "Las fotos de la muestra en curso que están POR_REPETIR, por número, con su motivo; "
                + "vacía si no hay.") List<FotoPorRepetir> fotosPorRepetir) {

    /**
     * Una foto por repetir.
     *
     * @param numero su número
     * @param motivo su motivo
     */
    @Schema(description = "Una foto de la muestra que hay que repetir (RF-06, RF-07, RF-09, D-44).")
    public record FotoPorRepetir(
            @Schema(description = "Su número en la muestra, de 1 a 3.", example = "2") int numero,
            @Schema(description = "Por qué hay que repetirla.", allowableValues = {"BORROSA", "POCA_LUZ", "GRANOS_PEGADOS", "GRANOS_DUDOSOS"},
                    example = "GRANOS_DUDOSOS") String motivo) {

        static List<FotoPorRepetir> de(List<co.edu.usco.inspectia.analisis.aplicacion.FotoPorRepetir> fotos) {
            return fotos.stream().map(foto -> new FotoPorRepetir(foto.numero(), foto.motivo().name())).toList();
        }
    }

    /**
     * La respuesta de un avance.
     *
     * @param avance el avance
     * @return la respuesta
     */
    static AnalisisRespuesta de(AvanceDelAnalisis avance) {
        return new AnalisisRespuesta(avance.muestra(), avance.estado() == null ? null : avance.estado().name(),
                avance.fechaEnvio(), avance.fechaInicio(), avance.fechaFin(), avance.fallo(),
                avance.motivoDevolucion() == null ? null : avance.motivoDevolucion().name(), avance.repeticiones(),
                FotoPorRepetir.de(avance.fotosPorRepetir()));
    }
}
