/*
 * Nombre: RangoRecomendacion.java
 * Descripción: Entidad JPA de la tabla rango_recomendacion.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.7)
 * Versión: 1.7
 * Entidades: rango_recomendacion 1.2
 */
package co.edu.usco.inspectia.rango;

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
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.generator.EventType;

/**
 * Rango de una categoría probable: desde qué porcentaje de granos sanos aplica y qué se recomienda
 * (D-10, anexo A.3). Cada empresa tiene siempre las 4 categorías; solo cambian sus límites y sus textos
 * (RF-13).
 *
 * <p>Tabla {@code rango_recomendacion} (RF-13, RF-02, RF-12, D-10). Las fechas las pone la base: la de creación al
 * insertar y la de actualización con un trigger en cada cambio.
 */
@Entity
@Table(name = "rango_recomendacion")
@Getter
@Setter
public class RangoRecomendacion {

    /** Identificador (UUID v7): lo genera Hibernate al guardar; la base tiene uuidv7() para los scripts. */
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Empresa dueña del registro (RNF-03). */
    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    /** Categoría (RF-12, RF-13). */
    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 30)
    private RangoRecomendacionCategoria categoria;

    /** Límite inferior (% de granos sanos) (RF-12, RF-13). */
    @Column(name = "limite_inferior", nullable = false, precision = 4, scale = 1)
    private BigDecimal limiteInferior;

    /** Recomendación (RF-12, RF-13). */
    @Column(name = "recomendacion", nullable = false, length = 300)
    private String recomendacion;

    /** Fecha de creación del registro: la pone la base al insertar. */
    @Generated
    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    /** Fecha de la última actualización: la mantiene un trigger de la base. */
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;
}
