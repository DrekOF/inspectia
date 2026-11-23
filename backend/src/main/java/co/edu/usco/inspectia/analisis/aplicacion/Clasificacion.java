/*
 * Nombre: Clasificacion.java
 * Descripción: Lo que responde el clasificador de granos para una muestra: el modelo que la clasificó y sus granos
 *              (RF-07, RF-08).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.util.List;

/**
 * La clasificación de los granos de una muestra (RF-07, RF-08).
 *
 * @param modelo el modelo que los clasificó
 * @param granos los granos de todas las fotos, en el orden en que se guardan
 */
public record Clasificacion(ModeloUsado modelo, List<GranoClasificado> granos) {
}
