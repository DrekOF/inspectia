/*
 * Nombre: DatosDeLote.java
 * Descripción: Los datos con que se registra un lote, ya validados y recortados (RF-04).
 * Fecha de creación: 10/11/2026
 * Actualización: 10/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.time.LocalDate;

/**
 * Los datos con que se registra un lote (RF-04), ya validados y sin espacios al inicio ni al final.
 *
 * @param codigo el código del lote
 * @param fechaRecepcion la fecha en que la empresa recibió el lote
 * @param origen la finca o el proveedor
 * @param observaciones las observaciones; {@code null} si no tiene
 */
public record DatosDeLote(String codigo, LocalDate fechaRecepcion, String origen, String observaciones) {
}
