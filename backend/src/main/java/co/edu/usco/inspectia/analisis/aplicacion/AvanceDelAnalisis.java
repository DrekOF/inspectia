/*
 * Nombre: AvanceDelAnalisis.java
 * Descripción: El avance del análisis de la muestra en curso de un lote: su número, su estado, sus fechas, si el último
 *              envío volvió sin resultado, por qué y qué fotos hay que repetir (RNF-01 C4, RNF-02 C3, RF-09, D-44).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeMuestra;
import co.edu.usco.inspectia.analisis.dominio.MotivoDeDevolucion;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * El avance del análisis de la muestra en curso de un lote (RNF-01 C4, RNF-02 C3): la página lo pregunta mientras
 * espera el resultado. No dice la posición en la cola, que es de todas las empresas (RNF-03).
 *
 * @param muestra el número de la muestra en curso, o {@code null} si el lote no tiene muestra
 * @param estado su estado, o {@code null} si el lote no tiene muestra
 * @param fechaEnvio cuándo se envió a analizar, o {@code null}
 * @param fechaInicio cuándo empezó el análisis, o {@code null}
 * @param fechaFin cuándo terminó, o {@code null}
 * @param fallo si el último envío volvió sin resultado: la muestra está pendiente y tiene fecha de envío
 * @param motivoDevolucion por qué volvió, o {@code null} si no volvió o la devolvió una foto (D-44)
 * @param repeticiones las fotos que RF-09 pidió repetir en esta muestra, de 0 a 2
 * @param fotosPorRepetir las fotos de la muestra que están por repetir, por número
 */
public record AvanceDelAnalisis(Integer muestra, EstadoDeMuestra estado, Instant fechaEnvio, Instant fechaInicio,
        Instant fechaFin, boolean fallo, MotivoDeDevolucion motivoDevolucion, int repeticiones,
        List<FotoPorRepetir> fotosPorRepetir) {

    /**
     * Copia la lista.
     */
    public AvanceDelAnalisis {
        fotosPorRepetir = List.copyOf(Objects.requireNonNullElse(fotosPorRepetir, List.of()));
    }

    /**
     * Un avance sin motivo, sin repeticiones y sin fotos por repetir.
     *
     * @param muestra el número de la muestra, o {@code null}
     * @param estado su estado, o {@code null}
     * @param fechaEnvio cuándo se envió, o {@code null}
     * @param fechaInicio cuándo empezó, o {@code null}
     * @param fechaFin cuándo terminó, o {@code null}
     * @param fallo si el último envío volvió sin resultado
     */
    public AvanceDelAnalisis(Integer muestra, EstadoDeMuestra estado, Instant fechaEnvio, Instant fechaInicio,
            Instant fechaFin, boolean fallo) {
        this(muestra, estado, fechaEnvio, fechaInicio, fechaFin, fallo, null, 0, List.of());
    }

    /**
     * El avance de un lote que todavía no tiene muestra.
     *
     * @return el avance vacío
     */
    public static AvanceDelAnalisis sinMuestra() {
        return new AvanceDelAnalisis(null, null, null, null, null, false);
    }

    /**
     * El avance de una muestra; si está pendiente con fecha de envío, el último envío falló.
     *
     * @param numero su número
     * @param estado su estado
     * @param fechaEnvio cuándo se envió, o {@code null}
     * @param fechaInicio cuándo empezó el análisis, o {@code null}
     * @param fechaFin cuándo terminó, o {@code null}
     * @return el avance
     */
    public static AvanceDelAnalisis de(int numero, EstadoDeMuestra estado, Instant fechaEnvio, Instant fechaInicio,
            Instant fechaFin) {
        return de(numero, estado, fechaEnvio, fechaInicio, fechaFin, null, 0, List.of());
    }

    /**
     * El avance de una muestra con lo que dejó la cola si la devolvió.
     *
     * @param numero su número
     * @param estado su estado
     * @param fechaEnvio cuándo se envió, o {@code null}
     * @param fechaInicio cuándo empezó el análisis, o {@code null}
     * @param fechaFin cuándo terminó, o {@code null}
     * @param motivoDevolucion por qué volvió, o {@code null}
     * @param repeticiones las repeticiones que pidió RF-09
     * @param fotosPorRepetir las fotos por repetir
     * @return el avance
     */
    public static AvanceDelAnalisis de(int numero, EstadoDeMuestra estado, Instant fechaEnvio, Instant fechaInicio,
            Instant fechaFin, MotivoDeDevolucion motivoDevolucion, int repeticiones,
            List<FotoPorRepetir> fotosPorRepetir) {
        boolean fallo = estado == EstadoDeMuestra.PENDIENTE && fechaEnvio != null;
        return new AvanceDelAnalisis(numero, estado, fechaEnvio, fechaInicio, fechaFin, fallo, motivoDevolucion,
                repeticiones, fotosPorRepetir);
    }
}
