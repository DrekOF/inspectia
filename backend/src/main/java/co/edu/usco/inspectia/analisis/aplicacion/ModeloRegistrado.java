/*
 * Nombre: ModeloRegistrado.java
 * Descripción: Un modelo de IA registrado en la tabla modelo, con los recall con que se corrige el porcentaje (RF-08,
 *              D-39).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.RecallsDelModelo;
import java.util.UUID;

/**
 * Un modelo de la tabla {@code modelo} (RF-08, D-39).
 *
 * @param id su identificador
 * @param nombre su nombre
 * @param version su versión
 * @param recalls el recall de cada clase, con que se corrige el porcentaje del lote
 */
public record ModeloRegistrado(UUID id, String nombre, String version, RecallsDelModelo recalls) {
}
