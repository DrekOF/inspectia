/*
 * Nombre: VistaDeLote.java
 * Descripción: Un lote como se consulta: sus datos, quién lo registró y su resultado, si ya lo tiene (RF-04,
 *              RF-14).
 * Fecha de creación: 10/11/2026
 * Actualización: 10/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeLote;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Un lote como se consulta en el historial y en su detalle (RF-14). Mientras está pendiente, el
 * porcentaje, la categoría y la fecha del resultado van vacíos.
 *
 * @param id identificador del lote
 * @param codigo el código
 * @param fechaRecepcion la fecha de recepción
 * @param origen la finca o el proveedor
 * @param observaciones las observaciones, o {@code null}
 * @param estado pendiente o analizado
 * @param usuarioId quién lo registró
 * @param usuarioNombre su nombre
 * @param porcentajeSanos el porcentaje de granos sanos, o {@code null}
 * @param categoria la categoría probable, o {@code null}
 * @param fechaResultado la fecha del resultado, o {@code null}
 * @param fechaCreacion la fecha de registro
 */
public record VistaDeLote(UUID id, String codigo, LocalDate fechaRecepcion, String origen, String observaciones,
        EstadoDeLote estado, UUID usuarioId, String usuarioNombre, BigDecimal porcentajeSanos, String categoria,
        OffsetDateTime fechaResultado, OffsetDateTime fechaCreacion) {
}
