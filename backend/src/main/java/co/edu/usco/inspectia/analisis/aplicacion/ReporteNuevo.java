/*
 * Nombre: ReporteNuevo.java
 * Descripción: Evento de un reporte PDF recién guardado en el almacén: si la transacción que guarda su base en el lote
 *              no se confirma, sus archivos se borran (RF-15, D-50).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

/**
 * Un reporte recién armado (RF-15). Sus archivos se escriben en el almacén antes de guardar su base en el lote; si la
 * transacción se deshace, nadie los encontraría, así que se borran.
 *
 * @param base la base de sus archivos, {@code reportes/<empresa>/<lote>/<uuid>}
 */
public record ReporteNuevo(String base) {
}
