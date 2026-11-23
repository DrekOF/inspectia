/*
 * Nombre: FotosJpa.java
 * Descripción: Repositorio de Spring Data de la tabla foto; siempre filtra por la empresa (RNF-03).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** La tabla {@code foto}. Todas las consultas reciben la empresa de la sesión (RNF-03). */
public interface FotosJpa extends JpaRepository<FotoEntidad, UUID> {

    /**
     * Las fotos de una muestra de la empresa, por número.
     *
     * @param empresa la empresa
     * @param muestra la muestra
     * @return las fotos
     */
    @Query("select f from FotoEntidad f where f.empresaId = :empresa and f.muestraId = :muestra order by f.numero")
    List<FotoEntidad> deLaMuestra(@Param("empresa") UUID empresa, @Param("muestra") UUID muestra);

    /**
     * Una foto de una muestra de la empresa.
     *
     * @param empresa la empresa
     * @param muestra la muestra
     * @param numero el número de la foto
     * @return la foto, o vacío si ese número no tiene foto
     */
    @Query("select f from FotoEntidad f where f.empresaId = :empresa and f.muestraId = :muestra "
            + "and f.numero = :numero")
    Optional<FotoEntidad> deLaMuestra(@Param("empresa") UUID empresa, @Param("muestra") UUID muestra,
            @Param("numero") int numero);

    /**
     * Una foto de la empresa.
     *
     * @param empresa la empresa
     * @param id la foto
     * @return la foto, o vacío si no existe o es de otra empresa
     */
    @Query("select f from FotoEntidad f where f.empresaId = :empresa and f.id = :id")
    Optional<FotoEntidad> deLaEmpresa(@Param("empresa") UUID empresa, @Param("id") UUID id);
}
