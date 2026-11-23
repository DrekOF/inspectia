/*
 * Nombre: MuestraEntidad.java
 * Descripción: Entidad JPA de la tabla muestra (specs/entidades/muestra.json), del módulo de análisis (RF-05, RF-10,
 *              RNF-02, D-25).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeMuestra;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.generator.EventType;

/**
 * La tabla {@code muestra}. Su esquema lo genera {@code generador/} desde {@code specs/entidades/muestra.json} (V1);
 * esta entidad se escribe a mano, como {@link LoteEntidad}, porque la muestra es del módulo de análisis (D-21).
 * Hibernate valida al arrancar que coincida con la tabla. Pasa de pendiente, mientras se toman las fotos, a «en
 * espera» cuando se envía, a «en proceso» cuando la toma la cola ({@link ColaEnLaBase}) y a terminada con sus conteos,
 * su modelo y la copia de los parámetros (RF-10 C6); terminarla o devolverla a pendiente son {@code UPDATE} con una
 * condición sobre su estado ({@link MuestrasJpa}).
 */
@Entity
@Table(name = "muestra")
@DynamicUpdate
@Getter
@Setter
public class MuestraEntidad {

    /** Identificador de la muestra: UUID v7, que genera Hibernate al guardar. */
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    /** La empresa de la muestra (RNF-03). */
    @Column(name = "empresa_id", nullable = false, updatable = false)
    private UUID empresaId;

    /** El lote de la muestra. */
    @Column(name = "lote_id", nullable = false, updatable = false)
    private UUID loteId;

    /** Número de la muestra: 1, o 2 para la segunda (D-25). */
    @Column(name = "numero", nullable = false)
    private Integer numero;

    /** Quien tomó la muestra (RF-14, RF-15). */
    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    /** Estado de la muestra; pendiente mientras se toman las fotos. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoDeMuestra estado;

    /** El modelo de IA que la analizó (RF-08). */
    @Column(name = "modelo_id")
    private UUID modeloId;

    /** Total de granos (RF-07, RF-10). */
    @Column(name = "total_granos")
    private Integer totalGranos;

    /** Granos sanos (RF-10). */
    @Column(name = "granos_sanos")
    private Integer granosSanos;

    /** Aviso de confianza baja (RF-09). */
    @Column(name = "aviso_confianza_baja")
    private Boolean avisoConfianzaBaja;

    /** Repeticiones de foto que pidió el análisis, de 0 a 2 (RF-09). */
    @Column(name = "repeticiones", nullable = false)
    private Integer repeticiones = 0;

    /** Umbral de confianza baja que usó (RF-09, P-15). */
    @Column(name = "umbral_confianza_baja", precision = 5, scale = 2)
    private BigDecimal umbralConfianzaBaja;

    /** Máximo de granos dudosos que usó (RF-09, P-15). */
    @Column(name = "porcentaje_max_dudosos", precision = 4, scale = 1)
    private BigDecimal porcentajeMaxDudosos;

    /** Cuándo se enviaron las fotos (RNF-01, RNF-02). */
    @Column(name = "fecha_envio")
    private OffsetDateTime fechaEnvio;

    /** Cuándo empezó el análisis (RNF-02). */
    @Column(name = "fecha_inicio")
    private OffsetDateTime fechaInicio;

    /** Cuándo terminó el análisis (RNF-01). */
    @Column(name = "fecha_fin")
    private OffsetDateTime fechaFin;

    /** Fecha de creación: la pone la base al insertar. */
    @Generated
    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    /** Fecha de la última actualización: la mantiene un trigger de la base. */
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;
}
