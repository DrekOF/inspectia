/*
 * Nombre: ParametrosDelAnalisis.java
 * Descripción: Los parámetros del sistema con que se analiza una muestra: el umbral de confianza baja, el máximo de
 *              granos dudosos, el factor del margen y a cuántos puntos de un límite se sugiere otra muestra (P-15,
 *              P-19, D-24).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Los parámetros del análisis. Los fija el equipo en el servidor, no la empresa (RF-09, restricción 4), y así, cuando
 * Julián cierre P-15 y P-19, se cambia un número y no el código. Cada muestra copia el umbral y el máximo de dudosos
 * que usó.
 *
 * @param umbralConfianzaBaja el umbral de confianza baja, en porcentaje de 0 a 100 y con hasta 2 decimales (P-15)
 * @param porcentajeMaxDudosos el máximo de granos dudosos de una muestra, en porcentaje de 0 a 100 y con hasta 1
 *        decimal (RF-09)
 * @param factorDelMargen el factor {@code k} del margen, mayor que 0 (P-19)
 * @param puntosParaOtraMuestra a cuántos puntos de un límite se sugiere otra muestra, mayor que 0 (D-24)
 */
public record ParametrosDelAnalisis(BigDecimal umbralConfianzaBaja, BigDecimal porcentajeMaxDudosos,
        BigDecimal factorDelMargen, BigDecimal puntosParaOtraMuestra) {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    /**
     * Revisa los parámetros: la columna del umbral es {@code numeric(5,2)} y la del máximo, {@code numeric(4,1)}.
     *
     * @throws IllegalArgumentException si falta uno o alguno no cabe en su rango
     */
    public ParametrosDelAnalisis {
        Objects.requireNonNull(umbralConfianzaBaja, "umbralConfianzaBaja");
        Objects.requireNonNull(porcentajeMaxDudosos, "porcentajeMaxDudosos");
        Objects.requireNonNull(factorDelMargen, "factorDelMargen");
        Objects.requireNonNull(puntosParaOtraMuestra, "puntosParaOtraMuestra");
        revisarPorcentaje(umbralConfianzaBaja, 2, "umbral-confianza-baja");
        revisarPorcentaje(porcentajeMaxDudosos, 1, "porcentaje-max-dudosos");
        if (factorDelMargen.signum() <= 0) {
            throw new IllegalArgumentException("factor-del-margen debe ser mayor que 0, y es " + factorDelMargen);
        }
        if (puntosParaOtraMuestra.signum() <= 0) {
            throw new IllegalArgumentException("puntos-para-otra-muestra debe ser mayor que 0, y es "
                    + puntosParaOtraMuestra);
        }
    }

    /**
     * Las reglas de la confianza con estos parámetros.
     *
     * @return las reglas
     */
    public ReglasDeConfianza reglasDeConfianza() {
        return new ReglasDeConfianza(umbralConfianzaBaja, porcentajeMaxDudosos);
    }

    private static void revisarPorcentaje(BigDecimal valor, int decimales, String nombre) {
        if (valor.signum() < 0 || valor.compareTo(CIEN) > 0) {
            throw new IllegalArgumentException(nombre + " debe ir de 0 a 100, y es " + valor);
        }
        if (valor.stripTrailingZeros().scale() > decimales) {
            throw new IllegalArgumentException(nombre + " admite hasta " + decimales + " decimales, y es " + valor);
        }
    }
}
