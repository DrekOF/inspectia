/*
 * Nombre: MotivoDeDevolucion.java
 * Descripción: Por qué la cola devolvió una muestra a pendiente sin resultado: menos de 100 granos o sin respuesta del
 *              servicio de IA (RF-07 C3, RF-08 C6, D-44).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

/**
 * El motivo con que la cola devuelve una muestra a pendiente (D-44). Son los códigos de
 * {@code specs/entidades/muestra.json}. Si la devuelve una foto por repetir, el motivo va en la foto y la muestra no
 * lleva ninguno; otra falla, como no poder leer una foto, tampoco.
 */
public enum MotivoDeDevolucion {

    /** Entre todas las fotos hay menos de 100 granos válidos (RF-07 C3, D-23). */
    POCOS_GRANOS,

    /** El servicio de IA no respondió a tiempo, no se pudo conectar o falló (RF-08 C6). */
    SIN_RESPUESTA_DE_LA_IA
}
