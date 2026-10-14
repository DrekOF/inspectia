/*
 * Nombre: VerificacionDeClasesTest.java
 * Descripción: Revisa que la JVM acepte todas las clases compiladas del backend (VerifyError).
 * Fecha de creación: 14/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Carga cada clase compilada del backend y le pide sus constructores, así la JVM la verifica: una
 * clase mal compilada falla con {@link VerifyError}.
 *
 * <p>Pasó con los constructores que genera Lombok: con el JDK 25, al recompilar sin {@code clean},
 * quedaban sin la llamada a {@code super()}. Por eso el backend escribe sus constructores (ver
 * {@code CLAUDE.md}). Si esta prueba falla, compila desde cero con {@code ./mvnw clean verify}.
 */
class VerificacionDeClasesTest {

    @Test
    @DisplayName("RP-01 · la JVM acepta todas las clases compiladas del backend")
    void todasLasClasesPasanLaVerificacion() throws Exception {
        Path clases = Path.of(InspectiaAplicacion.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        List<String> rechazadas = new ArrayList<>();
        try (Stream<Path> archivos = Files.walk(clases)) {
            for (Path archivo : archivos.filter(ruta -> ruta.toString().endsWith(".class")).toList()) {
                String relativa = clases.relativize(archivo).toString().replace(File.separatorChar, '.');
                String nombre = relativa.substring(0, relativa.length() - ".class".length());
                try {
                    Class.forName(nombre, false, getClass().getClassLoader()).getDeclaredConstructors();
                } catch (VerifyError error) {
                    rechazadas.add(nombre);
                }
            }
        }

        assertThat(rechazadas).as("clases que la JVM rechaza; compila desde cero con ./mvnw clean verify").isEmpty();
    }
}
