/*
 * Nombre: ReglasDeConfianzaTest.java
 * Descripción: Pruebas unitarias de la confianza de los granos: el umbral de los dudosos, el máximo de dudosos de una
 *              muestra y el aviso de confianza baja, con sus límites (RF-09, RF-11, P-15).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** La confianza de los granos (RF-09, P-15), con el umbral provisional de 60 y el máximo de 10 %. */
class ReglasDeConfianzaTest {

    private final ReglasDeConfianza reglas = new ReglasDeConfianza(new BigDecimal("60"), new BigDecimal("10"));

    @Test
    @DisplayName("P-15 · un grano con confianza de 60,00 ya no es dudoso; uno de 59,99 sí (prueba de límite)")
    void umbralDeLosDudosos() {
        assertThat(reglas.esDudoso(new BigDecimal("60.00"))).isFalse();
        assertThat(reglas.esDudoso(new BigDecimal("59.99"))).isTrue();
        assertThat(reglas.esDudoso(new BigDecimal("55.00"))).isTrue();
        assertThat(reglas.esDudoso(new BigDecimal("90.00"))).isFalse();
    }

    @Test
    @DisplayName("RF-09 · una muestra pasa del máximo con más del 10 % de dudosos: 10 de 100 no pasa y 11 sí (prueba de límite)")
    void maximoDeDudosos() {
        assertThat(reglas.pasaDelMaximo(10, 100)).isFalse();
        assertThat(reglas.pasaDelMaximo(11, 100)).isTrue();
        assertThat(reglas.pasaDelMaximo(15, 150)).isFalse();
        assertThat(reglas.pasaDelMaximo(16, 150)).isTrue();
        assertThat(reglas.pasaDelMaximo(0, 100)).isFalse();
        assertThat(reglas.pasaDelMaximo(1, 1)).isTrue();
        assertThatThrownBy(() -> reglas.pasaDelMaximo(0, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RF-09 · el aviso de confianza baja sale solo si sigue pasando del máximo después de 2 repeticiones")
    void avisoDeConfianzaBaja() {
        assertThat(reglas.aviso(11, 100, 2)).isTrue();
        assertThat(reglas.aviso(11, 100, 1)).isFalse();
        assertThat(reglas.aviso(11, 100, 0)).as("sin repeticiones, como en el PR 3").isFalse();
        assertThat(reglas.aviso(10, 100, 2)).isFalse();
    }

    @Test
    @DisplayName("RF-09 · C1 · con más del 10 % de dudosos pide repetir una foto mientras la muestra lleve menos de 2 repeticiones; con 10 % o menos, no pide nada (C2)")
    void pideRepetir() {
        assertThat(reglas.pideRepetir(11, 100, 0)).isTrue();
        assertThat(reglas.pideRepetir(11, 100, 1)).isTrue();
        assertThat(reglas.pideRepetir(11, 100, 2)).as("con 2 repeticiones sale el aviso, no otra repetición").isFalse();
        assertThat(reglas.pideRepetir(10, 100, 0)).isFalse();
        assertThat(reglas.aviso(11, 100, 2)).isNotEqualTo(reglas.pideRepetir(11, 100, 2));
    }

    @Test
    @DisplayName("P-15 · el umbral y el máximo de dudosos van de 0 a 100 (prueba de límite)")
    void parametrosValidos() {
        assertThat(new ReglasDeConfianza(BigDecimal.ZERO, new BigDecimal("100")).umbral()).isEqualByComparingTo("0");
        assertThat(new ReglasDeConfianza(new BigDecimal("100"), BigDecimal.ZERO).maximoDeDudosos())
                .isEqualByComparingTo("0");
        assertThatThrownBy(() -> new ReglasDeConfianza(new BigDecimal("-0.01"), BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ReglasDeConfianza(new BigDecimal("100.01"), BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ReglasDeConfianza(BigDecimal.TEN, new BigDecimal("-0.1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ReglasDeConfianza(BigDecimal.TEN, new BigDecimal("100.1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
