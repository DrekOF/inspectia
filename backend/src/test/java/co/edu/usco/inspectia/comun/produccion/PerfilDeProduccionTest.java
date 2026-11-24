/*
 * Nombre: PerfilDeProduccionTest.java
 * Descripción: Prueba de contexto de Spring con el perfil produccion (D-16), sin red: no arranca sin el almacén en S3 o
 *              sin el servicio de IA, y arranca con los dos, con S3 en lugar de la carpeta, el filtro de CloudFront,
 *              los registros en JSON y Swagger UI cerrado.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.produccion;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.analisis.adaptadores.carpeta.AlmacenEnCarpeta;
import co.edu.usco.inspectia.analisis.adaptadores.carpeta.ConfiguracionDelAlmacenEnCarpeta;
import co.edu.usco.inspectia.analisis.adaptadores.s3.AlmacenEnS3;
import co.edu.usco.inspectia.analisis.adaptadores.s3.ConfiguracionDelAlmacenEnS3;
import java.nio.file.Path;
import java.time.Clock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

/**
 * El perfil {@code produccion} con sus archivos de configuración reales ({@code application.yml} y
 * {@code application-produccion.yml}) y las configuraciones de los almacenes, sin la base ni la red: crear el cliente y
 * el firmador de S3 no se conecta a AWS.
 */
class PerfilDeProduccionTest {

    private static final String[] AWS = {"inspectia.archivos.bucket=inspectia-archivos-123456789012",
        "inspectia.archivos.region=us-east-1"};

    @TempDir
    private Path carpeta;

    private ApplicationContextRunner contexto(String... perfiles) {
        return new ApplicationContextRunner()
                .withInitializer(contexto -> contexto.getEnvironment().setActiveProfiles(perfiles))
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withBean(Clock.class, Clock::systemUTC)
                .withUserConfiguration(ConfiguracionDeProduccion.class, ConfiguracionDelAlmacenEnS3.class,
                        ConfiguracionDelAlmacenEnCarpeta.class)
                .withPropertyValues("inspectia.archivos.carpeta=" + carpeta.toString().replace('\\', '/'))
                .withPropertyValues(AWS);
    }

    @Test
    @DisplayName("D-16 · con el perfil produccion, el almacén en S3 y el servicio de IA, el servidor arranca con S3, sin la carpeta, con el filtro de CloudFront, registros JSON para CloudWatch y Swagger UI cerrado")
    void arrancaConS3YElServicio() {
        contexto("produccion")
                .withPropertyValues("inspectia.archivos.almacen=s3", "inspectia.ia.clasificador=servicio")
                .run(cargado -> {
                    assertThat(cargado).hasNotFailed().hasSingleBean(AlmacenEnS3.class)
                            .doesNotHaveBean(AlmacenEnCarpeta.class).hasSingleBean(ComprobacionDeProduccion.class)
                            .hasSingleBean(FilterRegistrationBean.class);
                    assertThat(cargado.getBean(FilterRegistrationBean.class).getFilter())
                            .isInstanceOf(EncabezadosDeCloudFront.class);
                    var entorno = cargado.getEnvironment();
                    assertThat(entorno.getProperty("logging.structured.format.console")).isEqualTo("ecs");
                    assertThat(entorno.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
                    assertThat(entorno.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");
                    assertThat(entorno.getProperty("server.forward-headers-strategy")).isEqualTo("none");
                });
    }

    @Test
    @DisplayName("D-16 · con el perfil produccion, sin decir el almacén o con la carpeta, el servidor no arranca y dice que falta INSPECTIA_ARCHIVOS_ALMACEN=s3")
    void sinS3NoArranca() {
        contexto("produccion").withPropertyValues("inspectia.ia.clasificador=servicio")
                .run(cargado -> assertThat(cargado).hasFailed().getFailure()
                        .as("la comprobación falla primero, antes de crear cualquier bean")
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("INSPECTIA_ARCHIVOS_ALMACEN=s3")
                        .hasMessageNotContaining("INSPECTIA_IA_CLASIFICADOR"));
        contexto("produccion")
                .withPropertyValues("inspectia.archivos.almacen=carpeta", "inspectia.ia.clasificador=servicio")
                .run(cargado -> assertThat(cargado).hasFailed().getFailure().isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("INSPECTIA_ARCHIVOS_ALMACEN=s3").hasMessageContaining("«carpeta»"));
    }

    @Test
    @DisplayName("D-16 · con el perfil produccion y el clasificador falso de application.yml, el servidor no arranca y dice que falta INSPECTIA_IA_CLASIFICADOR=servicio")
    void conElClasificadorFalsoNoArranca() {
        contexto("produccion").withPropertyValues("inspectia.archivos.almacen=s3")
                .run(cargado -> assertThat(cargado).hasFailed().getFailure().isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("INSPECTIA_IA_CLASIFICADOR=servicio").hasMessageContaining("«falso»")
                        .hasMessageNotContaining("INSPECTIA_ARCHIVOS_ALMACEN"));
    }

    @Test
    @DisplayName("D-41 · sin el perfil produccion, como en desarrollo y en la CI, no se exige nada: el almacén es la carpeta, sin el filtro de CloudFront y con Swagger UI")
    void sinElPerfilNoSeExige() {
        contexto().run(cargado -> {
            assertThat(cargado).hasNotFailed().hasSingleBean(AlmacenEnCarpeta.class)
                    .doesNotHaveBean(ComprobacionDeProduccion.class).doesNotHaveBean(FilterRegistrationBean.class);
            assertThat(cargado.getEnvironment().getProperty("springdoc.swagger-ui.enabled")).isNull();
            assertThat(cargado.getEnvironment().getProperty("inspectia.ia.clasificador")).isEqualTo("falso");
        });
    }
}
