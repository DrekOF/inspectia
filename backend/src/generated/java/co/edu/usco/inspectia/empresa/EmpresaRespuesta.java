/*
 * Nombre: EmpresaRespuesta.java
 * Descripción: Empresa en la respuesta de la API.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.10)
 * Versión: 1.10
 * Entidades: empresa 1.3, usuario 1.8, rango_recomendacion 1.3
 */
package co.edu.usco.inspectia.empresa;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Un registro de {@code empresa} como lo responde la API. Nunca trae contraseñas.
 *
 * @param id identificador (UUID v7)
 * @param nombre Nombre
 * @param nit NIT
 * @param ciudad Ciudad
 * @param activa Activa
 * @param fechaCreacion fecha de creación del registro
 * @param fechaActualizacion fecha de la última actualización
 */
@Schema(description = "Empresa.")
public record EmpresaRespuesta(
        @Schema(description = "Identificador (UUID v7).") UUID id,
        @Schema(description = "Nombre", example = "Cacaos del Huila") String nombre,
        @Schema(description = "NIT", example = "900123456-8") String nit,
        @Schema(description = "Ciudad", example = "Neiva") String ciudad,
        @Schema(description = "Activa", example = "true") Boolean activa,
        @Schema(description = "Fecha de creación del registro.") OffsetDateTime fechaCreacion,
        @Schema(description = "Fecha de la última actualización.") OffsetDateTime fechaActualizacion) {
}
