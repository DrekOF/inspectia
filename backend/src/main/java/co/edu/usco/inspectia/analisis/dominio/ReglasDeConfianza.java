/*
 * Nombre: ReglasDeConfianza.java
 * Descripción: Las reglas de la confianza de los granos: cuándo un grano es dudoso, cuándo una muestra tiene demasiados
 *              dudosos y cuándo sale con el aviso de confianza baja (RF-09, RF-11, P-15).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Las reglas de la confianza (RF-09, P-15), con los parámetros que copió la muestra: así, si el equipo cambia el umbral,
 * los granos dudosos de las muestras ya analizadas no cambian (RF-09 C5).
 *
 * <ul>
 *   <li>Un grano es <b>dudoso</b> si su confianza es menor que el umbral: con 60, un grano de 60,00 ya no lo es
 *       (P-15).</li>
 *   <li>Una muestra <b>pasa del máximo</b> si sus dudosos son más del {@code maximoDeDudosos} % de sus granos: con
 *       10 %, 10 de 100 no pasa y 11 sí (RF-09 C2).</li>
 *   <li>Sale con el <b>aviso de confianza baja</b> si pasa del máximo después de {@value #MAXIMO_DE_REPETICIONES}
 *       repeticiones (RF-09, restricción 3). Hasta que RF-09 llegue con el servicio de IA (PR 4), las muestras no
 *       tienen repeticiones y el aviso sale falso.</li>
 * </ul>
 *
 * @param umbral el umbral de confianza baja, en porcentaje
 * @param maximoDeDudosos el máximo de granos dudosos de una muestra, en porcentaje
 */
public record ReglasDeConfianza(BigDecimal umbral, BigDecimal maximoDeDudosos) {

    /** Repeticiones de foto que se piden como máximo por muestra (RF-09). */
    public static final int MAXIMO_DE_REPETICIONES = 2;

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    /**
     * Revisa los parámetros.
     *
     * @throws IllegalArgumentException si falta uno o no van de 0 a 100
     */
    public ReglasDeConfianza {
        Objects.requireNonNull(umbral, "umbral");
        Objects.requireNonNull(maximoDeDudosos, "maximoDeDudosos");
        if (umbral.signum() < 0 || umbral.compareTo(CIEN) > 0) {
            throw new IllegalArgumentException("El umbral de confianza baja debe ir de 0 a 100, y es " + umbral);
        }
        if (maximoDeDudosos.signum() < 0 || maximoDeDudosos.compareTo(CIEN) > 0) {
            throw new IllegalArgumentException("El máximo de dudosos debe ir de 0 a 100, y es " + maximoDeDudosos);
        }
    }

    /**
     * Si un grano es dudoso.
     *
     * @param confianza la confianza del grano, en porcentaje, como la guarda la base (2 decimales)
     * @return {@code true} si es menor que el umbral
     */
    public boolean esDudoso(BigDecimal confianza) {
        return confianza.compareTo(umbral) < 0;
    }

    /**
     * Si una muestra tiene más dudosos de los permitidos, sin redondear: {@code 100 · dudosos > máximo · total}.
     *
     * @param dudosos los granos dudosos
     * @param total el total de granos, mayor que 0
     * @return {@code true} si los dudosos pasan del máximo
     */
    public boolean pasaDelMaximo(int dudosos, int total) {
        if (total < 1) {
            throw new IllegalArgumentException("Una muestra sin granos no tiene porcentaje de dudosos");
        }
        return CIEN.multiply(BigDecimal.valueOf(dudosos))
                .compareTo(maximoDeDudosos.multiply(BigDecimal.valueOf(total))) > 0;
    }

    /**
     * Si la muestra sale con el aviso de confianza baja: sigue pasando del máximo después de las repeticiones
     * permitidas (RF-09).
     *
     * @param dudosos los granos dudosos
     * @param total el total de granos
     * @param repeticiones las repeticiones de foto que ya pidió el análisis
     * @return {@code true} si lleva el aviso
     */
    public boolean aviso(int dudosos, int total, int repeticiones) {
        return repeticiones >= MAXIMO_DE_REPETICIONES && pasaDelMaximo(dudosos, total);
    }
}
