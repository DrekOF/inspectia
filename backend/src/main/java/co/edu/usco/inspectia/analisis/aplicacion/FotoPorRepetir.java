/*
 * Nombre: FotoPorRepetir.java
 * Descripción: Una foto de la muestra en curso que hay que repetir: su número y su motivo (RF-06, RF-07, RF-09, D-44).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.MotivoDeFoto;

/**
 * Una foto por repetir de la muestra en curso (D-44): la página dice cuál y por qué cuando la muestra vuelve de la cola.
 *
 * @param numero su número en la muestra, de 1 a 3
 * @param motivo por qué hay que repetirla
 */
public record FotoPorRepetir(int numero, MotivoDeFoto motivo) {
}
