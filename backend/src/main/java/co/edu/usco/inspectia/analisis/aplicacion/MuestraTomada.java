/*
 * Nombre: MuestraTomada.java
 * Descripción: Una muestra que la cola de análisis tomó para atenderla: quedó «en proceso» con su hora de inicio
 *              (RNF-02 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.time.Instant;
import java.util.UUID;

/**
 * Una muestra que se tomó de la cola (RNF-02 C3). La hora de inicio es la ficha de este turno: al terminar o al
 * devolverla, solo cambia si sigue «en proceso» con esa misma hora, así dos ejecuciones nunca guardan dos veces.
 *
 * @param id la muestra
 * @param empresaId su empresa: el trabajador de la cola no tiene sesión (RNF-03)
 * @param loteId su lote
 * @param numero su número en el lote: 1, o 2 para la segunda
 * @param repeticiones las repeticiones de foto que ya pidió el análisis (RF-09)
 * @param fechaInicio cuándo se tomó, al microsegundo, como la guarda la base
 */
public record MuestraTomada(UUID id, UUID empresaId, UUID loteId, int numero, int repeticiones,
        Instant fechaInicio) {
}
