/*
 * Nombre: MotivoDeFoto.java
 * Descripción: Por qué hay que repetir una foto de la muestra: borrosa, con poca luz, con granos pegados o con muchos
 *              granos dudosos (RF-06, RF-07, RF-09, D-46).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

/**
 * El motivo de una foto {@link EstadoDeFoto#POR_REPETIR} (RF-06, RF-07, RF-09). Son los códigos de
 * {@code specs/entidades/foto.json}; la página los traduce con las etiquetas generadas ({@code foto.motivo.*}).
 */
public enum MotivoDeFoto {

    /** La foto está borrosa: hay que tomarla de nuevo sin mover el celular (RF-06 C1). */
    BORROSA,

    /** La foto está oscura: hay que tomarla de nuevo con más luz (RF-06 C2). */
    POCA_LUZ,

    /** La foto tiene granos pegados, marcados en sus regiones (RF-07 C2, D-45). */
    GRANOS_PEGADOS,

    /** La muestra pasó del máximo de dudosos y esta foto es la que más tiene: la pone el servidor (RF-09 C1). */
    GRANOS_DUDOSOS
}
