/*
 * Nombre: FotoRespuesta.java
 * Descripción: Una foto de la muestra como la responde la API: sus datos y el enlace para verla (RF-05, RF-14 C5).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.rest;

import co.edu.usco.inspectia.analisis.aplicacion.VistaDeFoto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Una foto de la muestra (RF-05, RF-14 C5).
 *
 * @param numero su número en la muestra, de 1 a 3
 * @param formato {@code JPG} o {@code PNG}
 * @param tamanoBytes cuánto pesa
 * @param estado {@code CARGADA}, {@code APROBADA} o {@code POR_REPETIR}
 * @param vista el enlace para verla, sin el token, que vence en minutos (RNF-03 C3)
 */
@Schema(description = "Una foto de la muestra, con el enlace para verla.")
public record FotoRespuesta(
        @Schema(description = "Su número en la muestra, de 1 a 3.", example = "1") int numero,
        @Schema(description = "Su formato.", allowableValues = {"JPG", "PNG"}, example = "JPG") String formato,
        @Schema(description = "Cuánto pesa, en bytes: hasta 10.485.760.", example = "2048") long tamanoBytes,
        @Schema(description = "Su estado: CARGADA hasta que la revise la IA (RF-06).",
                allowableValues = {"CARGADA", "APROBADA", "POR_REPETIR"}, example = "CARGADA") String estado,
        @Schema(description = "El enlace para verla.") Enlace vista) {

    /**
     * Un enlace temporal para ver la foto.
     *
     * @param url el enlace: relativo a la página con la carpeta local, absoluto con S3
     * @param vence cuándo deja de servir
     */
    @Schema(description = "El enlace para ver la foto: se usa con GET, sin el token (D-41).")
    public record Enlace(
            @Schema(description = "El enlace firmado: relativo a la página con la carpeta local (D-41), absoluto con "
                    + "S3. Sirve en un <img>.") String url,
            @Schema(description = "Cuándo deja de servir: 15 minutos después de pedirlo. La lista de las fotos da "
                    + "enlaces nuevos.") Instant vence) {
    }

    /**
     * La respuesta de una foto.
     *
     * @param foto la foto
     * @return la respuesta
     */
    public static FotoRespuesta de(VistaDeFoto foto) {
        return new FotoRespuesta(foto.numero(), foto.formato().name(), foto.tamanoBytes(), foto.estado().name(),
                new Enlace(foto.vista().url(), foto.vista().vence()));
    }
}
