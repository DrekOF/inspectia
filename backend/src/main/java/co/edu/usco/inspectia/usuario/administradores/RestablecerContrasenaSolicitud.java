/*
 * Nombre: RestablecerContrasenaSolicitud.java
 * Descripción: La contraseña nueva de un administrador de empresa, con las mismas validaciones de la
 *              contraseña inicial (RF-02 C12).
 * Fecha de creación: 18/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario.administradores;

import co.edu.usco.inspectia.comun.validacion.MaximoBytes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * La contraseña nueva de un administrador de empresa (RF-02 C12). Pasa por las mismas validaciones y
 * con los mismos mensajes que la de {@code contrasena_hash} en {@code specs/entidades/usuario.json}: de
 * 8 a 64 caracteres, como máximo 72 bytes y no solo espacios. Si cambian allí, cambian también aquí;
 * {@code RestablecerContrasenaIT} prueba los límites.
 *
 * @param contrasena la contraseña nueva, tal cual: no se recortan sus espacios
 */
@Schema(description = "Contraseña nueva de un administrador de empresa (RF-02 C12).")
public record RestablecerContrasenaSolicitud(
        @Schema(description = "Contraseña nueva", format = "password", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validacion.obligatorio}")
        @Size(max = 64, message = "{usuario.contrasena_hash.longitud_max}")
        @Size(min = 8, message = "{usuario.contrasena_hash.longitud_min}")
        @MaximoBytes(value = 72, message = "{usuario.contrasena_hash.bytes_max}")
        String contrasena) {

    /** No muestra la contraseña si la solicitud termina en un registro del servidor. */
    @Override
    public String toString() {
        return "RestablecerContrasenaSolicitud[contrasena=****]";
    }
}
