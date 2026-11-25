/*
 * Nombre: ModeloComparado.java
 * Descripción: Entidad JPA de solo lectura de la tabla modelo, con todas las métricas de la comparación de D-18 y
 *              D-48, para la pantalla «Modelo de IA» (RF-17).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.estadistica.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * Un modelo de la tabla {@code modelo} (specs/entidades/modelo.json), común a todas las empresas (RNF-03), con las
 * métricas del conjunto de prueba que guarda la comparación de D-18: tipo, F1, precisión y recall macro, precisión y
 * recall de cada clase, la matriz de confusión y el tiempo por grano (D-48). El modelo del clasificador falso de
 * desarrollo solo tiene sus recall; las demás quedan vacías (CHECK {@code modelo_datos_completos}).
 *
 * <p>Es de solo lectura ({@link Immutable}): el módulo {@code analisis} registra las filas. Tiene su propio nombre de
 * entidad para no chocar con la entidad del análisis sobre la misma tabla, que solo lee los recall.
 */
@Entity(name = "ModeloComparado")
@Table(name = "modelo")
@Immutable
@Getter
public class ModeloComparado {

    /** Identificador del modelo. */
    @Id
    private UUID id;

    /** Nombre, como {@code EfficientNet-B0}. */
    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    /** Versión, como {@code 1.0}. */
    @Column(name = "version", nullable = false, length = 20)
    private String version;

    /** Tipo: CNN o TRANSFORMER (RP-25); vacío en el clasificador falso. */
    @Column(name = "tipo", length = 30)
    private String tipo;

    /** F1-score macro, en porcentaje (RNF-07). */
    @Column(name = "f1_macro", precision = 5, scale = 2)
    private BigDecimal f1Macro;

    /** Precisión macro, en porcentaje. */
    @Column(name = "precision_macro", precision = 5, scale = 2)
    private BigDecimal precisionMacro;

    /** Recall macro, en porcentaje. */
    @Column(name = "recall_macro", precision = 5, scale = 2)
    private BigDecimal recallMacro;

    /** Recall de la clase sano, en porcentaje (D-39). */
    @Column(name = "recall_sano", nullable = false, precision = 5, scale = 2)
    private BigDecimal recallSano;

    /** Recall de la clase no sano, en porcentaje (D-39). */
    @Column(name = "recall_no_sano", nullable = false, precision = 5, scale = 2)
    private BigDecimal recallNoSano;

    /** Precisión de la clase sano, en porcentaje (D-48). */
    @Column(name = "precision_sano", precision = 5, scale = 2)
    private BigDecimal precisionSano;

    /** Precisión de la clase no sano, en porcentaje (D-48). */
    @Column(name = "precision_no_sano", precision = 5, scale = 2)
    private BigDecimal precisionNoSano;

    /** Granos sanos del conjunto de prueba que el modelo clasificó como sanos (D-48). */
    @Column(name = "sanos_como_sanos")
    private Integer sanosComoSanos;

    /** Granos sanos que clasificó como no sanos (D-48). */
    @Column(name = "sanos_como_no_sanos")
    private Integer sanosComoNoSanos;

    /** Granos no sanos que clasificó como sanos (D-48). */
    @Column(name = "no_sanos_como_sanos")
    private Integer noSanosComoSanos;

    /** Granos no sanos que clasificó como no sanos (D-48). */
    @Column(name = "no_sanos_como_no_sanos")
    private Integer noSanosComoNoSanos;

    /** Tiempo por grano, en milisegundos (D-18). */
    @Column(name = "tiempo_por_grano_ms", precision = 8, scale = 2)
    private BigDecimal tiempoPorGranoMs;

    /** Si es el modelo en uso (D-18); solo puede haber uno. */
    @Column(name = "en_uso", nullable = false)
    private Boolean enUso;
}
