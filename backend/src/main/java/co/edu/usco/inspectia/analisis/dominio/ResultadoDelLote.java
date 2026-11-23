/*
 * Nombre: ResultadoDelLote.java
 * Descripción: El resultado de un lote con sus 1 o 2 muestras terminadas: porcentaje corregido y su margen, contado,
 *              categoría, recomendación, sugerencia y aviso de confianza baja (RF-10, RF-12, D-25).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * El resultado de un lote (RF-10, RF-12), que se guarda con el lote: si después cambian los rangos o los parámetros,
 * no cambia (RF-13 C3). Con 2 muestras sale de la suma de sus granos, no sugiere otra muestra, y lleva el aviso de
 * confianza baja si alguna de las dos lo tiene (D-25, RF-09 C6).
 *
 * @param porcentajeSanos el porcentaje corregido (D-39), con un decimal
 * @param margen su margen, en puntos y con un decimal (P-19)
 * @param porcentajeContado el porcentaje que contó el modelo, con un decimal
 * @param totalGranos el total de granos de las muestras
 * @param granosSanos los granos que el modelo marcó como sanos
 * @param categoria el código de la categoría probable
 * @param recomendacion el texto de la recomendación de su rango
 * @param sugerirOtraMuestra si se sugiere analizar otra muestra (D-24)
 * @param avisoConfianzaBaja si alguna muestra salió con el aviso de confianza baja (RF-09)
 */
public record ResultadoDelLote(BigDecimal porcentajeSanos, BigDecimal margen, BigDecimal porcentajeContado,
        int totalGranos, int granosSanos, String categoria, String recomendacion, boolean sugerirOtraMuestra,
        boolean avisoConfianzaBaja) {

    /**
     * Calcula el resultado de un lote.
     *
     * @param muestras las muestras terminadas del lote: 1 o 2 (D-25)
     * @param rangos los rangos vigentes de la empresa (RF-13 C2)
     * @param parametros los parámetros del análisis
     * @return el resultado
     * @throws IllegalArgumentException si las muestras no son 1 o 2, o si ningún rango cubre el porcentaje
     */
    public static ResultadoDelLote calcular(List<ConteoDeMuestra> muestras, List<RangoDeCategoria> rangos,
            ParametrosDelAnalisis parametros) {
        Objects.requireNonNull(muestras, "muestras");
        Objects.requireNonNull(parametros, "parametros");
        if (muestras.isEmpty() || muestras.size() > ReglasDeRecomendacion.MAXIMO_DE_MUESTRAS) {
            throw new IllegalArgumentException("Un lote tiene 1 o 2 muestras terminadas, no " + muestras.size()
                    + " (D-25)");
        }
        PorcentajeDelLote porcentaje = PorcentajeDelLote.de(muestras, parametros.factorDelMargen());
        Recomendacion recomendacion = ReglasDeRecomendacion.recomendar(porcentaje.getCorregido(), rangos,
                muestras.size(), parametros.puntosParaOtraMuestra());
        boolean aviso = muestras.stream().anyMatch(ConteoDeMuestra::avisoConfianzaBaja);
        return new ResultadoDelLote(porcentaje.getCorregido(), porcentaje.getMargen(), porcentaje.getContado(),
                porcentaje.getTotalGranos(), porcentaje.getGranosSanos(), recomendacion.categoria(),
                recomendacion.texto(), recomendacion.sugerirOtraMuestra(), aviso);
    }
}
