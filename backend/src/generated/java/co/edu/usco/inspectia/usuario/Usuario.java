/*
 * Nombre: Usuario.java
 * Descripción: Entidad JPA de la tabla usuario.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.10)
 * Versión: 1.10
 * Entidades: usuario 1.8
 */
package co.edu.usco.inspectia.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Persona que usa InspectIA, con su rol. Los usuarios no se borran: se desactivan, para no perder quién
 * hizo cada análisis (RF-03). Solo el administrador de la plataforma no tiene empresa (RNF-03).
 *
 * <p>Tabla {@code usuario} (RF-03, RF-01, RF-02, RNF-03, RNF-04). Las fechas las pone la base: la de creación al
 * insertar y la de actualización con un trigger en cada cambio. Cada UPDATE escribe solo las columnas que
 * cambiaron ({@code @DynamicUpdate}): así dos cambios a la vez sobre columnas distintas no se pisan.
 */
@Entity
@Table(name = "usuario")
@DynamicUpdate
@Getter
@Setter
public class Usuario {

    /** Identificador (UUID v7): lo genera Hibernate al guardar; la base tiene uuidv7() para los scripts. */
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Empresa dueña del registro (RNF-03). */
    @Column(name = "empresa_id")
    private UUID empresaId;

    /** Nombre (RF-02, RF-03). */
    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    /** Correo (RF-01, RF-02, RF-03). */
    @Column(name = "correo", nullable = false, length = 254)
    private String correo;

    /** Contraseña (RF-01, RF-02, RF-03, RNF-04). */
    @Column(name = "contrasena_hash", nullable = false, length = 100)
    private String contrasenaHash;

    /** Rol (RF-01, RF-03, D-05, D-13). */
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 30)
    private UsuarioRol rol;

    /** Activo (RF-01, RF-03). */
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    /** Intentos fallidos (RF-01, RNF-04). */
    @Column(name = "intentos_fallidos", nullable = false)
    private Integer intentosFallidos = 0;

    /** Bloqueado hasta (RF-01, RNF-04). */
    @Column(name = "bloqueado_hasta")
    private OffsetDateTime bloqueadoHasta;

    /** Versión de sesión (RNF-04, D-30, D-35). Solo lo cambian las reglas: JPA no lo escribe. */
    @Column(name = "version_sesion", nullable = false, insertable = false, updatable = false)
    private Integer versionSesion = 0;

    /** Fecha de creación del registro: la pone la base al insertar. */
    @Generated
    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    /** Fecha de la última actualización: la mantiene un trigger de la base. */
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;
}
