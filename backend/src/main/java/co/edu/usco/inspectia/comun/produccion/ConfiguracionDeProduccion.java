/*
 * Nombre: ConfiguracionDeProduccion.java
 * Descripción: Lo que agrega el perfil produccion (AWS, D-16) además de application-produccion.yml: la comprobación
 *              del almacén y del clasificador al arrancar y el filtro de los encabezados de CloudFront.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.produccion;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;

/**
 * El perfil {@code produccion} (D-16), el del docker-compose de AWS. Fuera de él, como en el PC de desarrollo, las
 * pruebas y la CI, nada de esto se carga.
 */
@Configuration(proxyBeanMethods = false)
@Profile("produccion")
public class ConfiguracionDeProduccion {

    /**
     * Revisa el almacén y el clasificador antes de crear los demás beans. Es {@code static} porque Spring lo corre
     * antes de crear esta configuración.
     *
     * @return la comprobación
     */
    @Bean
    static ComprobacionDeProduccion comprobacionDeProduccion() {
        return new ComprobacionDeProduccion();
    }

    /**
     * El filtro de los encabezados de CloudFront, antes que todos los demás, incluida la seguridad.
     *
     * @return el registro del filtro
     */
    @Bean
    FilterRegistrationBean<EncabezadosDeCloudFront> encabezadosDeCloudFront() {
        FilterRegistrationBean<EncabezadosDeCloudFront> registro =
                new FilterRegistrationBean<>(new EncabezadosDeCloudFront());
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registro;
    }
}
