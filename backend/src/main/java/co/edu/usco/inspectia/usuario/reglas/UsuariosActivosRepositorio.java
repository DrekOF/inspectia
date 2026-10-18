/*
 * Nombre: UsuariosActivosRepositorio.java
 * Descripción: Cuenta los usuarios activos de una empresa, para la regla del máximo de 25 (RF-03, RNF-02).
 * Fecha de creación: 18/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario.reglas;

import co.edu.usco.inspectia.usuario.Usuario;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Cuenta los usuarios activos de una empresa, contando a sus administradores (RF-03, RNF-02). */
public interface UsuariosActivosRepositorio extends Repository<Usuario, UUID> {

    /**
     * Cuántos usuarios activos tiene una empresa.
     *
     * @param empresaId la empresa
     * @return la cantidad, contando a sus administradores
     */
    long countByEmpresaIdAndActivoTrue(UUID empresaId);
}
