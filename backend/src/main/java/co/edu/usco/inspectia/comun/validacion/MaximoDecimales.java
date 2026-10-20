/*
 * Nombre: MaximoDecimales.java
 * Descripción: Validación de los decimales de un número, contados sin los ceros del final, como el límite
 *              de un rango de recomendación (RF-13).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.validacion;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * El número tiene como máximo {@code value} decimales, sin contar los ceros del final: con un decimal,
 * 90.50 sirve porque vale 90.5, y 90.55 no. {@code @Digits} de Bean Validation sí los contaría y
 * rechazaría 90.50. Un valor {@code null} es válido: la obligatoriedad la revisa {@code @NotNull}.
 *
 * <p>Sale de la escala de cada atributo {@code decimal} de {@code specs/entidades} (RP-23).
 */
@Documented
@Constraint(validatedBy = MaximoDecimalesValidador.class)
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface MaximoDecimales {

    /**
     * Decimales que puede tener el número.
     *
     * @return el máximo, sin contar los ceros del final
     */
    int value();

    /**
     * Mensaje si el número tiene más decimales.
     *
     * @return la clave del mensaje, entre llaves
     */
    String message() default "{validacion.decimales}";

    /**
     * Grupos de validación.
     *
     * @return los grupos
     */
    Class<?>[] groups() default {};

    /**
     * Datos adicionales de la validación.
     *
     * @return los datos
     */
    Class<? extends Payload>[] payload() default {};
}
