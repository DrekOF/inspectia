/*
 * Nombre: ConfiguracionDelAnalisis.java
 * Descripción: Lee y valida al arrancar los parámetros del análisis de application.yml y se los da a los casos de uso
 *              (P-15, P-19, D-24, RNF-02 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.configuracion;

import co.edu.usco.inspectia.analisis.dominio.ParametrosDelAnalisis;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Los parámetros del análisis ({@link PropiedadesDelAnalisis}). Si alguno falta o no sirve, el servidor no arranca.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PropiedadesDelAnalisis.class)
public class ConfiguracionDelAnalisis {

    /**
     * Los parámetros del análisis para los casos de uso.
     *
     * @param propiedades las propiedades {@code inspectia.analisis}
     * @return los parámetros, ya revisados por el dominio
     */
    @Bean
    ParametrosDelAnalisis parametrosDelAnalisis(PropiedadesDelAnalisis propiedades) {
        ParametrosDelAnalisis parametros = propiedades.parametros();
        log.info("Parámetros del análisis: umbral de confianza baja {} %, máximo de dudosos {} %, factor del margen {}, "
                + "otra muestra a menos de {} puntos de un límite", parametros.umbralConfianzaBaja(),
                parametros.porcentajeMaxDudosos(), parametros.factorDelMargen(), parametros.puntosParaOtraMuestra());
        return parametros;
    }
}
