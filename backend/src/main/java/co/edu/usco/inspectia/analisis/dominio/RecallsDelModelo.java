/*
 * Nombre: RecallsDelModelo.java
 * Descripción: El recall de cada clase de un modelo de IA, en porcentaje, con que se corrige el porcentaje de granos
 *              sanos del lote (D-39, RF-10).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * El recall de cada clase de un modelo (D-39), en porcentaje de 0 a 100 y con 2 decimales, como las columnas
 * {@code modelo.recall_sano} y {@code modelo.recall_no_sano}. Con EfficientNet-B0 son 91,20 y 53,40.
 *
 * <p>Solo sirve un modelo mejor que el azar: {@code recallSano + recallNoSano > 100}. Si no, la corrección dividiría
 * por cero o daría el resultado al revés; la base tiene el mismo CHECK ({@code modelo_mejor_que_el_azar}).
 *
 * @param recallSano el recall de la clase sano, en porcentaje
 * @param recallNoSano el recall de la clase no sano («mala» en D-39), en porcentaje
 */
public record RecallsDelModelo(BigDecimal recallSano, BigDecimal recallNoSano) {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    /**
     * Revisa los recall.
     *
     * @throws IllegalArgumentException si falta uno, si no van de 0 a 100 o si el modelo no es mejor que el azar
     */
    public RecallsDelModelo {
        Objects.requireNonNull(recallSano, "recallSano");
        Objects.requireNonNull(recallNoSano, "recallNoSano");
        revisarPorcentaje(recallSano, "recallSano");
        revisarPorcentaje(recallNoSano, "recallNoSano");
        if (recallSano.add(recallNoSano).compareTo(CIEN) <= 0) {
            throw new IllegalArgumentException("Los recall " + recallSano + " y " + recallNoSano
                    + " no suman más de 100: el modelo no es mejor que el azar (D-39)");
        }
    }

    /**
     * Cuánto se equivoca el modelo con los granos no sanos: el porcentaje que marca como sanos,
     * {@code 100 − recallNoSano}. Con EfficientNet-B0, 46,60.
     *
     * @return el porcentaje de no sanos que el modelo marca como sanos
     */
    public BigDecimal noSanosMarcadosComoSanos() {
        return CIEN.subtract(recallNoSano);
    }

    /**
     * Cuánto separa el modelo las dos clases, en puntos: {@code recallSano + recallNoSano − 100}. Es el denominador de
     * la corrección (D-39) y siempre es mayor que 0. Con EfficientNet-B0, 44,60.
     *
     * @return la separación, en puntos de porcentaje
     */
    public BigDecimal separacion() {
        return recallSano.add(recallNoSano).subtract(CIEN);
    }

    private static void revisarPorcentaje(BigDecimal valor, String nombre) {
        if (valor.signum() < 0 || valor.compareTo(CIEN) > 0) {
            throw new IllegalArgumentException(nombre + " debe ir de 0 a 100, y es " + valor);
        }
    }
}
