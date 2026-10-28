/*
 * Nombre: EmpresaRepositorio.java
 * Descripción: Repositorio de Spring Data JPA de la tabla empresa.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.10)
 * Versión: 1.10
 * Entidades: empresa 1.3
 */
package co.edu.usco.inspectia.empresa;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de la tabla {@code empresa}.
 *
 * <p>Las consultas {@code existe...} revisan los datos únicos antes de guardar, para responder 409 con
 * el campo repetido. El índice único de la base es la última defensa.
 */
public interface EmpresaRepositorio extends JpaRepository<Empresa, UUID> {

    /**
     * Dice si ya hay un registro con este valor de NIT.
     *
     * @param valor el valor que se quiere guardar
     * @return {@code true} si ya existe
     */
    @Query("select case when count(e) > 0 then true else false end from Empresa e where e.nit = :valor")
    boolean existeNit(@Param("valor") String valor);

    /**
     * Dice si otro registro, distinto de {@code id}, ya tiene este valor de NIT. Sirve al editar.
     *
     * @param valor el valor que se quiere guardar
     * @param id el registro que se edita
     * @return {@code true} si otro registro ya lo tiene
     */
    @Query("select case when count(e) > 0 then true else false end from Empresa e where e.nit = :valor and e.id <> :id")
    boolean existeNitEnOtro(@Param("valor") String valor, @Param("id") UUID id);

    /**
     * El filtro {@code ?activo=} del listado (D-33): una página de los registros activos o de los
     * desactivados.
     *
     * @param activa {@code true} para los activos y {@code false} para los desactivados
     * @param pagina la página que se pide
     * @return la página
     */
    Page<Empresa> findAllByActiva(Boolean activa, Pageable pagina);
}
