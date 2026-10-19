/*
 * Nombre: AdministradorRespuesta.java
 * Descripción: Un administrador de empresa como lo lista la API del administrador de la plataforma,
 *              sin su contraseña (RF-02 C12).
 * Fecha de creación: 18/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario.administradores;

import co.edu.usco.inspectia.usuario.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Un administrador de empresa, para elegir a cuál restablecerle la contraseña. Nunca lleva la
 * contraseña, su hash, los intentos fallidos ni el bloqueo.
 *
 * @param id su identificador
 * @param nombre su nombre
 * @param correo su correo, con el que inicia sesión
 * @param activo si puede iniciar sesión (RF-03)
 */
@Schema(description = "Un administrador de empresa, sin su contraseña (RF-02 C12).")
public record AdministradorRespuesta(
        @Schema(description = "Identificador (UUID v7).") UUID id,
        @Schema(description = "Nombre", example = "Ana Pérez") String nombre,
        @Schema(description = "Correo", example = "ana.perez@example.com") String correo,
        @Schema(description = "Si puede iniciar sesión (RF-03).") Boolean activo) {

    /** El administrador a partir de su usuario. */
    static AdministradorRespuesta de(Usuario usuario) {
        return new AdministradorRespuesta(usuario.getId(), usuario.getNombre(), usuario.getCorreo(), usuario.getActivo());
    }
}
