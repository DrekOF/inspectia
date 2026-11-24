/*
 * Nombre: GranoDeFoto.java
 * Descripción: Un grano de una foto, como se guarda en la base: su foto, su posición, su clase y su confianza (RF-07,
 *              RF-08, RF-11).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.ClaseDeGrano;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Un grano de una foto, como está en la tabla {@code grano} (RF-07, RF-08). La confianza no sale de la API: solo si el
 * grano es dudoso (RF-11 C4, D-42).
 *
 * @param fotoId su foto
 * @param x la posición horizontal de su esquina superior izquierda, en píxeles de la foto original
 * @param y la posición vertical de su esquina superior izquierda
 * @param ancho su ancho
 * @param alto su alto
 * @param clase sano o no sano
 * @param confianza la confianza de su clase, en porcentaje y con 2 decimales
 */
public record GranoDeFoto(UUID fotoId, int x, int y, int ancho, int alto, ClaseDeGrano clase, BigDecimal confianza) {
}
