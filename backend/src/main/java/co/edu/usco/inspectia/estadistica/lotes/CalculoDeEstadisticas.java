/*
 * Nombre: CalculoDeEstadisticas.java
 * Descripción: Arma las estadísticas de la empresa a partir de sus lotes analizados en el periodo (RF-16): el número
 *              de lotes, el promedio de granos sanos, los lotes por categoría, el promedio por mes de Colombia y el
 *              promedio por finca o proveedor, agrupando la misma finca escrita de otra forma (RF-16 C4).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.estadistica.lotes;

import co.edu.usco.inspectia.comun.texto.Textos;
import co.edu.usco.inspectia.estadistica.lotes.Estadisticas.LotesPorCategoria;
import co.edu.usco.inspectia.estadistica.lotes.Estadisticas.PromedioPorMes;
import co.edu.usco.inspectia.estadistica.lotes.Estadisticas.PromedioPorOrigen;
import co.edu.usco.inspectia.rango.RangoRecomendacionCategoria;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * El cálculo de las estadísticas, sin Spring ni base de datos (RF-16). Cada promedio es la media de los resultados de
 * los lotes (RF-16 restricción 2), redondeada a 1 decimal con la mitad hacia arriba, como el resultado de un lote.
 */
public final class CalculoDeEstadisticas {

    /** Los meses son de Colombia, como los días del periodo («Fechas» en {@code specs/modelo-datos.md}). */
    static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");

    /** Los decimales de los promedios: los mismos del porcentaje de un lote. */
    private static final int DECIMALES = 1;

    private CalculoDeEstadisticas() {
    }

    /**
     * Las estadísticas de unos lotes analizados.
     *
     * @param desde el primer día del periodo, o {@code null}; solo se devuelve con las cifras
     * @param hasta el último día del periodo, o {@code null}; solo se devuelve con las cifras
     * @param lotes los lotes analizados de la empresa en el periodo
     * @return las estadísticas
     */
    public static Estadisticas calcular(LocalDate desde, LocalDate hasta, List<LoteAnalizado> lotes) {
        return new Estadisticas(desde, hasta, lotes.isEmpty(), lotes.size(), promedio(lotes), porCategoria(lotes),
                porMes(lotes), porOrigen(lotes));
    }

    /** La media de los porcentajes, con 1 decimal, o {@code null} sin lotes. */
    private static BigDecimal promedio(List<LoteAnalizado> lotes) {
        if (lotes.isEmpty()) {
            return null;
        }
        BigDecimal suma = lotes.stream().map(LoteAnalizado::porcentajeSanos).reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.divide(BigDecimal.valueOf(lotes.size()), DECIMALES, RoundingMode.HALF_UP);
    }

    /** Las 4 categorías, en el orden de los rangos, con sus lotes; las que no tienen, en 0. */
    private static List<LotesPorCategoria> porCategoria(List<LoteAnalizado> lotes) {
        return Arrays.stream(RangoRecomendacionCategoria.values())
                .map(categoria -> new LotesPorCategoria(categoria.name(),
                        lotes.stream().filter(lote -> categoria.name().equals(lote.categoria())).count()))
                .toList();
    }

    /**
     * El promedio de cada mes de Colombia, del primero con lotes al último. Los meses del medio sin lotes van con 0
     * lotes y sin promedio: así la evolución no une dos meses como si fueran seguidos.
     */
    private static List<PromedioPorMes> porMes(List<LoteAnalizado> lotes) {
        TreeMap<YearMonth, List<LoteAnalizado>> porMes = new TreeMap<>();
        for (LoteAnalizado lote : lotes) {
            porMes.computeIfAbsent(YearMonth.from(lote.fechaResultado().atZone(COLOMBIA)), mes -> new ArrayList<>())
                    .add(lote);
        }
        List<PromedioPorMes> meses = new ArrayList<>();
        if (porMes.isEmpty()) {
            return meses;
        }
        for (YearMonth mes = porMes.firstKey(); !mes.isAfter(porMes.lastKey()); mes = mes.plusMonths(1)) {
            List<LoteAnalizado> delMes = porMes.getOrDefault(mes, List.of());
            meses.add(new PromedioPorMes(mes.toString(), delMes.size(), promedio(delMes)));
        }
        return meses;
    }

    /**
     * El promedio de cada finca o proveedor. Las formas con la misma {@link Textos#claveDeBusqueda clave de búsqueda}
     * son la misma finca: «La Esperanza» y «la esperanza », o «Paraíso» y «Paraiso» (RF-16 C4). Se muestra con la forma
     * más usada en el periodo y, si empatan, con la del resultado más reciente, sin los espacios del inicio y del
     * final. Van de mayor a menor promedio; si empatan, primero la de más lotes y después por su clave.
     */
    private static List<PromedioPorOrigen> porOrigen(List<LoteAnalizado> lotes) {
        Map<String, Finca> porClave = new HashMap<>();
        for (LoteAnalizado lote : lotes) {
            porClave.computeIfAbsent(Textos.claveDeBusqueda(lote.origen()), clave -> new Finca()).agregar(lote);
        }
        return porClave.values().stream()
                .map(finca -> new PromedioPorOrigen(finca.formaPreferida(), finca.lotes.size(), promedio(finca.lotes)))
                .sorted(Comparator.comparing(PromedioPorOrigen::promedioSanos).reversed()
                        .thenComparing(Comparator.comparingLong(PromedioPorOrigen::lotes).reversed())
                        .thenComparing(origen -> Textos.claveDeBusqueda(origen.origen())))
                .toList();
    }

    /** Los lotes de una finca, con cuántas veces y cuándo por última vez se usó cada forma de escribirla. */
    private static final class Finca {

        private final List<LoteAnalizado> lotes = new ArrayList<>();
        private final Map<String, Integer> veces = new HashMap<>();
        private final Map<String, Instant> ultimaVez = new HashMap<>();

        void agregar(LoteAnalizado lote) {
            lotes.add(lote);
            String forma = Textos.recortar(lote.origen());
            veces.merge(forma, 1, Integer::sum);
            ultimaVez.merge(forma, lote.fechaResultado(), (una, otra) -> una.isAfter(otra) ? una : otra);
        }

        /** La forma más usada y, si empatan, la del resultado más reciente; si también empatan, la primera en orden. */
        String formaPreferida() {
            return veces.keySet().stream()
                    .max(Comparator.comparing((String forma) -> veces.get(forma))
                            .thenComparing(ultimaVez::get)
                            .thenComparing(Comparator.<String>reverseOrder()))
                    .orElseThrow();
        }
    }
}
