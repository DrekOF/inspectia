/*
 * Nombre: LotesJpa.java
 * Descripción: Repositorio de Spring Data de la tabla lote, con las consultas del registro, el lote como se consulta,
 *              las sugerencias de finca o proveedor, quienes registraron lotes, la sugerencia de otra muestra y el
 *              resultado guardado con su límite (RF-04, RF-14, RNF-03, D-25, D-47).
 * Fecha de creación: 10/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.3
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import co.edu.usco.inspectia.analisis.aplicacion.OrigenUsado;
import co.edu.usco.inspectia.analisis.aplicacion.Registrador;
import co.edu.usco.inspectia.analisis.aplicacion.ResultadoGuardado;
import co.edu.usco.inspectia.analisis.aplicacion.VistaDeLote;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeLote;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de Spring Data de la tabla {@code lote}. Cada consulta filtra por la empresa que recibe
 * (RNF-03). El historial con sus filtros lo arma {@link RepositorioDeLotesJpa}, porque su consulta cambia con los
 * filtros pedidos; usa {@link #VISTA} y {@link #ORDEN_DEL_HISTORIAL}.
 */
public interface LotesJpa extends JpaRepository<LoteEntidad, UUID> {

    /** El lote como se consulta, con el nombre de quien lo registró y si sugiere otra muestra. */
    String VISTA = "select new co.edu.usco.inspectia.analisis.aplicacion.VistaDeLote(l.id, l.codigo, "
            + "l.fechaRecepcion, l.origen, l.observaciones, l.estado, l.usuarioId, u.nombre, l.porcentajeSanos, "
            + "l.categoria, l.sugerirOtraMuestra, l.fechaResultado, l.fechaCreacion) "
            + "from LoteEntidad l join Usuario u on u.id = l.usuarioId ";

    /**
     * El orden del historial: del último movimiento al más antiguo, que es la fecha del resultado si el lote ya se
     * analizó, o la de registro si está pendiente (RF-14 restricción 3). Los empates, por identificador (UUID v7, que
     * también crece con el tiempo).
     */
    String ORDEN_DEL_HISTORIAL = " order by coalesce(l.fechaResultado, l.fechaCreacion) desc, l.id desc";

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
     * Quienes registraron lotes de la empresa, una vez cada uno, con su identificador y su nombre y sin su correo
     * (RF-14 restricción 6). No filtra por {@code activo}: también salen los desactivados (RF-03 C6). La llave foránea
     * {@code (usuario_id, empresa_id)} asegura que cada uno es de la misma empresa (RNF-03).
     *
     * @param empresa la empresa
     * @return quienes registraron lotes, sin orden
     */
    @Query("select distinct new co.edu.usco.inspectia.analisis.aplicacion.Registrador(u.id, u.nombre) "
            + "from LoteEntidad l join Usuario u on u.id = l.usuarioId where l.empresaId = :empresa")
    List<Registrador> registradores(@Param("empresa") UUID empresa);

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

    /**
     * El estado de un lote de la empresa.
     *
     * @param empresa la empresa
     * @param id el lote
     * @return el estado, o vacío si no existe o es de otra empresa
     */
    @Query("select l.estado from LoteEntidad l where l.id = :id and l.empresaId = :empresa")
    Optional<EstadoDeLote> estado(@Param("empresa") UUID empresa, @Param("id") UUID id);

    /**
     * Si el resultado guardado de un lote de la empresa sugiere otra muestra (D-24, D-25).
     *
     * @param empresa la empresa
     * @param id el lote
     * @return el valor guardado, o vacío si el lote está pendiente, no existe o es de otra empresa
     */
    @Query("select l.sugerirOtraMuestra from LoteEntidad l where l.id = :id and l.empresaId = :empresa")
    Optional<Boolean> sugiereOtraMuestra(@Param("empresa") UUID empresa, @Param("id") UUID id);

    /**
     * Un lote de la empresa, con su fila bloqueada hasta el final de la transacción ({@code SELECT … FOR UPDATE}).
     *
     * @param empresa la empresa
     * @param id el lote
     * @return el lote, o vacío si no existe o es de otra empresa
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LoteEntidad l where l.id = :id and l.empresaId = :empresa")
    Optional<LoteEntidad> paraActualizar(@Param("empresa") UUID empresa, @Param("id") UUID id);

    /**
     * El resultado guardado de un lote de la empresa, tal cual, sin recalcularlo (RF-14 C3), con el límite de la
     * sugerencia y su categoría (D-47).
     *
     * @param empresa la empresa
     * @param id el lote
     * @return el resultado, o vacío si no existe o es de otra empresa
     */
    @Query("select new co.edu.usco.inspectia.analisis.aplicacion.ResultadoGuardado(l.id, l.codigo, l.fechaRecepcion, "
            + "l.origen, l.estado, l.fechaResultado, l.porcentajeSanos, l.margen, l.categoria, l.recomendacion, "
            + "l.sugerirOtraMuestra, l.limiteSugerencia, l.categoriaDelLimite, l.avisoConfianzaBaja) "
            + "from LoteEntidad l where l.id = :id and l.empresaId = :empresa")
    Optional<ResultadoGuardado> resultado(@Param("empresa") UUID empresa, @Param("id") UUID id);
}
