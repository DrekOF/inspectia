/*
 * Nombre: ReporteDescartado.java
 * Descripción: Evento de un reporte PDF que ya no sirve: sus archivos se borran del almacén después de confirmar la
 *              transacción (RF-15, D-25, D-50).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

/**
 * Un reporte que ya no sirve (RF-15): el de la muestra 1 cuando termina la 2, porque el resultado cambió (D-25), o uno
 * al que le falta un archivo y se vuelve a armar. Sus archivos, uno por idioma (D-50), se borran solo cuando la
 * transacción se confirma: si se deshace, el lote sigue con su reporte.
 *
 * @param base la base de sus archivos, {@code reportes/<empresa>/<lote>/<uuid>}
 */
public record ReporteDescartado(String base) {
}
