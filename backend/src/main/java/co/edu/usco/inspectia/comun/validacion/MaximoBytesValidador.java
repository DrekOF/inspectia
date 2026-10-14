/*
 * Nombre: MaximoBytesValidador.java
 * Descripción: Revisa la validación @MaximoBytes contando los bytes UTF-8 del texto.
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.validacion;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

/** Revisa {@link MaximoBytes}: cuenta los bytes del texto en UTF-8, la codificación de BCrypt. */
public class MaximoBytesValidador implements ConstraintValidator<MaximoBytes, CharSequence> {

    private int maximo;

    @Override
    public void initialize(MaximoBytes anotacion) {
        maximo = anotacion.value();
    }

    @Override
    public boolean isValid(CharSequence texto, ConstraintValidatorContext contexto) {
        return texto == null || texto.toString().getBytes(StandardCharsets.UTF_8).length <= maximo;
    }
}
