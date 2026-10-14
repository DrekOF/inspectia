/*
 * Nombre: ConfiguracionContenedores.java
 * Descripción: Entorno de las pruebas de integración: PostgreSQL 18 en un contenedor de Testcontainers
 *              y una clave de tokens nueva en cada corrida.
 * Fecha de creación: 13/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia;

import java.security.SecureRandom;
import java.util.Base64;
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
 * </ul>
 */
@TestConfiguration(proxyBeanMethods = false)
public class ConfiguracionContenedores {

    /** Imagen de PostgreSQL de las pruebas: la misma de {@code infra/docker-compose.yml}. */
    public static final String IMAGEN_POSTGRES = "postgres:18-alpine";

    /** Clave HS256 de los tokens en esta corrida, en Base64: 32 bytes al azar. */
    public static final String CLAVE_JWT = claveAleatoria();

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

    private static String claveAleatoria() {
        byte[] clave = new byte[32];
        new SecureRandom().nextBytes(clave);
        return Base64.getEncoder().encodeToString(clave);
    }
}
