/*
 * Nombre: MetricasDelModeloRespuesta.java
 * Descripción: La respuesta de la API del modelo de IA: los modelos de la comparación con sus métricas y cuál está en
 *              uso, y la confianza promedio de los análisis de la empresa (RF-17).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.estadistica.modelo;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Lo que ve el administrador de empresa en «Modelo de IA» (RF-17).
 *
 * @param modelos los modelos registrados, del mayor F1 macro al menor, y al final los que no tienen métricas
 * @param confianza la confianza promedio de los análisis de la empresa
 */
@Schema(description = "Las métricas de los modelos de IA y la confianza promedio de la empresa (RF-17).")
public record MetricasDelModeloRespuesta(
        @ArraySchema(arraySchema = @Schema(description = "Los modelos registrados, del mayor F1 macro al menor; los "
                + "que no tienen métricas van al final. El modelo en uso lleva enUso en true."))
        List<ModeloMedidoRespuesta> modelos,
        @Schema(description = "La confianza promedio de los análisis de la empresa.")
        ConfianzaPromedioRespuesta confianza) {
}
