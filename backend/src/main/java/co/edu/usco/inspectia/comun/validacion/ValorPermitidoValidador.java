/*
 * Nombre: ValorPermitidoValidador.java
 * Descripción: Revisa @ValorPermitido: el valor de una lista es uno de los que se pueden asignar desde
 *              el CRUD (RF-03).
 * Fecha de creación: 18/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.validacion;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;

/** Revisa {@link ValorPermitido}: compara el código del valor (el nombre del {@code enum}) con la lista. */
public class ValorPermitidoValidador implements ConstraintValidator<ValorPermitido, Object> {

    private Set<String> permitidos;

    @Override
    public void initialize(ValorPermitido anotacion) {
        permitidos = Set.of(anotacion.valores());
    }

    @Override
    public boolean isValid(Object valor, ConstraintValidatorContext contexto) {
        if (valor == null) {
            return true;
        }
        String codigo = valor instanceof Enum<?> constante ? constante.name() : valor.toString();
        return permitidos.contains(codigo);
    }
}
