/*
 * Nombre: EmpresaCrearSolicitud.java
 * Descripción: Datos para registrar empresa por la API.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.6)
 * Versión: 1.6
 * Entidades: empresa 1.3, usuario 1.6, rango_recomendacion 1.2
 */
package co.edu.usco.inspectia.empresa;

import co.edu.usco.inspectia.comun.texto.Textos;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar empresa.
 * Antes de validar se normalizan los textos como pide el JSON: sin espacios al inicio ni al final
 * y, si corresponde, en minúsculas.
 *
 * @param nombre Nombre
 * @param nit NIT
 * @param ciudad Ciudad
 * @param administrador Primer administrador
 */
@Schema(description = "Datos para registrar empresa.")
public record EmpresaCrearSolicitud(
        @Schema(description = "Nombre", example = "Cacaos del Huila", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validacion.obligatorio}")
        @Size(max = 120, message = "{validacion.longitud_max}")
        String nombre,
        @Schema(description = "NIT", example = "900123456-8", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validacion.obligatorio}")
        @Size(max = 15, message = "{validacion.longitud_max}")
        @Pattern(regexp = "^[0-9]{6,10}-[0-9]$", message = "{empresa.nit.patron}")
        String nit,
        @Schema(description = "Ciudad", example = "Neiva", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validacion.obligatorio}")
        @Size(max = 80, message = "{validacion.longitud_max}")
        String ciudad,
        @Schema(description = "Primer administrador", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{validacion.obligatorio}")
        @Valid
        AdministradorSolicitud administrador) {

    /** Normaliza los textos antes de validarlos. */
    public EmpresaCrearSolicitud {
        nombre = Textos.recortar(nombre);
        nit = Textos.recortar(nit);
        ciudad = Textos.recortar(ciudad);
    }
}
