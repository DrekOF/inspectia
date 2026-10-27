/*
 * Nombre: RangoRecomendacionRepositorio.java
 * Descripción: Repositorio de Spring Data JPA de la tabla rango_recomendacion.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.8)
 * Versión: 1.8
 * Entidades: rango_recomendacion 1.2
 */
package co.edu.usco.inspectia.rango;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repositorio de la tabla {@code rango_recomendacion}.
 */
public interface RangoRecomendacionRepositorio extends JpaRepository<RangoRecomendacion, UUID> {

    /**
     * Las filas de una empresa, sin un orden fijo: la API nunca lista las de otra (RNF-03).
     *
     * @param empresaId la empresa de la sesión
     * @return las filas
     */
    List<RangoRecomendacion> findAllByEmpresaId(UUID empresaId);

    /**
     * Las filas de una empresa, bloqueadas hasta que termine la transacción ({@code SELECT … FOR UPDATE}).
     * Si otra transacción las está cambiando, espera a que termine y las lee como quedaron: así dos
     * ediciones a la vez no mezclan sus filas.
     *
     * <p>Solo sirve dentro de la transacción que después guarda, así que exige una: fuera de ella, el
     * bloqueo se soltaría al instante.
     *
     * @param empresaId la empresa de la sesión
     * @return las filas
     */
    @Transactional(propagation = Propagation.MANDATORY)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from RangoRecomendacion e where e.empresaId = :empresaId")
    List<RangoRecomendacion> bloquearLasDeLaEmpresa(@Param("empresaId") UUID empresaId);
}
