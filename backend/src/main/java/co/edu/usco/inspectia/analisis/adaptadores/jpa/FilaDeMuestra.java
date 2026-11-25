/*
 * Nombre: FilaDeMuestra.java
 * Descripción: Una muestra como la lee la consulta del resultado: con el nombre de quien la analizó y el nombre y la
 *              versión de su modelo (RF-14 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import co.edu.usco.inspectia.analisis.aplicacion.MuestraGuardada;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeMuestra;
import co.edu.usco.inspectia.analisis.dominio.MotivoDeDevolucion;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Una fila de {@link MuestrasJpa#conDetalle}, con las fechas como las lee JPA; {@link #guardada()} la pasa al caso de
 * uso.
 *
 * @param id la muestra
 * @param numero su número
 * @param estado su estado
 * @param usuarioId quien la analizó
 * @param usuarioNombre su nombre
 * @param modeloNombre el nombre de su modelo, o {@code null}
 * @param modeloVersion su versión, o {@code null}
 * @param totalGranos su total de granos, o {@code null}
 * @param granosSanos sus granos sanos, o {@code null}
 * @param umbralConfianzaBaja la copia del umbral, o {@code null}
 * @param porcentajeMaxDudosos la copia del máximo de dudosos, o {@code null}
 * @param fechaEnvio cuándo se envió, o {@code null}
 * @param fechaFin cuándo terminó, o {@code null}
 * @param motivoDevolucion por qué la cola la devolvió, o {@code null} (D-44)
 * @param repeticiones las repeticiones que pidió RF-09
 * @param avisoConfianzaBaja su aviso de confianza baja, o {@code null} mientras no termina
 */
public record FilaDeMuestra(UUID id, Integer numero, EstadoDeMuestra estado, UUID usuarioId, String usuarioNombre,
        String modeloNombre, String modeloVersion, Integer totalGranos, Integer granosSanos,
        BigDecimal umbralConfianzaBaja, BigDecimal porcentajeMaxDudosos, OffsetDateTime fechaEnvio,
        OffsetDateTime fechaFin, MotivoDeDevolucion motivoDevolucion, Integer repeticiones,
        Boolean avisoConfianzaBaja) {

    /** @return la muestra para el caso de uso */
    MuestraGuardada guardada() {
        return new MuestraGuardada(id, numero, estado, usuarioId, usuarioNombre, modeloNombre, modeloVersion,
                totalGranos, granosSanos, umbralConfianzaBaja, porcentajeMaxDudosos, instante(fechaEnvio),
                instante(fechaFin), motivoDevolucion, repeticiones == null ? 0 : repeticiones, avisoConfianzaBaja);
    }

    private static Instant instante(OffsetDateTime fecha) {
        return fecha == null ? null : fecha.toInstant();
    }
}
