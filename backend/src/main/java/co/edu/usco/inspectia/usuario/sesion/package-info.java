/*
 * Nombre: package-info.java (usuario.sesion)
 * Descripción: Inicio de sesión, sesión actual y cambio de la propia contraseña del servidor de InspectIA
 *              (RF-01, RF-18, D-28).
 * Fecha de creación: 14/10/2026
 * Actualización: 19/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
/**
 * Inicio de sesión, sesión actual y cambio de la propia contraseña (RF-01, RF-18, RNF-04, D-28):
 * <ul>
 *   <li>{@code POST /api/v1/sesion} revisa el correo y la contraseña, bloquea la cuenta después de 5
 *       intentos fallidos seguidos y entrega el token.</li>
 *   <li>{@code GET /api/v1/sesion} responde quién es el usuario de la sesión y cuándo vence.</li>
 *   <li>{@code PUT /api/v1/sesion/contrasena} cambia la contraseña del usuario de la sesión: la actual
 *       errada cuenta para el mismo bloqueo, las demás sesiones dejan de servir y esta sigue con un token
 *       nuevo (D-30).</li>
 * </ul>
 *
 * <p>Se escribe a mano: no es un CRUD. Los tokens se firman y se revisan en
 * {@code co.edu.usco.inspectia.comun.seguridad}; aquí se lee de la base el usuario de cada token.
 */
package co.edu.usco.inspectia.usuario.sesion;
