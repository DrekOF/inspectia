/*
 * Nombre: ComprobacionDeProduccion.java
 * Descripción: Con el perfil produccion, el servidor no arranca si el almacén de los archivos no es S3 o el
 *              clasificador de granos no es el servicio de IA (D-16): un despliegue nunca cae en la carpeta y sus rutas
 *              sin sesión (D-41) ni en el clasificador falso.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.produccion;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * Revisa al arrancar con el perfil {@code produccion} que el almacén y el clasificador sean los de AWS. Lo pidió
 * Cristhian el 23/11/2026: los dos valores se dicen explícitos en el despliegue y, si faltan o son otros, el servidor
 * no arranca. Sin ellos, Spring cargaría la carpeta de desarrollo, con sus rutas firmadas que no piden sesión (D-41), o
 * el clasificador falso, que no da resultados reales.
 *
 * <p>Corre antes de crear cualquier bean, así su mensaje es el primero y el único: dice qué variable de entorno falta.
 * Compara los valores como lo hacen las condiciones de Spring, sin distinguir mayúsculas.
 */
public class ComprobacionDeProduccion implements BeanFactoryPostProcessor, EnvironmentAware {

    /** El almacén que exige el perfil. */
    static final String ALMACEN = "s3";

    /** El clasificador que exige el perfil. */
    static final String CLASIFICADOR = "servicio";

    private Environment entorno;

    @Override
    public void setEnvironment(Environment entorno) {
        this.entorno = entorno;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory fabrica) {
        revisar(entorno);
    }

    /**
     * Revisa el almacén y el clasificador.
     *
     * @param entorno las propiedades del servidor
     * @throws IllegalStateException si alguno no es el de AWS, con lo que hay que corregir
     */
    public static void revisar(Environment entorno) {
        List<String> problemas = new ArrayList<>();
        String almacen = entorno.getProperty("inspectia.archivos.almacen");
        if (!ALMACEN.equalsIgnoreCase(almacen)) {
            problemas.add("inspectia.archivos.almacen debe ser «s3» (variable INSPECTIA_ARCHIVOS_ALMACEN=s3) y es "
                    + valor(almacen) + ": con otro valor, las fotos quedarían en una carpeta del servidor, con rutas que "
                    + "no piden sesión (D-41).");
        }
        String clasificador = entorno.getProperty("inspectia.ia.clasificador");
        if (!CLASIFICADOR.equalsIgnoreCase(clasificador)) {
            problemas.add("inspectia.ia.clasificador debe ser «servicio» (variable INSPECTIA_IA_CLASIFICADOR=servicio) "
                    + "y es " + valor(clasificador) + ": con otro valor, el análisis no usaría el modelo de IA.");
        }
        if (!problemas.isEmpty()) {
            throw new IllegalStateException("El perfil produccion (AWS, D-16) no arranca así. "
                    + String.join(" ", problemas) + " Revisa las variables del backend en el docker-compose de "
                    + "producción.");
        }
    }

    private static String valor(String valor) {
        return valor == null ? "vacío (no está)" : "«" + valor + "»";
    }
}
