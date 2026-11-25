/*
 * Nombre: Clasificacion.java
 * Descripción: Lo que responde el clasificador de granos para una muestra: el modelo que la clasificó, sus granos y, con
 *              el servicio de IA, la revisión de cada foto (RF-06, RF-07, RF-08, D-46).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * La clasificación de los granos de una muestra (RF-07, RF-08). El servicio de IA revisa cada foto antes de clasificarla
 * (D-46) y responde esa revisión por número de foto; el clasificador falso no revisa, y la deja vacía.
 *
 * @param modelo el modelo que los clasificó
 * @param granos los granos de todas las fotos, en el orden en que se guardan; ninguno de una foto por repetir
 * @param revisiones la revisión de cada foto, por número; vacía si el clasificador no revisa
 */
public record Clasificacion(ModeloUsado modelo, List<GranoClasificado> granos, Map<Integer, RevisionDeFoto> revisiones) {

    /**
     * Copia las listas.
     */
    public Clasificacion {
        granos = List.copyOf(Objects.requireNonNull(granos, "granos"));
        revisiones = Map.copyOf(Objects.requireNonNullElse(revisiones, Map.of()));
    }

    /**
     * Una clasificación sin la revisión de las fotos, como la del clasificador falso.
     *
     * @param modelo el modelo que los clasificó
     * @param granos los granos de todas las fotos
     */
    public Clasificacion(ModeloUsado modelo, List<GranoClasificado> granos) {
        this(modelo, granos, Map.of());
    }
}
