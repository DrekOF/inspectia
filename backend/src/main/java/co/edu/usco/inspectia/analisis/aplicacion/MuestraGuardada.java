/*
 * Nombre: MuestraGuardada.java
 * Descripción: Una muestra de un lote como se consulta en el resultado: su estado, quién la analizó, su modelo, sus
 *              conteos, sus parámetros, sus fechas, su aviso y lo que dejó la cola si la devolvió (RF-09, RF-10 C6,
 *              RF-14 C3, D-44).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeMuestra;
import co.edu.usco.inspectia.analisis.dominio.MotivoDeDevolucion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Una muestra de un lote, para el resultado (RF-14 C3). Mientras no termina, el modelo, los conteos, el aviso y la
 * fecha de fin van en {@code null}; la copia de los parámetros también, salvo si RF-09 ya pidió repetir una foto.
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
 * @param motivoDevolucion por qué la cola la devolvió sin resultado, o {@code null} (D-44)
 * @param repeticiones las fotos que RF-09 pidió repetir, de 0 a 2
 * @param avisoConfianzaBaja si terminó con el aviso de confianza baja, o {@code null} mientras no termina (RF-09)
 */
public record MuestraGuardada(UUID id, int numero, EstadoDeMuestra estado, UUID usuarioId, String usuarioNombre,
        String modeloNombre, String modeloVersion, Integer totalGranos, Integer granosSanos,
        BigDecimal umbralConfianzaBaja, BigDecimal porcentajeMaxDudosos, Instant fechaEnvio, Instant fechaFin,
        MotivoDeDevolucion motivoDevolucion, int repeticiones, Boolean avisoConfianzaBaja) {

    /**
     * Una muestra sin lo que deja la cola al devolverla y sin aviso.
     *
     * @param id la muestra
     * @param numero su número
     * @param estado su estado
     * @param usuarioId quien la envió
     * @param usuarioNombre su nombre
     * @param modeloNombre el nombre del modelo, o {@code null}
     * @param modeloVersion su versión, o {@code null}
     * @param totalGranos su total de granos, o {@code null}
     * @param granosSanos sus granos sanos, o {@code null}
     * @param umbralConfianzaBaja la copia del umbral, o {@code null}
     * @param porcentajeMaxDudosos la copia del máximo de dudosos, o {@code null}
     * @param fechaEnvio cuándo se envió, o {@code null}
     * @param fechaFin cuándo terminó, o {@code null}
     */
    public MuestraGuardada(UUID id, int numero, EstadoDeMuestra estado, UUID usuarioId, String usuarioNombre,
            String modeloNombre, String modeloVersion, Integer totalGranos, Integer granosSanos,
            BigDecimal umbralConfianzaBaja, BigDecimal porcentajeMaxDudosos, Instant fechaEnvio, Instant fechaFin) {
        this(id, numero, estado, usuarioId, usuarioNombre, modeloNombre, modeloVersion, totalGranos, granosSanos,
                umbralConfianzaBaja, porcentajeMaxDudosos, fechaEnvio, fechaFin, null, 0, null);
    }
}
