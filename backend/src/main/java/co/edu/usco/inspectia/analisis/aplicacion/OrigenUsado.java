/*
 * Nombre: OrigenUsado.java
 * Descripción: Una finca o un proveedor que ya usó la empresa, con la última vez que lo usó (RF-04).
 * Fecha de creación: 10/11/2026
 * Actualización: 10/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.time.OffsetDateTime;

/**
 * Una forma en que la empresa escribió una finca o un proveedor, y la última vez que la usó en un lote.
 * Con ella se elige qué forma sugerir cuando la misma finca se escribió de varias (RF-04, restricción 3).
 *
 * @param origen la finca o el proveedor, como se escribió
 * @param ultimaVez cuándo se registró el último lote con esa forma
 */
public record OrigenUsado(String origen, OffsetDateTime ultimaVez) {
}
