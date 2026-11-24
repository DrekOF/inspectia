/*
 * Nombre: MetricasDelModeloRepositorio.java
 * Descripción: Consultas de la pantalla «Modelo de IA»: los modelos de la comparación con sus métricas y la confianza
 *              promedio de los granos de los análisis de una empresa (RF-17, RNF-03).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.estadistica.modelo;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Las consultas de la pantalla «Modelo de IA». Solo leen. La tabla {@code modelo} es común a todas las empresas
 * (RNF-03); los granos, en cambio, siempre se filtran por la empresa.
 */
public interface MetricasDelModeloRepositorio extends Repository<ModeloComparado, UUID> {

    /**
     * Los modelos registrados, del mayor F1 macro al menor, como la tabla comparativa de D-18; los que no tienen
     * métricas, como el del clasificador falso, van al final. Los empates, por nombre y versión.
     *
     * @return los modelos
     */
    @Query("select m from ModeloComparado m order by m.f1Macro desc nulls last, m.nombre, m.version")
    List<ModeloComparado> comparados();

    /**
     * La confianza promedio de los granos de los análisis de una empresa, redondeada a 2 decimales, con cuántos granos
     * y de cuántas muestras. El análisis solo guarda los granos de una muestra cuando termina: los de una muestra
     * devuelta o en curso no están. Es de todos los análisis de la empresa, con cualquier modelo.
     *
     * @param empresa la empresa de la sesión
     * @return una fila; sin granos, el promedio va vacío y los conteos en 0
     */
    @Query(value = """
            SELECT round(avg(g.confianza), 2) AS promedio, count(g.id) AS granos,
                count(DISTINCT f.muestra_id) AS muestras
            FROM grano g JOIN foto f ON f.id = g.foto_id AND f.empresa_id = g.empresa_id
            WHERE g.empresa_id = :empresa""", nativeQuery = true)
    ConfianzaDeLaEmpresa confianzaDe(@Param("empresa") UUID empresa);
}
