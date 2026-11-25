/*
 * Nombre: PropiedadesDelServicioDeIa.java
 * Descripción: Las propiedades del cliente del servicio de IA: su dirección y los tiempos límite de la revisión rápida,
 *              del análisis y de la conexión (RF-06, RF-08, RNF-01, D-46).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.ia;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Las propiedades del servicio de IA, en {@code application.yml} bajo {@code inspectia.ia}. Solo se cargan con
 * {@code inspectia.ia.clasificador=servicio}; entonces la dirección es obligatoria: sin ella, el servidor no arranca.
 *
 * @param url la dirección del servicio, como {@code http://ia-servicio:8000} (variable {@code INSPECTIA_IA_URL})
 * @param tiempos los tiempos límite
 */
@ConfigurationProperties("inspectia.ia")
@Validated
public record PropiedadesDelServicioDeIa(@NotNull URI url, @Valid @DefaultValue Tiempos tiempos) {

    /**
     * Los tiempos límite hacia el servicio (contrato común del despliegue, sección 1).
     *
     * @param conexion cuánto espera para conectarse; 5 s sin la propiedad
     * @param revisar cuánto espera la revisión rápida de una foto (D-46); 10 s sin la propiedad
     * @param analizar cuánto espera el análisis de una muestra (RNF-01); 60 s sin la propiedad
     * @param reintentoDeModelos cada cuánto vuelve a pedir los modelos si al arrancar el servicio no respondió; 15 s
     */
    public record Tiempos(@DefaultValue("5s") Duration conexion, @DefaultValue("10s") Duration revisar,
            @DefaultValue("60s") Duration analizar, @DefaultValue("15s") Duration reintentoDeModelos) {

        /**
         * Revisa que todos sean mayores que 0.
         *
         * @throws IllegalArgumentException si alguno no lo es
         */
        public Tiempos {
            for (Duration tiempo : new Duration[] {conexion, revisar, analizar, reintentoDeModelos}) {
                if (tiempo == null || tiempo.isZero() || tiempo.isNegative()) {
                    throw new IllegalArgumentException("Los tiempos de inspectia.ia.tiempos deben ser mayores que 0");
                }
            }
        }
    }
}
