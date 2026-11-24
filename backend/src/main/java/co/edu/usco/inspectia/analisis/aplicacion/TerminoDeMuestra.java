/*
 * Nombre: TerminoDeMuestra.java
 * Descripción: Lo que se guarda en una muestra al terminarla: su modelo, sus conteos, su aviso, la copia de los
 *              parámetros y su fecha de fin (RF-10 C6, RF-08 C3, RF-09).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Lo que guarda una muestra al terminar (RF-10 C6). Copia los parámetros de la confianza que usó, así sus granos
 * dudosos no cambian si el equipo los ajusta (RF-09 C5).
 *
 * @param modeloId el modelo que la analizó (RF-08 C3)
 * @param totalGranos su total de granos, 100 o más
 * @param granosSanos los que el modelo marcó como sanos
 * @param avisoConfianzaBaja si sale con el aviso de confianza baja (RF-09)
 * @param umbralConfianzaBaja la copia del umbral de confianza baja (P-15)
 * @param porcentajeMaxDudosos la copia del máximo de dudosos (RF-09)
 * @param fechaFin cuándo terminó, al microsegundo
 */
public record TerminoDeMuestra(UUID modeloId, int totalGranos, int granosSanos, boolean avisoConfianzaBaja,
        BigDecimal umbralConfianzaBaja, BigDecimal porcentajeMaxDudosos, Instant fechaFin) {
}
