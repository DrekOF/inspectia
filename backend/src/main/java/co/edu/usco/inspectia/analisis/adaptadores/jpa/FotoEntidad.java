/*
 * Nombre: FotoEntidad.java
 * Descripción: Entidad JPA de la tabla foto (specs/entidades/foto.json), del módulo de análisis (RF-05, RF-06, D-46).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.FormatoDeFoto;
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
 * La tabla {@code foto}. Su esquema lo genera {@code generador/} desde {@code specs/entidades/foto.json} (V1); esta
 * entidad se escribe a mano, como {@link LoteEntidad}, porque la foto es del módulo de análisis (D-21). Hibernate
 * valida al arrancar que coincida con la tabla.
 */
@Entity
@Table(name = "foto")
@DynamicUpdate
@Getter
@Setter
public class FotoEntidad {

    /** Identificador de la foto: UUID v7, que genera Hibernate al guardar. */
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    /** La empresa de la foto (RNF-03). */
    @Column(name = "empresa_id", nullable = false, updatable = false)
    private UUID empresaId;

    /** La muestra de la foto. */
    @Column(name = "muestra_id", nullable = false, updatable = false)
    private UUID muestraId;

    /** Número de la foto en la muestra, de 1 a 3 (RF-05 C2). */
    @Column(name = "numero", nullable = false)
    private Integer numero;

    /** Llave del archivo en el almacén: S3 o, en local, la carpeta (D-41). */
    @Column(name = "ruta", nullable = false, length = 255)
    private String ruta;

    /** Formato: JPG o PNG (RF-05 C3). */
    @Enumerated(EnumType.STRING)
    @Column(name = "formato", nullable = false, length = 30)
    private FormatoDeFoto formato;

    /** Cuánto pesa, hasta 10 MB (RF-05 C3). */
    @Column(name = "tamano_bytes", nullable = false)
    private Integer tamanoBytes;

    /** Estado: cargada hasta que la revise el servicio de IA (RF-06). */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoDeFoto estado;

    /** Motivo para repetirla; solo si está por repetir (RF-06, RF-07, RF-09). */
    @Column(name = "motivo", length = 30)
    private String motivo;

    /** Los granos válidos que encontró su revisión; vacío mientras está cargada (RF-06, RF-07, D-46). */
    @Column(name = "granos_encontrados")
    private Integer granosEncontrados;

    /** Fecha de creación: la pone la base al insertar. */
    @Generated
    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    /** Fecha de la última actualización: la mantiene un trigger de la base. */
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;
}
