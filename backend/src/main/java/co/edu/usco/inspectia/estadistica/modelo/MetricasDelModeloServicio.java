/*
 * Nombre: MetricasDelModeloServicio.java
 * Descripción: Arma lo que ve el administrador de empresa en «Modelo de IA»: los modelos con sus métricas del
 *              conjunto de prueba y la confianza promedio de los análisis de su empresa (RF-17, RNF-03).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.estadistica.modelo;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las métricas del modelo de IA (RF-17). Las de prueba son las que guarda la tabla {@code modelo} de la comparación de
 * D-18 (C1), comunes a todas las empresas; la confianza promedio sale solo de los granos de la empresa de la sesión
 * (C2, RNF-03).
 */
@Service
@Transactional(readOnly = true)
public class MetricasDelModeloServicio {

    private final MetricasDelModeloRepositorio repositorio;

    /**
     * Crea el servicio; Spring le pasa el repositorio.
     *
     * @param repositorio las consultas de los modelos y de los granos
     */
    public MetricasDelModeloServicio(MetricasDelModeloRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Las métricas de los modelos y la confianza promedio de una empresa.
     *
     * @param empresaId la empresa de la sesión
     * @return los modelos, del mayor F1 macro al menor, y la confianza promedio de sus análisis
     */
    public MetricasDelModeloRespuesta consultar(UUID empresaId) {
        return new MetricasDelModeloRespuesta(
                repositorio.comparados().stream().map(ModeloMedidoRespuesta::de).toList(),
                ConfianzaPromedioRespuesta.de(repositorio.confianzaDe(empresaId)));
    }
}
