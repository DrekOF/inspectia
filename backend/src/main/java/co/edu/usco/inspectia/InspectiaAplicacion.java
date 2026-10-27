/*
 * Nombre: InspectiaAplicacion.java
 * Descripción: Punto de entrada del servidor de InspectIA (Spring Boot) y su reloj, al microsegundo como
 *              PostgreSQL (RF-18).
 * Fecha de creación: 13/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia;

import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Punto de entrada del servidor de InspectIA.
 *
 * <p>Es un solo programa dividido en módulos (D-21): {@code empresa}, {@code usuario},
 * {@code rango}, {@code analisis}, {@code estadistica} y {@code comun}.
 */
@SpringBootApplication
public class InspectiaAplicacion {

    /**
     * Arranca el servidor.
     *
     * @param args argumentos de la línea de comandos
     */
    public static void main(String[] args) {
        SpringApplication.run(InspectiaAplicacion.class, args);
    }

    /**
     * La hora del servidor, en UTC y al microsegundo, la precisión con que PostgreSQL guarda la hora. Las
     * clases que dependen de la hora la reciben de aquí, así sus pruebas pueden usar un reloj fijo; por
     * ejemplo, el vencimiento de la sesión y el bloqueo.
     *
     * <p>En Windows, la hora de Java trae décimas de microsegundo, y el driver de PostgreSQL las redondea al
     * guardar. Así, una hora guardada y vuelta a leer podía quedar un poco después de la que calculó el
     * servidor: el quinto fallo de RF-18 respondía que faltaban 901 segundos de un bloqueo de 900. Con el
     * reloj al microsegundo, lo que se guarda es lo mismo que se calculó, también en el código que venga.
     *
     * @return el reloj del sistema, al microsegundo
     */
    @Bean
    Clock reloj() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }
}
