/*
 * Nombre: InspectiaAplicacion.java
 * Descripción: Punto de entrada del servidor de InspectIA (Spring Boot).
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
}
