/*
 * Nombre: MetricasDelModeloRepositorio.java
 * Descripción: Consultas de la pantalla «Modelo de IA»: los modelos de la comparación con sus métricas y la confianza
 *              promedio de los granos de los análisis de una empresa (RF-17, RNF-03).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.estadistica.modelo;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Los modelos de la pantalla «Modelo de IA». Solo lee. La tabla {@code modelo} es común a todas las empresas (RNF-03);
 * la confianza de los granos, que sí es de cada empresa, la lee {@link ConfianzaDeLosAnalisisRepositorio}.
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
}
