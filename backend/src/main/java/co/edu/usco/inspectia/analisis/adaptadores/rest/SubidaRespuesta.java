/*
 * Nombre: SubidaRespuesta.java
 * Descripción: El enlace para subir una foto, como lo responde la API, con la llave que lleva la muestra para la que
 *              se pidió (RF-05, D-41, D-25).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.adaptadores.rest;

import co.edu.usco.inspectia.analisis.aplicacion.SubidaDeFoto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

/**
 * El enlace para subir una foto (RF-05, D-41), con la llave con que después se confirma.
 *
 * @param llave la llave del archivo, {@code <empresa>/<lote>/m<muestra>-<uuid>.<jpg|png>}, para confirmar la foto
 * @param url el enlace: relativo a la página con la carpeta local, absoluto con S3
 * @param metodo el método con que se sube: {@code PUT}
 * @param encabezados los encabezados que hay que enviar, como el {@code Content-Type}
 * @param vence cuándo deja de servir
 */
@Schema(description = "El enlace para subir una foto: se usa con PUT, sin el token (D-41).")
public record SubidaRespuesta(
        @Schema(description = "La llave del archivo, <empresa>/<lote>/m<muestra>-<uuid>.<jpg|png>, con la muestra para "
                + "la que se pidió (D-25): se manda al confirmar la foto.",
                example = "0199f0a0-0000-7000-8000-000000000001/0199f0a0-0000-7000-8000-000000000143/"
                        + "m1-0199f0a0-0000-7000-8000-000000000201.jpg") String llave,
        @Schema(description = "El enlace firmado: relativo a la página con la carpeta local (D-41), absoluto con S3. "
                + "Se usa sin el token.") String url,
        @Schema(description = "El método con que se sube.", example = "PUT") String metodo,
        @Schema(description = "Los encabezados que hay que mandar tal cual, como el Content-Type firmado.")
        Map<String, String> encabezados,
        @Schema(description = "Cuándo deja de servir el enlace: 10 minutos después de pedirlo.") Instant vence) {

    /**
     * La respuesta de una subida pedida.
     *
     * @param subida la subida
     * @return la respuesta
     */
    public static SubidaRespuesta de(SubidaDeFoto subida) {
        return new SubidaRespuesta(subida.llave(), subida.enlace().url(), subida.enlace().metodo(),
                subida.enlace().encabezados(), subida.enlace().vence());
    }
}
