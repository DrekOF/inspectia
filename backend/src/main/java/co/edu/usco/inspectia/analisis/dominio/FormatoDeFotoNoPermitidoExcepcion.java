/*
 * Nombre: FormatoDeFotoNoPermitidoExcepcion.java
 * Descripción: Una foto no es JPG ni PNG, por el tipo que declara o por su contenido (RF-05 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

/** Se lanza cuando una foto no es JPG ni PNG, por el tipo que declara o por su contenido (RF-05 C3). */
public class FormatoDeFotoNoPermitidoExcepcion extends RuntimeException {

    /** Crea la excepción. */
    public FormatoDeFotoNoPermitidoExcepcion() {
        super("La foto no es JPG ni PNG");
    }
}
