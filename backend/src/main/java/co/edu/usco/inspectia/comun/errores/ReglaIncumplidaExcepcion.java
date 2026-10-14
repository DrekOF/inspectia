/*
 * Nombre: ReglaIncumplidaExcepcion.java
 * Descripción: Una regla de una entidad no se cumple; la API responde 400.
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.errores;

import java.util.List;

/**
 * Una regla de las que se programan a mano no se cumple, como el dígito de verificación del NIT
 * ({@code reglas} de {@code specs/entidades}). La API responde 400.
 */
public class ReglaIncumplidaExcepcion extends ExcepcionConCampos {

    /**
     * Crea la excepción.
     *
     * @param campos los datos que incumplen alguna regla
     */
    public ReglaIncumplidaExcepcion(List<CampoInvalido> campos) {
        super(campos);
    }
}
