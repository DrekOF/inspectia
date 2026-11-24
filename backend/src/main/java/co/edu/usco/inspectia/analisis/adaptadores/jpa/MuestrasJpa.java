/*
 * Nombre: MuestrasJpa.java
 * Descripción: Repositorio de Spring Data de la tabla muestra: las muestras de un lote, terminarlas o devolverlas con
 *              una condición sobre su estado, y sus conteos y su detalle para el resultado; siempre filtra por la
 *              empresa (RNF-03, RF-10, RNF-02).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeMuestra;
import co.edu.usco.inspectia.analisis.dominio.MotivoDeDevolucion;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** La tabla {@code muestra}. Todas las consultas reciben la empresa de la sesión o la de la muestra (RNF-03). */
public interface MuestrasJpa extends JpaRepository<MuestraEntidad, UUID> {

    /**
     * Las muestras de un lote de la empresa, de la última a la primera.
     *
     * @param empresa la empresa
     * @param lote el lote
     * @return las muestras: ninguna, una o dos (D-25)
     */
    @Query("select m from MuestraEntidad m where m.empresaId = :empresa and m.loteId = :lote order by m.numero desc")
    List<MuestraEntidad> delLote(@Param("empresa") UUID empresa, @Param("lote") UUID lote);

    /**
     * Termina una muestra con su resultado, solo si sigue en el estado y con la hora de inicio con que se tomó. Manda
     * antes a la base lo pendiente de la transacción.
     *
     * @param empresa la empresa de la muestra
     * @param id la muestra
     * @param enProceso el estado que debe tener: {@code EN_PROCESO}
     * @param inicio la hora de inicio con que se tomó
     * @param terminada el estado nuevo: {@code TERMINADA}
     * @param modelo el modelo que la analizó
     * @param total su total de granos
     * @param sanos sus granos sanos
     * @param aviso su aviso de confianza baja
     * @param umbral la copia del umbral de confianza baja
     * @param maximo la copia del máximo de dudosos
     * @param fin su fecha de fin
     * @return 1 si la terminó, 0 si no
     */
    @Modifying(flushAutomatically = true)
    @Query("update MuestraEntidad m set m.estado = :terminada, m.modeloId = :modelo, m.totalGranos = :total, "
            + "m.granosSanos = :sanos, m.avisoConfianzaBaja = :aviso, m.umbralConfianzaBaja = :umbral, "
            + "m.porcentajeMaxDudosos = :maximo, m.fechaFin = :fin "
            + "where m.id = :id and m.empresaId = :empresa and m.estado = :enProceso and m.fechaInicio = :inicio")
    int terminar(@Param("empresa") UUID empresa, @Param("id") UUID id, @Param("enProceso") EstadoDeMuestra enProceso,
            @Param("inicio") OffsetDateTime inicio, @Param("terminada") EstadoDeMuestra terminada,
            @Param("modelo") UUID modelo, @Param("total") Integer total, @Param("sanos") Integer sanos,
            @Param("aviso") Boolean aviso, @Param("umbral") BigDecimal umbral, @Param("maximo") BigDecimal maximo,
            @Param("fin") OffsetDateTime fin);

    /**
     * Cambia el estado de una muestra, solo si sigue en el estado y con la hora de inicio con que se tomó. Conserva sus
     * fechas.
     *
     * @param empresa la empresa de la muestra
     * @param id la muestra
     * @param actual el estado que debe tener
     * @param inicio la hora de inicio con que se tomó
     * @param nuevo el estado nuevo
     * @return 1 si la cambió, 0 si no
     */
    @Modifying(flushAutomatically = true)
    @Query("update MuestraEntidad m set m.estado = :nuevo "
            + "where m.id = :id and m.empresaId = :empresa and m.estado = :actual and m.fechaInicio = :inicio")
    int cambiarEstado(@Param("empresa") UUID empresa, @Param("id") UUID id, @Param("actual") EstadoDeMuestra actual,
            @Param("inicio") OffsetDateTime inicio, @Param("nuevo") EstadoDeMuestra nuevo);

    /**
     * Devuelve una muestra a pendiente con el motivo, solo si sigue en el estado y con la hora de inicio con que se tomó.
     * Conserva sus fechas (D-44).
     *
     * @param empresa la empresa de la muestra
     * @param id la muestra
     * @param actual el estado que debe tener: {@code EN_PROCESO}
     * @param inicio la hora de inicio con que se tomó
     * @param pendiente el estado nuevo: {@code PENDIENTE}
     * @param motivo el motivo, o {@code null}
     * @return 1 si la devolvió, 0 si no
     */
    @Modifying(flushAutomatically = true)
    @Query("update MuestraEntidad m set m.estado = :pendiente, m.motivoDevolucion = :motivo "
            + "where m.id = :id and m.empresaId = :empresa and m.estado = :actual and m.fechaInicio = :inicio")
    int devolver(@Param("empresa") UUID empresa, @Param("id") UUID id, @Param("actual") EstadoDeMuestra actual,
            @Param("inicio") OffsetDateTime inicio, @Param("pendiente") EstadoDeMuestra pendiente,
            @Param("motivo") MotivoDeDevolucion motivo);

    /**
     * Devuelve una muestra a pendiente para repetir una foto (RF-09 C1): suma 1 a sus repeticiones y copia el umbral y
     * el máximo de dudosos, solo si sigue en el estado y con la hora de inicio con que se tomó.
     *
     * @param empresa la empresa de la muestra
     * @param id la muestra
     * @param actual el estado que debe tener: {@code EN_PROCESO}
     * @param inicio la hora de inicio con que se tomó
     * @param pendiente el estado nuevo: {@code PENDIENTE}
     * @param umbral la copia del umbral de confianza baja
     * @param maximo la copia del máximo de dudosos
     * @return 1 si la devolvió, 0 si no
     */
    @Modifying(flushAutomatically = true)
    @Query("update MuestraEntidad m set m.estado = :pendiente, m.motivoDevolucion = null, "
            + "m.repeticiones = m.repeticiones + 1, m.umbralConfianzaBaja = :umbral, m.porcentajeMaxDudosos = :maximo "
            + "where m.id = :id and m.empresaId = :empresa and m.estado = :actual and m.fechaInicio = :inicio")
    int pedirRepeticion(@Param("empresa") UUID empresa, @Param("id") UUID id, @Param("actual") EstadoDeMuestra actual,
            @Param("inicio") OffsetDateTime inicio, @Param("pendiente") EstadoDeMuestra pendiente,
            @Param("umbral") BigDecimal umbral, @Param("maximo") BigDecimal maximo);

    /**
     * Los conteos de las muestras de un lote en un estado, por número, con los recall del modelo de cada una.
     *
     * @param empresa la empresa
     * @param lote el lote
     * @param terminada el estado: {@code TERMINADA}
     * @return los conteos
     */
    @Query("select new co.edu.usco.inspectia.analisis.adaptadores.jpa.FilaDeConteo(m.granosSanos, m.totalGranos, "
            + "mo.recallSano, mo.recallNoSano, m.avisoConfianzaBaja) "
            + "from MuestraEntidad m join ModeloEntidad mo on mo.id = m.modeloId "
            + "where m.empresaId = :empresa and m.loteId = :lote and m.estado = :terminada order by m.numero")
    List<FilaDeConteo> terminadas(@Param("empresa") UUID empresa, @Param("lote") UUID lote,
            @Param("terminada") EstadoDeMuestra terminada);

    /**
     * Las muestras de un lote de la empresa, por número, con el nombre de quien la analizó y el nombre y la versión de
     * su modelo, si ya lo tiene.
     *
     * @param empresa la empresa
     * @param lote el lote
     * @return las muestras
     */
    @Query("select new co.edu.usco.inspectia.analisis.adaptadores.jpa.FilaDeMuestra(m.id, m.numero, m.estado, "
            + "m.usuarioId, u.nombre, mo.nombre, mo.version, m.totalGranos, m.granosSanos, m.umbralConfianzaBaja, "
            + "m.porcentajeMaxDudosos, m.fechaEnvio, m.fechaFin, m.motivoDevolucion, m.repeticiones, "
            + "m.avisoConfianzaBaja) "
            + "from MuestraEntidad m join Usuario u on u.id = m.usuarioId "
            + "left join ModeloEntidad mo on mo.id = m.modeloId "
            + "where m.empresaId = :empresa and m.loteId = :lote order by m.numero")
    List<FilaDeMuestra> conDetalle(@Param("empresa") UUID empresa, @Param("lote") UUID lote);
}
