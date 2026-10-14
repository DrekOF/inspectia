/*
 * Nombre: DatoRepetidoExcepcion.java
 * Descripción: Un dato único ya existe; la API responde 409.
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.errores;

import java.util.List;

/** Un dato que debe ser único ya existe, como el NIT de una empresa. La API responde 409. */
public class DatoRepetidoExcepcion extends ExcepcionConCampos {

    /**
     * Crea la excepción.
     *
     * @param campos los datos repetidos
     */
    public DatoRepetidoExcepcion(List<CampoInvalido> campos) {
        super(campos);
    }
}
