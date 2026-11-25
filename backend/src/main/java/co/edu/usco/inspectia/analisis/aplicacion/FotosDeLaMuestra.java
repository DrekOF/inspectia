/*
 * Nombre: FotosDeLaMuestra.java
 * Descripción: Las fotos de la muestra en curso de un lote, para retomarlo, también las de la muestra 2 (RF-05,
 *              RF-14 C5 y C6, D-25).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeMuestra;
import java.util.List;

/**
 * Las fotos de la muestra en curso de un lote (RF-05, RF-14 C5).
 *
 * @param muestra el número de la muestra, o {@code null} si todavía no tiene fotos, también la muestra 2 de un lote
 *     que sugiere otra muestra (D-25)
 * @param estado el estado de la muestra, o {@code null} si todavía no tiene fotos
 * @param fotos las fotos, por número
 */
public record FotosDeLaMuestra(Integer muestra, EstadoDeMuestra estado, List<VistaDeFoto> fotos) {
}
