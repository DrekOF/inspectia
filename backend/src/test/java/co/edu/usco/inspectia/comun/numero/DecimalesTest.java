/*
 * Nombre: DecimalesTest.java
 * Descripción: Pruebas unitarias de Decimales: un número queda con la escala de su columna, como la guarda
 *              la base, sin redondear (RF-13).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.numero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Pruebas de {@link Decimales}. */
class DecimalesTest {

    @ParameterizedTest(name = "{0} con escala {1} queda {2}")
    @DisplayName("RF-13 · un número queda con la escala de su columna, como lo guarda la base: 90.50 como 90.5 y 100 como 100.0")
    @CsvSource({"90.50, 1, 90.5", "90.5, 1, 90.5", "100, 1, 100.0", "1E+1, 1, 10.0", "0, 1, 0.0", "12.3, 2, 12.30"})
    void conLaEscalaDeSuColumna(String numero, int escala, String queda) {
        assertThat(Decimales.conEscala(new BigDecimal(numero), escala)).hasToString(queda);
    }

    @Test
    @DisplayName("RP-23 · un dato que no llegó sigue sin valor")
    void sinDato() {
        assertThat(Decimales.conEscala(null, 1)).isNull();
    }

    @Test
    @DisplayName("RF-13 · no redondea: un número con más decimales de los de su escala es un error, porque la validación ya debió rechazarlo")
    void noRedondea() {
        assertThatThrownBy(() -> Decimales.conEscala(new BigDecimal("90.55"), 1)).isInstanceOf(ArithmeticException.class);
    }
}
