/*
 * Nombre: FechaDeRecepcionFuturaExcepcion.java
 * Descripción: La fecha de recepción de un lote es posterior a hoy (RF-04 C4).
 * Fecha de creación: 10/11/2026
 * Actualización: 10/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

import java.time.LocalDate;

/** Se lanza al registrar un lote cuya fecha de recepción es posterior a hoy (RF-04 C4). */
public class FechaDeRecepcionFuturaExcepcion extends RuntimeException {

    /**
     * Crea la excepción.
     *
     * @param fechaRecepcion la fecha que llegó
     * @param hoy el día de hoy en Colombia
     */
    public FechaDeRecepcionFuturaExcepcion(LocalDate fechaRecepcion, LocalDate hoy) {
        super("La fecha de recepción " + fechaRecepcion + " es posterior a hoy, " + hoy);
    }
}
