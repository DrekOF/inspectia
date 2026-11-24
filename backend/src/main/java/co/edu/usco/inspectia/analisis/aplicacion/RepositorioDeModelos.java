/*
 * Nombre: RepositorioDeModelos.java
 * Descripción: Puerto de los modelos de IA registrados en la base (D-21, RF-08, D-39).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
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
}
