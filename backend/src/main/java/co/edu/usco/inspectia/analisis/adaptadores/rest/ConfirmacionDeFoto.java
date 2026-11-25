/*
 * Nombre: ConfirmacionDeFoto.java
 * Descripción: Los datos con que se confirma una foto subida: la llave que dio el enlace de subida (RF-05).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.adaptadores.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Los datos con que se confirma una foto subida (RF-05).
 *
 * @param llave la llave que dio el enlace de subida, con la muestra para la que se pidió (D-25)
 */
@Schema(description = "La foto subida que se confirma (RF-05).")
public record ConfirmacionDeFoto(
        @Schema(description = "La llave que dio el enlace de subida. Lleva la muestra para la que se pidió: si ya no "
                + "es la que recibe las fotos, responde 409 (D-25).", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validacion.obligatorio}")
        String llave) {
}
