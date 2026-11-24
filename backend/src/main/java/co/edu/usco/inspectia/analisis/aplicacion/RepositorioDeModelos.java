/*
 * Nombre: RepositorioDeModelos.java
 * Descripción: Puerto de los modelos de IA registrados en la base (D-21, RF-08, D-39): buscarlos, registrarlos y
 *              marcar el que está en uso (D-18, D-48).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.RecallsDelModelo;
import java.util.Optional;

/**
 * Puerto de la tabla {@code modelo} (D-21), común a todas las empresas (RNF-03). Lo implementa el adaptador de JPA.
 */
public interface RepositorioDeModelos {

    /**
     * Un modelo por su nombre y su versión.
     *
     * @param nombre el nombre
     * @param version la versión
     * @return el modelo, o vacío si no está registrado
     */
    Optional<ModeloRegistrado> buscar(String nombre, String version);

    /**
     * Registra un modelo sin las métricas de RF-17 y sin ponerlo en uso, si no estaba: si ya hay uno con ese nombre y
     * esa versión, no lo cambia. Lo usa el clasificador falso al arrancar.
     *
     * @param nombre el nombre
     * @param version la versión
     * @param recalls el recall de cada clase
     */
    void registrarSiFalta(String nombre, String version, RecallsDelModelo recalls);

    /**
     * Registra un modelo de la comparación con todas sus métricas y sin ponerlo en uso, si no estaba: si ya hay uno con
     * ese nombre y esa versión, no lo cambia. Lo usa el servidor al arrancar con el servicio de IA (D-18, D-48).
     *
     * @param modelo el modelo y sus métricas
     * @return {@code true} si lo registró; {@code false} si ya estaba
     */
    boolean registrarSiFalta(MetricasDelModelo modelo);

    /**
     * Deja en uso solo ese modelo: el que carga el servicio de IA (RF-17). La base exige que tenga todas sus métricas y
     * un F1 macro de 65 % o más (P-02).
     *
     * @param nombre el nombre
     * @param version la versión
     * @throws IllegalStateException si el modelo no está registrado
     */
    void ponerEnUso(String nombre, String version);
}
