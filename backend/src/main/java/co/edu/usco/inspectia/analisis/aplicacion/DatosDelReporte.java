/*
 * Nombre: DatosDelReporte.java
 * Descripción: Lo que el reporte PDF de un lote lee aparte de su resultado: el nombre y el NIT de su empresa y la base
 *              de los archivos del reporte, si ya se armó (RF-15, D-50).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

/**
 * La empresa de un lote y la base de su reporte (RF-15).
 *
 * @param empresa el nombre de la empresa, como está hoy
 * @param nit su NIT
 * @param rutaDelReporte la base de los archivos del reporte, {@code reportes/<empresa>/<lote>/<uuid>}, o {@code null}
 *     si todavía no se armó o se descartó al terminar la muestra 2 (D-25, D-50)
 */
public record DatosDelReporte(String empresa, String nit, String rutaDelReporte) {
}
