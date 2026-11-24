/*
 * Nombre: MuestraEnviada.java
 * Descripción: Evento de una muestra que se envió a analizar: despierta a la cola cuando la transacción se confirma
 *              (RNF-02 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.util.UUID;

/**
 * Una muestra se envió a analizar (RNF-02 C3). El trabajador de la cola lo escucha después de confirmar la
 * transacción, para atenderla sin esperar su próxima revisión de la tabla.
 *
 * @param muestraId la muestra
 */
public record MuestraEnviada(UUID muestraId) {
}
