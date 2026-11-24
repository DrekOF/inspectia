/*
 * Nombre: GranoClasificado.java
 * Descripción: Un grano que encontró y clasificó el clasificador en una foto: su posición, su clase y su confianza
 *              (RF-07, RF-08).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.ClaseDeGrano;
import java.math.BigDecimal;

/**
 * Un grano clasificado (RF-07, RF-08). La posición es un rectángulo sin rotación, en píxeles de la foto original.
 *
 * @param numeroDeFoto el número de la foto donde está, de 1 a 3
 * @param x la posición horizontal de su esquina superior izquierda, desde 0
 * @param y la posición vertical de su esquina superior izquierda, desde 0
 * @param ancho su ancho, desde 1
 * @param alto su alto, desde 1
 * @param clase sano o no sano
 * @param confianza la confianza de su clase, en porcentaje de 0 a 100
 */
public record GranoClasificado(int numeroDeFoto, int x, int y, int ancho, int alto, ClaseDeGrano clase,
        BigDecimal confianza) {
}
