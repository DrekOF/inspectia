/*
 * Nombre: FotoRespuesta.java
 * Descripción: Una foto de la muestra como la responde la API: sus datos y el enlace para verla (RF-05, RF-14 C5).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.adaptadores.rest;

import co.edu.usco.inspectia.analisis.aplicacion.VistaDeFoto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Una foto de la muestra (RF-05, RF-14 C5).
 *
 * @param numero su número en la muestra, de 1 a 3
 * @param formato {@code JPG} o {@code PNG}
 * @param tamanoBytes cuánto pesa
 * @param estado {@code CARGADA}, {@code APROBADA} o {@code POR_REPETIR}
 * @param vista el enlace para verla, sin el token, que vence en minutos (RNF-03 C3)
 * @param motivo por qué hay que repetirla, o vacío si no está por repetir (RF-06, RF-07, RF-09)
 * @param granosEncontrados los granos válidos que encontró su revisión, o vacío mientras está cargada (D-46)
 * @param regionesPegadas dónde están los granos pegados, solo con {@code GRANOS_PEGADOS} (D-45)
 * @param granosDudosos los granos dudosos, solo con {@code GRANOS_DUDOSOS} (RF-09 C1)
 */
@Schema(description = "Una foto de la muestra, con el enlace para verla.")
public record FotoRespuesta(
        @Schema(description = "Su número en la muestra, de 1 a 3.", example = "1") int numero,
        @Schema(description = "Su formato.", allowableValues = {"JPG", "PNG"}, example = "JPG") String formato,
        @Schema(description = "Cuánto pesa, en bytes: hasta 10.485.760.", example = "2048") long tamanoBytes,
        @Schema(description = "Su estado: APROBADA o POR_REPETIR según la revisión rápida del servicio de IA al "
                + "confirmarla; CARGADA si el servicio no respondió o no hay revisión (clasificador falso), y entonces "
                + "la revisa la cola (RF-06, D-46).",
                allowableValues = {"CARGADA", "APROBADA", "POR_REPETIR"}, example = "APROBADA") String estado,
        @Schema(description = "El enlace para verla.") Enlace vista,
        @Schema(description = "Por qué hay que repetirla; vacío salvo con POR_REPETIR (RF-06, RF-07, RF-09).",
                allowableValues = {"BORROSA", "POCA_LUZ", "GRANOS_PEGADOS", "GRANOS_DUDOSOS"}, example = "GRANOS_PEGADOS",
                nullable = true) String motivo,
        @Schema(description = "Los granos válidos que encontró la revisión: sin los cortados por el borde, sin manchas "
                + "y sin contar las regiones de granos pegados; vacío mientras está CARGADA (D-46). Al terminar la "
                + "muestra con el servicio de IA, los granos que la cola clasificó en esa foto.", example = "52",
                nullable = true) Integer granosEncontrados,
        @Schema(description = "Dónde están los granos pegados, en píxeles de la foto ya girada según su EXIF (D-45); "
                + "solo con el motivo GRANOS_PEGADOS, si no, vacía.") List<Rectangulo> regionesPegadas,
        @Schema(description = "Los granos dudosos de la foto que RF-09 pidió repetir, sin su clase ni su confianza "
                + "(RF-11 C4); solo con el motivo GRANOS_DUDOSOS, si no, vacía.") List<Rectangulo> granosDudosos) {

    /**
     * Un rectángulo de la foto.
     *
     * @param x posición horizontal
     * @param y posición vertical
     * @param ancho ancho
     * @param alto alto
     */
    @Schema(description = "Un rectángulo sin rotación, en píxeles de la foto ya girada según su EXIF (D-45). La página "
            + "lo ubica en porcentaje del tamaño natural de la imagen.")
    public record Rectangulo(
            @Schema(description = "Posición horizontal de su esquina superior izquierda, desde 0.", example = "1210")
            int x,
            @Schema(description = "Posición vertical de su esquina superior izquierda, desde 0.", example = "860")
            int y,
            @Schema(description = "Ancho, desde 1.", example = "240") int ancho,
            @Schema(description = "Alto, desde 1.", example = "130") int alto) {

        static List<Rectangulo> de(List<co.edu.usco.inspectia.analisis.aplicacion.Rectangulo> rectangulos) {
            return rectangulos.stream()
                    .map(rectangulo -> new Rectangulo(rectangulo.x(), rectangulo.y(), rectangulo.ancho(),
                            rectangulo.alto()))
                    .toList();
        }
    }

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
                new Enlace(foto.vista().url(), foto.vista().vence()),
                foto.motivo() == null ? null : foto.motivo().name(), foto.granosEncontrados(),
                Rectangulo.de(foto.regionesPegadas()), Rectangulo.de(foto.granosDudosos()));
    }
}
