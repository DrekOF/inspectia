/*
 * Nombre: UsuarioRespuesta.java
 * Descripción: Usuario en la respuesta de la API.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.11)
 * Versión: 1.11
 * Entidades: usuario 1.8
 */
package co.edu.usco.inspectia.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Un registro de {@code usuario} como lo responde la API. Nunca trae contraseñas.
 *
 * @param id identificador (UUID v7)
 * @param nombre Nombre
 * @param correo Correo
 * @param rol Rol
 * @param activo Activo
 * @param fechaCreacion fecha de creación del registro
 * @param fechaActualizacion fecha de la última actualización
 */
@Schema(description = "Usuario.")
public record UsuarioRespuesta(
        @Schema(description = "Identificador (UUID v7).") UUID id,
        @Schema(description = "Nombre", example = "Ana Pérez") String nombre,
        @Schema(description = "Correo", example = "ana.perez@example.com") String correo,
        @Schema(description = "Rol", example = "USUARIO_FINAL") UsuarioRol rol,
        @Schema(description = "Activo", example = "true") Boolean activo,
        @Schema(description = "Fecha de creación del registro.") OffsetDateTime fechaCreacion,
        @Schema(description = "Fecha de la última actualización.") OffsetDateTime fechaActualizacion) {
}
