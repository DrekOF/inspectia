/*
 * Nombre: AvanceDelAnalisis.java
 * Descripción: El avance del análisis de la muestra en curso de un lote: su número, su estado, sus fechas y si el último
 *              envío falló (RNF-01 C4, RNF-02 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeMuestra;
import java.time.Instant;

/**
 * El avance del análisis de la muestra en curso de un lote (RNF-01 C4, RNF-02 C3): la página lo pregunta mientras
 * espera el resultado. No dice la posición en la cola, que es de todas las empresas (RNF-03).
 *
 * @param muestra el número de la muestra en curso, o {@code null} si el lote no tiene muestra
 * @param estado su estado, o {@code null} si el lote no tiene muestra
 * @param fechaEnvio cuándo se envió a analizar, o {@code null}
 * @param fechaInicio cuándo empezó el análisis, o {@code null}
 * @param fechaFin cuándo terminó, o {@code null}
 * @param fallo si el último envío volvió sin resultado: la muestra está pendiente y tiene fecha de envío
 */
public record AvanceDelAnalisis(Integer muestra, EstadoDeMuestra estado, Instant fechaEnvio, Instant fechaInicio,
        Instant fechaFin, boolean fallo) {

    /**
     * El avance de un lote que todavía no tiene muestra.
     *
     * @return el avance vacío
     */
    public static AvanceDelAnalisis sinMuestra() {
        return new AvanceDelAnalisis(null, null, null, null, null, false);
    }

    /**
     * El avance de una muestra; si está pendiente con fecha de envío, el último envío falló.
     *
     * @param numero su número
     * @param estado su estado
     * @param fechaEnvio cuándo se envió, o {@code null}
     * @param fechaInicio cuándo empezó el análisis, o {@code null}
     * @param fechaFin cuándo terminó, o {@code null}
     * @return el avance
     */
    public static AvanceDelAnalisis de(int numero, EstadoDeMuestra estado, Instant fechaEnvio, Instant fechaInicio,
            Instant fechaFin) {
        boolean fallo = estado == EstadoDeMuestra.PENDIENTE && fechaEnvio != null;
        return new AvanceDelAnalisis(numero, estado, fechaEnvio, fechaInicio, fechaFin, fallo);
    }
}
