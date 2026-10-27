/*
 * Nombre: Empresa.java
 * Descripción: Entidad JPA de la tabla empresa.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.8)
 * Versión: 1.8
 * Entidades: empresa 1.3
 */
package co.edu.usco.inspectia.empresa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.generator.EventType;

/**
 * Empresa cliente de InspectIA. Sus usuarios, rangos, lotes y resultados están separados de los de las
 * demás empresas (RNF-03). No se borra: se desactiva (D-27).
 *
 * <p>Tabla {@code empresa} (RF-02, RNF-03, D-13, D-27). Las fechas las pone la base: la de creación al
 * insertar y la de actualización con un trigger en cada cambio. Cada UPDATE escribe solo las columnas que
 * cambiaron ({@code @DynamicUpdate}): así dos cambios a la vez sobre columnas distintas no se pisan.
 */
@Entity
@Table(name = "empresa")
@DynamicUpdate
@Getter
@Setter
public class Empresa {

    /** Identificador (UUID v7): lo genera Hibernate al guardar; la base tiene uuidv7() para los scripts. */
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Nombre (RF-02). */
    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    /** NIT (RF-02, D-27). */
    @Column(name = "nit", nullable = false, length = 15)
    private String nit;

    /** Ciudad (RF-02). */
    @Column(name = "ciudad", nullable = false, length = 80)
    private String ciudad;

    /** Activa (D-27, RF-01). */
    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    /** Fecha de creación del registro: la pone la base al insertar. */
    @Generated
    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    /** Fecha de la última actualización: la mantiene un trigger de la base. */
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;
}
