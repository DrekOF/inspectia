/*
 * Nombre: Rectangulo.java
 * Descripción: Un rectángulo sin rotación en una foto, en píxeles de la foto ya girada según su EXIF: una región de
 *              granos pegados o un grano dudoso (RF-07 C2, RF-09 C1, D-45).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

/**
 * Un rectángulo de una foto (D-45): una región de granos pegados (RF-07 C2) o un grano dudoso de una foto por repetir
 * (RF-09 C1). Va en píxeles de la foto ya girada según su orientación EXIF, que es como la muestra el navegador.
 *
 * @param x la posición horizontal de su esquina superior izquierda, desde 0
 * @param y la posición vertical de su esquina superior izquierda, desde 0
 * @param ancho su ancho, desde 1
 * @param alto su alto, desde 1
 */
public record Rectangulo(int x, int y, int ancho, int alto) {

    /**
     * Revisa la posición y el tamaño, como los CHECK de {@code region_pegada} y {@code grano}.
     *
     * @throws IllegalArgumentException si la posición es negativa o el tamaño es menor que 1
     */
    public Rectangulo {
        if (x < 0 || y < 0 || ancho < 1 || alto < 1) {
            throw new IllegalArgumentException("Un rectángulo necesita x e y desde 0 y ancho y alto desde 1, y es ("
                    + x + ", " + y + ", " + ancho + ", " + alto + ")");
        }
    }
}
