/*
 * Nombre: FotosDeLaMuestraRespuesta.java
 * Descripción: Las fotos de la muestra en curso de un lote, como las responde la API, también las de la muestra 2
 *              (RF-05, RF-14 C5 y C6, D-25).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.adaptadores.rest;

import co.edu.usco.inspectia.analisis.aplicacion.FotosDeLaMuestra;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Las fotos de la muestra en curso de un lote (RF-05, RF-14 C5). Un lote analizado que sugiere otra muestra y todavía
 * no tiene la 2 responde la lista vacía y sin número: es la muestra 2 que se va a tomar (RF-14 C6, D-25).
 *
 * @param muestra el número de la muestra, o {@code null} si todavía no tiene fotos
 * @param estado el estado de la muestra, o {@code null} si todavía no tiene fotos
 * @param fotos las fotos, por número
 */
@Schema(description = "Las fotos de la muestra en curso del lote: la 1, o la 2 de un lote que sugiere otra muestra "
        + "(D-25).")
public record FotosDeLaMuestraRespuesta(
        @Schema(description = "El número de la muestra: 1, o 2 para la segunda; vacío si todavía no tiene fotos, "
                + "también mientras la muestra 2 de un lote que sugiere otra muestra no tiene ninguna.", example = "1")
        Integer muestra,
        @Schema(description = "El estado de la muestra; vacío si todavía no tiene fotos.",
                allowableValues = {"PENDIENTE", "EN_ESPERA", "EN_PROCESO", "TERMINADA"}, example = "PENDIENTE")
        String estado,
        @Schema(description = "Las fotos, por número; vacía si todavía no tiene.") List<FotoRespuesta> fotos) {

    /**
     * La respuesta de las fotos de una muestra.
     *
     * @param fotos la muestra y sus fotos
     * @return la respuesta
     */
    public static FotosDeLaMuestraRespuesta de(FotosDeLaMuestra fotos) {
        return new FotosDeLaMuestraRespuesta(fotos.muestra(), fotos.estado() == null ? null : fotos.estado().name(),
                fotos.fotos().stream().map(FotoRespuesta::de).toList());
    }
}
