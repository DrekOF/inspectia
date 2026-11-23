/*
 * Nombre: EstadoDeMuestra.java
 * Descripción: Los estados de una muestra de un lote: pendiente mientras se toman las fotos, en espera, en proceso y
 *              terminada (specs/modelo-datos.md, tabla muestra).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

/**
 * Estado de una muestra (RF-05, RNF-02). Pasa de {@link #PENDIENTE}, mientras se toman las fotos, a
 * {@link #EN_ESPERA} cuando el usuario las envía, a {@link #EN_PROCESO} cuando el servidor la atiende y a
 * {@link #TERMINADA} con el resultado. Solo una muestra pendiente recibe fotos.
 */
public enum EstadoDeMuestra {

    /** Se están tomando sus fotos. */
    PENDIENTE,

    /** Sus fotos se enviaron y espera su turno (RNF-02). */
    EN_ESPERA,

    /** El servidor la está analizando. */
    EN_PROCESO,

    /** Ya tiene su resultado. */
    TERMINADA
}
