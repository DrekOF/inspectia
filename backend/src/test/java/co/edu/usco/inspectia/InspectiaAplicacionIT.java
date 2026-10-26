/*
 * Nombre: InspectiaAplicacionIT.java
 * Descripción: Prueba de arranque del servidor con PostgreSQL 18 en un contenedor, y del reloj del
 *              servidor, que da la hora con la precisión de PostgreSQL (RF-18).
 * Fecha de creación: 13/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Prueba de arranque: el servidor sube con sus módulos, corre Flyway y se conecta a PostgreSQL 18. También
 * revisa el bean {@code reloj} del contexto real: su hora tiene la precisión de PostgreSQL (RF-18).
 */
@PruebaIntegracion
class InspectiaAplicacionIT {

    /** Lecturas seguidas del reloj. */
    private static final int LECTURAS = 1_000;

    /** Cada cuánto se lee el reloj: las 1.000 lecturas cubren unos 50 ms. */
    private static final Duration CADA = Duration.ofNanos(50_000);

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Clock reloj;

    @Test
    @DisplayName("D-21 · el backend arranca con sus módulos, Flyway y PostgreSQL 18")
    void arrancaConPostgresql18() {
        Integer version = jdbc.queryForObject(
                "SELECT current_setting('server_version_num')::int / 10000", Integer.class);

        assertThat(version).isEqualTo(18);
    }

    /**
     * PostgreSQL guarda la hora al microsegundo, y el driver redondea lo que sobra. Si el reloj del servidor
     * diera fracciones de microsegundo, una hora guardada y vuelta a leer podría quedar un poco después de la
     * que calculó el servidor: el quinto fallo de RF-18 respondía que faltaban 901 segundos de un bloqueo de
     * 900.
     *
     * <p>En Windows, la hora de Java trae décimas de microsegundo, y esa fracción cambia más o menos cada
     * milisegundo. 1.000 lecturas sin pausa duran unos 30 µs y pueden caer todas en un momento en que la
     * fracción es 0; por eso van una cada 50 µs.
     */
    @Test
    @DisplayName("RF-18 · el reloj del servidor da la hora al microsegundo, la precisión de PostgreSQL, para que una hora guardada valga lo mismo al leerla: en 1.000 lecturas seguidas, una cada 50 µs, ninguna trae fracciones de microsegundo")
    void elRelojDaLaHoraAlMicrosegundo() {
        List<Instant> conFraccion = new ArrayList<>();
        long siguiente = System.nanoTime();
        for (int lectura = 0; lectura < LECTURAS; lectura++) {
            while (System.nanoTime() - siguiente < 0) {
                Thread.onSpinWait();
            }
            Instant ahora = reloj.instant();
            if (ahora.getNano() % 1_000 != 0) {
                conFraccion.add(ahora);
            }
            siguiente += CADA.toNanos();
        }

        assertThat(conFraccion.size())
                .as("de %d lecturas, las que traen fracciones de microsegundo; por ejemplo, %s", LECTURAS,
                        conFraccion.isEmpty() ? "ninguna" : conFraccion.getFirst())
                .isZero();
    }
}
