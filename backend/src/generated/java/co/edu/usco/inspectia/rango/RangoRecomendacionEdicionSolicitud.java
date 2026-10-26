/*
 * Nombre: RangoRecomendacionEdicionSolicitud.java
 * Descripción: Datos para editar rangos de recomendación por la API: todas las filas juntas.
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Las filas de rangos de recomendación de la empresa, una por cada categoría: se editan todas juntas.
 *
 * @param filas las filas, una por cada categoría
 */
@Schema(description = "Las filas de rangos de recomendación de la empresa, una por cada categoría: se editan todas juntas.")
public record RangoRecomendacionEdicionSolicitud(
        @Schema(description = "Las filas, una por cada categoría, sin repetir.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{validacion.obligatorio}")
        @Valid
        List<RangoRecomendacionFilaSolicitud> filas) {
}
