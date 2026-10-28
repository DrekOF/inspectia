/*
 * Nombre: UsuarioRepositorio.java
 * Descripción: Repositorio de Spring Data JPA de la tabla usuario.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.10)
 * Versión: 1.10
 * Entidades: usuario 1.8
 */
package co.edu.usco.inspectia.usuario;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de la tabla {@code usuario}.
 *
 * <p>Las consultas {@code existe...} revisan los datos únicos antes de guardar, para responder 409 con
 * el campo repetido. El índice único de la base es la última defensa.
 */
public interface UsuarioRepositorio extends JpaRepository<Usuario, UUID> {

    /**
     * Dice si ya hay un registro con este valor de correo, sin distinguir mayúsculas ni espacios al inicio o al final.
     *
     * @param valor el valor que se quiere guardar
     * @return {@code true} si ya existe
     */
    @Query("select case when count(e) > 0 then true else false end from Usuario e where lower(trim(e.correo)) = lower(trim(:valor))")
    boolean existeCorreo(@Param("valor") String valor);

    /**
     * Dice si otro registro, distinto de {@code id}, ya tiene este valor de correo. Sirve al editar.
     *
     * @param valor el valor que se quiere guardar
     * @param id el registro que se edita
     * @return {@code true} si otro registro ya lo tiene
     */
    @Query("select case when count(e) > 0 then true else false end from Usuario e where lower(trim(e.correo)) = lower(trim(:valor)) and e.id <> :id")
    boolean existeCorreoEnOtro(@Param("valor") String valor, @Param("id") UUID id);

    /**
     * Una página de los registros de una empresa: la API nunca lista los de otra (RNF-03).
     *
     * @param empresaId la empresa de la sesión
     * @param pagina la página que se pide
     * @return la página
     */
    Page<Usuario> findAllByEmpresaId(UUID empresaId, Pageable pagina);

    /**
     * Un registro de una empresa: si es de otra, la API responde que no existe (RNF-03).
     *
     * @param id el registro
     * @param empresaId la empresa de la sesión
     * @return el registro, si existe y es de esa empresa
     */
    Optional<Usuario> findByIdAndEmpresaId(UUID id, UUID empresaId);

    /**
     * El filtro {@code ?activo=} del listado (D-33): una página de los registros activos o de los
     * desactivados de una empresa.
     *
     * @param empresaId la empresa de la sesión
     * @param activo {@code true} para los activos y {@code false} para los desactivados
     * @param pagina la página que se pide
     * @return la página
     */
    Page<Usuario> findAllByEmpresaIdAndActivo(UUID empresaId, Boolean activo, Pageable pagina);
}
