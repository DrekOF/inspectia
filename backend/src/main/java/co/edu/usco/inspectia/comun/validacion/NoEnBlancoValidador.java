/*
 * Nombre: NoEnBlancoValidador.java
 * Descripción: Revisa @NoEnBlanco: un texto opcional que, si llega, no está vacío ni tiene solo espacios
 *              (RF-03).
 * Fecha de creación: 18/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.validacion;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Revisa {@link NoEnBlanco}: sin valor es válido; con valor, debe tener algo distinto de espacios. */
public class NoEnBlancoValidador implements ConstraintValidator<NoEnBlanco, CharSequence> {

    @Override
    public boolean isValid(CharSequence valor, ConstraintValidatorContext contexto) {
        return valor == null || !valor.toString().isBlank();
    }
}
