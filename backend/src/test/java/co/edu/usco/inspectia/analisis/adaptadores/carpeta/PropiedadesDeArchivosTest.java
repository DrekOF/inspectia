/*
 * Nombre: PropiedadesDeArchivosTest.java
 * Descripción: Pruebas unitarias de la carpeta de las fotos (D-41): sin la variable, o vacía, la de siempre; relativa,
 *              no deja arrancar, con un mensaje que dice qué hacer.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.carpeta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** La carpeta de las fotos que sale de {@code INSPECTIA_FOTOS_CARPETA} (D-41). */
class PropiedadesDeArchivosTest {

    @TempDir
    private Path temporal;

    @Test
    @DisplayName("D-41 · sin INSPECTIA_FOTOS_CARPETA, o vacía, las fotos van a <carpeta del usuario>/.inspectia/fotos")
    void porDefecto() {
        Path esperada = Path.of(System.getProperty("user.home"), ".inspectia", "fotos");

        assertThat(new PropiedadesDeArchivos(null).carpetaDeFotos()).isEqualTo(esperada);
        assertThat(new PropiedadesDeArchivos("").carpetaDeFotos()).isEqualTo(esperada);
        assertThat(new PropiedadesDeArchivos("   ").carpetaDeFotos()).isEqualTo(esperada);
    }

    @Test
    @DisplayName("D-41 · una ruta completa se usa tal cual, sin los espacios de los extremos")
    void rutaCompleta() {
        assertThat(new PropiedadesDeArchivos("  " + temporal + "  ").carpetaDeFotos()).isEqualTo(temporal);
    }

    @Test
    @DisplayName("D-41 · una ruta relativa no deja arrancar el backend, con un mensaje que dice qué hacer: las fotos nunca quedan dentro del repositorio")
    void rutaRelativa() {
        assertThatThrownBy(() -> new PropiedadesDeArchivos("fotos").carpetaDeFotos())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ruta completa").hasMessageContaining("INSPECTIA_FOTOS_CARPETA")
                .hasMessageContaining("infra/.env.example");
    }
}
