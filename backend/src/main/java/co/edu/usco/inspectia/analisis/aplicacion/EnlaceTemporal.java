/*
 * Nombre: EnlaceTemporal.java
 * Descripción: Un enlace temporal del almacén de archivos, para subir o ver una foto sin la sesión (D-41).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.time.Instant;
import java.util.Map;

/**
 * Un enlace temporal del almacén de archivos (D-41). Con la carpeta local es relativo a la página, como
 * {@code /api/v1/archivos/...}; con S3 será absoluto.
 *
 * @param url la dirección
 * @param metodo el método HTTP con que se usa: {@code PUT} para subir y {@code GET} para ver
 * @param encabezados los encabezados que hay que enviar, como el {@code Content-Type} firmado
 * @param vence cuándo deja de servir
 */
public record EnlaceTemporal(String url, String metodo, Map<String, String> encabezados, Instant vence) {
}
