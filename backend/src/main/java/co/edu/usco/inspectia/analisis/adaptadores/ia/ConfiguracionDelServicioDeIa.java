/*
 * Nombre: ConfiguracionDelServicioDeIa.java
 * Descripción: Carga el cliente del servicio de IA con inspectia.ia.clasificador=servicio: el clasificador de granos, la
 *              revisión rápida de las fotos y el registro de los modelos (RF-06, RF-07, RF-08, RF-17, D-40, D-46).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.ia;

import co.edu.usco.inspectia.analisis.aplicacion.RepositorioDeModelos;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El servicio de IA ({@code inspectia.ia.clasificador=servicio}): el modelo de Julián (D-40) en el contenedor
 * {@code ia-servicio}, en la misma red de Docker (D-16). Con esta propiedad, {@code inspectia.ia.url} es obligatoria;
 * con {@code falso}, nada de esto se carga.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "inspectia.ia", name = "clasificador", havingValue = "servicio")
@EnableConfigurationProperties(PropiedadesDelServicioDeIa.class)
public class ConfiguracionDelServicioDeIa {

    /**
     * El cliente HTTP del servicio.
     *
     * @param propiedades su dirección y sus tiempos
     * @return el cliente
     */
    @Bean
    ClienteDelServicioDeIa clienteDelServicioDeIa(PropiedadesDelServicioDeIa propiedades) {
        log.info("El análisis usa el servicio de IA en {} (revisión {}, análisis {})", propiedades.url(),
                propiedades.tiempos().revisar(), propiedades.tiempos().analizar());
        return new ClienteDelServicioDeIa(propiedades.url(), propiedades.tiempos());
    }

    /**
     * El registro de los modelos del servicio, que corre después de crear todos los beans.
     *
     * @param cliente el cliente del servicio
     * @param modelos la tabla {@code modelo}
     * @param propiedades cada cuánto se reintenta
     * @return el registro
     */
    @Bean
    RegistroDeModelosDelServicio registroDeModelosDelServicio(ClienteDelServicioDeIa cliente,
            RepositorioDeModelos modelos, PropiedadesDelServicioDeIa propiedades) {
        return new RegistroDeModelosDelServicio(cliente, modelos, propiedades.tiempos().reintentoDeModelos());
    }

    /**
     * El clasificador de granos con el servicio.
     *
     * @param cliente el cliente del servicio
     * @param registro el registro de sus modelos
     * @return el clasificador
     */
    @Bean
    ClasificadorDelServicio clasificadorDelServicio(ClienteDelServicioDeIa cliente,
            RegistroDeModelosDelServicio registro) {
        return new ClasificadorDelServicio(cliente, registro);
    }

    /**
     * La revisión rápida de las fotos con el servicio.
     *
     * @param cliente el cliente del servicio
     * @return el revisor
     */
    @Bean
    RevisorDelServicio revisorDelServicio(ClienteDelServicioDeIa cliente) {
        return new RevisorDelServicio(cliente);
    }
}
