/*
 * Nombre: package-info.java (usuario.administradores)
 * Descripción: Rutas del administrador de la plataforma sobre los administradores de una empresa:
 *              listarlos y restablecerles la contraseña (RF-02 C12, D-30).
 * Fecha de creación: 18/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
/**
 * Lo que hace el administrador de la plataforma con los administradores de una empresa (RF-02 C12,
 * D-30). Es la regla {@code restablecer_contrasena_administrador}, de tipo {@code accion}, de
 * {@code specs/entidades/usuario.json}:
 * <ul>
 *   <li>{@code GET /api/v1/empresas/{empresaId}/administradores} los lista, para elegir a cuál
 *       restablecer.</li>
 *   <li>{@code PUT /api/v1/empresas/{empresaId}/administradores/{usuarioId}/contrasena} le restablece
 *       la contraseña, con las mismas validaciones de la inicial: corta sus sesiones abiertas y
 *       desbloquea su cuenta (RNF-04 C6).</li>
 * </ul>
 *
 * <p>Se escribe a mano: las acciones no son parte del CRUD generado (RP-23). Solo las usa el
 * administrador de la plataforma, que no tiene empresa: la empresa sale de la ruta.
 */
package co.edu.usco.inspectia.usuario.administradores;
