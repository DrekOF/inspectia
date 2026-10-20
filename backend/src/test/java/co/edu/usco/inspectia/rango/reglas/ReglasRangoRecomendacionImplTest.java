/*
 * Nombre: ReglasRangoRecomendacionImplTest.java
 * Descripción: Pruebas unitarias de la regla de los rangos de recomendación que se programa a mano: los
 *              límites bajan en orden, de Premium o Especial a Fuera de norma (RF-13).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.rango.reglas;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.rango.RangoRecomendacion;
import co.edu.usco.inspectia.rango.RangoRecomendacionCategoria;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Pruebas de {@link ReglasRangoRecomendacionImpl}, sin base de datos ni sesión. */
class ReglasRangoRecomendacionImplTest {

    private final ReglasRangoRecomendacionImpl reglas = new ReglasRangoRecomendacionImpl();

    @ParameterizedTest(name = "Premium {0}, Estándar {1}, Corriente {2} y Fuera de norma {3}: ¿se cumple? {4}")
    @DisplayName("RF-13 · C1 · los límites deben bajar estrictamente de Premium o Especial a Fuera de norma: un empate o un límite mayor que el anterior no cumplen (prueba de límite)")
    @CsvSource({
            "96.0, 90.0, 82.0, 0.0, true",
            "100.0, 99.9, 0.1, 0.0, true",
            "96.0, 97.0, 82.0, 0.0, false",
            "96.0, 96.0, 82.0, 0.0, false",
            "96.0, 90.0, 90.0, 0.0, false",
            "96.0, 90.0, 0.0, 0.0, false",
            "96.0, 90.0, 82.0, 83.0, false"})
    void bajanEnOrden(String premium, String estandar, String corriente, String fueraDeNorma, boolean cumple) {
        List<RangoRecomendacion> filas = List.of(fila(RangoRecomendacionCategoria.PREMIUM, premium),
                fila(RangoRecomendacionCategoria.ESTANDAR, estandar), fila(RangoRecomendacionCategoria.CORRIENTE, corriente),
                fila(RangoRecomendacionCategoria.FUERA_DE_NORMA, fueraDeNorma));

        assertThat(reglas.cumpleLimitesEnOrden(filas, null)).isEqualTo(cumple);
    }

    @Test
    @DisplayName("RF-13 · la regla compara los límites en el orden de las categorías, no en el que lleguen las filas")
    void enElOrdenDeLasCategorias() {
        List<RangoRecomendacion> desordenadas = new ArrayList<>(List.of(
                fila(RangoRecomendacionCategoria.CORRIENTE, "82.0"), fila(RangoRecomendacionCategoria.FUERA_DE_NORMA, "0.0"),
                fila(RangoRecomendacionCategoria.PREMIUM, "96.0"), fila(RangoRecomendacionCategoria.ESTANDAR, "90.0")));

        assertThat(reglas.cumpleLimitesEnOrden(desordenadas, null)).isTrue();
        assertThat(desordenadas.getFirst().getCategoria()).as("no cambia la lista que recibe")
                .isEqualTo(RangoRecomendacionCategoria.CORRIENTE);
    }

    @Test
    @DisplayName("RF-13 · los límites se comparan por su valor: 90 y 90.0 son un empate")
    void comparaPorValor() {
        List<RangoRecomendacion> filas = List.of(fila(RangoRecomendacionCategoria.PREMIUM, "90"),
                fila(RangoRecomendacionCategoria.ESTANDAR, "90.0"), fila(RangoRecomendacionCategoria.CORRIENTE, "82.0"),
                fila(RangoRecomendacionCategoria.FUERA_DE_NORMA, "0.0"));

        assertThat(reglas.cumpleLimitesEnOrden(filas, null)).isFalse();
    }

    private static RangoRecomendacion fila(RangoRecomendacionCategoria categoria, String limite) {
        RangoRecomendacion fila = new RangoRecomendacion();
        fila.setCategoria(categoria);
        fila.setLimiteInferior(new BigDecimal(limite));
        return fila;
    }
}
