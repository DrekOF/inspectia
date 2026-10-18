/*
 * Nombre: NoEnBlanco.java
 * Descripción: Validación de un texto opcional: puede no llegar, pero si llega no puede estar vacío ni
 *              tener solo espacios. La usa la contraseña al editar un usuario (RF-03).
 * Fecha de creación: 18/10/2026
 * Actualización: 18/10/2026
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
 * Un texto opcional que, si llega, no puede estar vacío ni tener solo espacios. Un valor {@code null}
 * es válido: el dato no llegó. Es lo mismo que {@code @NotBlank} cuando el dato sí llega.
 *
 * <p>Por ejemplo, al editar un usuario la contraseña es opcional: si no llega, queda la que tenía. Una
 * de solo espacios se rechaza con el mismo mensaje que al crear, «Este dato es obligatorio.», porque
 * tampoco serviría para iniciar sesión. El manejador de errores lo informa antes que los demás errores
 * del mismo campo, como a {@code @NotBlank}.
 */
@Documented
@Constraint(validatedBy = NoEnBlancoValidador.class)
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface NoEnBlanco {

    /**
     * Mensaje si el texto llega en blanco.
     *
     * @return la clave del mensaje, entre llaves
     */
    String message() default "{validacion.obligatorio}";

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
