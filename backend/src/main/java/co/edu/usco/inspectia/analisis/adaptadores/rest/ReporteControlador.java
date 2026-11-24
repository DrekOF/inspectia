/*
 * Nombre: ReporteControlador.java
 * Descripción: API REST del reporte PDF de un lote: lo descarga en español o en inglés, con la sesión, para el usuario
 *              final y la administradora de empresa (RF-15, RNF-03, RNF-04, D-50).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.rest;

import co.edu.usco.inspectia.analisis.aplicacion.DescargarReporte;
import co.edu.usco.inspectia.analisis.aplicacion.IdiomaDelReporte;
import co.edu.usco.inspectia.analisis.aplicacion.ReporteDelLote;
import co.edu.usco.inspectia.comun.errores.CampoInvalido;
import co.edu.usco.inspectia.comun.errores.ReglaIncumplidaExcepcion;
import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST del reporte PDF de un lote, en {@value #RUTA} (RF-15). La respuesta es el PDF y no JSON: la página lo pide
 * con el token de la sesión y lo guarda como archivo. Los errores sí van en {@code application/problem+json}. La
 * empresa sale de la sesión: un lote de otra empresa responde 404, como si no existiera (RNF-03). Los permisos los
 * revisan la regla por ruta de los lotes ({@link LotesAcceso}, que deja consultar a los dos roles de la empresa) y
 * {@code @PreAuthorize} (RNF-04).
 */
@RestController
@RequestMapping(ReporteControlador.RUTA)
@Tag(name = "Análisis", description = "Enviar a analizar la muestra de un lote, ver el avance y ver el resultado con "
        + "las fotos marcadas (RF-05, RF-10, RF-11, RF-12, RF-14).")
public class ReporteControlador {

    /** Ruta del reporte de un lote. */
    public static final String RUTA = LotesControlador.RUTA + "/{id}/reporte";

    private final DescargarReporte descargar;

    /**
     * Crea el controlador; Spring le pasa el caso de uso.
     *
     * @param descargar descargar el reporte
     */
    public ReporteControlador(DescargarReporte descargar) {
        this.descargar = descargar;
    }

    /**
     * El reporte PDF del lote, como archivo para descargar.
     *
     * @param id el lote
     * @param idioma {@code es} o {@code en}; sin él, el de la petición
     * @param region el idioma de la petición ({@code Accept-Language})
     * @param sesion el usuario de la sesión: solo descarga los lotes de su empresa
     * @return el PDF, con su nombre en {@code Content-Disposition}
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USUARIO_FINAL', 'ADMIN_EMPRESA')")
    @Operation(summary = "Descargar el reporte PDF del lote", description = "El reporte del lote en PDF, en español o "
            + "en inglés: datos del lote y de la empresa, fecha del resultado, porcentaje corregido con su rango "
            + "probable, lo que contó el modelo, categoría probable con la nota de la prueba de corte, recomendación, "
            + "sugerencia de otra muestra con su límite, aviso de confianza baja y, por cada muestra terminada, quién "
            + "la analizó, su modelo, sus granos y sus fotos marcadas (RF-15). La primera descarga lo arma en los dos "
            + "idiomas y lo guarda; las siguientes entregan el mismo archivo, aunque la empresa cambie sus rangos "
            + "(RF-15 C4). Cuando termina la segunda muestra, el reporte anterior se borra y el siguiente trae la suma "
            + "y las dos muestras (RF-15 C5, D-25). Responde application/pdf con Content-Disposition attachment y un "
            + "nombre como InspectIA-L-0143-es.pdf. Un lote pendiente responde 409; uno de otra empresa, 404 "
            + "(RNF-03); un idioma que no es es ni en, 400 en «idioma». Roles: USUARIO_FINAL y ADMIN_EMPRESA.")
    @ApiResponse(responseCode = "200", description = "El reporte en PDF.",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "400", description = "El idioma no es es ni en.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    @ApiResponse(responseCode = "404", description = "El lote no existe o es de otra empresa (RNF-03).",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    @ApiResponse(responseCode = "409", description = "El lote todavía no tiene resultado.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    public ResponseEntity<byte[]> descargar(@PathVariable("id") UUID id,
            @Parameter(description = "El idioma del reporte: es (español) o en (inglés). Sin él, el de "
                    + "Accept-Language, y si no es inglés, español.", example = "es")
            @RequestParam(name = "idioma", required = false) String idioma,
            @Parameter(hidden = true) Locale region,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        IdiomaDelReporte elegido = idioma == null ? IdiomaDelReporte.de(region)
                : IdiomaDelReporte.de(idioma).orElseThrow(
                        () -> new ReglaIncumplidaExcepcion(List.of(new CampoInvalido("idioma", "reporte.idioma"))));
        ReporteDelLote reporte = descargar.descargar(sesion.empresaId(), id, elegido);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(reporte.contenido().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(reporte.nombre(), StandardCharsets.UTF_8).build().toString())
                .cacheControl(CacheControl.noStore())
                .body(reporte.contenido());
    }
}
