/*
 * Nombre: InspectiaAplicacionIT.java
 * Descripción: Prueba de arranque del servidor con PostgreSQL 18 en un contenedor.
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Prueba de arranque: el servidor sube con sus módulos, corre Flyway y se conecta a PostgreSQL 18.
 */
@PruebaIntegracion
class InspectiaAplicacionIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("D-21 · el backend arranca con sus módulos, Flyway y PostgreSQL 18")
    void arrancaConPostgresql18() {
        Integer version = jdbc.queryForObject(
                "SELECT current_setting('server_version_num')::int / 10000", Integer.class);

        assertThat(version).isEqualTo(18);
    }
}
