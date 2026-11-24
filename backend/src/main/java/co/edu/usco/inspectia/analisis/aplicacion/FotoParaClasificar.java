/*
 * Nombre: FotoParaClasificar.java
 * Descripción: Una foto de la muestra como la recibe el clasificador de granos: su número, su formato y su contenido
 *              (RF-07, RF-08).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.FormatoDeFoto;

/**
 * Una foto de la muestra para clasificar sus granos (RF-07, RF-08). El servidor le manda al clasificador el contenido de
 * la foto, no su enlace, como dice specs/arquitectura.md.
 *
 * @param numero su número en la muestra, de 1 a 3
 * @param formato JPG o PNG
 * @param contenido los bytes de la foto, como se subió
 */
public record FotoParaClasificar(int numero, FormatoDeFoto formato, byte[] contenido) {
}
