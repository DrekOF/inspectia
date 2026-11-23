/*
 * Nombre: FotosDeLaMuestra.java
 * Descripción: Las fotos de la muestra en curso de un lote, para retomarlo (RF-05, RF-14 C5).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeMuestra;
import java.util.List;

/**
 * Las fotos de la muestra en curso de un lote (RF-05, RF-14 C5).
 *
 * @param muestra el número de la muestra, o {@code null} si el lote todavía no tiene fotos
 * @param estado el estado de la muestra, o {@code null} si todavía no tiene fotos
 * @param fotos las fotos, por número
 */
public record FotosDeLaMuestra(Integer muestra, EstadoDeMuestra estado, List<VistaDeFoto> fotos) {
}
