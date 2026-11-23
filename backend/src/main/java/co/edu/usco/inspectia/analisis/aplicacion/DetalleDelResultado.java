/*
 * Nombre: DetalleDelResultado.java
 * Descripción: El resultado de un lote y el detalle de cada muestra, con sus fotos marcadas (RF-10, RF-11, RF-12,
 *              RF-14 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.math.BigDecimal;
import java.util.List;

/**
 * El resultado de un lote con el detalle de sus muestras (RF-14 C3). Los granos del lote son la suma de sus muestras
 * terminadas (D-25); mientras el lote está pendiente, van en {@code null}.
 *
 * @param lote el resultado guardado del lote
 * @param totalGranos el total de granos de sus muestras terminadas, o {@code null}
 * @param granosSanos sus granos sanos, o {@code null}
 * @param porcentajeContado el porcentaje que contó el modelo, o {@code null}
 * @param muestras sus muestras, por número
 */
public record DetalleDelResultado(ResultadoGuardado lote, Integer totalGranos, Integer granosSanos,
        BigDecimal porcentajeContado, List<DetalleDeMuestra> muestras) {
}
