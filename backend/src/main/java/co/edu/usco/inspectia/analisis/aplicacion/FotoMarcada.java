/*
 * Nombre: FotoMarcada.java
 * Descripción: Una foto de una muestra en el resultado, con su enlace para verla y sus granos marcados (RF-11, D-41).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.util.List;

/**
 * Una foto del resultado (RF-11), con un enlace nuevo para verla que vence en 15 minutos (D-41) y sus granos, en el
 * orden en que se guardaron. Mientras la muestra no termina, no tiene granos.
 *
 * @param numero su número en la muestra, de 1 a 3
 * @param vista el enlace para verla
 * @param granos sus granos marcados
 */
public record FotoMarcada(int numero, EnlaceTemporal vista, List<GranoMarcado> granos) {
}
