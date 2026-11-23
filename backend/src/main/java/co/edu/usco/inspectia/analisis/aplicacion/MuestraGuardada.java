/*
 * Nombre: MuestraGuardada.java
 * Descripción: Una muestra de un lote como se consulta en el resultado: su estado, quién la analizó, su modelo, sus
 *              conteos, sus parámetros y sus fechas (RF-10 C6, RF-14 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeMuestra;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Una muestra de un lote, para el resultado (RF-14 C3). Mientras no termina, el modelo, los conteos, los parámetros y
 * la fecha de fin van en {@code null}.
 *
 * @param id la muestra
 * @param numero su número: 1, o 2 para la segunda
 * @param estado su estado
 * @param usuarioId quien la envió a analizar («Analizada por»)
 * @param usuarioNombre su nombre
 * @param modeloNombre el nombre del modelo que la analizó, o {@code null}
 * @param modeloVersion su versión, o {@code null}
 * @param totalGranos su total de granos, o {@code null}
 * @param granosSanos sus granos sanos, o {@code null}
 * @param umbralConfianzaBaja la copia del umbral de confianza baja, o {@code null}
 * @param porcentajeMaxDudosos la copia del máximo de dudosos, o {@code null}
 * @param fechaEnvio cuándo se envió, o {@code null}
 * @param fechaFin cuándo terminó, o {@code null}
 */
public record MuestraGuardada(UUID id, int numero, EstadoDeMuestra estado, UUID usuarioId, String usuarioNombre,
        String modeloNombre, String modeloVersion, Integer totalGranos, Integer granosSanos,
        BigDecimal umbralConfianzaBaja, BigDecimal porcentajeMaxDudosos, Instant fechaEnvio, Instant fechaFin) {
}
