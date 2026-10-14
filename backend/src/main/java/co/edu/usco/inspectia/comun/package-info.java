/*
 * Nombre: package-info.java (comun)
 * Descripción: Módulo comun del servidor de InspectIA (D-21).
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
/**
 * Módulo comun: lo que comparten los demás módulos, escrito a mano.
 * <ul>
 *   <li>{@code seguridad}: la cadena de seguridad, los roles por ruta y BCrypt (RNF-04). La sesión
 *       con JWT llega con RF-01.</li>
 *   <li>{@code errores}: las respuestas de error en {@code application/problem+json}.</li>
 *   <li>{@code api}: la página de los listados.</li>
 *   <li>{@code validacion} y {@code texto}: lo que usan las validaciones generadas.</li>
 *   <li>Los mensajes generales, en español y en inglés, están en {@code i18n/mensajes.properties}
 *       (RP-11).</li>
 * </ul>
 */
package co.edu.usco.inspectia.comun;
