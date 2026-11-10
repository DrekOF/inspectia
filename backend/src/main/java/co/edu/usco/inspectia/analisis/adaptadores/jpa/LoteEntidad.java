/*
 * Nombre: LoteEntidad.java
 * Descripción: Entidad JPA de la tabla lote (specs/entidades/lote.json), del módulo de análisis (RF-04, RF-12,
 *              RF-14).
 * Fecha de creación: 10/11/2026
 * Actualización: 10/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeLote;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.generator.EventType;

/**
 * La tabla {@code lote}. Su esquema lo genera {@code generador/} desde {@code specs/entidades/lote.json}
 * (V1); esta entidad se escribe a mano porque el lote no es un CRUD generado: sus reglas van en el dominio
 * del módulo de análisis (D-21). Hibernate valida al arrancar que coincida con la tabla.
 *
 * <p>Como las entidades generadas, lleva {@code @DynamicUpdate} (D-36) y el identificador UUID v7 lo genera
 * Hibernate al guardar.
 */
@Entity
@Table(name = "lote")
@DynamicUpdate
@Getter
@Setter
public class LoteEntidad {

    /** Identificador del lote: UUID v7, que genera Hibernate al guardar. */
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    /** La empresa del lote (RNF-03). */
    @Column(name = "empresa_id", nullable = false, updatable = false)
    private UUID empresaId;

    /** Código del lote, único en la empresa sin distinguir mayúsculas ni espacios (RF-04). */
    @Column(name = "codigo", nullable = false, length = 30)
    private String codigo;

    /** Fecha en que la empresa recibió el lote, no posterior a hoy (RF-04 C4). */
    @Column(name = "fecha_recepcion", nullable = false)
    private LocalDate fechaRecepcion;

    /** Finca o proveedor (RF-04, RF-16). */
    @Column(name = "origen", nullable = false, length = 120)
    private String origen;

    /** Observaciones; vacías si no tiene. */
    @Column(name = "observaciones", length = 500)
    private String observaciones;

    /** Pendiente de análisis o analizado. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoDeLote estado;

    /** El usuario que registró el lote. */
    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    /** Porcentaje de granos sanos, con 1 decimal; vacío mientras está pendiente (RF-10). */
    @Column(name = "porcentaje_sanos", precision = 4, scale = 1)
    private BigDecimal porcentajeSanos;

    /** Categoría probable; vacía mientras está pendiente (RF-12). */
    @Column(name = "categoria", length = 30)
    private String categoria;

    /** Copia del texto de la recomendación; vacía mientras está pendiente (RF-12). */
    @Column(name = "recomendacion", length = 300)
    private String recomendacion;

    /** Si se sugiere analizar otra muestra; vacío mientras está pendiente (D-24, D-25). */
    @Column(name = "sugerir_otra_muestra")
    private Boolean sugerirOtraMuestra;

    /** Si alguna muestra tiene el aviso de confianza baja; vacío mientras está pendiente (RF-09). */
    @Column(name = "aviso_confianza_baja")
    private Boolean avisoConfianzaBaja;

    /** Fecha del resultado; vacía mientras está pendiente (RF-14). */
    @Column(name = "fecha_resultado")
    private OffsetDateTime fechaResultado;

    /** Llave del PDF del lote en el almacenamiento; vacía hasta la primera descarga (RF-15). */
    @Column(name = "reporte_ruta", length = 255)
    private String reporteRuta;

    /** Fecha de registro del lote: la pone la base al insertar. */
    @Generated
    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    /** Fecha de la última actualización: la mantiene un trigger de la base. */
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;
}
