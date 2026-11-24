/*
 * Nombre: Estadisticas.java
 * Descripción: Las estadísticas de la empresa en un periodo, como las responde la API (RF-16): lotes analizados,
 *              promedio de granos sanos, lotes por categoría, promedio por mes y promedio por finca o proveedor.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.estadistica.lotes;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Las estadísticas de la empresa en un periodo (RF-16, salida). Sin lotes en el periodo, {@code sinLotes} es
 * {@code true}, el promedio va vacío, sin meses ni fincas, y las 4 categorías en 0 (RF-16 C2).
 *
 * @param desde el primer día del periodo, o {@code null} si no tiene
 * @param hasta el último día del periodo, o {@code null} si no tiene
 * @param sinLotes si no hay lotes analizados en el periodo
 * @param lotesAnalizados cuántos lotes analizados hay en el periodo
 * @param promedioSanos el promedio de sus porcentajes de granos sanos, con 1 decimal, o {@code null} sin lotes
 * @param porCategoria los lotes de cada categoría probable, en el orden de los rangos, con las 4
 * @param porMes el promedio de cada mes, del primero con lotes al último
 * @param porOrigen el promedio de cada finca o proveedor, de mayor a menor
 */
@Schema(description = "Las estadísticas de la empresa en el periodo (RF-16).")
public record Estadisticas(
        @Schema(description = "El primer día del periodo, en Colombia e incluido; vacío si no tiene.",
                example = "2026-11-01") LocalDate desde,
        @Schema(description = "El último día del periodo, en Colombia e incluido; vacío si no tiene.",
                example = "2026-11-30") LocalDate hasta,
        @Schema(description = "Si no hay lotes analizados en el periodo (RF-16 C2).") boolean sinLotes,
        @Schema(description = "Cuántos lotes analizados hay en el periodo.", example = "12") long lotesAnalizados,
        @Schema(description = "El promedio del porcentaje de granos sanos de esos lotes, con 1 decimal; vacío sin "
                + "lotes.", example = "91.3") BigDecimal promedioSanos,
        @Schema(description = "Los lotes de cada categoría probable, en el orden de los rangos, con las 4.")
        List<LotesPorCategoria> porCategoria,
        @Schema(description = "El promedio de cada mes de Colombia, del primero con lotes al último; un mes del medio "
                + "sin lotes va con 0 lotes y sin promedio.") List<PromedioPorMes> porMes,
        @Schema(description = "El promedio de cada finca o proveedor, sin distinguir mayúsculas, tildes ni los "
                + "espacios del inicio y del final, de mayor a menor promedio.") List<PromedioPorOrigen> porOrigen) {

    /**
     * Los lotes de una categoría.
     *
     * @param categoria el código de la categoría, como {@code PREMIUM}
     * @param lotes cuántos lotes del periodo tienen esa categoría
     */
    @Schema(description = "Los lotes de una categoría probable.")
    public record LotesPorCategoria(
            @Schema(description = "La categoría probable.", example = "PREMIUM") String categoria,
            @Schema(description = "Cuántos lotes del periodo la tienen.", example = "4") long lotes) {
    }

    /**
     * El promedio de un mes.
     *
     * @param mes el mes de Colombia, como {@code 2026-11}
     * @param lotes cuántos lotes tuvieron su resultado ese mes
     * @param promedioSanos su promedio, con 1 decimal, o {@code null} si no hubo lotes
     */
    @Schema(description = "El promedio de granos sanos de un mes.")
    public record PromedioPorMes(
            @Schema(description = "El mes, en la hora de Colombia (aaaa-mm).", example = "2026-11") String mes,
            @Schema(description = "Cuántos lotes tuvieron su resultado ese mes.", example = "5") long lotes,
            @Schema(description = "Su promedio, con 1 decimal; vacío si no hubo lotes.", example = "90.2")
            BigDecimal promedioSanos) {
    }

    /**
     * El promedio de una finca o un proveedor.
     *
     * @param origen la finca o el proveedor, con la forma más usada en el periodo
     * @param lotes cuántos lotes del periodo son de ahí
     * @param promedioSanos su promedio, con 1 decimal
     */
    @Schema(description = "El promedio de granos sanos de una finca o un proveedor.")
    public record PromedioPorOrigen(
            @Schema(description = "La finca o el proveedor, con la forma más usada en el periodo y, si empatan, la "
                    + "del resultado más reciente.", example = "Finca La Esperanza") String origen,
            @Schema(description = "Cuántos lotes del periodo son de ahí.", example = "3") long lotes,
            @Schema(description = "Su promedio, con 1 decimal.", example = "92.4") BigDecimal promedioSanos) {
    }
}
