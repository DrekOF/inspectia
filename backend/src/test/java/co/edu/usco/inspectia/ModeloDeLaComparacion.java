/*
 * Nombre: ModeloDeLaComparacion.java
 * Descripción: Modelos de la comparación de Julián (D-18) con sus 11 métricas, para las pruebas que insertan un modelo
 *              en la tabla modelo: la base solo deja en uso un modelo con todas sus métricas y un F1 macro de 65 % o
 *              más (P-02, D-48).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Modelos de la comparación de Julián (release {@code modelos-v1.0}, D-18), con las métricas que guarda la tabla
 * {@code modelo}: tipo, F1, precisión y recall macro, recall de cada clase (los de D-39, de 3 decimales), precisión de
 * cada clase, matriz de confusión del conjunto de prueba y tiempo por grano (D-48). Los porcentajes van de 0 a 100, como
 * en la base.
 */
public enum ModeloDeLaComparacion {

    /** El modelo de la app (P-01): F1 macro 73,50 y matriz 238 / 23 / 34 / 39. */
    EFFICIENTNET_B0("EfficientNet-B0", "73.50", "75.20", "72.31", "91.20", "53.40", "87.50", "62.90",
            new int[] {238, 23, 34, 39}, "19.00"),

    /** F1 macro 69,90 y matriz 228 / 33 / 35 / 38: puede estar en uso. */
    RESNET_50("ResNet-50", "69.90", "70.11", "69.71", "87.40", "52.10", "86.69", "53.52",
            new int[] {228, 33, 35, 38}, "50.80"),

    /** F1 macro 63,80, menos que el 65 de P-02: no puede estar en uso. */
    MOBILENET_V3_L("MobileNetV3-L", "63.80", "77.77", "61.67", "97.30", "26.00", "82.47", "73.08",
            new int[] {254, 7, 54, 19}, "52.10");

    private static final String INSERTAR = """
            INSERT INTO modelo (nombre, version, tipo, f1_macro, precision_macro, recall_macro, recall_sano,
                recall_no_sano, precision_sano, precision_no_sano, sanos_como_sanos, sanos_como_no_sanos,
                no_sanos_como_sanos, no_sanos_como_no_sanos, tiempo_por_grano_ms, en_uso)
            VALUES (?, '1.0', 'CNN', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""";

    private final String nombre;
    private final BigDecimal[] metricas;
    private final int[] matriz;
    private final BigDecimal tiempoPorGrano;

    /**
     * Arma un modelo con sus métricas.
     *
     * @param nombre el nombre, que va con la versión 1.0
     * @param f1Macro el F1 macro (%)
     * @param precisionMacro la precisión macro (%)
     * @param recallMacro el recall macro (%)
     * @param recallSano el recall de sano (%), el de D-39
     * @param recallNoSano el recall de no sano (%), el de D-39
     * @param precisionSano la precisión de sano (%)
     * @param precisionNoSano la precisión de no sano (%)
     * @param matriz sanos como sanos, sanos como no sanos, no sanos como sanos y no sanos como no sanos
     * @param tiempoPorGrano el tiempo por grano (ms)
     */
    ModeloDeLaComparacion(String nombre, String f1Macro, String precisionMacro, String recallMacro, String recallSano,
            String recallNoSano, String precisionSano, String precisionNoSano, int[] matriz, String tiempoPorGrano) {
        this.nombre = nombre;
        this.metricas = new BigDecimal[] {new BigDecimal(f1Macro), new BigDecimal(precisionMacro),
                new BigDecimal(recallMacro), new BigDecimal(recallSano), new BigDecimal(recallNoSano),
                new BigDecimal(precisionSano), new BigDecimal(precisionNoSano)};
        this.matriz = matriz.clone();
        this.tiempoPorGrano = new BigDecimal(tiempoPorGrano);
    }

    /**
     * El nombre del modelo, que va con la versión 1.0.
     *
     * @return el nombre
     */
    public String nombre() {
        return nombre;
    }

    /**
     * Inserta el modelo, versión 1.0, con sus 11 métricas.
     *
     * @param jdbc la conexión de la prueba
     * @param enUso si queda en uso
     * @return su identificador
     */
    public UUID insertar(JdbcTemplate jdbc, boolean enUso) {
        return jdbc.queryForObject(INSERTAR, UUID.class, nombre, metricas[0], metricas[1], metricas[2], metricas[3],
                metricas[4], metricas[5], metricas[6], matriz[0], matriz[1], matriz[2], matriz[3], tiempoPorGrano,
                enUso);
    }
}
