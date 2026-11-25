/*
 * Nombre: ModeloMedidoRespuesta.java
 * Descripción: Un modelo de IA con sus métricas del conjunto de prueba, como lo responde la API del modelo (RF-17,
 *              D-18, D-48).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.estadistica.modelo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Un modelo de la comparación de D-18 con sus métricas del conjunto de prueba (RF-17). Los porcentajes van de 0 a 100,
 * con 2 decimales. Un modelo sin las métricas de la comparación, como el del clasificador falso de desarrollo, solo trae
 * sus recall: lo demás va vacío.
 *
 * @param id su identificador
 * @param nombre su nombre
 * @param version su versión
 * @param tipo CNN o TRANSFORMER, o vacío
 * @param enUso si es el modelo en uso
 * @param f1Macro el F1-score macro (%)
 * @param precisionMacro la precisión macro (%)
 * @param recallMacro el recall macro (%)
 * @param recallSano el recall de la clase sano (%)
 * @param recallNoSano el recall de la clase no sano (%)
 * @param precisionSano la precisión de la clase sano (%)
 * @param precisionNoSano la precisión de la clase no sano (%)
 * @param matriz la matriz de confusión, o vacía
 * @param tiempoPorGranoMs el tiempo por grano (ms)
 */
@Schema(description = "Un modelo de IA con sus métricas del conjunto de prueba (RF-17, D-18, D-48).")
public record ModeloMedidoRespuesta(
        @Schema(description = "Identificador (UUID v7).") UUID id,
        @Schema(description = "Nombre", example = "EfficientNet-B0") String nombre,
        @Schema(description = "Versión", example = "1.0") String version,
        @Schema(description = "Tipo: CNN o TRANSFORMER; vacío en un modelo sin métricas.", example = "CNN",
                allowableValues = {"CNN", "TRANSFORMER"}) String tipo,
        @Schema(description = "Si es el modelo en uso; solo uno lo está.") Boolean enUso,
        @Schema(description = "F1-score macro (%)", example = "73.50") BigDecimal f1Macro,
        @Schema(description = "Precisión macro (%)", example = "75.20") BigDecimal precisionMacro,
        @Schema(description = "Recall macro (%)", example = "72.31") BigDecimal recallMacro,
        @Schema(description = "Recall de sano (%)", example = "91.20") BigDecimal recallSano,
        @Schema(description = "Recall de no sano (%)", example = "53.40") BigDecimal recallNoSano,
        @Schema(description = "Precisión de sano (%)", example = "87.50") BigDecimal precisionSano,
        @Schema(description = "Precisión de no sano (%)", example = "62.90") BigDecimal precisionNoSano,
        @Schema(description = "Matriz de confusión del conjunto de prueba; vacía en un modelo sin métricas.")
        MatrizDeConfusionRespuesta matriz,
        @Schema(description = "Tiempo por grano (ms)", example = "19.00") BigDecimal tiempoPorGranoMs) {

    /**
     * El modelo como lo responde la API.
     *
     * @param modelo la fila de la tabla {@code modelo}
     * @return el modelo
     */
    static ModeloMedidoRespuesta de(ModeloComparado modelo) {
        return new ModeloMedidoRespuesta(modelo.getId(), modelo.getNombre(), modelo.getVersion(), modelo.getTipo(),
                Boolean.TRUE.equals(modelo.getEnUso()), modelo.getF1Macro(), modelo.getPrecisionMacro(),
                modelo.getRecallMacro(), modelo.getRecallSano(), modelo.getRecallNoSano(), modelo.getPrecisionSano(),
                modelo.getPrecisionNoSano(), MatrizDeConfusionRespuesta.de(modelo), modelo.getTiempoPorGranoMs());
    }
}
