/*
 * Nombre: MuestrasJpa.java
 * Descripción: Repositorio de Spring Data de la tabla muestra; siempre filtra por la empresa (RNF-03).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** La tabla {@code muestra}. Todas las consultas reciben la empresa de la sesión (RNF-03). */
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
}
