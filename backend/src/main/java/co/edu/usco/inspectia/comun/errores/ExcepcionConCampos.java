/*
 * Nombre: ExcepcionConCampos.java
 * Descripción: Base de las excepciones de los servicios que señalan qué datos corregir.
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.errores;

import java.util.List;
import java.util.stream.Collectors;

/** Excepción de un servicio que señala qué datos corregir: cada uno con su campo y su mensaje. */
public abstract class ExcepcionConCampos extends RuntimeException {

    private final transient List<CampoInvalido> campos;

    /**
     * Crea la excepción.
     *
     * @param campos los datos que hay que corregir; al menos uno
     */
    protected ExcepcionConCampos(List<CampoInvalido> campos) {
        super(campos.stream().map(c -> c.campo() + ": " + c.clave()).collect(Collectors.joining(", ")));
        this.campos = List.copyOf(campos);
    }

    /**
     * Los datos que hay que corregir.
     *
     * @return la lista, que no se puede modificar
     */
    public List<CampoInvalido> getCampos() {
        return campos;
    }
}
