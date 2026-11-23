/*
 * Nombre: PeticionRechazadaExcepcion.java
 * Descripción: Una petición que no es de un formulario se rechaza por su contenido, como un archivo que no es una
 *              foto JPG ni PNG (RF-05 C3). La API responde 400 con el motivo.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.errores;

import lombok.Getter;

/**
 * Una petición que no es de un formulario se rechaza por su contenido (400), como un archivo que no es una foto JPG
 * ni PNG. No hay campos que marcar: la respuesta dice el motivo en {@code detail}, con el mensaje de su clave.
 */
@Getter
public class PeticionRechazadaExcepcion extends RuntimeException {

    /** La clave del mensaje del motivo. */
    private final String clave;

    /**
     * Crea la excepción.
     *
     * @param clave la clave del mensaje del motivo
     */
    public PeticionRechazadaExcepcion(String clave) {
        super(clave);
        this.clave = clave;
    }
}
