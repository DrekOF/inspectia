/*
 * Nombre: PruebaIntegracion.java
 * Descripción: Anotación de las pruebas de integración: Spring completo, PostgreSQL 18 en Testcontainers
 *              y MockMvc para llamar la API.
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/**
 * Prueba de integración con todo el backend: Spring completo, Flyway, PostgreSQL 18 en un contenedor
 * ({@link ConfiguracionContenedores}) y {@code MockMvcTester} para llamar la API. Todas las pruebas con
 * esta anotación tienen la misma configuración, así que comparten el contexto de Spring y el contenedor:
 * la base arranca una sola vez.
 *
 * <p>Los nombres de estas clases terminan en {@code IT}: las corre {@code maven-failsafe-plugin} con
 * {@code mvnw verify}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@AutoConfigureMockMvc
@Import(ConfiguracionContenedores.class)
public @interface PruebaIntegracion {
}
