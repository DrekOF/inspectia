/*
 * Nombre: RepositorioDeModelosJpa.java
 * Descripción: El puerto RepositorioDeModelos con JPA, sobre la tabla modelo (RF-08, D-39).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;

import co.edu.usco.inspectia.analisis.aplicacion.ModeloRegistrado;
import co.edu.usco.inspectia.analisis.aplicacion.RepositorioDeModelos;
import co.edu.usco.inspectia.analisis.dominio.RecallsDelModelo;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** El puerto {@link RepositorioDeModelos} con JPA, sobre la tabla {@code modelo}. */
@Component
public class RepositorioDeModelosJpa implements RepositorioDeModelos {

    private final ModelosJpa modelos;

    /**
     * Crea el adaptador; Spring le pasa el repositorio de Spring Data.
     *
     * @param modelos la tabla {@code modelo}
     */
    public RepositorioDeModelosJpa(ModelosJpa modelos) {
        this.modelos = modelos;
    }

    @Override
    public Optional<ModeloRegistrado> buscar(String nombre, String version) {
        return modelos.porNombreYVersion(nombre, version).map(modelo -> new ModeloRegistrado(modelo.getId(),
                modelo.getNombre(), modelo.getVersion(),
                new RecallsDelModelo(modelo.getRecallSano(), modelo.getRecallNoSano())));
    }

    @Override
    @Transactional
    public void registrarSiFalta(String nombre, String version, RecallsDelModelo recalls) {
        modelos.registrarSiFalta(nombre, version, recalls.recallSano(), recalls.recallNoSano());
    }
}
