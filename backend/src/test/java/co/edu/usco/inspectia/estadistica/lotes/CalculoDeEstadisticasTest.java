/*
 * Nombre: CalculoDeEstadisticasTest.java
 * Descripción: Pruebas unitarias del cálculo de las estadísticas de la empresa (RF-16): el número de lotes, el
 *              promedio de granos sanos, los lotes por categoría, el promedio por mes en la hora de Colombia y el
 *              promedio por finca o proveedor, que no distingue mayúsculas, tildes ni los espacios del inicio y del
 *              final (RF-16 C2 y C4).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.estadistica.lotes;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.estadistica.lotes.Estadisticas.LotesPorCategoria;
import co.edu.usco.inspectia.estadistica.lotes.Estadisticas.PromedioPorMes;
import co.edu.usco.inspectia.estadistica.lotes.Estadisticas.PromedioPorOrigen;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** El cálculo, sin base de datos: recibe los lotes analizados del periodo y arma las cifras (RF-16). */
class CalculoDeEstadisticasTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 9, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 11, 30);

    /** Un lote analizado con su finca, su porcentaje, su categoría y la fecha del resultado con su zona. */
    private static LoteAnalizado lote(String origen, String porcentaje, String categoria, String fecha) {
        return new LoteAnalizado(origen, new BigDecimal(porcentaje), categoria, OffsetDateTime.parse(fecha).toInstant());
    }

    private static Estadisticas calcular(LoteAnalizado... lotes) {
        return CalculoDeEstadisticas.calcular(DESDE, HASTA, List.of(lotes));
    }

    @Test
    @DisplayName("RF-16 · C2 · sin lotes en el periodo, la respuesta lo dice: 0 lotes, sin promedio, sin meses ni fincas y las 4 categorías en 0")
    void sinLotes() {
        Estadisticas estadisticas = calcular();

        assertThat(estadisticas.sinLotes()).isTrue();
        assertThat(estadisticas.lotesAnalizados()).isZero();
        assertThat(estadisticas.promedioSanos()).isNull();
        assertThat(estadisticas.porMes()).isEmpty();
        assertThat(estadisticas.porOrigen()).isEmpty();
        assertThat(estadisticas.porCategoria()).containsExactly(new LotesPorCategoria("PREMIUM", 0),
                new LotesPorCategoria("ESTANDAR", 0), new LotesPorCategoria("CORRIENTE", 0),
                new LotesPorCategoria("FUERA_DE_NORMA", 0));
        assertThat(estadisticas.desde()).isEqualTo(DESDE);
        assertThat(estadisticas.hasta()).isEqualTo(HASTA);
    }

    @Test
    @DisplayName("RF-16 · el promedio de granos sanos es la media de los resultados de los lotes, redondeada a 1 decimal (restricción 2)")
    void promedioDeLosLotes() {
        Estadisticas estadisticas = calcular(
                lote("Finca A", "90.0", "ESTANDAR", "2026-11-10T10:00-05:00"),
                lote("Finca A", "91.0", "ESTANDAR", "2026-11-11T10:00-05:00"),
                lote("Finca B", "92.2", "ESTANDAR", "2026-11-12T10:00-05:00"));

        assertThat(estadisticas.sinLotes()).isFalse();
        assertThat(estadisticas.lotesAnalizados()).isEqualTo(3);
        // 273,2 / 3 = 91,0666… → 91,1
        assertThat(estadisticas.promedioSanos()).isEqualByComparingTo("91.1");
        assertThat(estadisticas.promedioSanos().scale()).isEqualTo(1);
    }

    @Test
    @DisplayName("RF-16 · el promedio que queda justo en la mitad de un décimo se redondea hacia arriba, como el resultado del lote (prueba de límite)")
    void redondeoEnLaMitad() {
        Estadisticas estadisticas = calcular(
                lote("Finca A", "90.0", "ESTANDAR", "2026-11-10T10:00-05:00"),
                lote("Finca A", "90.1", "ESTANDAR", "2026-11-11T10:00-05:00"));

        assertThat(estadisticas.promedioSanos()).isEqualByComparingTo("90.1");
    }

    @Test
    @DisplayName("RF-16 · los lotes por categoría van en el orden de los rangos, con las 4 aunque alguna no tenga lotes")
    void porCategoria() {
        Estadisticas estadisticas = calcular(
                lote("Finca A", "97.0", "PREMIUM", "2026-11-10T10:00-05:00"),
                lote("Finca A", "70.0", "FUERA_DE_NORMA", "2026-11-11T10:00-05:00"),
                lote("Finca A", "98.0", "PREMIUM", "2026-11-12T10:00-05:00"));

        assertThat(estadisticas.porCategoria()).containsExactly(new LotesPorCategoria("PREMIUM", 2),
                new LotesPorCategoria("ESTANDAR", 0), new LotesPorCategoria("CORRIENTE", 0),
                new LotesPorCategoria("FUERA_DE_NORMA", 1));
    }

    @Test
    @DisplayName("RF-16 · el promedio por mes usa el mes de Colombia: un resultado del 30 de noviembre a las 23:30 de Bogotá, que en UTC ya es diciembre, cuenta en noviembre (prueba de límite)")
    void mesEnColombia() {
        Estadisticas estadisticas = calcular(
                lote("Finca A", "90.0", "ESTANDAR", "2026-11-30T23:30-05:00"),
                lote("Finca A", "94.0", "ESTANDAR", "2026-11-01T00:00-05:00"));

        assertThat(estadisticas.porMes()).containsExactly(new PromedioPorMes("2026-11", 2, new BigDecimal("92.0")));
    }

    @Test
    @DisplayName("RF-16 · la evolución por mes va del primer mes con lotes al último, en orden, y los meses del medio sin lotes van con 0 lotes y sin promedio, para no unir puntos que no existen")
    void mesesSinLotes() {
        Estadisticas estadisticas = calcular(
                lote("Finca A", "95.0", "PREMIUM", "2026-11-05T10:00-05:00"),
                lote("Finca A", "85.0", "CORRIENTE", "2026-09-05T10:00-05:00"),
                lote("Finca A", "86.0", "CORRIENTE", "2026-09-06T10:00-05:00"));

        assertThat(estadisticas.porMes()).containsExactly(
                new PromedioPorMes("2026-09", 2, new BigDecimal("85.5")),
                new PromedioPorMes("2026-10", 0, null),
                new PromedioPorMes("2026-11", 1, new BigDecimal("95.0")));
    }

    @Test
    @DisplayName("RF-16 · C4 · «La Esperanza» y «la esperanza » cuentan como la misma finca: sin distinguir mayúsculas ni los espacios del inicio y del final")
    void mismaFincaEscritaDeOtraForma() {
        Estadisticas estadisticas = calcular(
                lote("La Esperanza", "90.0", "ESTANDAR", "2026-11-10T10:00-05:00"),
                lote("la esperanza ", "92.0", "ESTANDAR", "2026-11-11T10:00-05:00"),
                lote("  LA ESPERANZA", "94.0", "ESTANDAR", "2026-11-12T10:00-05:00"),
                lote("Bellavista", "80.0", "CORRIENTE", "2026-11-13T10:00-05:00"));

        assertThat(estadisticas.porOrigen()).hasSize(2);
        assertThat(estadisticas.porOrigen()).extracting(PromedioPorOrigen::lotes).containsExactly(3L, 1L);
        assertThat(estadisticas.porOrigen()).extracting(PromedioPorOrigen::promedioSanos)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("92.0"), new BigDecimal("80.0"));
    }

    @Test
    @DisplayName("RF-16 · «Paraíso» y «Paraiso» también cuentan como la misma finca: las tildes no la separan, como en las sugerencias (decisión de Cristhian del 23/11/2026)")
    void mismaFincaConYSinTildes() {
        Estadisticas estadisticas = calcular(
                lote("Paraíso", "80.0", "CORRIENTE", "2026-11-12T10:00-05:00"),
                lote("Paraiso", "82.0", "CORRIENTE", "2026-11-13T10:00-05:00"),
                lote("  PARAÍSO", "84.0", "CORRIENTE", "2026-11-14T10:00-05:00"));

        assertThat(estadisticas.porOrigen()).hasSize(1);
        assertThat(estadisticas.porOrigen()).extracting(PromedioPorOrigen::lotes).containsExactly(3L);
        assertThat(estadisticas.porOrigen()).extracting(PromedioPorOrigen::promedioSanos)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("82.0"));
    }

    @Test
    @DisplayName("RF-16 · la finca se muestra con la forma más usada en el periodo y, si empatan, con la del resultado más reciente, sin los espacios del inicio y del final")
    void formaQueSeMuestra() {
        Estadisticas estadisticas = calcular(
                lote("Paraiso", "80.0", "CORRIENTE", "2026-11-12T10:00-05:00"),
                lote("Paraíso", "82.0", "CORRIENTE", "2026-11-13T10:00-05:00"),
                lote("Paraíso", "84.0", "CORRIENTE", "2026-11-14T10:00-05:00"),
                lote("la esperanza ", "90.0", "ESTANDAR", "2026-11-10T10:00-05:00"),
                lote("La Esperanza", "92.0", "ESTANDAR", "2026-11-11T10:00-05:00"));

        assertThat(estadisticas.porOrigen()).extracting(PromedioPorOrigen::origen)
                .containsExactly("La Esperanza", "Paraíso");
    }

    @Test
    @DisplayName("RF-16 · las fincas van de mayor a menor promedio; si empatan, la de más lotes primero y después por nombre")
    void ordenDeLasFincas() {
        Estadisticas estadisticas = calcular(
                lote("Bellavista", "85.0", "CORRIENTE", "2026-11-10T10:00-05:00"),
                lote("Arenal", "95.0", "PREMIUM", "2026-11-10T10:00-05:00"),
                lote("Cedral", "85.0", "CORRIENTE", "2026-11-10T10:00-05:00"),
                lote("Cedral", "85.0", "CORRIENTE", "2026-11-11T10:00-05:00"),
                lote("Álamo", "85.0", "CORRIENTE", "2026-11-11T10:00-05:00"));

        assertThat(estadisticas.porOrigen()).extracting(PromedioPorOrigen::origen)
                .containsExactly("Arenal", "Cedral", "Álamo", "Bellavista");
    }
}
