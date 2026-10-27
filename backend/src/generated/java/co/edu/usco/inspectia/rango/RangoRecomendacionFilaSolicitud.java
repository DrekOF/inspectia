/*
 * Nombre: RangoRecomendacionFilaSolicitud.java
 * Descripción: Una fila de rangos de recomendación en la edición en bloque por la API.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.8)
 * Versión: 1.8
 * Entidades: rango_recomendacion 1.2
 */
package co.edu.usco.inspectia.rango;

import co.edu.usco.inspectia.comun.texto.Textos;
import co.edu.usco.inspectia.comun.validacion.MaximoDecimales;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Una fila de rangos de recomendación: el valor de categoría que la identifica y sus datos.
 * Antes de validar se normalizan los textos como pide el JSON: sin espacios al inicio ni al final
 * y, si corresponde, en minúsculas.
 *
 * @param categoria Categoría
 * @param limiteInferior Límite inferior (% de granos sanos)
 * @param recomendacion Recomendación
 */
@Schema(description = "Una fila de rangos de recomendación: el valor de categoría que la identifica y sus datos.")
public record RangoRecomendacionFilaSolicitud(
        @Schema(description = "Categoría", example = "PREMIUM", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{validacion.obligatorio}")
        RangoRecomendacionCategoria categoria,
        @Schema(description = "Límite inferior (% de granos sanos)", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{validacion.obligatorio}")
        @DecimalMin(value = "0", message = "{validacion.minimo}")
        @DecimalMax(value = "100", message = "{validacion.maximo}")
        @MaximoDecimales(value = 1, message = "{validacion.decimales_uno}")
        BigDecimal limiteInferior,
        @Schema(description = "Recomendación", example = "Recomendación de prueba", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validacion.obligatorio}")
        @Size(max = 300, message = "{validacion.longitud_max}")
        String recomendacion) {

    /** Normaliza los textos antes de validarlos. */
    public RangoRecomendacionFilaSolicitud {
        recomendacion = Textos.recortar(recomendacion);
    }
}
