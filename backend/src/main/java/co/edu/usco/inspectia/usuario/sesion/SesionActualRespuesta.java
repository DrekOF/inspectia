/*
 * Nombre: SesionActualRespuesta.java
 * Descripción: Respuesta de la sesión actual: cuándo vence y el usuario con su rol y su empresa
 *              (RF-01, D-28).
 * Fecha de creación: 14/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario.sesion;

import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Respuesta de la sesión actual (RF-01, D-28).
 *
 * @param vence cuándo vence la sesión
 * @param usuario el usuario, con su rol y su empresa leídos de la base en esta petición
 */
@Schema(description = "Sesión actual (RF-01, D-28).")
public record SesionActualRespuesta(
        @Schema(description = "Cuándo vence la sesión (RNF-04).", example = "2026-10-14T23:20:30Z") Instant vence,
        @Schema(description = "El usuario, con su rol y su empresa leídos de la base en esta petición.")
        UsuarioDeSesionRespuesta usuario) {

    /** La respuesta para el usuario de la sesión. */
    static SesionActualRespuesta de(UsuarioEnSesion usuario) {
        return new SesionActualRespuesta(usuario.vence(), UsuarioDeSesionRespuesta.de(usuario));
    }
}
