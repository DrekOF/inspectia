/*
 * Nombre: ClaseDeGrano.java
 * Descripción: Las dos clases en que el modelo de IA clasifica cada grano: sano o no sano (RF-08, RF-11).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

/**
 * La clase de un grano (RF-08). Los valores son los de la columna {@code grano.clase}
 * ({@code specs/entidades/grano.json}). La clase «no sano» es la que Julián llama «mala» (D-39).
 */
public enum ClaseDeGrano {

    /** Un grano sano: en la foto marcada va verde y con chulo (RF-11). */
    SANO,

    /** Un grano no sano: en la foto marcada va rojo y con equis (RF-11). */
    NO_SANO
}
