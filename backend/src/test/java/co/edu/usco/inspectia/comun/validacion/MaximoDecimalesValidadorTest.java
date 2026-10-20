/*
 * Nombre: MaximoDecimalesValidadorTest.java
 * Descripción: Pruebas unitarias de @MaximoDecimales: un número con un máximo de decimales, contados sin
 *              los ceros del final, como el límite de un rango de recomendación (RF-13).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.validacion;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Pruebas de {@link MaximoDecimales} con Bean Validation, sin Spring. */
class MaximoDecimalesValidadorTest {

    private static final Validator VALIDADOR = Validation.buildDefaultValidatorFactory().getValidator();

    /** Un dato con un decimal como máximo, como el límite de un rango de recomendación. */
    record UnDecimal(@MaximoDecimales(1) BigDecimal limite) {
    }

    /** Un dato con dos decimales como máximo. */
    record DosDecimales(@MaximoDecimales(2) BigDecimal valor) {
    }

    @ParameterizedTest(name = "{0}: ¿sirve? {1}")
    @CsvSource({"90.5, true", "90.50, true", "90.500, true", "90.55, false", "100, true", "1E+2, true",
            "0.0, true", "0.05, false", "-0.1, true", "-0.15, false"})
    @DisplayName("RF-13 · @MaximoDecimales cuenta los decimales sin los ceros del final: 90,50 vale 90,5 (prueba de límite)")
    void cuentaSinLosCerosDelFinal(String valor, boolean sirve) {
        assertThat(VALIDADOR.validate(new UnDecimal(new BigDecimal(valor))).isEmpty()).isEqualTo(sirve);
    }

    @Test
    @DisplayName("RF-13 · @MaximoDecimales deja pasar un dato que no llegó: si es obligatorio, lo revisa @NotNull")
    void sinDato() {
        assertThat(VALIDADOR.validate(new UnDecimal(null))).isEmpty();
    }

    @ParameterizedTest(name = "{0}: ¿sirve? {1}")
    @CsvSource({"12.34, true", "12.340, true", "12.345, false"})
    @DisplayName("RP-23 · @MaximoDecimales usa el máximo de su anotación, que sale de la escala del JSON")
    void otroMaximo(String valor, boolean sirve) {
        assertThat(VALIDADOR.validate(new DosDecimales(new BigDecimal(valor))).isEmpty()).isEqualTo(sirve);
    }

    @Test
    @DisplayName("RP-23 · @MaximoDecimales responde en el campo con el mensaje general de los decimales")
    void mensaje() {
        assertThat(VALIDADOR.validate(new DosDecimales(new BigDecimal("1.234")))).singleElement().satisfies(error -> {
            assertThat(error.getPropertyPath()).hasToString("valor");
            assertThat(error.getMessageTemplate()).isEqualTo("{validacion.decimales}");
        });
    }
}
