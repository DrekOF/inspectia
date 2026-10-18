/*
 * Nombre: package-info.java (usuario.sesion)
 * Descripción: Inicio de sesión y sesión actual del servidor de InspectIA (RF-01, D-28).
 * Fecha de creación: 14/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
/**
 * Inicio de sesión y sesión actual (RF-01, RNF-04, D-28):
 * <ul>
 *   <li>{@code POST /api/v1/sesion} revisa el correo y la contraseña, bloquea la cuenta después de 5
 *       intentos fallidos seguidos y entrega el token.</li>
 *   <li>{@code GET /api/v1/sesion} responde quién es el usuario de la sesión y cuándo vence.</li>
 * </ul>
 *
 * <p>Se escribe a mano: no es un CRUD. Los tokens se firman y se revisan en
 * {@code co.edu.usco.inspectia.comun.seguridad}; aquí se lee de la base el usuario de cada token.
 */
package co.edu.usco.inspectia.usuario.sesion;
