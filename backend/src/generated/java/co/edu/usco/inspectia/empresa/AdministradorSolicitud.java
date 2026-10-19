/*
 * Nombre: AdministradorSolicitud.java
 * Descripción: Datos del primer administrador al registrar empresa.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.5)
 * Versión: 1.5
 * Entidades: empresa 1.2, usuario 1.5, rango_recomendacion 1.1
 */
package co.edu.usco.inspectia.empresa;

import co.edu.usco.inspectia.comun.texto.Textos;
import co.edu.usco.inspectia.comun.validacion.MaximoBytes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Primer administrador, que se crea junto con empresa (RF-02).
 * Antes de validar se normalizan los textos como pide el JSON: sin espacios al inicio ni al final
 * y, si corresponde, en minúsculas.
 *
 * @param nombre Nombre
 * @param correo Correo
 * @param contrasena Contraseña
 */
@Schema(description = "Primer administrador, que se crea junto con empresa (RF-02).")
public record AdministradorSolicitud(
        @Schema(description = "Nombre", example = "Ana Pérez", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validacion.obligatorio}")
        @Size(max = 120, message = "{validacion.longitud_max}")
        String nombre,
        @Schema(description = "Correo", example = "ana.perez@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validacion.obligatorio}")
        @Size(max = 254, message = "{validacion.longitud_max}")
        @Email(message = "{validacion.correo}")
        String correo,
        @Schema(description = "Contraseña", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validacion.obligatorio}")
        @Size(max = 64, message = "{usuario.contrasena_hash.longitud_max}")
        @Size(min = 8, message = "{usuario.contrasena_hash.longitud_min}")
        @MaximoBytes(value = 72, message = "{usuario.contrasena_hash.bytes_max}")
        String contrasena) {

    /** Normaliza los textos antes de validarlos. */
    public AdministradorSolicitud {
        nombre = Textos.recortar(nombre);
        correo = Textos.minusculas(Textos.recortar(correo));
    }
}
