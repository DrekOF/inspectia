/*
 * Nombre: UsuarioEnSesion.java
 * Descripción: El usuario de la sesión de una petición, con su rol y su empresa leídos de la base
 *              (RF-01, RNF-03, D-28).
 * Fecha de creación: 14/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.seguridad;

import java.time.Instant;
import java.util.UUID;

/**
 * El usuario de la sesión de una petición. Se lee de la base en cada petición (D-28): si cambia su
 * rol, la siguiente petición ya lo trae. Los controladores lo reciben con
 * {@code @AuthenticationPrincipal UsuarioEnSesion usuario}; con {@code empresaId} filtran los datos
 * de su empresa (RNF-03).
 *
 * @param id identificador del usuario
 * @param nombre su nombre
 * @param correo su correo
 * @param rol su rol: {@code USUARIO_FINAL}, {@code ADMIN_EMPRESA} o {@code ADMIN_PLATAFORMA}
 * @param empresaId su empresa; {@code null} para el administrador de la plataforma
 * @param empresaNombre el nombre de su empresa; {@code null} si no tiene
 * @param vence cuándo vence la sesión (RNF-04)
 */
public record UsuarioEnSesion(UUID id, String nombre, String correo, String rol, UUID empresaId,
        String empresaNombre, Instant vence) {
}
