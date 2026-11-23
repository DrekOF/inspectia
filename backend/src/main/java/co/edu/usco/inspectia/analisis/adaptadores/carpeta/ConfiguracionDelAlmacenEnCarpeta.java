/*
 * Nombre: ConfiguracionDelAlmacenEnCarpeta.java
 * Descripción: Arma el almacén de las fotos en una carpeta del servidor, con su clave al azar para firmar los enlaces,
 *              solo si el almacén es la carpeta (D-41).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.carpeta;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El almacén de las fotos en una carpeta (D-41). Solo se carga con el almacén en carpeta, el de siempre por ahora
 * ({@code inspectia.archivos.almacen=carpeta} o sin la propiedad). Con S3, que llega con el despliegue, no se crean ni
 * la carpeta, ni la clave, ni las rutas firmadas de {@link ArchivosControlador}.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "inspectia.archivos", name = "almacen", havingValue = "carpeta",
        matchIfMissing = true)
@EnableConfigurationProperties(PropiedadesDeArchivos.class)
public class ConfiguracionDelAlmacenEnCarpeta {

    /**
     * La firma de los enlaces, con una clave al azar que solo vive en la memoria del servidor (D-41).
     *
     * @param reloj el reloj de la aplicación
     * @return la firma
     */
    @Bean
    FirmaDeEnlaces firmaDeEnlaces(Clock reloj) {
        return new FirmaDeEnlaces(FirmaDeEnlaces.claveAlAzar(), reloj);
    }

    /**
     * El almacén, en la carpeta de las fotos. La crea si no existe.
     *
     * @param propiedades la carpeta de las fotos
     * @param firma la firma de los enlaces
     * @param reloj el reloj de la aplicación
     * @return el almacén
     */
    @Bean
    AlmacenEnCarpeta almacenEnCarpeta(PropiedadesDeArchivos propiedades, FirmaDeEnlaces firma, Clock reloj) {
        Path carpeta = propiedades.carpetaDeFotos();
        try {
            Files.createDirectories(carpeta);
        } catch (IOException excepcion) {
            throw new UncheckedIOException("No se pudo crear la carpeta de las fotos " + carpeta + ": revisa "
                    + "INSPECTIA_FOTOS_CARPETA en infra/.env, como dice infra/.env.example", excepcion);
        }
        log.info("Las fotos de las muestras se guardan en {}", carpeta);
        return new AlmacenEnCarpeta(carpeta, firma, reloj);
    }
}
