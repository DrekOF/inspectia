/*
 * Nombre: AdministradoresRepositorio.java
 * Descripción: Consultas de los administradores de una empresa sobre la tabla usuario (RF-02 C12).
 * Fecha de creación: 18/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario.administradores;

import co.edu.usco.inspectia.usuario.Usuario;
import co.edu.usco.inspectia.usuario.UsuarioRol;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.Repository;

/** Los usuarios de una empresa con un rol dado: aquí, sus administradores (RF-02 C12). */
public interface AdministradoresRepositorio extends Repository<Usuario, UUID> {

    /**
     * Los usuarios de una empresa con un rol, activos o no.
     *
     * @param empresaId la empresa
     * @param rol el rol
     * @param orden el orden del listado
     * @return los usuarios
     */
    List<Usuario> findAllByEmpresaIdAndRol(UUID empresaId, UsuarioRol rol, Sort orden);

    /**
     * Un usuario, solo si es de esa empresa y tiene ese rol.
     *
     * @param id el usuario
     * @param empresaId la empresa
     * @param rol el rol
     * @return el usuario, si cumple las dos condiciones
     */
    Optional<Usuario> findByIdAndEmpresaIdAndRol(UUID id, UUID empresaId, UsuarioRol rol);
}
