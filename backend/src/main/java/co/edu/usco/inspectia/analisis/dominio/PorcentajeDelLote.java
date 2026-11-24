/*
 * Nombre: PorcentajeDelLote.java
 * Descripción: El porcentaje de granos sanos del lote: el contado, el corregido con los recall de cada muestra y su
 *              margen, con aritmética exacta y un solo redondeo al final (RF-10, D-39, P-19).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * El porcentaje de granos sanos de un lote, con sus 1 o 2 muestras terminadas (RF-10, D-25). Todo con
 * {@link BigDecimal}, sin {@code double}, y redondeado una sola vez, al final, a un decimal y hacia arriba en el empate
 * ({@link RoundingMode#HALF_UP}): 377 de 400 da 94,3 %.
 *
 * <ul>
 *   <li><b>Contado</b>: {@code 100 · S / N}, con S los granos sanos y N el total de las muestras: se suman los granos,
 *       no se promedian los porcentajes (D-25).</li>
 *   <li><b>Corregido</b> (D-39), con los recall en porcentaje de cada muestra:
 *       {@code num = 100·S − Σ nᵢ·(100 − recallNoSanoᵢ)} y {@code den = Σ nᵢ·(recallSanoᵢ + recallNoSanoᵢ − 100)}. Si
 *       {@code num ≤ 0} es 0,0; si {@code num ≥ den}, 100,0; si no, {@code 100 · num / den}. Con un solo modelo es la
 *       fórmula de D-39, {@code (p_obs − (1 − rm)) / (rs − (1 − rm))}, limitada a [0, 1].</li>
 *   <li><b>Margen</b> (P-19, provisional): {@code 100 · k · sqrt(N·p·(1 − p)) / Σ nᵢ·Jᵢ}, con {@code p = S/N} y
 *       {@code Jᵢ = (recallSanoᵢ + recallNoSanoᵢ − 100) / 100}. La raíz se calcula con
 *       {@link MathContext#DECIMAL64}. Un margen de más de 100 puntos, que solo da un modelo casi al azar, se deja en
 *       100: el rango probable ya cubre de 0 a 100 % (D-43) y la columna {@code lote.margen} va de 0 a 100.</li>
 * </ul>
 */
public final class PorcentajeDelLote {

    /** Decimales de los porcentajes y del margen (RF-10). */
    public static final int DECIMALES = 1;

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal DIEZ_MIL = BigDecimal.valueOf(10_000);
    private static final BigDecimal CERO_CON_DECIMAL = new BigDecimal("0.0");
    private static final BigDecimal CIEN_CON_DECIMAL = new BigDecimal("100.0");

    private final int granosSanos;
    private final int totalGranos;
    private final BigDecimal contado;
    private final BigDecimal corregido;
    private final BigDecimal margen;

    private PorcentajeDelLote(int granosSanos, int totalGranos, BigDecimal contado, BigDecimal corregido,
            BigDecimal margen) {
        this.granosSanos = granosSanos;
        this.totalGranos = totalGranos;
        this.contado = contado;
        this.corregido = corregido;
        this.margen = margen;
    }

    /**
     * Calcula los porcentajes y el margen de un lote.
     *
     * @param muestras las muestras terminadas del lote: 1 o 2 (D-25)
     * @param factorDelMargen el factor {@code k} del margen (P-19), mayor que 0
     * @return el porcentaje del lote
     * @throws IllegalArgumentException si no hay muestras o el factor no es mayor que 0
     */
    public static PorcentajeDelLote de(List<ConteoDeMuestra> muestras, BigDecimal factorDelMargen) {
        Objects.requireNonNull(muestras, "muestras");
        Objects.requireNonNull(factorDelMargen, "factorDelMargen");
        if (muestras.isEmpty()) {
            throw new IllegalArgumentException("El porcentaje del lote necesita al menos una muestra terminada");
        }
        if (factorDelMargen.signum() <= 0) {
            throw new IllegalArgumentException("El factor del margen debe ser mayor que 0, y es " + factorDelMargen);
        }
        int sanos = 0;
        int total = 0;
        BigDecimal numerador = BigDecimal.ZERO;
        BigDecimal denominador = BigDecimal.ZERO;
        for (ConteoDeMuestra muestra : muestras) {
            BigDecimal granos = BigDecimal.valueOf(muestra.totalGranos());
            sanos += muestra.granosSanos();
            total += muestra.totalGranos();
            numerador = numerador.subtract(granos.multiply(muestra.recalls().noSanosMarcadosComoSanos()));
            denominador = denominador.add(granos.multiply(muestra.recalls().separacion()));
        }
        numerador = numerador.add(CIEN.multiply(BigDecimal.valueOf(sanos)));
        return new PorcentajeDelLote(sanos, total, contado(sanos, total), corregido(numerador, denominador),
                margen(sanos, total, denominador, factorDelMargen));
    }

    /**
     * El porcentaje contado, {@code 100 · sanos / total}, con un decimal y hacia arriba en el empate.
     *
     * @param granosSanos los granos sanos
     * @param totalGranos el total de granos, mayor que 0
     * @return el porcentaje
     * @throws IllegalArgumentException si el total no es positivo o los sanos no van de 0 al total
     */
    public static BigDecimal contado(int granosSanos, int totalGranos) {
        if (totalGranos < 1 || granosSanos < 0 || granosSanos > totalGranos) {
            throw new IllegalArgumentException("No hay porcentaje de " + granosSanos + " sanos de " + totalGranos);
        }
        return CIEN.multiply(BigDecimal.valueOf(granosSanos))
                .divide(BigDecimal.valueOf(totalGranos), DECIMALES, RoundingMode.HALF_UP);
    }

    /** El corregido, limitado a [0, 100], con un solo redondeo. */
    private static BigDecimal corregido(BigDecimal numerador, BigDecimal denominador) {
        if (numerador.signum() <= 0) {
            return CERO_CON_DECIMAL;
        }
        if (numerador.compareTo(denominador) >= 0) {
            return CIEN_CON_DECIMAL;
        }
        return CIEN.multiply(numerador).divide(denominador, DECIMALES, RoundingMode.HALF_UP);
    }

    /**
     * El margen: {@code 100 · k · sqrt(S·(N − S)/N) / (den / 100)}, que es la fórmula de P-19 con
     * {@code N·p·(1 − p) = S·(N − S)/N} y {@code Σ nᵢ·Jᵢ = den / 100}.
     */
    private static BigDecimal margen(int sanos, int total, BigDecimal denominador, BigDecimal factor) {
        BigDecimal varianza = BigDecimal.valueOf((long) sanos * (total - sanos))
                .divide(BigDecimal.valueOf(total), MathContext.DECIMAL64);
        BigDecimal margen = DIEZ_MIL.multiply(factor).multiply(varianza.sqrt(MathContext.DECIMAL64))
                .divide(denominador, DECIMALES, RoundingMode.HALF_UP);
        return margen.min(CIEN_CON_DECIMAL);
    }

    /** @return los granos sanos de las muestras, sumados */
    public int getGranosSanos() {
        return granosSanos;
    }

    /** @return el total de granos de las muestras, sumado */
    public int getTotalGranos() {
        return totalGranos;
    }

    /** @return el porcentaje contado, con un decimal */
    public BigDecimal getContado() {
        return contado;
    }

    /** @return el porcentaje corregido (D-39), de 0,0 a 100,0, con un decimal */
    public BigDecimal getCorregido() {
        return corregido;
    }

    /** @return el margen del corregido, en puntos y con un decimal (P-19) */
    public BigDecimal getMargen() {
        return margen;
    }
}
