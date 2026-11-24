/*
 * Nombre: ConfiguracionDelAlmacenEnS3Test.java
 * Descripción: Pruebas de cuándo se carga el almacén en S3 (D-16): solo con inspectia.archivos.almacen=s3, y entonces
 *              sin la carpeta ni sus rutas firmadas (D-41); sin el bucket o la región, el servidor no arranca. Sin red.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.s3;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.analisis.adaptadores.carpeta.AlmacenEnCarpeta;
import co.edu.usco.inspectia.analisis.adaptadores.carpeta.ConfiguracionDelAlmacenEnCarpeta;
import co.edu.usco.inspectia.analisis.aplicacion.AlmacenDeArchivos;
import java.nio.file.Path;
import java.time.Clock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** Cuándo se carga el almacén en S3 (D-16). Crear el cliente y el firmador no se conecta a AWS. */
class ConfiguracionDelAlmacenEnS3Test {

    @TempDir
    private Path carpeta;

    private ApplicationContextRunner contexto() {
        return new ApplicationContextRunner()
                .withBean(Clock.class, Clock::systemUTC)
                .withUserConfiguration(ConfiguracionDelAlmacenEnS3.class, ConfiguracionDelAlmacenEnCarpeta.class)
                .withPropertyValues("inspectia.archivos.carpeta=" + carpeta.toString().replace('\\', '/'));
    }

    @Test
    @DisplayName("D-16 · con inspectia.archivos.almacen=s3, el bucket y la región, el almacén es S3 y no se cargan la carpeta ni su firma")
    void conS3() {
        contexto().withPropertyValues("inspectia.archivos.almacen=s3",
                "inspectia.archivos.bucket=inspectia-archivos-123456789012", "inspectia.archivos.region=us-east-1")
                .run(cargado -> {
                    assertThat(cargado).hasNotFailed().hasSingleBean(AlmacenDeArchivos.class)
                            .hasSingleBean(AlmacenEnS3.class).hasSingleBean(S3Client.class)
                            .hasSingleBean(S3Presigner.class).doesNotHaveBean(AlmacenEnCarpeta.class);
                    assertThat(cargado.getBean(S3Client.class).serviceClientConfiguration().region().id())
                            .isEqualTo("us-east-1");
                });
    }

    @Test
    @DisplayName("D-41 · sin decir el almacén, o con la carpeta, no se carga nada de S3")
    void sinS3() {
        contexto().run(cargado -> assertThat(cargado).hasSingleBean(AlmacenEnCarpeta.class)
                .doesNotHaveBean(AlmacenEnS3.class).doesNotHaveBean(S3Client.class));
        contexto().withPropertyValues("inspectia.archivos.almacen=carpeta").run(cargado -> assertThat(cargado)
                .hasSingleBean(AlmacenEnCarpeta.class).doesNotHaveBean(AlmacenEnS3.class));
    }

    @Test
    @DisplayName("D-16 · con S3 pero sin el bucket, con un bucket que S3 no acepta o sin la región, el servidor no arranca y dice qué variable corregir")
    void sinBucketORegionNoArranca() {
        contexto().withPropertyValues("inspectia.archivos.almacen=s3", "inspectia.archivos.region=us-east-1")
                .run(cargado -> assertThat(cargado).hasFailed().getFailure().rootCause()
                        .hasMessageContaining("INSPECTIA_ARCHIVOS_BUCKET"));
        contexto().withPropertyValues("inspectia.archivos.almacen=s3", "inspectia.archivos.region=us-east-1",
                "inspectia.archivos.bucket=Inspectia_Archivos")
                .run(cargado -> assertThat(cargado).hasFailed().getFailure().rootCause()
                        .hasMessageContaining("INSPECTIA_ARCHIVOS_BUCKET").hasMessageContaining("Inspectia_Archivos"));
        contexto().withPropertyValues("inspectia.archivos.almacen=s3",
                "inspectia.archivos.bucket=inspectia-archivos-123456789012", "inspectia.archivos.region=")
                .run(cargado -> assertThat(cargado).hasFailed().getFailure().rootCause()
                        .hasMessageContaining("AWS_REGION"));
    }
}
