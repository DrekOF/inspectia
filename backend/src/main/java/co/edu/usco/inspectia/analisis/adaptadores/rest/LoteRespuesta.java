/*
 * Nombre: LoteRespuesta.java
 * Descripción: Un lote como lo responde la API: sus datos, quién lo registró y su resultado, si ya lo tiene, con la
 *              marca de otra muestra sugerida (RF-04, RF-14, D-25).
 * Fecha de creación: 10/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.adaptadores.rest;

import co.edu.usco.inspectia.analisis.aplicacion.VistaDeLote;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeLote;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Un lote como lo responde la API, en el registro, el historial y el detalle (RF-04, RF-14). Mientras está
 * pendiente, el porcentaje, la categoría, la sugerencia y la fecha del resultado van vacíos.
 *
 * @param id identificador del lote
 * @param codigo el código
 * @param fechaRecepcion la fecha de recepción
 * @param origen la finca o el proveedor
 * @param observaciones las observaciones, o vacías
 * @param estado pendiente o analizado
 * @param usuario quién lo registró
 * @param porcentajeSanos el porcentaje de granos sanos, o vacío
 * @param categoria la categoría probable, o vacía
 * @param sugerirOtraMuestra si se sugiere analizar otra muestra, hasta que termina la segunda, o vacío
 * @param fechaResultado la fecha del resultado, o vacía
 * @param fechaCreacion la fecha de registro
 */
@Schema(description = "Un lote, con su resultado cuando ya se analizó.")
public record LoteRespuesta(
        @Schema(description = "Identificador del lote.") UUID id,
        @Schema(description = "Código del lote.", example = "L-001") String codigo,
        @Schema(description = "Fecha de recepción.", example = "2026-11-02") LocalDate fechaRecepcion,
        @Schema(description = "Finca o proveedor.", example = "Finca La Esperanza") String origen,
        @Schema(description = "Observaciones; vacías si no tiene.") String observaciones,
        @Schema(description = "PENDIENTE (de análisis) o ANALIZADO.", example = "PENDIENTE") EstadoDeLote estado,
        @Schema(description = "Quién registró el lote.") UsuarioDelLote usuario,
        @Schema(description = "Porcentaje de granos sanos, con 1 decimal; vacío mientras está pendiente.",
                example = "93.0") BigDecimal porcentajeSanos,
        @Schema(description = "Categoría probable; vacía mientras está pendiente.", example = "ESTANDAR") String categoria,
        @Schema(description = "Si se sugiere analizar otra muestra (D-24): la marca «Sugiere otra muestra» de la "
                + "lista. Sigue verdadero mientras se toma y se analiza la muestra 2, y queda falso cuando termina "
                + "(D-25); vacío mientras está pendiente.", example = "false") Boolean sugerirOtraMuestra,
        @Schema(description = "Fecha del resultado; vacía mientras está pendiente.") OffsetDateTime fechaResultado,
        @Schema(description = "Fecha de registro del lote.") OffsetDateTime fechaCreacion) {

    /**
     * El usuario que registró el lote.
     *
     * @param id su identificador
     * @param nombre su nombre
     */
    @Schema(description = "El usuario que registró el lote.")
    public record UsuarioDelLote(
            @Schema(description = "Identificador del usuario.") UUID id,
            @Schema(description = "Nombre del usuario.", example = "Luis Gómez") String nombre) {
    }

    /**
     * Pasa un lote consultado a lo que responde la API.
     *
     * @param vista el lote
     * @return la respuesta
     */
    static LoteRespuesta de(VistaDeLote vista) {
        return new LoteRespuesta(vista.id(), vista.codigo(), vista.fechaRecepcion(), vista.origen(),
                vista.observaciones(), vista.estado(), new UsuarioDelLote(vista.usuarioId(), vista.usuarioNombre()),
                vista.porcentajeSanos(), vista.categoria(), vista.sugerirOtraMuestra(), vista.fechaResultado(),
                vista.fechaCreacion());
    }
}
