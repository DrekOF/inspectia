/*
 * Nombre: FotosJpa.java
 * Descripción: Repositorio de Spring Data de la tabla foto; siempre filtra por la empresa (RNF-03). También aprueba
 *              las fotos de una muestra analizada (RF-06).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeFoto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** La tabla {@code foto}. Todas las consultas reciben la empresa de la sesión (RNF-03). */
public interface FotosJpa extends JpaRepository<FotoEntidad, UUID> {

    /**
     * Cambia el estado de las fotos de una muestra de la empresa y les quita el motivo. Manda antes a la base lo
     * pendiente de la transacción.
     *
     * @param empresa la empresa
     * @param muestra la muestra
     * @param estado el estado nuevo, como {@code APROBADA}
     * @return cuántas cambió
     */
    @Modifying(flushAutomatically = true)
    @Query("update FotoEntidad f set f.estado = :estado, f.motivo = null "
            + "where f.empresaId = :empresa and f.muestraId = :muestra")
    int cambiarEstado(@Param("empresa") UUID empresa, @Param("muestra") UUID muestra,
            @Param("estado") EstadoDeFoto estado);

    /**
     * Guarda la revisión de una foto de la empresa: su estado, su motivo y sus granos encontrados (D-46). Manda antes a
     * la base lo pendiente de la transacción y después olvida las entidades leídas, así la foto se vuelve a leer con su
     * revisión.
     *
     * @param empresa la empresa
     * @param id la foto
     * @param estado {@code APROBADA} o {@code POR_REPETIR}
     * @param motivo su motivo, solo si está por repetir
     * @param granos los granos válidos que encontró la revisión
     * @return cuántas cambió: 1, o 0 si no existe
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update FotoEntidad f set f.estado = :estado, f.motivo = :motivo, f.granosEncontrados = :granos "
            + "where f.empresaId = :empresa and f.id = :id")
    int guardarRevision(@Param("empresa") UUID empresa, @Param("id") UUID id, @Param("estado") EstadoDeFoto estado,
            @Param("motivo") String motivo, @Param("granos") Integer granos);

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
