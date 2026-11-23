/*
 * Nombre: ConflictoExcepcion.java
 * Descripción: El estado de un registro no permite la operación, como subir fotos a un lote que ya no está
 *              pendiente (RF-05). La API responde 409 con el motivo.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.errores;

import lombok.Getter;

/**
 * El estado de un registro no permite la operación (409), como subir fotos a un lote que ya no está pendiente. La
 * respuesta dice el motivo en {@code detail}, con el mensaje de su clave.
 */
@Getter
public class ConflictoExcepcion extends RuntimeException {

    /** La clave del mensaje del motivo. */
    private final String clave;

    /**
     * Crea la excepción.
     *
     * @param clave la clave del mensaje del motivo
     */
    public ConflictoExcepcion(String clave) {
        super(clave);
        this.clave = clave;
    }
}
