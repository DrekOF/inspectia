/*
 * Nombre: OrigenUsado.java
 * Descripción: Una forma en que la empresa escribió una finca o un proveedor, con cuántas veces la usó y la
 *              última vez que la usó (RF-04).
 * Fecha de creación: 10/11/2026
 * Actualización: 10/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.time.OffsetDateTime;

/**
 * Una forma en que la empresa escribió una finca o un proveedor, con cuántos lotes la usaron y la última vez
 * que se usó. Con ellas se elige qué forma sugerir cuando la misma finca se escribió de varias: la más usada
 * y, si empatan, la más reciente (RF-04, restricción 3).
 *
 * @param origen la finca o el proveedor, como se escribió
 * @param veces cuántos lotes de la empresa la usan
 * @param ultimaVez cuándo se registró el último lote con esa forma
 */
public record OrigenUsado(String origen, long veces, OffsetDateTime ultimaVez) {

    /**
     * Si esta forma se sugiere antes que otra de la misma finca: la más usada y, si empatan, la más reciente.
     *
     * @param otra la otra forma
     * @return {@code true} si se prefiere esta
     */
    public boolean preferidaSobre(OrigenUsado otra) {
        if (veces != otra.veces) {
            return veces > otra.veces;
        }
        return ultimaVez.isAfter(otra.ultimaVez);
    }
}
