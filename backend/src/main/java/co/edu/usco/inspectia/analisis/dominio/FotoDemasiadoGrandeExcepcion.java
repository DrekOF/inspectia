/*
 * Nombre: FotoDemasiadoGrandeExcepcion.java
 * Descripción: Una foto pesa más de 10 MB (RF-05 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

/** Se lanza cuando una foto pesa más de 10 MB (RF-05 C3). */
public class FotoDemasiadoGrandeExcepcion extends RuntimeException {

    /**
     * Crea la excepción.
     *
     * @param bytes cuánto pesa la foto
     */
    public FotoDemasiadoGrandeExcepcion(long bytes) {
        super("La foto pesa " + bytes + " bytes, más de " + ReglasDeFotos.MAXIMO_DE_BYTES);
    }
}
