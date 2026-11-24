/*
 * Nombre: GranoEntidad.java
 * Descripción: Entidad JPA de la tabla grano (specs/entidades/grano.json), del módulo de análisis (RF-07, RF-08,
 *              RF-11). Sin las fechas de la fila, para guardar los granos en lotes de inserción sin releer cada uno.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import co.edu.usco.inspectia.analisis.dominio.ClaseDeGrano;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/**
 * La tabla {@code grano}. Su esquema lo genera {@code generador/} desde {@code specs/entidades/grano.json} (V1); esta
 * entidad se escribe a mano, como {@link FotoEntidad}, porque el grano es del módulo de análisis (D-21).
 *
 * <p>No mapea {@code fecha_creacion} ni {@code fecha_actualizacion}: las pone la base. Con ellas, Hibernate volvería a
 * leer cada fila después de insertarla, unos 150 a 450 viajes a la base por muestra. Sin ellas, y con el identificador
 * UUID v7 que genera Hibernate, los granos se insertan en lotes ({@code hibernate.jdbc.batch_size} en
 * {@code application.yml}). Los granos no se cambian después de guardarlos.
 */
@Entity
@Table(name = "grano")
@Getter
@Setter
public class GranoEntidad {

    /** Identificador del grano: UUID v7, que genera Hibernate al guardar y crece en el orden en que se guardan. */
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    /** La empresa del grano (RNF-03). */
    @Column(name = "empresa_id", nullable = false, updatable = false)
    private UUID empresaId;

    /** La foto del grano; se borra con ella (RF-05). */
    @Column(name = "foto_id", nullable = false, updatable = false)
    private UUID fotoId;

    /** Posición horizontal, en píxeles de la foto original (RF-07, RF-11). */
    @Column(name = "x", nullable = false)
    private Integer x;

    /** Posición vertical, en píxeles de la foto original (RF-07, RF-11). */
    @Column(name = "y", nullable = false)
    private Integer y;

    /** Ancho, en píxeles (RF-07, RF-11). */
    @Column(name = "ancho", nullable = false)
    private Integer ancho;

    /** Alto, en píxeles (RF-07, RF-11). */
    @Column(name = "alto", nullable = false)
    private Integer alto;

    /** Clase: sano o no sano (RF-08, RF-11). */
    @Enumerated(EnumType.STRING)
    @Column(name = "clase", nullable = false, length = 30)
    private ClaseDeGrano clase;

    /** Confianza de su clase, en porcentaje con 2 decimales (RF-08, RF-09, RF-17). */
    @Column(name = "confianza", nullable = false, precision = 5, scale = 2)
    private BigDecimal confianza;
}
