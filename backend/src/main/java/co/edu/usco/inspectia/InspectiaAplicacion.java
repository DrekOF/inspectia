/*
 * Nombre: InspectiaAplicacion.java
 * Descripción: Punto de entrada del servidor de InspectIA (Spring Boot) y su reloj.
 * Fecha de creación: 13/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia;

import java.time.Clock;
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
     * La hora del servidor, en UTC. Las clases que dependen de la hora la reciben de aquí, así sus
     * pruebas pueden usar un reloj fijo; por ejemplo, el vencimiento de la sesión y el bloqueo.
     *
     * @return el reloj del sistema
     */
    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }
}
