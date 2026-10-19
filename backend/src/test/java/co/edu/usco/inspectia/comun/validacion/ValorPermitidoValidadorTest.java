/*
 * Nombre: ValorPermitidoValidadorTest.java
 * Descripción: Pruebas unitarias de @ValorPermitido: los valores de una lista que se pueden asignar
 *              desde el CRUD (valores_en_formulario, RF-03).
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

/** Pruebas de {@link ValorPermitido} con Bean Validation, sin Spring. */
class ValorPermitidoValidadorTest {

    private static final Validator VALIDADOR = Validation.buildDefaultValidatorFactory().getValidator();

    /** Una lista de prueba, como las que genera el generador. */
    enum Rol { USUARIO_FINAL, ADMIN_EMPRESA, ADMIN_PLATAFORMA }

    /** Un dato con un rol que solo admite dos de los tres valores. */
    record Datos(@ValorPermitido(valores = {"USUARIO_FINAL", "ADMIN_EMPRESA"}) Rol rol) {
    }

    @ParameterizedTest(name = "{0}: ¿se permite? {1}")
    @CsvSource({"USUARIO_FINAL, true", "ADMIN_EMPRESA, true", "ADMIN_PLATAFORMA, false", ", true"})
    @DisplayName("RF-03 · @ValorPermitido solo deja los valores de valores_en_formulario; sin valor no opina")
    void soloLosValoresPermitidos(Rol rol, boolean permitido) {
        Set<ConstraintViolation<Datos>> errores = VALIDADOR.validate(new Datos(rol));

        assertThat(errores.isEmpty()).isEqualTo(permitido);
        if (!permitido) {
            assertThat(errores).singleElement().satisfies(error -> {
                assertThat(error.getPropertyPath()).hasToString("rol");
                assertThat(error.getConstraintDescriptor().getAnnotation()).isInstanceOf(ValorPermitido.class);
            });
        }
    }
}
