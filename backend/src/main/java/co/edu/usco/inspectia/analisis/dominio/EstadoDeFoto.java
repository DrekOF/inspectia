/*
 * Nombre: EstadoDeFoto.java
 * Descripción: Los estados de una foto de la muestra: cargada, aprobada o por repetir (RF-05, RF-06).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

/**
 * Estado de una foto (RF-05, RF-06). Toda foto queda {@link #CARGADA} al subirla; el servicio de IA la pasa a
 * {@link #APROBADA} o a {@link #POR_REPETIR}, con su motivo, cuando la revisa (RF-06).
 */
public enum EstadoDeFoto {

    /** Se subió y todavía no se revisa. */
    CARGADA,

    /** Tiene la nitidez y la luz suficientes (RF-06). */
    APROBADA,

    /** Hay que tomarla de nuevo, por el motivo que dice (RF-06, RF-07, RF-09). */
    POR_REPETIR
}
