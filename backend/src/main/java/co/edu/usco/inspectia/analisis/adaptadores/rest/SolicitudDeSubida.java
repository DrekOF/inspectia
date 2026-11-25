/*
 * Nombre: SolicitudDeSubida.java
 * Descripción: Los datos con que se pide el enlace para subir una foto: la muestra que espera la página, de 1 a 2
 *              (D-25), su tipo y su tamaño (RF-05 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.adaptadores.rest;

import co.edu.usco.inspectia.analisis.dominio.ReglasDeRecomendacion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Los datos con que se pide el enlace para subir una foto (RF-05 C3). Que sea JPG o PNG y de hasta 10 MB lo revisa
 * el caso de uso, con mensajes que dicen qué hacer (RNF-06 C3). La muestra que espera la página la compara el caso de
 * uso con la que recibe las fotos en ese momento: si el lote cambió mientras la página estaba abierta, responde 409
 * (D-25).
 *
 * @param muestra la muestra que espera la página: 1, o 2 para la segunda muestra de un lote que la sugiere
 * @param tipo el tipo del archivo: {@code image/jpeg} o {@code image/png}
 * @param tamanoBytes cuánto pesa, de 1 a 10.485.760 bytes
 */
@Schema(description = "El archivo de la foto que se va a subir y la muestra que espera la página (RF-05, D-25).")
public record SolicitudDeSubida(
        @Schema(description = "La muestra que espera la página, de 1 a 2: la 1 en un lote pendiente y la 2 en un lote "
                + "que sugiere otra muestra. Si ya no es la que recibe las fotos, responde 409.", example = "1",
                minimum = "1", maximum = "2",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{validacion.obligatorio}")
        @Min(value = 1, message = "{validacion.minimo}")
        @Max(value = ReglasDeRecomendacion.MAXIMO_DE_MUESTRAS, message = "{validacion.maximo}")
        Integer muestra,
        @Schema(description = "Tipo del archivo: image/jpeg o image/png.", example = "image/jpeg",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validacion.obligatorio}")
        String tipo,
        @Schema(description = "Cuánto pesa, de 1 a 10.485.760 bytes (10 MB).", example = "2516582",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{validacion.obligatorio}")
        @Min(value = 1, message = "{validacion.minimo}")
        Long tamanoBytes) {
}
