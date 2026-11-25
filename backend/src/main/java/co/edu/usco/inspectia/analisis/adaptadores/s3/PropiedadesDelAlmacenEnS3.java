/*
 * Nombre: PropiedadesDelAlmacenEnS3.java
 * Descripción: Propiedades inspectia.archivos.bucket e inspectia.archivos.region del almacén en S3 (D-16): el bucket de
 *              los archivos y su región, de INSPECTIA_ARCHIVOS_BUCKET y AWS_REGION. Sin ellas, o con un nombre que S3
 *              no acepta, el servidor no arranca.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.s3;

import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import software.amazon.awssdk.regions.Region;

/**
 * El bucket de los archivos y su región (D-16). Solo se leen con el almacén en S3.
 *
 * @param bucket el bucket, de la variable de entorno {@code INSPECTIA_ARCHIVOS_BUCKET}, como
 *               {@code inspectia-archivos-<cuenta>}
 * @param region la región, de la variable de entorno {@code AWS_REGION}, como {@code us-east-1}
 */
@ConfigurationProperties("inspectia.archivos")
public record PropiedadesDelAlmacenEnS3(String bucket, String region) {

    /** Las reglas de S3 para el nombre de un bucket, sin puntos: así el enlace prefirmado sirve con HTTPS. */
    private static final Pattern BUCKET = Pattern.compile("^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$");

    /** Las regiones, como {@code us-east-1}. */
    private static final Pattern REGION = Pattern.compile("^[a-z]{2}(-[a-z]+)+-\\d$");

    /**
     * El bucket, revisado.
     *
     * @return el nombre del bucket
     * @throws IllegalStateException si falta o no es un nombre de bucket
     */
    public String bucketRevisado() {
        if (bucket == null || !BUCKET.matcher(bucket.strip()).matches()) {
            throw new IllegalStateException("Con inspectia.archivos.almacen=s3 hay que decir el bucket de los archivos "
                    + "en INSPECTIA_ARCHIVOS_BUCKET, como inspectia-archivos-<cuenta> (minúsculas, números y guiones), y "
                    + "«" + bucket + "» no lo es.");
        }
        return bucket.strip();
    }

    /**
     * La región, revisada.
     *
     * @return la región de AWS
     * @throws IllegalStateException si falta o no es una región
     */
    public Region regionRevisada() {
        if (region == null || !REGION.matcher(region.strip()).matches()) {
            throw new IllegalStateException("Con inspectia.archivos.almacen=s3 hay que decir la región del bucket en "
                    + "AWS_REGION, como us-east-1, y «" + region + "» no lo es.");
        }
        return Region.of(region.strip());
    }
}
