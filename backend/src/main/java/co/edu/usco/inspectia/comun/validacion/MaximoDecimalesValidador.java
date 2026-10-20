/*
 * Nombre: MaximoDecimalesValidador.java
 * Descripción: Revisa la validación @MaximoDecimales contando los decimales sin los ceros del final.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.validacion;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

/**
 * Revisa {@link MaximoDecimales}. La escala de un {@link BigDecimal} cuenta los ceros del final (la de
 * 90.50 es 2), así que primero se quitan. Un número entero escrito como 1E+2 queda con escala negativa,
 * que siempre cumple.
 */
public class MaximoDecimalesValidador implements ConstraintValidator<MaximoDecimales, BigDecimal> {

    private int maximo;

    @Override
    public void initialize(MaximoDecimales anotacion) {
        maximo = anotacion.value();
    }

    @Override
    public boolean isValid(BigDecimal numero, ConstraintValidatorContext contexto) {
        return numero == null || numero.stripTrailingZeros().scale() <= maximo;
    }
}
