/*
 * Nombre: VersionDeSesionRepositorio.java
 * Descripción: Sube la versión de sesión de un usuario con una sola sentencia, para cortar sus sesiones
 *              abiertas al cambiar o restablecer su contraseña (RNF-04 C6, D-30).
 * Fecha de creación: 26/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario.reglas;

import co.edu.usco.inspectia.usuario.Usuario;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * La versión de sesión de los usuarios ({@code usuario.version_sesion}). JPA la lee, pero no la escribe: solo
 * cambia aquí (RNF-04 C6, D-30).
 */
public interface VersionDeSesionRepositorio extends Repository<Usuario, UUID> {

    /**
     * Sube en uno la versión de sesión de un usuario y devuelve la nueva. Es una sola sentencia
     * ({@code UPDATE … RETURNING}): si dos cambios de contraseña llegan a la vez, la base bloquea la fila y los
     * aplica uno tras otro, así la versión sube dos veces y ninguno se pierde.
     *
     * <p>Solo sirve dentro de la transacción que guarda la contraseña nueva, así que exige una: si esa
     * transacción no termina, la versión tampoco sube.
     *
     * @param usuarioId el usuario
     * @return su versión de sesión nueva
     */
    @Transactional(propagation = Propagation.MANDATORY)
    @Query(value = "UPDATE usuario SET version_sesion = version_sesion + 1 WHERE id = :usuarioId"
            + " RETURNING version_sesion", nativeQuery = true)
    int subir(@Param("usuarioId") UUID usuarioId);
}
