/*
 * Nombre: RevisionDeFoto.java
 * Descripción: Lo que dice la revisión de una foto: aprobada o por repetir, con su motivo, cuántos granos válidos
 *              encontró y dónde están los granos pegados (RF-06, RF-07, D-45, D-46).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.MotivoDeFoto;
import java.util.List;
import java.util.Objects;

/**
 * La revisión de una foto (RF-06, RF-07, D-46): la rápida al confirmarla, la de la cola o la que pide repetirla por
 * granos dudosos (RF-09). Cumple los CHECK de {@code foto}: una foto aprobada no lleva motivo y una por repetir sí; solo
 * la de granos pegados lleva regiones.
 *
 * @param estado {@link EstadoDeFoto#APROBADA} o {@link EstadoDeFoto#POR_REPETIR}
 * @param motivo el motivo, solo con {@code POR_REPETIR}
 * @param granosEncontrados los granos válidos: sin los cortados por el borde, sin manchas y sin los de las regiones de
 *     granos pegados; desde 0
 * @param regionesPegadas dónde están los granos pegados; vacía salvo con {@link MotivoDeFoto#GRANOS_PEGADOS}
 */
public record RevisionDeFoto(EstadoDeFoto estado, MotivoDeFoto motivo, int granosEncontrados,
        List<Rectangulo> regionesPegadas) {

    /**
     * Revisa que la revisión cumpla las reglas de la foto.
     *
     * @throws IllegalArgumentException si el estado es CARGADA, si el motivo no va con el estado, si los granos son
     *     negativos o si lleva regiones sin el motivo de granos pegados
     */
    public RevisionDeFoto {
        Objects.requireNonNull(estado, "estado");
        regionesPegadas = List.copyOf(Objects.requireNonNullElse(regionesPegadas, List.of()));
        if (estado == EstadoDeFoto.CARGADA) {
            throw new IllegalArgumentException("Una revisión deja la foto aprobada o por repetir, no cargada");
        }
        if ((estado == EstadoDeFoto.POR_REPETIR) != (motivo != null)) {
            throw new IllegalArgumentException("Solo una foto por repetir lleva motivo: " + estado + " con " + motivo);
        }
        if (granosEncontrados < 0) {
            throw new IllegalArgumentException("Los granos encontrados no pueden ser negativos: " + granosEncontrados);
        }
        if (!regionesPegadas.isEmpty() && motivo != MotivoDeFoto.GRANOS_PEGADOS) {
            throw new IllegalArgumentException("Solo una foto con granos pegados lleva regiones");
        }
    }

    /**
     * Una foto aprobada.
     *
     * @param granosEncontrados los granos válidos que tiene
     * @return la revisión
     */
    public static RevisionDeFoto aprobada(int granosEncontrados) {
        return new RevisionDeFoto(EstadoDeFoto.APROBADA, null, granosEncontrados, List.of());
    }

    /**
     * Una foto por repetir, sin regiones.
     *
     * @param motivo por qué
     * @param granosEncontrados los granos válidos que tiene
     * @return la revisión
     */
    public static RevisionDeFoto porRepetir(MotivoDeFoto motivo, int granosEncontrados) {
        return new RevisionDeFoto(EstadoDeFoto.POR_REPETIR, Objects.requireNonNull(motivo, "motivo"),
                granosEncontrados, List.of());
    }

    /**
     * Si pide repetir la foto.
     *
     * @return {@code true} si está por repetir
     */
    public boolean pideRepetir() {
        return estado == EstadoDeFoto.POR_REPETIR;
    }
}
