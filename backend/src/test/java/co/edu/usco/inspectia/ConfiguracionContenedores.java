/*
 * Nombre: ConfiguracionContenedores.java
 * Descripción: Entorno de las pruebas de integración: PostgreSQL 18 en un contenedor de Testcontainers,
 *              una clave de tokens nueva en cada corrida y una carpeta temporal para las fotos (D-41).
 * Fecha de creación: 13/10/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Comparator;
import java.util.stream.Stream;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Entorno de las pruebas de integración:
 * <ul>
 *   <li>PostgreSQL 18 en un contenedor, la misma versión de desarrollo y de producción. Spring toma la
 *       conexión del contenedor ({@link ServiceConnection}), así que las pruebas no dependen de
 *       {@code infra/.env}.</li>
 *   <li>La clave de los tokens (D-28): una nueva en cada corrida, así ninguna clave queda en el
 *       repositorio.</li>
 *   <li>La carpeta de las fotos (D-41): una temporal en cada corrida, que se borra al terminar. Así las
 *       pruebas nunca escriben en la carpeta de fotos de quien las corre.</li>
 * </ul>
 */
@TestConfiguration(proxyBeanMethods = false)
public class ConfiguracionContenedores {

    /** Imagen de PostgreSQL de las pruebas: la misma de {@code infra/docker-compose.yml}. */
    public static final String IMAGEN_POSTGRES = "postgres:18-alpine";

    /** Clave HS256 de los tokens en esta corrida, en Base64: 32 bytes al azar. */
    public static final String CLAVE_JWT = claveAleatoria();

    /** Carpeta de las fotos en esta corrida (D-41): temporal, y se borra al terminar. */
    public static final Path CARPETA_DE_FOTOS = carpetaTemporal();

    /**
     * Contenedor de PostgreSQL que comparten las pruebas con esta configuración.
     *
     * @return el contenedor; Spring lo arranca y lo detiene
     */
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer(DockerImageName.parse(IMAGEN_POSTGRES));
    }

    /**
     * Pasa la clave de esta corrida al backend, en lugar de {@code INSPECTIA_JWT_CLAVE}.
     *
     * @return el registro de la propiedad {@code inspectia.jwt.clave}
     */
    @Bean
    DynamicPropertyRegistrar claveJwtDePrueba() {
        return registro -> registro.add("inspectia.jwt.clave", () -> CLAVE_JWT);
    }

    /**
     * Pasa la carpeta temporal al backend, en lugar de {@code INSPECTIA_FOTOS_CARPETA}.
     *
     * @return el registro de la propiedad {@code inspectia.archivos.carpeta}
     */
    @Bean
    DynamicPropertyRegistrar carpetaDeFotosDePrueba() {
        return registro -> registro.add("inspectia.archivos.carpeta", () -> CARPETA_DE_FOTOS.toString());
    }

    /**
     * Borra lo que hay dentro de la carpeta de las fotos, sin borrar la carpeta: para las pruebas que suben
     * fotos, al terminar cada una.
     */
    public static void vaciarCarpetaDeFotos() {
        try (Stream<Path> rutas = Files.walk(CARPETA_DE_FOTOS)) {
            rutas.sorted(Comparator.reverseOrder()).filter(ruta -> !ruta.equals(CARPETA_DE_FOTOS)).forEach(ruta -> {
                try {
                    Files.deleteIfExists(ruta);
                } catch (IOException excepcion) {
                    throw new UncheckedIOException(excepcion);
                }
            });
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }

    private static String claveAleatoria() {
        byte[] clave = new byte[32];
        new SecureRandom().nextBytes(clave);
        return Base64.getEncoder().encodeToString(clave);
    }

    private static Path carpetaTemporal() {
        try {
            Path carpeta = Files.createTempDirectory("inspectia-fotos-");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                vaciarCarpetaDeFotos();
                try {
                    Files.deleteIfExists(carpeta);
                } catch (IOException excepcion) {
                    // Si el sistema la tiene abierta, queda en la carpeta temporal y la limpia el sistema.
                }
            }));
            return carpeta;
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }
}
