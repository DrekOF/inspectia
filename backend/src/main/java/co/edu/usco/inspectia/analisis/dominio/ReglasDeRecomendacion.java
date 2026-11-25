/*
 * Nombre: ReglasDeRecomendacion.java
 * Descripción: Las reglas de la recomendación del lote: la categoría probable según los rangos de la empresa, cuándo
 *              se sugiere analizar otra muestra y por qué límite, el más cercano y en empate el más alto (RF-12, D-24,
 *              D-25, D-47).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.dominio;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Las reglas de la recomendación (RF-12), sin Spring.
 *
 * <ul>
 *   <li><b>Categoría</b>: la del rango de límite inferior más alto que no pasa del porcentaje corregido, ya redondeado
 *       a un decimal (restricción 2). Con los rangos por defecto, 90,0 es Estándar y 89,9, Corriente (C4).</li>
 *   <li><b>Sugerir otra muestra</b> (D-24): solo con 1 muestra (D-25), si el porcentaje queda a menos de
 *       {@code puntos} de algún límite de la empresa. Cuentan los límites de Premium, Estándar y Corriente; el 0 de
 *       Fuera de norma no (restricción 6). «A menos de» es estricto: con 1 punto, 90,9 sugiere y 91,0 no (C3).</li>
 *   <li><b>El límite de la sugerencia</b> (D-47): de los límites que cuentan, el más cercano al porcentaje y, si dos
 *       quedan a la misma distancia, el más alto, sin importar el orden de la lista. Su categoría puede ser otra que la
 *       del lote: con Premium desde 97,5, 97,0 % es Estándar y sugiere por el límite de Premium.</li>
 * </ul>
 */
public final class ReglasDeRecomendacion {

    /** Muestras que puede tener un lote como máximo (D-25). */
    public static final int MAXIMO_DE_MUESTRAS = 2;

    private ReglasDeRecomendacion() {
    }

    /**
     * La recomendación de un lote.
     *
     * @param porcentaje el porcentaje corregido, ya redondeado a un decimal (RF-10)
     * @param rangos los rangos vigentes de la empresa (RF-13)
     * @param muestras cuántas muestras terminadas tiene el lote: 1 o 2
     * @param puntos a cuántos puntos de un límite se sugiere otra muestra (D-24)
     * @return la categoría, su texto, si se sugiere otra muestra y, si se sugiere, el límite y su categoría
     * @throws IllegalArgumentException si las muestras no son 1 o 2, o si ningún rango cubre el porcentaje
     */
    public static Recomendacion recomendar(BigDecimal porcentaje, List<RangoDeCategoria> rangos, int muestras,
            BigDecimal puntos) {
        Objects.requireNonNull(porcentaje, "porcentaje");
        Objects.requireNonNull(rangos, "rangos");
        Objects.requireNonNull(puntos, "puntos");
        if (muestras < 1 || muestras > MAXIMO_DE_MUESTRAS) {
            throw new IllegalArgumentException("Un lote tiene 1 o 2 muestras, no " + muestras + " (D-25)");
        }
        RangoDeCategoria rango = rangos.stream()
                .filter(candidato -> candidato.limiteInferior().compareTo(porcentaje) <= 0)
                .max(Comparator.comparing(RangoDeCategoria::limiteInferior))
                .orElseThrow(() -> new IllegalArgumentException("Ningún rango de la empresa cubre " + porcentaje));
        Recomendacion sinSugerencia = new Recomendacion(rango.categoria(), rango.recomendacion(), false, null, null);
        if (muestras == MAXIMO_DE_MUESTRAS) {
            return sinSugerencia;
        }
        Comparator<RangoDeCategoria> masCercanoYMasAlto = Comparator
                .comparing((RangoDeCategoria candidato) -> distancia(porcentaje, candidato))
                .thenComparing(RangoDeCategoria::limiteInferior, Comparator.reverseOrder());
        return rangos.stream()
                .filter(candidato -> candidato.limiteInferior().signum() > 0)
                .filter(candidato -> distancia(porcentaje, candidato).compareTo(puntos) < 0)
                .min(masCercanoYMasAlto)
                .map(limite -> new Recomendacion(rango.categoria(), rango.recomendacion(), true,
                        limite.limiteInferior(), limite.categoria()))
                .orElse(sinSugerencia);
    }

    /** A cuántos puntos queda el porcentaje del límite inferior del rango, hacia arriba o hacia abajo. */
    private static BigDecimal distancia(BigDecimal porcentaje, RangoDeCategoria rango) {
        return porcentaje.subtract(rango.limiteInferior()).abs();
    }
}
