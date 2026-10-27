/*
 * Nombre: DatosDeSesion.java
 * Descripción: Lo que necesita la sesión de un usuario, leído de la base junto con su empresa (RF-01,
 *              D-27, D-28), incluida la versión de sesión que corta sus sesiones (RNF-04, D-30).
 * Fecha de creación: 14/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
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
 * @param versionSesion su versión de sesión: sube cada vez que cambian su contraseña (RNF-04 C6, D-30)
 * @param empresaId su empresa; {@code null} para el administrador de la plataforma
 * @param empresaNombre el nombre de su empresa
 * @param empresaActiva si su empresa está activa (D-27); {@code null} si no tiene empresa
 */
public record DatosDeSesion(UUID id, String nombre, String correo, String contrasenaHash, UsuarioRol rol,
        Boolean activo, OffsetDateTime bloqueadoHasta, Integer versionSesion, UUID empresaId,
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
     * Si un token es de otra versión de sesión: es de una sesión abierta antes de que cambiaran la
     * contraseña y ya no sirve (RNF-04 C6). Solo se comparan los números, así que no depende de la hora del
     * servidor. Una versión mayor que la de la base tampoco sirve.
     */
    boolean otraVersionDeSesion(int versionDelToken) {
        return versionSesion != versionDelToken;
    }

    /** El usuario de una sesión que vence en {@code vence}. */
    UsuarioEnSesion enSesion(Instant vence) {
        return new UsuarioEnSesion(id, nombre, correo, rol.name(), empresaId, empresaNombre, vence);
    }

    /** No muestra el hash de la contraseña si los datos terminan en un registro del servidor. */
    @Override
    public String toString() {
        return "DatosDeSesion[id=" + id + ", rol=" + rol + ", activo=" + activo + ", bloqueadoHasta=" + bloqueadoHasta
                + ", versionSesion=" + versionSesion + ", empresaId=" + empresaId + ", empresaActiva=" + empresaActiva
                + "]";
    }
}
