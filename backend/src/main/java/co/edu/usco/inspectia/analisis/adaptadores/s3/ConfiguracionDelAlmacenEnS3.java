/*
 * Nombre: ConfiguracionDelAlmacenEnS3.java
 * Descripción: Arma el almacén de los archivos en S3 (D-16), con el cliente y el firmador del AWS SDK en la región de
 *              AWS_REGION y las credenciales del rol de la instancia, solo si el almacén es S3
 *              (inspectia.archivos.almacen=s3).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.s3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * El almacén en S3 (D-16). Solo se carga con {@code inspectia.archivos.almacen=s3}; entonces no existen la carpeta, su
 * clave ni sus rutas firmadas (D-41). Las credenciales salen de la cadena por defecto del SDK: en la instancia, el rol
 * {@code inspectia-ec2} por IMDSv2; nunca de un archivo del repositorio. Crear el cliente y el firmador no se conecta a
 * AWS: las credenciales se piden con la primera llamada.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "inspectia.archivos", name = "almacen", havingValue = "s3")
@EnableConfigurationProperties(PropiedadesDelAlmacenEnS3.class)
public class ConfiguracionDelAlmacenEnS3 {

    /**
     * Las credenciales de AWS: las del rol de la instancia.
     *
     * @return la cadena por defecto del SDK
     */
    @Bean(destroyMethod = "close")
    DefaultCredentialsProvider credencialesDeAws() {
        return DefaultCredentialsProvider.builder().build();
    }

    /**
     * El cliente de S3, para revisar, leer, guardar y borrar los archivos.
     *
     * @param propiedades el bucket y su región
     * @param credenciales las credenciales
     * @return el cliente
     */
    @Bean(destroyMethod = "close")
    S3Client clienteDeS3(PropiedadesDelAlmacenEnS3 propiedades, AwsCredentialsProvider credenciales) {
        return S3Client.builder()
                .region(propiedades.regionRevisada())
                .credentialsProvider(credenciales)
                .build();
    }

    /**
     * El firmador de los enlaces prefirmados de subida y de vista.
     *
     * @param propiedades el bucket y su región
     * @param credenciales las credenciales
     * @return el firmador
     */
    @Bean(destroyMethod = "close")
    S3Presigner firmadorDeS3(PropiedadesDelAlmacenEnS3 propiedades, AwsCredentialsProvider credenciales) {
        return S3Presigner.builder()
                .region(propiedades.regionRevisada())
                .credentialsProvider(credenciales)
                .build();
    }

    /**
     * El almacén, en el bucket de los archivos.
     *
     * @param propiedades el bucket y su región
     * @param cliente el cliente de S3
     * @param firmador el firmador de los enlaces
     * @return el almacén
     */
    @Bean
    AlmacenEnS3 almacenEnS3(PropiedadesDelAlmacenEnS3 propiedades, S3Client cliente, S3Presigner firmador) {
        String bucket = propiedades.bucketRevisado();
        Region region = propiedades.regionRevisada();
        log.info("Los archivos se guardan en S3: s3://{}/{} y s3://{}/reportes/, en {}", bucket,
                AlmacenEnS3.PREFIJO_DE_FOTOS, bucket, region);
        return new AlmacenEnS3(cliente, firmador, bucket);
    }
}
