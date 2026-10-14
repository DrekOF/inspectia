/*
 * Nombre: DigitoVerificacionNitTest.java
 * Descripción: Pruebas unitarias del dígito de verificación del NIT con el algoritmo de la DIAN (RF-02).
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.empresa.reglas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas del dígito de verificación del NIT (RF-02 C4). Los tres primeros NIT son los de la ficha;
 * los demás cubren el NIT de la DIAN, uno de 6 dígitos y los residuos 0 y 1, en los que el dígito es
 * el mismo residuo.
 */
class DigitoVerificacionNitTest {

    @ParameterizedTest(name = "{0} → {1}")
    @DisplayName("RF-02 · C4 · calcula el dígito de verificación con el algoritmo de la DIAN")
    @CsvSource({
        "890903938, 8",
        "899999068, 1",
        "900123456, 8",
        "800197268, 4",
        "123456,    3",
        "900123455, 0",
        "900123459, 1"
    })
    void calculaElDigito(String numero, int digito) {
        assertThat(DigitoVerificacionNit.calcular(numero)).isEqualTo(digito);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("RF-02 · C4 · acepta un NIT con su dígito de verificación correcto")
    @ValueSource(strings = {"890903938-8", "899999068-1", "900123456-8", "800197268-4", "123456-3",
        "900123455-0", "900123459-1"})
    void aceptaElDigitoCorrecto(String nit) {
        assertThat(DigitoVerificacionNit.esValido(nit)).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("RF-02 · C4 · rechaza un NIT cuyo dígito de verificación no corresponde")
    @ValueSource(strings = {"900123456-7", "890903938-9", "899999068-0", "900123455-1", "900123459-0"})
    void rechazaUnDigitoQueNoCorresponde(String nit) {
        assertThat(DigitoVerificacionNit.esValido(nit)).isFalse();
    }

    @ParameterizedTest(name = "«{0}»")
    @DisplayName("RF-02 · C4 · rechaza un NIT sin dígito de verificación o con otro formato")
    @NullAndEmptySource
    @ValueSource(strings = {"900123456", "900123456-", "900.123.456-8", "90012345a-8", "12345-6",
        "12345678901-2"})
    void rechazaOtroFormato(String nit) {
        assertThat(DigitoVerificacionNit.esValido(nit)).isFalse();
    }
}
