/*
 * Nombre: package-info.java (comun.errores)
 * Descripción: Errores de la API de InspectIA y su formato de respuesta.
 * Fecha de creación: 13/10/2026
 * Actualización: 19/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
/**
 * Errores de la API: los servicios lanzan {@link co.edu.usco.inspectia.comun.errores.NoEncontradoExcepcion}
 * (404), {@link co.edu.usco.inspectia.comun.errores.DatoRepetidoExcepcion} (409) y
 * {@link co.edu.usco.inspectia.comun.errores.ReglaIncumplidaExcepcion} (400), y
 * {@link co.edu.usco.inspectia.comun.errores.ManejadorDeErrores} los responde en
 * {@code application/problem+json} con la lista {@code errores}. Los de la sesión tienen los suyos: el
 * inicio de sesión rechazado (401) y, al cambiar la propia contraseña, la actual errada (400) y la cuenta
 * bloqueada (429).
 */
package co.edu.usco.inspectia.comun.errores;
