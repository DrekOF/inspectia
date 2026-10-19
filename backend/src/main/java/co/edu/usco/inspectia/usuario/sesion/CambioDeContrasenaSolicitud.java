/*
 * Nombre: CambioDeContrasenaSolicitud.java
 * Descripción: La contraseña actual y la nueva, para cambiar la propia contraseña (RF-18).
 * Fecha de creación: 19/10/2026
 * Actualización: 19/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario.sesion;

import co.edu.usco.inspectia.comun.validacion.MaximoBytes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * La contraseña actual y la nueva (RF-18). Las dos llegan tal cual: no se recortan sus espacios.
 *
 * <p>La nueva pasa por las mismas validaciones y con los mismos mensajes que la de {@code contrasena_hash}
 * en {@code specs/entidades/usuario.json}: de 8 a 64 caracteres, como máximo 72 bytes y no solo espacios.
 * Si cambian allí, cambian también aquí; {@code CambioDeContrasenaIT} prueba los límites. La actual solo
 * es obligatoria: si no coincide, la revisa el servicio y cuenta como intento fallido.
 *
 * @param contrasenaActual la contraseña de ahora
 * @param contrasenaNueva la contraseña nueva
 */
@Schema(description = "La contraseña actual y la nueva (RF-18).")
public record CambioDeContrasenaSolicitud(
        @Schema(description = "Contraseña actual", format = "password", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validacion.obligatorio}")
        String contrasenaActual,
        @Schema(description = "Contraseña nueva", format = "password", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validacion.obligatorio}")
        @Size(max = 64, message = "{usuario.contrasena_hash.longitud_max}")
        @Size(min = 8, message = "{usuario.contrasena_hash.longitud_min}")
        @MaximoBytes(value = 72, message = "{usuario.contrasena_hash.bytes_max}")
        String contrasenaNueva) {

    /** No muestra las contraseñas si la solicitud termina en un registro del servidor. */
    @Override
    public String toString() {
        return "CambioDeContrasenaSolicitud[contrasenaActual=****, contrasenaNueva=****]";
    }
}
