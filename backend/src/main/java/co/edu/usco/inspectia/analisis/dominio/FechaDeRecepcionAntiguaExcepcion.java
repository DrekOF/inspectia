/*
 * Nombre: FechaDeRecepcionAntiguaExcepcion.java
 * Descripción: La fecha de recepción de un lote es de hace más de un año (RF-04, restricción 4).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

import java.time.LocalDate;

/** Se lanza al registrar un lote cuya fecha de recepción es de hace más de un año (RF-04, restricción 4). */
public class FechaDeRecepcionAntiguaExcepcion extends RuntimeException {

    /**
     * Crea la excepción.
     *
     * @param fechaRecepcion la fecha que llegó
     * @param primeraFecha la fecha de recepción más antigua que se acepta: hace un año en Colombia
     */
    public FechaDeRecepcionAntiguaExcepcion(LocalDate fechaRecepcion, LocalDate primeraFecha) {
        super("La fecha de recepción " + fechaRecepcion + " es anterior a " + primeraFecha + ", hace un año");
    }
}
