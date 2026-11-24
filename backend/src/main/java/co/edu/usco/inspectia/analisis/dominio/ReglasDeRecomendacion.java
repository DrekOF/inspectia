/*
 * Nombre: ReglasDeRecomendacion.java
 * Descripción: Las reglas de la recomendación del lote: la categoría probable según los rangos de la empresa y cuándo
 *              se sugiere analizar otra muestra (RF-12, D-24, D-25).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
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
     * @return la categoría, su texto y si se sugiere otra muestra
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
        boolean sugerir = muestras == 1 && rangos.stream()
                .map(RangoDeCategoria::limiteInferior)
                .filter(limite -> limite.signum() > 0)
                .anyMatch(limite -> porcentaje.subtract(limite).abs().compareTo(puntos) < 0);
        return new Recomendacion(rango.categoria(), rango.recomendacion(), sugerir);
    }
}
