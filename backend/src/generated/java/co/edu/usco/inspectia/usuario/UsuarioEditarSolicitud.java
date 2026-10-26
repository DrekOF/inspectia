/*
 * Nombre: UsuarioEditarSolicitud.java
 * Descripción: Datos para editar usuario por la API.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.8)
 * Versión: 1.8
 * Entidades: usuario 1.7
 */
package co.edu.usco.inspectia.usuario;

import co.edu.usco.inspectia.comun.texto.Textos;
import co.edu.usco.inspectia.comun.validacion.MaximoBytes;
import co.edu.usco.inspectia.comun.validacion.NoEnBlanco;
import co.edu.usco.inspectia.comun.validacion.ValorPermitido;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para editar usuario.
 * Antes de validar se normalizan los textos como pide el JSON: sin espacios al inicio ni al final
 * y, si corresponde, en minúsculas.
 *
 * @param nombre Nombre
 * @param correo Correo
 * @param contrasena Contraseña
 * @param rol Rol
 */
@Schema(description = "Datos para editar usuario.")
public record UsuarioEditarSolicitud(
        @Schema(description = "Nombre", example = "Ana Pérez", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validacion.obligatorio}")
        @Size(max = 120, message = "{validacion.longitud_max}")
        String nombre,
        @Schema(description = "Correo", example = "ana.perez@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validacion.obligatorio}")
        @Size(max = 254, message = "{validacion.longitud_max}")
        @Email(message = "{validacion.correo}")
        String correo,
        @Schema(description = "Contraseña")
        @NoEnBlanco(message = "{validacion.obligatorio}")
        @Size(max = 64, message = "{usuario.contrasena_hash.longitud_max}")
        @Size(min = 8, message = "{usuario.contrasena_hash.longitud_min}")
        @MaximoBytes(value = 72, message = "{usuario.contrasena_hash.bytes_max}")
        String contrasena,
        @Schema(description = "Rol", example = "USUARIO_FINAL", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{validacion.obligatorio}")
        @ValorPermitido(valores = {"USUARIO_FINAL", "ADMIN_EMPRESA"}, message = "{usuario.rol.valores_en_formulario}")
        UsuarioRol rol) {

    /** Normaliza los textos antes de validarlos. */
    public UsuarioEditarSolicitud {
        nombre = Textos.recortar(nombre);
        correo = Textos.minusculas(Textos.recortar(correo));
    }
}
