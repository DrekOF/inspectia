/*
 * Nombre: RangoRecomendacionRepositorio.java
 * Descripción: Repositorio de Spring Data JPA de la tabla rango_recomendacion.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.3)
 * Versión: 1.3
 * Entidades: rango_recomendacion 1.1
 */
package co.edu.usco.inspectia.rango;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de la tabla {@code rango_recomendacion}.
 */
public interface RangoRecomendacionRepositorio extends JpaRepository<RangoRecomendacion, UUID> {

    /**
     * Una página de los registros de una empresa: la API nunca lista los de otra (RNF-03).
     *
     * @param empresaId la empresa de la sesión
     * @param pagina la página que se pide
     * @return la página
     */
    Page<RangoRecomendacion> findAllByEmpresaId(UUID empresaId, Pageable pagina);

    /**
     * Un registro de una empresa: si es de otra, la API responde que no existe (RNF-03).
     *
     * @param id el registro
     * @param empresaId la empresa de la sesión
     * @return el registro, si existe y es de esa empresa
     */
    Optional<RangoRecomendacion> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
