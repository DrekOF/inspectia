/*
 * Nombre: ResultadoGuardado.java
 * Descripción: El resultado de un lote como está guardado: sus datos y las cifras de su resultado, vacías mientras está
 *              pendiente (RF-10, RF-12, RF-14 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
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
 * El resultado guardado de un lote (RF-14 C3): se muestra tal cual, sin recalcular con los rangos de hoy (RF-13 C3).
 * Mientras el lote está pendiente, las cifras del resultado van en {@code null}.
 *
 * @param id el lote
 * @param codigo su código
 * @param fechaRecepcion su fecha de recepción
 * @param origen la finca o el proveedor
 * @param estado pendiente o analizado
 * @param fechaResultado la fecha del resultado, o {@code null}
 * @param porcentajeSanos el porcentaje corregido, o {@code null}
 * @param margen su margen, o {@code null}
 * @param categoria la categoría probable, o {@code null}
 * @param recomendacion el texto de la recomendación, o {@code null}
 * @param sugerirOtraMuestra si se sugiere otra muestra, o {@code null}
 * @param avisoConfianzaBaja si lleva el aviso de confianza baja, o {@code null}
 */
public record ResultadoGuardado(UUID id, String codigo, LocalDate fechaRecepcion, String origen, EstadoDeLote estado,
        OffsetDateTime fechaResultado, BigDecimal porcentajeSanos, BigDecimal margen, String categoria,
        String recomendacion, Boolean sugerirOtraMuestra, Boolean avisoConfianzaBaja) {
}
