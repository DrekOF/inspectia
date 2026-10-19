/*
 * Nombre: NoEnBlancoValidadorTest.java
 * Descripción: Pruebas unitarias de @NoEnBlanco: un texto opcional que, si llega, no puede estar en
 *              blanco, como la contraseña al editar un usuario (RF-03).
 * Fecha de creación: 18/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.validacion;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Pruebas de {@link NoEnBlanco} con Bean Validation, sin Spring. */
class NoEnBlancoValidadorTest {

    private static final Validator VALIDADOR = Validation.buildDefaultValidatorFactory().getValidator();

    /** Un dato con una contraseña opcional, como al editar un usuario. */
    record Datos(@NoEnBlanco String contrasena) {
    }

    @ParameterizedTest(name = "«{0}»: ¿sirve? {1}")
    @CsvSource(delimiter = '|', textBlock = """
                       | true
            ''         | false
            '        ' | false
            '       x' | true
            ' Clave '  | true
            """)
    @DisplayName("RF-03 · @NoEnBlanco deja no enviar el dato, pero si llega no puede estar vacío ni tener solo espacios (prueba de límite)")
    void soloConAlgoDistintoDeEspacios(String contrasena, boolean sirve) {
        Set<ConstraintViolation<Datos>> errores = VALIDADOR.validate(new Datos(contrasena));

        assertThat(errores.isEmpty()).isEqualTo(sirve);
    }

    @ParameterizedTest(name = "caso {index}")
    @ValueSource(strings = {"\t\t", "\n", " \t\n "})
    @DisplayName("RF-03 · @NoEnBlanco también rechaza un texto con solo tabuladores o saltos de línea, con el mensaje de un dato obligatorio")
    void otrosEspacios(String contrasena) {
        assertThat(VALIDADOR.validate(new Datos(contrasena))).singleElement().satisfies(error -> {
            assertThat(error.getPropertyPath()).hasToString("contrasena");
            assertThat(error.getMessageTemplate()).isEqualTo("{validacion.obligatorio}");
        });
    }
}
