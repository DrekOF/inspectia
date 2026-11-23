/*
 * Nombre: DominioSinDependenciasTest.java
 * Descripción: Revisa que el dominio del análisis no dependa de Spring, de la base, de la API ni del servicio de IA:
 *              solo de Java y de sí mismo (RF-10 C7, D-21).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El dominio del análisis es Java puro (D-21): sus pruebas, unitarias y de mutación ({@code ./mvnw -Pmutacion test}),
 * no necesitan el servidor ni el servicio de IA (RF-10 C7). Esta prueba lee las fuentes del paquete y revisa sus
 * {@code import}. Corre desde {@code backend/}, la carpeta del proyecto de Maven.
 */
class DominioSinDependenciasTest {

    private static final Path FUENTES = Path.of("src", "main", "java", "co", "edu", "usco", "inspectia", "analisis",
            "dominio");
    private static final Pattern IMPORTACION = Pattern.compile("^import\\s+(?:static\\s+)?([\\w.]+)", Pattern.MULTILINE);

    @Test
    @DisplayName("RF-10 · C7 · el cálculo del porcentaje vive en el dominio sin depender de Spring, de la base ni del servicio de IA, así sus pruebas unitarias y de mutación corren solas")
    void soloJavaYElDominio() throws IOException {
        List<String> ajenas = new ArrayList<>();
        List<Path> archivos;
        try (Stream<Path> rutas = Files.list(FUENTES)) {
            archivos = rutas.filter(ruta -> ruta.toString().endsWith(".java")).toList();
        }
        for (Path archivo : archivos) {
            Matcher importacion = IMPORTACION.matcher(Files.readString(archivo, StandardCharsets.UTF_8));
            while (importacion.find()) {
                String clase = importacion.group(1);
                if (!clase.startsWith("java.") && !clase.startsWith("co.edu.usco.inspectia.analisis.dominio.")) {
                    ajenas.add(archivo.getFileName() + ": " + clase);
                }
            }
        }

        assertThat(archivos).as("las fuentes del dominio").extracting(ruta -> ruta.getFileName().toString())
                .contains("PorcentajeDelLote.java", "ReglasDeRecomendacion.java", "ResultadoDelLote.java");
        assertThat(ajenas).as("importaciones de fuera de Java y del dominio").isEmpty();
    }
}
