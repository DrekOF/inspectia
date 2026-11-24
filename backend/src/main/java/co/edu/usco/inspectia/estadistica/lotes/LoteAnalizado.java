/*
 * Nombre: LoteAnalizado.java
 * Descripción: Lo que las estadísticas leen de un lote analizado: su finca o proveedor, su resultado, su categoría y
 *              la fecha del resultado (RF-16).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.estadistica.lotes;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Un lote analizado, como lo usan las estadísticas. Su resultado es el del lote: con 2 muestras, el de la suma (D-25,
 * RF-16 restricción 2).
 *
 * @param origen la finca o el proveedor, como se escribió
 * @param porcentajeSanos el porcentaje de granos sanos del lote, con 1 decimal
 * @param categoria el código de la categoría probable, como {@code ESTANDAR}
 * @param fechaResultado la fecha del resultado
 */
public record LoteAnalizado(String origen, BigDecimal porcentajeSanos, String categoria, Instant fechaResultado) {
}
