/*
 * Nombre: FotosDeLaMuestraRespuesta.java
 * Descripción: Las fotos de la muestra en curso de un lote, como las responde la API (RF-05, RF-14 C5).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.rest;

import co.edu.usco.inspectia.analisis.aplicacion.FotosDeLaMuestra;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Las fotos de la muestra en curso de un lote (RF-05, RF-14 C5).
 *
 * @param muestra el número de la muestra, o {@code null} si todavía no tiene fotos
 * @param estado el estado de la muestra, o {@code null} si todavía no tiene fotos
 * @param fotos las fotos, por número
 */
@Schema(description = "Las fotos de la muestra en curso del lote.")
public record FotosDeLaMuestraRespuesta(Integer muestra, String estado, List<FotoRespuesta> fotos) {

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
