/*
 * Nombre: ConteoDeMuestra.java
 * Descripción: Lo que contó el modelo en una muestra terminada: sus granos sanos, su total, los recall del modelo que
 *              la analizó y su aviso de confianza baja (RF-10, D-25, D-39).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

import java.util.Objects;

/**
 * El conteo de una muestra terminada (RF-10): cuántos granos marcó el modelo como sanos de cuántos, con los recall del
 * modelo que la analizó (D-39) y si salió con el aviso de confianza baja (RF-09). Con 2 muestras, el lote suma sus
 * conteos (D-25).
 *
 * <p>Que la muestra tenga al menos {@value #MINIMO_DE_GRANOS} granos lo revisa el caso de uso antes de guardarla
 * (RF-07 C3, D-26): aquí solo se exige que el total no sea 0, para que el porcentaje exista.
 *
 * @param granosSanos los granos que el modelo marcó como sanos
 * @param totalGranos el total de granos de la muestra
 * @param recalls los recall del modelo que la analizó
 * @param avisoConfianzaBaja si la muestra salió con el aviso de confianza baja (RF-09)
 */
public record ConteoDeMuestra(int granosSanos, int totalGranos, RecallsDelModelo recalls, boolean avisoConfianzaBaja) {

    /** Granos que necesita una muestra como mínimo (D-23, D-26). */
    public static final int MINIMO_DE_GRANOS = 100;

    /**
     * Revisa el conteo.
     *
     * @throws IllegalArgumentException si el total no es positivo o los sanos no van de 0 al total
     */
    public ConteoDeMuestra {
        Objects.requireNonNull(recalls, "recalls");
        if (totalGranos < 1) {
            throw new IllegalArgumentException("Una muestra tiene al menos un grano, y esta tiene " + totalGranos);
        }
        if (granosSanos < 0 || granosSanos > totalGranos) {
            throw new IllegalArgumentException("Los granos sanos (" + granosSanos + ") deben ir de 0 al total ("
                    + totalGranos + ")");
        }
    }

    /**
     * Un conteo sin el aviso de confianza baja.
     *
     * @param granosSanos los granos que el modelo marcó como sanos
     * @param totalGranos el total de granos de la muestra
     * @param recalls los recall del modelo que la analizó
     */
    public ConteoDeMuestra(int granosSanos, int totalGranos, RecallsDelModelo recalls) {
        this(granosSanos, totalGranos, recalls, false);
    }

    /**
     * Si una muestra tiene los granos suficientes para dar un resultado (RF-07 C3, D-26).
     *
     * @param totalGranos el total de granos de la muestra
     * @return {@code true} si tiene {@value #MINIMO_DE_GRANOS} o más
     */
    public static boolean alcanzaElMinimo(int totalGranos) {
        return totalGranos >= MINIMO_DE_GRANOS;
    }
}
