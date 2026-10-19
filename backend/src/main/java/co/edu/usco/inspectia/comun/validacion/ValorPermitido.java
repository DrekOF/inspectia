/*
 * Nombre: ValorPermitido.java
 * Descripción: Validación de los valores de una lista que se pueden asignar desde el CRUD
 *              (valores_en_formulario); si no, la API responde 403 (RF-03, RNF-04).
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
 * El valor de una lista es uno de los que se pueden asignar desde el CRUD. Por ejemplo, un
 * administrador de empresa no puede asignar el rol ADMIN_PLATAFORMA (RF-03 C10). Un valor
 * {@code null} es válido: la obligatoriedad la revisa {@code @NotNull}.
 *
 * <p>Sale de {@code valores_en_formulario} de {@code specs/entidades} (RP-23). Un valor que existe
 * pero no se puede asignar es un tema de permisos, no de datos: el manejador de errores responde
 * 403 y le da prioridad sobre los demás errores (RNF-04).
 */
@Documented
@Constraint(validatedBy = ValorPermitidoValidador.class)
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface ValorPermitido {

    /**
     * Los códigos que se pueden asignar.
     *
     * @return los códigos de la lista, como en el JSON
     */
    String[] valores();

    /**
     * Mensaje si el valor no se puede asignar.
     *
     * @return la clave del mensaje, entre llaves
     */
    String message() default "{validacion.valor_no_permitido}";

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
