/*
 * Nombre: MaximoBytes.java
 * Descripción: Validación del tamaño de un texto en bytes UTF-8, para las contraseñas (BCrypt solo
 *              usa 72 bytes, RF-02).
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
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
 * El texto ocupa como máximo {@code value} bytes en UTF-8. Cada letra con tilde y cada eñe ocupan
 * 2 bytes, así que el límite en bytes puede ser más estricto que el de caracteres (RF-02 C6). Un
 * valor {@code null} es válido: la obligatoriedad la revisa {@code @NotBlank}.
 *
 * <p>Sale de la validación {@code bytes_max} de {@code specs/entidades} (RP-23).
 */
@Documented
@Constraint(validatedBy = MaximoBytesValidador.class)
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface MaximoBytes {

    /**
     * Bytes que puede ocupar el texto.
     *
     * @return el máximo, en bytes UTF-8
     */
    int value();

    /**
     * Mensaje si el texto es más largo.
     *
     * @return la clave del mensaje, entre llaves
     */
    String message() default "{validacion.bytes_max}";

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
