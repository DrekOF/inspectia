/*
 * Nombre: DetalleDeMuestra.java
 * Descripción: Una muestra en el resultado de un lote: sus datos, su porcentaje contado y sus fotos marcadas (RF-11,
 *              RF-14 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.math.BigDecimal;
import java.util.List;

/**
 * Una muestra del resultado (RF-11, RF-14 C3).
 *
 * @param muestra sus datos guardados
 * @param porcentajeContado el porcentaje que contó el modelo, o {@code null} mientras no termina
 * @param fotos sus fotos, por número, con sus granos marcados
 */
public record DetalleDeMuestra(MuestraGuardada muestra, BigDecimal porcentajeContado, List<FotoMarcada> fotos) {
}
