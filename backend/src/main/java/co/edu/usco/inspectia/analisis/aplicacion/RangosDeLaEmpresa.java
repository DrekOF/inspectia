/*
 * Nombre: RangosDeLaEmpresa.java
 * Descripción: Puerto de los rangos de recomendación vigentes de una empresa, para calcular su categoría probable (RF-12,
 *              RF-13, D-21).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.RangoDeCategoria;
import java.util.List;
import java.util.UUID;

/**
 * Puerto de los rangos de recomendación de una empresa (RF-13). El análisis usa los vigentes al terminar la muestra
 * (RF-13 C2) y el lote guarda una copia de la categoría y del texto, así un cambio posterior no lo cambia (RF-13 C3).
 */
public interface RangosDeLaEmpresa {

    /**
     * Los rangos vigentes de la empresa: las 4 categorías. Si a la empresa le falta alguna, va la de por defecto
     * (D-10), sin guardarla.
     *
     * @param empresaId la empresa
     * @return los rangos
     */
    List<RangoDeCategoria> vigentes(UUID empresaId);
}
