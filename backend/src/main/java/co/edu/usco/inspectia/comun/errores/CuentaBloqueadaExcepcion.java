/*
 * Nombre: CuentaBloqueadaExcepcion.java
 * Descripción: La cuenta del usuario está bloqueada por intentos fallidos; al cambiar la propia
 *              contraseña, la API responde 429 con la hora en que termina el bloqueo (RF-18, RF-01).
 * Fecha de creación: 19/10/2026
 * Actualización: 19/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.errores;

import java.time.Instant;

/**
 * La cuenta del usuario de la sesión está bloqueada por intentos fallidos seguidos (RF-01), así que
 * tampoco puede cambiar su contraseña hasta que termine el bloqueo (RF-18 C2). {@link ManejadorDeErrores}
 * responde 429 con {@code bloqueadoHasta} y el encabezado {@code Retry-After}. Al dueño de la sesión sí
 * se le dice que su cuenta está bloqueada: a diferencia del inicio de sesión, ya sabe que es suya.
 */
public class CuentaBloqueadaExcepcion extends RuntimeException {

    private final Instant bloqueadoHasta;
    private final long segundosRestantes;

    /**
     * Crea la excepción.
     *
     * @param bloqueadoHasta cuándo termina el bloqueo
     * @param segundosRestantes cuántos segundos faltan para que termine, redondeados hacia arriba
     */
    public CuentaBloqueadaExcepcion(Instant bloqueadoHasta, long segundosRestantes) {
        super("Cuenta bloqueada hasta " + bloqueadoHasta);
        this.bloqueadoHasta = bloqueadoHasta;
        this.segundosRestantes = segundosRestantes;
    }

    /**
     * Cuándo termina el bloqueo.
     *
     * @return la hora
     */
    public Instant getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    /**
     * Cuántos segundos faltan para que termine el bloqueo.
     *
     * @return los segundos, para el encabezado {@code Retry-After}
     */
    public long getSegundosRestantes() {
        return segundosRestantes;
    }
}
