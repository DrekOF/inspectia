/*
 * Nombre: ConfiguracionContenedores.java
 * Descripción: Base de datos de las pruebas: PostgreSQL 18 en un contenedor de Testcontainers.
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base de datos de las pruebas: PostgreSQL 18 en un contenedor, la misma versión de desarrollo y de
 * producción. Spring toma la conexión del contenedor ({@link ServiceConnection}), así que las
 * pruebas no dependen de {@code infra/.env}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ConfiguracionContenedores {

    /** Imagen de PostgreSQL de las pruebas: la misma de {@code infra/docker-compose.yml}. */
    public static final String IMAGEN_POSTGRES = "postgres:18-alpine";

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
}
