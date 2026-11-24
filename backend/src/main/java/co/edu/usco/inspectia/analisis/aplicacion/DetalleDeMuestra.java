/*
 * Nombre: DetalleDeMuestra.java
 * Descripción: Una muestra en el resultado de un lote: sus datos, su porcentaje contado, sus fotos marcadas y las que
 *              hay que repetir si volvió de la cola (RF-09, RF-11, RF-14 C3, D-44).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Una muestra del resultado (RF-11, RF-14 C3).
 *
 * @param muestra sus datos guardados
 * @param porcentajeContado el porcentaje que contó el modelo, o {@code null} mientras no termina
 * @param fotos sus fotos, por número, con sus granos marcados
 * @param fotosPorRepetir sus fotos por repetir, por número; vacía si no hay o si ya terminó
 */
public record DetalleDeMuestra(MuestraGuardada muestra, BigDecimal porcentajeContado, List<FotoMarcada> fotos,
        List<FotoPorRepetir> fotosPorRepetir) {

    /**
     * Copia la lista.
     */
    public DetalleDeMuestra {
        fotosPorRepetir = List.copyOf(Objects.requireNonNullElse(fotosPorRepetir, List.of()));
    }

    /**
     * Una muestra sin fotos por repetir.
     *
     * @param muestra sus datos guardados
     * @param porcentajeContado el porcentaje contado, o {@code null}
     * @param fotos sus fotos marcadas
     */
    public DetalleDeMuestra(MuestraGuardada muestra, BigDecimal porcentajeContado, List<FotoMarcada> fotos) {
        this(muestra, porcentajeContado, fotos, List.of());
    }
}
