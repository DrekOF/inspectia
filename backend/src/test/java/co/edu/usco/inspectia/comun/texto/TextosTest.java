/*
 * Nombre: TextosTest.java
 * Descripción: Pruebas unitarias de la normalización de textos para buscar sin distinguir tildes (RF-04).
 * Fecha de creación: 10/11/2026
 * Actualización: 10/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.texto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Quitar las tildes y la clave de búsqueda, con que se sugieren las fincas y los proveedores (RF-04). */
class TextosTest {

    @Test
    @DisplayName("RF-04 · sin tildes: «Paraíso» queda «Paraiso», «Peña» queda «Pena» y «Güiza» queda «Guiza»")
    void quitaLasTildes() {
        assertThat(Textos.sinTildes("Finca El Paraíso")).isEqualTo("Finca El Paraiso");
        assertThat(Textos.sinTildes("Peña")).isEqualTo("Pena");
        assertThat(Textos.sinTildes("Güiza")).isEqualTo("Guiza");
        assertThat(Textos.sinTildes("ÁÉÍÓÚ áéíóú")).isEqualTo("AEIOU aeiou");
    }

    @Test
    @DisplayName("RF-04 · la clave de búsqueda no distingue mayúsculas, tildes ni los espacios del inicio y del final")
    void claveDeBusqueda() {
        assertThat(Textos.claveDeBusqueda("  Finca El PARAÍSO ")).isEqualTo("finca el paraiso")
                .isEqualTo(Textos.claveDeBusqueda("finca el paraiso"));
    }

    @Test
    @DisplayName("RF-04 · sin tildes y la clave de búsqueda dejan un texto vacío como llega")
    void textosVacios() {
        assertThat(Textos.sinTildes(null)).isNull();
        assertThat(Textos.claveDeBusqueda(null)).isNull();
        assertThat(Textos.claveDeBusqueda("   ")).isEmpty();
    }
}
