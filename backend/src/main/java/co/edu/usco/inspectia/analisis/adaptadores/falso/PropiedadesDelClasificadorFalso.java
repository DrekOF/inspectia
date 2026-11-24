/*
 * Nombre: PropiedadesDelClasificadorFalso.java
 * Descripción: Propiedad inspectia.ia.falso.demora: cuánto espera el clasificador falso antes de responder, para ver el
 *              indicador de avance en desarrollo (RNF-01 C4).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.falso;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Las propiedades del clasificador falso.
 *
 * @param demora cuánto espera antes de responder; 0, sin la propiedad. No puede ser negativa
 */
@ConfigurationProperties("inspectia.ia.falso")
public record PropiedadesDelClasificadorFalso(@DefaultValue("0s") Duration demora) {

    /**
     * Revisa la demora.
     *
     * @throws IllegalArgumentException si es negativa
     */
    public PropiedadesDelClasificadorFalso {
        if (demora == null) {
            demora = Duration.ZERO;
        }
        if (demora.isNegative()) {
            throw new IllegalArgumentException("inspectia.ia.falso.demora no puede ser negativa, y es " + demora);
        }
    }
}
