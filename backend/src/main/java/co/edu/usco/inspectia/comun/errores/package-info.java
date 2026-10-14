/*
 * Nombre: package-info.java (comun.errores)
 * Descripción: Errores de la API de InspectIA y su formato de respuesta.
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
/**
 * Errores de la API: los servicios lanzan {@link co.edu.usco.inspectia.comun.errores.NoEncontradoExcepcion}
 * (404), {@link co.edu.usco.inspectia.comun.errores.DatoRepetidoExcepcion} (409) y
 * {@link co.edu.usco.inspectia.comun.errores.ReglaIncumplidaExcepcion} (400), y
 * {@link co.edu.usco.inspectia.comun.errores.ManejadorDeErrores} los responde en
 * {@code application/problem+json} con la lista {@code errores}.
 */
package co.edu.usco.inspectia.comun.errores;
