/*
 * Nombre: LotesJpa.java
 * Descripción: Repositorio de Spring Data de la tabla lote, con las consultas del registro, el historial y
 *              las sugerencias de finca o proveedor (RF-04, RF-14, RNF-03).
 * Fecha de creación: 10/11/2026
 * Actualización: 10/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import co.edu.usco.inspectia.analisis.aplicacion.OrigenUsado;
import co.edu.usco.inspectia.analisis.aplicacion.VistaDeLote;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de Spring Data de la tabla {@code lote}. Cada consulta filtra por la empresa que recibe
 * (RNF-03).
 */
public interface LotesJpa extends JpaRepository<LoteEntidad, UUID> {

    /** El lote como se consulta, con el nombre de quien lo registró. */
    String VISTA = "select new co.edu.usco.inspectia.analisis.aplicacion.VistaDeLote(l.id, l.codigo, "
            + "l.fechaRecepcion, l.origen, l.observaciones, l.estado, l.usuarioId, u.nombre, l.porcentajeSanos, "
            + "l.categoria, l.fechaResultado, l.fechaCreacion) from LoteEntidad l join Usuario u on u.id = l.usuarioId ";

    /**
     * Si la empresa ya tiene un lote con ese código, con la misma expresión del índice único de la base.
     *
     * @param empresa la empresa
     * @param clave el código, sin espacios al inicio ni al final y en minúsculas
     * @return {@code true} si existe
     */
    @Query(value = "SELECT EXISTS (SELECT 1 FROM lote WHERE empresa_id = :empresa AND lower(btrim(codigo)) = :clave)",
            nativeQuery = true)
    boolean existeCodigo(@Param("empresa") UUID empresa, @Param("clave") String clave);

    /**
     * Un lote de la empresa.
     *
     * @param empresa la empresa
     * @param id el lote
     * @return el lote, o vacío si no existe o es de otra empresa
     */
    @Query(VISTA + "where l.id = :id and l.empresaId = :empresa")
    Optional<VistaDeLote> vista(@Param("empresa") UUID empresa, @Param("id") UUID id);

    /**
     * Una página de los lotes de la empresa, del último movimiento al más antiguo: la fecha del resultado
     * si el lote ya se analizó, o la de registro si está pendiente. Los empates, por identificador (UUID v7,
     * que también crece con el tiempo).
     *
     * @param empresa la empresa
     * @param pagina qué página y de qué tamaño
     * @return la página
     */
    @Query(value = VISTA + "where l.empresaId = :empresa "
            + "order by coalesce(l.fechaResultado, l.fechaCreacion) desc, l.id desc",
            countQuery = "select count(l) from LoteEntidad l where l.empresaId = :empresa")
    Page<VistaDeLote> historial(@Param("empresa") UUID empresa, Pageable pagina);

    /**
     * Las formas en que la empresa escribió sus fincas y proveedores, con cuántos lotes usan cada una y la
     * última vez que se usó.
     *
     * @param empresa la empresa
     * @return las formas
     */
    @Query("select new co.edu.usco.inspectia.analisis.aplicacion.OrigenUsado(l.origen, count(l), "
            + "max(l.fechaCreacion)) from LoteEntidad l where l.empresaId = :empresa group by l.origen")
    List<OrigenUsado> origenesUsados(@Param("empresa") UUID empresa);
}
