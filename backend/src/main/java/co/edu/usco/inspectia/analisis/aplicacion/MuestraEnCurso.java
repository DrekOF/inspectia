/*
 * Nombre: MuestraEnCurso.java
 * Descripción: La muestra de un lote cuyas fotos se están tomando o ya se enviaron (RF-05, D-25).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeMuestra;
import java.util.UUID;

/**
 * La muestra en curso de un lote (RF-05, D-25).
 *
 * @param id la muestra
 * @param numero su número: 1, o 2 para la segunda
 * @param estado su estado: solo una pendiente recibe fotos
 */
public record MuestraEnCurso(UUID id, int numero, EstadoDeMuestra estado) {
}
