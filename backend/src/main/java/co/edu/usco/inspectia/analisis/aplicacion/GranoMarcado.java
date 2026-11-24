/*
 * Nombre: GranoMarcado.java
 * Descripción: Un grano como se marca en la foto del resultado: su posición, su clase y si es dudoso, sin su confianza
 *              (RF-11 C4, D-42).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.ClaseDeGrano;

/**
 * Un grano marcado en la foto (RF-11). Lleva si es dudoso, calculado con el umbral que copió su muestra, y no su
 * confianza: la página nunca la recibe (RF-11 C4, D-42).
 *
 * @param x la posición horizontal de su esquina superior izquierda, en píxeles de la foto original
 * @param y la posición vertical de su esquina superior izquierda
 * @param ancho su ancho
 * @param alto su alto
 * @param clase sano o no sano
 * @param dudoso si su confianza es menor que el umbral de su muestra (P-15)
 */
public record GranoMarcado(int x, int y, int ancho, int alto, ClaseDeGrano clase, boolean dudoso) {
}
