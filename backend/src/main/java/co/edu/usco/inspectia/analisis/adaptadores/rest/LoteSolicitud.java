/*
 * Nombre: LoteSolicitud.java
 * Descripción: Los datos con que se registra un lote, con sus validaciones (RF-04).
 * Fecha de creación: 10/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.adaptadores.rest;

import co.edu.usco.inspectia.analisis.aplicacion.DatosDeLote;
import co.edu.usco.inspectia.comun.texto.Textos;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Los datos con que se registra un lote (RF-04). Las longitudes son las de la tabla {@code lote}. Los
 * textos llegan sin los espacios del inicio y del final, y unas observaciones en blanco quedan vacías. La
 * empresa y el usuario no se reciben: salen de la sesión (RNF-03), y lo que llegue con esos nombres se
 * ignora.
 *
 * @param codigo el código del lote
 * @param fechaRecepcion la fecha en que la empresa recibió el lote
 * @param origen la finca o el proveedor
 * @param observaciones las observaciones, opcionales
 */
@Schema(description = "Los datos con que se registra un lote (RF-04).")
public record LoteSolicitud(
        @Schema(description = "Código del lote, único en la empresa sin distinguir mayúsculas ni espacios al inicio "
                + "o al final.", example = "L-001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validacion.obligatorio}")
        @Size(max = 30, message = "{validacion.longitud_max}")
        String codigo,
        @Schema(description = "Fecha de recepción, no posterior a hoy en Colombia ni de hace más de un año. El "
                + "ejemplo, la de hace una semana, lo pone DocumentacionDeLotes.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{validacion.obligatorio}")
        LocalDate fechaRecepcion,
        @Schema(description = "Finca o proveedor.", example = "Finca La Esperanza",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validacion.obligatorio}")
        @Size(max = 120, message = "{validacion.longitud_max}")
        String origen,
        @Schema(description = "Observaciones, opcionales.", example = "Secado al sol durante 6 días")
        @Size(max = 500, message = "{validacion.longitud_max}")
        String observaciones) {

    /** Recorta los textos antes de validarlos; unas observaciones en blanco quedan vacías. */
    public LoteSolicitud {
        codigo = Textos.recortar(codigo);
        origen = Textos.recortar(origen);
        observaciones = Textos.recortar(observaciones);
        if (observaciones != null && observaciones.isEmpty()) {
            observaciones = null;
        }
    }

    /** @return los datos para el caso de uso */
    DatosDeLote datos() {
        return new DatosDeLote(codigo, fechaRecepcion, origen, observaciones);
    }
}
