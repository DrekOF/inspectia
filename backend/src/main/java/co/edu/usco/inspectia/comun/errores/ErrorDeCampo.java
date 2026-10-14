/*
 * Nombre: ErrorDeCampo.java
 * Descripción: Un error de la lista «errores» de las respuestas de error de la API.
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.errores;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Un error de la lista {@code errores} de una respuesta de error: el frontend marca el campo con el
 * mensaje.
 *
 * @param campo el campo de la petición; no aparece si el error no es de un campo
 * @param mensaje el mensaje, en el idioma de la petición
 */
@Schema(description = "Un dato que hay que corregir.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorDeCampo(
        @Schema(description = "Campo de la petición.", example = "nit") String campo,
        @Schema(description = "Qué corregir, en el idioma de la petición.",
                example = "Ya existe una empresa con este NIT.") String mensaje) {
}
