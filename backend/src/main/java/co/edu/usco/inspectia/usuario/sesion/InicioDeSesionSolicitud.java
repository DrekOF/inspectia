/*
 * Nombre: InicioDeSesionSolicitud.java
 * Descripción: Datos para iniciar sesión: correo y contraseña (RF-01).
 * Fecha de creación: 14/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario.sesion;

import co.edu.usco.inspectia.comun.texto.Textos;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Datos para iniciar sesión (RF-01). El correo se normaliza antes de validarlo, como al registrarlo:
 * sin espacios al inicio ni al final y en minúsculas. La contraseña llega tal cual.
 *
 * @param correo el correo
 * @param contrasena la contraseña
 */
@Schema(description = "Datos para iniciar sesión (RF-01).")
public record InicioDeSesionSolicitud(
        @Schema(description = "Correo. No distingue mayúsculas ni espacios al inicio o al final.",
                example = "ana.perez@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validacion.obligatorio}")
        String correo,
        @Schema(description = "Contraseña", format = "password", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validacion.obligatorio}")
        String contrasena) {

    /** Normaliza el correo antes de validarlo. */
    public InicioDeSesionSolicitud {
        correo = Textos.minusculas(Textos.recortar(correo));
    }

    /** No muestra la contraseña si la solicitud termina en un registro del servidor. */
    @Override
    public String toString() {
        return "InicioDeSesionSolicitud[correo=" + correo + ", contrasena=****]";
    }
}
