/*
 * Nombre: CargadorDeSesion.java
 * Descripción: Lee de la base, en cada petición, el usuario dueño de un token válido (D-28). Lo
 *              implementa el módulo usuario.
 * Fecha de creación: 14/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia.comun.seguridad;

import java.time.Instant;
import java.util.UUID;

/**
 * Lee de la base el usuario dueño de un token válido, en cada petición (D-28). Así el rol y la
 * empresa salen siempre de la base y no del token. Lo implementa el módulo usuario, para que
 * {@code comun} no dependa de él.
 */
@FunctionalInterface
public interface CargadorDeSesion {

    /**
     * El usuario dueño de un token cuya firma, emisor y vigencia ya se revisaron.
     *
     * @param usuario el identificador que trae el token
     * @param versionSesion la versión de sesión que trae el token: si la contraseña cambió desde que se
     *                      emitió, ya no es la de la base y la sesión no sirve (RNF-04 C6)
     * @param vence cuándo vence el token
     * @return el usuario, con su rol y su empresa de este momento
     * @throws SesionNoValidaExcepcion si el usuario no puede usar la sesión; por ejemplo, si no existe
     */
    UsuarioEnSesion cargar(UUID usuario, int versionSesion, Instant vence);
}
