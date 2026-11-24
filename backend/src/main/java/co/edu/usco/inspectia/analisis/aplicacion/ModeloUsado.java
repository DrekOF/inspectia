/*
 * Nombre: ModeloUsado.java
 * Descripción: El modelo de IA que clasificó los granos de una muestra: su nombre y su versión (RF-08 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

/**
 * El modelo que clasificó los granos (RF-08 C3). La muestra guarda este modelo, el que respondió el clasificador, y no
 * el que está «en uso»: así se corrige con sus recall (RF-10 C13).
 *
 * @param nombre el nombre, como {@code EfficientNet-B0}
 * @param version la versión, como {@code 1.0}
 */
public record ModeloUsado(String nombre, String version) {
}
