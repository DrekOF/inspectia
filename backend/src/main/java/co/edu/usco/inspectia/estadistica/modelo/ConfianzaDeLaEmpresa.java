/*
 * Nombre: ConfianzaDeLaEmpresa.java
 * Descripción: La confianza promedio de los granos de los análisis de una empresa, con cuántos granos y cuántas
 *              muestras la dan, como la responde la base (RF-17 C2, RNF-03).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.estadistica.modelo;

import java.math.BigDecimal;

/** Una fila de {@link MetricasDelModeloRepositorio#confianzaDe}: los nombres de sus métodos son los de sus columnas. */
public interface ConfianzaDeLaEmpresa {

    /**
     * La confianza promedio de los granos, en porcentaje y con 2 decimales.
     *
     * @return el promedio, o {@code null} si la empresa no tiene granos analizados
     */
    BigDecimal getPromedio();

    /**
     * Cuántos granos dan el promedio.
     *
     * @return los granos
     */
    Long getGranos();

    /**
     * De cuántas muestras analizadas son esos granos.
     *
     * @return las muestras
     */
    Long getMuestras();
}
