/*
 * Nombre: NoEncontradoExcepcion.java
 * Descripción: El registro pedido no existe; la API responde 404.
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.errores;

/**
 * El registro pedido no existe. La API responde 404; también cuando es de otra empresa, para no
 * revelar que existe (RNF-03).
 */
public class NoEncontradoExcepcion extends RuntimeException {

    /**
     * Crea la excepción.
     *
     * @param descripcion qué se buscó, para el registro del servidor; no se muestra al usuario
     */
    public NoEncontradoExcepcion(String descripcion) {
        super(descripcion);
    }
}
