/*
 * Nombre: ModeloEntidad.java
 * Descripción: Entidad JPA de la tabla modelo (specs/entidades/modelo.json), de solo lectura, con los recall de cada
 *              clase con que se corrige el porcentaje del lote (RF-08, RF-17, D-18, D-39).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * La tabla {@code modelo}, común a todas las empresas (RNF-03). Su esquema lo genera {@code generador/} desde
 * {@code specs/entidades/modelo.json} (V1). Es de solo lectura ({@link Immutable}): las filas las carga el script de la
 * base (RP-15) o, en desarrollo, el clasificador falso con un {@code INSERT … ON CONFLICT DO NOTHING}
 * ({@link ModelosJpa#registrarSiFalta}). Una versión nueva de un modelo es una fila nueva: así las muestras ya
 * analizadas conservan los recall con que se corrigieron (RF-10 C13).
 */
@Entity
@Table(name = "modelo")
@Immutable
@Getter
public class ModeloEntidad {

    /** Identificador del modelo. */
    @Id
    private UUID id;

    /** Nombre, como {@code EfficientNet-B0} (RF-08, RF-17). */
    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    /** Versión, como {@code 1.0} (RF-08, RF-17). */
    @Column(name = "version", nullable = false, length = 20)
    private String version;

    /** Tipo: CNN o TRANSFORMER; vacío en el clasificador falso (D-18, RP-25). */
    @Column(name = "tipo", length = 30)
    private String tipo;

    /** F1-score macro, en porcentaje (RF-17, RNF-07). */
    @Column(name = "f1_macro", precision = 5, scale = 2)
    private BigDecimal f1Macro;

    /** Precisión macro, en porcentaje (RF-17, RNF-07). */
    @Column(name = "precision_macro", precision = 5, scale = 2)
    private BigDecimal precisionMacro;

    /** Recall macro, en porcentaje (RF-17, RNF-07). */
    @Column(name = "recall_macro", precision = 5, scale = 2)
    private BigDecimal recallMacro;

    /** Recall de la clase sano, en porcentaje (RF-10, D-39). */
    @Column(name = "recall_sano", nullable = false, precision = 5, scale = 2)
    private BigDecimal recallSano;

    /** Recall de la clase no sano, en porcentaje (RF-10, D-39). */
    @Column(name = "recall_no_sano", nullable = false, precision = 5, scale = 2)
    private BigDecimal recallNoSano;

    /** Tiempo por grano, en milisegundos (D-18). */
    @Column(name = "tiempo_por_grano_ms", precision = 8, scale = 2)
    private BigDecimal tiempoPorGranoMs;

    /** Si es el modelo en uso (RF-17, D-18). */
    @Column(name = "en_uso", nullable = false)
    private Boolean enUso;

    /** Fecha de creación de la fila. */
    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    /** Fecha de la última actualización de la fila. */
    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;
}
