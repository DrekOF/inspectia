/*
 * Nombre: UsuariosActivosRepositorio.java
 * Descripción: Cuenta los usuarios activos de una empresa, con la fila de la empresa bloqueada, para la
 *              regla del máximo de 25 (RF-03, RNF-02).
 * Fecha de creación: 18/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.usuario.reglas;

import co.edu.usco.inspectia.usuario.Usuario;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Cuenta los usuarios activos de una empresa, contando a sus administradores (RF-03, RNF-02). */
public interface UsuariosActivosRepositorio extends Repository<Usuario, UUID> {

    /**
     * Bloquea la fila de la empresa hasta que termine la transacción ({@code SELECT … FOR UPDATE}). Si
     * dos administradores crean o reactivan un usuario a la vez, el segundo espera aquí a que el primero
     * guarde y después cuenta con ese usuario incluido: así la empresa no pasa de 25 activos (RF-03 C5).
     *
     * <p>Solo sirve dentro de la transacción que después guarda, así que exige una: fuera de ella, el
     * bloqueo se soltaría al instante.
     *
     * @param empresaId la empresa
     * @return la empresa, si existe
     */
    @Transactional(propagation = Propagation.MANDATORY)
    @Query(value = "SELECT id FROM empresa WHERE id = :empresaId FOR UPDATE", nativeQuery = true)
    Optional<UUID> bloquearEmpresa(@Param("empresaId") UUID empresaId);

    /**
     * Cuántos usuarios activos tiene una empresa.
     *
     * @param empresaId la empresa
     * @return la cantidad, contando a sus administradores
     */
    long countByEmpresaIdAndActivoTrue(UUID empresaId);
}
