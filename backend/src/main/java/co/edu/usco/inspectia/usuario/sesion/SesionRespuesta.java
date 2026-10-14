/*
 * Nombre: SesionRespuesta.java
 * Descripción: Respuesta del inicio de sesión: el token, cuándo vence y el usuario con su rol y su
 *              empresa (RF-01, D-28).
 * Fecha de creación: 14/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario.sesion;

import co.edu.usco.inspectia.comun.seguridad.EmisorDeTokens;
import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Respuesta del inicio de sesión (RF-01, D-28).
 *
 * @param token el token de la sesión
 * @param tipo el tipo del token: siempre {@value #TIPO}
 * @param vence cuándo vence la sesión
 * @param usuario el usuario, con su rol y su empresa
 */
@Schema(description = "Sesión iniciada (RF-01, D-28).")
public record SesionRespuesta(
        @Schema(description = "Token de la sesión. Va en el encabezado Authorization: Bearer de cada petición.")
        String token,
        @Schema(description = "Tipo del token.", example = TIPO) String tipo,
        @Schema(description = "Cuándo vence la sesión: 8 horas después de iniciarla (RNF-04).",
                example = "2026-10-14T23:20:30Z")
        Instant vence,
        @Schema(description = "El usuario, con su rol y su empresa, para armar el menú de su rol.")
        UsuarioDeSesionRespuesta usuario) {

    /** Tipo del token: va antes del token en el encabezado {@code Authorization}. */
    public static final String TIPO = "Bearer";

    /** La respuesta de un token recién emitido para el usuario. */
    static SesionRespuesta de(EmisorDeTokens.TokenEmitido token, UsuarioEnSesion usuario) {
        return new SesionRespuesta(token.token(), TIPO, token.vence(), UsuarioDeSesionRespuesta.de(usuario));
    }

    /** No muestra el token si la respuesta termina en un registro del servidor: con él se entra a la cuenta. */
    @Override
    public String toString() {
        return "SesionRespuesta[token=****, tipo=" + tipo + ", vence=" + vence + ", usuario=" + usuario + "]";
    }
}
