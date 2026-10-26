/*
 * Nombre: RangoRecomendacionRespuesta.java
 * Descripción: Rango de recomendación en la respuesta de la API.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.8)
 * Versión: 1.8
 * Entidades: rango_recomendacion 1.2
 */
package co.edu.usco.inspectia.rango;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Un registro de {@code rango_recomendacion} como lo responde la API. Nunca trae contraseñas.
 *
 * @param id identificador (UUID v7)
 * @param categoria Categoría
 * @param limiteInferior Límite inferior (% de granos sanos)
 * @param recomendacion Recomendación
 * @param fechaCreacion fecha de creación del registro
 * @param fechaActualizacion fecha de la última actualización
 */
@Schema(description = "Rango de recomendación.")
public record RangoRecomendacionRespuesta(
        @Schema(description = "Identificador (UUID v7).") UUID id,
        @Schema(description = "Categoría", example = "PREMIUM") RangoRecomendacionCategoria categoria,
        @Schema(description = "Límite inferior (% de granos sanos)", example = "0") BigDecimal limiteInferior,
        @Schema(description = "Recomendación", example = "Recomendación de prueba") String recomendacion,
        @Schema(description = "Fecha de creación del registro.") OffsetDateTime fechaCreacion,
        @Schema(description = "Fecha de la última actualización.") OffsetDateTime fechaActualizacion) {
}
