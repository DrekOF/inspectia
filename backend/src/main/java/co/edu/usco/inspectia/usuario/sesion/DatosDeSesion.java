/*
 * Nombre: DatosDeSesion.java
 * Descripción: Lo que necesita la sesión de un usuario, leído de la base junto con su empresa (RF-01,
 *              D-27, D-28), incluido desde cuándo sirven sus sesiones (RNF-04, D-30).
 * Fecha de creación: 14/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.usuario.sesion;

import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import co.edu.usco.inspectia.usuario.UsuarioRol;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Lo que necesita la sesión de un usuario, leído de la base junto con su empresa en una sola
 * consulta ({@link SesionRepositorio}). Lleva el hash de la contraseña: no va en ninguna respuesta.
 *
 * @param id identificador del usuario
 * @param nombre su nombre
 * @param correo su correo
 * @param contrasenaHash el hash BCrypt de su contraseña
 * @param rol su rol
 * @param activo si está activo (RF-03)
 * @param bloqueadoHasta hasta cuándo está bloqueada su cuenta; {@code null} si no lo está (RNF-04)
 * @param sesionesDesde desde cuándo sirven sus sesiones: la hora del último cambio de contraseña;
 *                      {@code null} si nunca cambió (RNF-04 C6, D-30)
 * @param empresaId su empresa; {@code null} para el administrador de la plataforma
 * @param empresaNombre el nombre de su empresa
 * @param empresaActiva si su empresa está activa (D-27); {@code null} si no tiene empresa
 */
public record DatosDeSesion(UUID id, String nombre, String correo, String contrasenaHash, UsuarioRol rol,
        Boolean activo, OffsetDateTime bloqueadoHasta, OffsetDateTime sesionesDesde, UUID empresaId,
        String empresaNombre, Boolean empresaActiva) {

    /** Si la cuenta está bloqueada en este momento. El bloqueo termina en el instante de {@code bloqueadoHasta}. */
    boolean bloqueadaEn(Instant ahora) {
        return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta.toInstant());
    }

    /** Si el usuario está desactivado (RF-03). */
    boolean desactivado() {
        return !Boolean.TRUE.equals(activo);
    }

    /** Si el usuario tiene empresa y está desactivada (D-27). */
    boolean empresaDesactivada() {
        return empresaId != null && !Boolean.TRUE.equals(empresaActiva);
    }

    /**
     * Si la contraseña cambió después de emitir un token: el token es de una sesión abierta antes del
     * cambio y ya no sirve (RNF-04 C6). Las dos fechas van en segundos enteros, así que un token del
     * mismo segundo del cambio sigue sirviendo.
     */
    boolean contrasenaCambiadaDespuesDe(Instant emitido) {
        return sesionesDesde != null && emitido.isBefore(sesionesDesde.toInstant());
    }

    /** El usuario de una sesión que vence en {@code vence}. */
    UsuarioEnSesion enSesion(Instant vence) {
        return new UsuarioEnSesion(id, nombre, correo, rol.name(), empresaId, empresaNombre, vence);
    }

    /** No muestra el hash de la contraseña si los datos terminan en un registro del servidor. */
    @Override
    public String toString() {
        return "DatosDeSesion[id=" + id + ", rol=" + rol + ", activo=" + activo + ", bloqueadoHasta=" + bloqueadoHasta
                + ", sesionesDesde=" + sesionesDesde + ", empresaId=" + empresaId + ", empresaActiva=" + empresaActiva
                + "]";
    }
}
