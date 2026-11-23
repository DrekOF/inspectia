/*
 * Nombre: SolicitudDeSubida.java
 * Descripción: Los datos con que se pide el enlace para subir una foto: su tipo y su tamaño (RF-05 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Los datos con que se pide el enlace para subir una foto (RF-05 C3). Que sea JPG o PNG y de hasta 10 MB lo revisa
 * el caso de uso, con mensajes que dicen qué hacer (RNF-06 C3).
 *
 * @param tipo el tipo del archivo: {@code image/jpeg} o {@code image/png}
 * @param tamanoBytes cuánto pesa, de 1 a 10.485.760 bytes
 */
@Schema(description = "El archivo de la foto que se va a subir (RF-05).")
public record SolicitudDeSubida(
        @Schema(description = "Tipo del archivo: image/jpeg o image/png.", example = "image/jpeg",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validacion.obligatorio}")
        String tipo,
        @Schema(description = "Cuánto pesa, de 1 a 10.485.760 bytes (10 MB).", example = "2516582",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{validacion.obligatorio}")
        @Min(value = 1, message = "{validacion.minimo}")
        Long tamanoBytes) {
}
