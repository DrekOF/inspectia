/*
 * Nombre: ConfiguracionDelAlmacenEnCarpetaTest.java
 * Descripción: Pruebas de cuándo se carga el almacén en carpeta (D-41): por defecto sí, con sus rutas firmadas; con S3,
 *              ni la carpeta ni sus rutas existen.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.carpeta;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Clock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Cuándo se carga el almacén en carpeta (D-41). Cristhian pidió que las rutas firmadas solo existan con la carpeta
 * local: con S3, que llega con el despliegue, no se activan.
 */
class ConfiguracionDelAlmacenEnCarpetaTest {

    @TempDir
    private Path carpeta;

    private ApplicationContextRunner contexto() {
        return new ApplicationContextRunner()
                .withBean(Clock.class, Clock::systemUTC)
                .withUserConfiguration(ConfiguracionDelAlmacenEnCarpeta.class, ArchivosControlador.class,
                        ArchivosAcceso.class)
                .withPropertyValues("inspectia.archivos.carpeta=" + carpeta.toString().replace('\\', '/'));
    }

    @Test
    @DisplayName("D-41 · sin decir el almacén, se cargan la carpeta, su firma y sus rutas firmadas")
    void porDefectoLaCarpeta() {
        contexto().run(cargado -> assertThat(cargado).hasSingleBean(AlmacenEnCarpeta.class)
                .hasSingleBean(FirmaDeEnlaces.class).hasSingleBean(ArchivosControlador.class)
                .hasSingleBean(ArchivosAcceso.class));
    }

    @Test
    @DisplayName("D-41 · con el almacén de S3, las rutas firmadas de la carpeta no existen ni se crea la clave")
    void conS3NoSeActivan() {
        contexto().withPropertyValues("inspectia.archivos.almacen=s3").run(cargado -> assertThat(cargado)
                .doesNotHaveBean(AlmacenEnCarpeta.class).doesNotHaveBean(FirmaDeEnlaces.class)
                .doesNotHaveBean(ArchivosControlador.class).doesNotHaveBean(ArchivosAcceso.class));
    }
}
