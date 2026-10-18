/*
 * Nombre: UsuarioDeSesionRespuesta.java
 * Descripción: El usuario de una sesión como lo responde la API: nombre, correo, rol y empresa, para
 *              armar el menú de su rol (RF-01, RNF-03).
 * Fecha de creación: 14/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario.sesion;

import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import co.edu.usco.inspectia.usuario.UsuarioRol;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * El usuario de una sesión como lo responde la API (RF-01). Nunca trae la contraseña.
 *
 * @param id identificador (UUID v7)
 * @param nombre su nombre
 * @param correo su correo
 * @param rol su rol, que define su menú
 * @param empresa su empresa; {@code null} para el administrador de la plataforma (RNF-03)
 */
@Schema(description = "Usuario de la sesión, para armar el menú de su rol (RF-01).")
public record UsuarioDeSesionRespuesta(
        @Schema(description = "Identificador (UUID v7).") UUID id,
        @Schema(description = "Nombre", example = "Ana Pérez") String nombre,
        @Schema(description = "Correo", example = "ana.perez@example.com") String correo,
        @Schema(description = "Rol: define el menú y las funciones que puede usar.") UsuarioRol rol,
        @Schema(description = "Su empresa; null para el administrador de la plataforma (RNF-03).")
        EmpresaDeSesion empresa) {

    /** La respuesta para el usuario de una sesión. */
    static UsuarioDeSesionRespuesta de(UsuarioEnSesion usuario) {
        EmpresaDeSesion empresa = usuario.empresaId() == null ? null
                : new EmpresaDeSesion(usuario.empresaId(), usuario.empresaNombre());
        return new UsuarioDeSesionRespuesta(usuario.id(), usuario.nombre(), usuario.correo(),
                UsuarioRol.valueOf(usuario.rol()), empresa);
    }

    /**
     * La empresa del usuario de una sesión.
     *
     * @param id identificador (UUID v7)
     * @param nombre su nombre
     */
    @Schema(description = "Empresa del usuario de la sesión.")
    public record EmpresaDeSesion(
            @Schema(description = "Identificador (UUID v7).") UUID id,
            @Schema(description = "Nombre", example = "Cacaos del Huila") String nombre) {
    }
}
