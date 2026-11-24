/*
 * Nombre: ReporteDelLote.java
 * Descripción: El reporte PDF de un lote listo para descargar: el nombre del archivo y sus bytes (RF-15).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

/**
 * El reporte PDF de un lote, para descargarlo (RF-15).
 *
 * @param nombre el nombre del archivo, como {@code InspectIA-L-0143-es.pdf}
 * @param contenido los bytes del PDF
 */
public record ReporteDelLote(String nombre, byte[] contenido) {
}
