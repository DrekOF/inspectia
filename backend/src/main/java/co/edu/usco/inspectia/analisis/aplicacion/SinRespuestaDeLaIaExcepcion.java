/*
 * Nombre: SinRespuestaDeLaIaExcepcion.java
 * Descripción: El servicio de IA no respondió a tiempo, no se pudo conectar o respondió con un error del servidor
 *              (RF-08 C6, D-44, D-46).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

/**
 * El servicio de IA no respondió (RF-08 C6): se acabó el tiempo, no se pudo conectar o respondió 5xx. En la cola, la
 * muestra vuelve a pendiente con el motivo {@code SIN_RESPUESTA_DE_LA_IA} (D-44); al confirmar una foto, la foto queda
 * «Cargada» y la revisa la cola (D-46). Un 400 del servicio u otra falla no es esta excepción.
 */
public class SinRespuestaDeLaIaExcepcion extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción.
     *
     * @param mensaje qué pasó, para el registro del servidor
     * @param causa el error del cliente HTTP
     */
    public SinRespuestaDeLaIaExcepcion(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
