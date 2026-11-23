/*
 * Nombre: SubidaRespuesta.java
 * Descripción: El enlace para subir una foto, como lo responde la API (RF-05, D-41).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.rest;

import co.edu.usco.inspectia.analisis.aplicacion.SubidaDeFoto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

/**
 * El enlace para subir una foto (RF-05, D-41), con la llave con que después se confirma.
 *
 * @param llave la llave del archivo, para confirmar la foto
 * @param url el enlace: relativo a la página con la carpeta local, absoluto con S3
 * @param metodo el método con que se sube: {@code PUT}
 * @param encabezados los encabezados que hay que enviar, como el {@code Content-Type}
 * @param vence cuándo deja de servir
 */
@Schema(description = "El enlace para subir una foto: se usa con PUT, sin el token (D-41).")
public record SubidaRespuesta(String llave, String url, String metodo, Map<String, String> encabezados,
        Instant vence) {

    /**
     * La respuesta de una subida pedida.
     *
     * @param subida la subida
     * @return la respuesta
     */
    public static SubidaRespuesta de(SubidaDeFoto subida) {
        return new SubidaRespuesta(subida.llave(), subida.enlace().url(), subida.enlace().metodo(),
                subida.enlace().encabezados(), subida.enlace().vence());
    }
}
