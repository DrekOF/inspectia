/*
 * Nombre: ReglasDeRecomendacionTest.java
 * Descripción: Pruebas unitarias de la recomendación: la categoría probable con los rangos de la empresa y cuándo se
 *              sugiere otra muestra, con los ejemplos de RF-12 y sus límites (RF-12, D-24, D-25).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** La recomendación (RF-12), con los rangos por defecto de D-10 salvo donde la prueba dice otros. */
class ReglasDeRecomendacionTest {

    static final String TEXTO_PREMIUM = "Apto para mercado especial o de exportación.";
    static final String TEXTO_ESTANDAR = "Apto para venta estándar.";
    static final String TEXTO_CORRIENTE = "Retirar los granos defectuosos antes de vender.";
    static final String TEXTO_FUERA = "No venderlo como cacao de calidad.";

    /** Los rangos por defecto (D-10), en desorden: las reglas ordenan por el límite, no por la lista. */
    static final List<RangoDeCategoria> POR_DEFECTO = List.of(
            new RangoDeCategoria("CORRIENTE", new BigDecimal("82.0"), TEXTO_CORRIENTE),
            new RangoDeCategoria("FUERA_DE_NORMA", new BigDecimal("0.0"), TEXTO_FUERA),
            new RangoDeCategoria("PREMIUM", new BigDecimal("96.0"), TEXTO_PREMIUM),
            new RangoDeCategoria("ESTANDAR", new BigDecimal("90.0"), TEXTO_ESTANDAR));

    private static final BigDecimal UN_PUNTO = BigDecimal.ONE;

    private static Recomendacion conUnaMuestra(String porcentaje) {
        return ReglasDeRecomendacion.recomendar(new BigDecimal(porcentaje), POR_DEFECTO, 1, UN_PUNTO);
    }

    @Test
    @DisplayName("RF-12 · C1 · con 99 %, la categoría es Premium o Especial, sin sugerencia: está a 3 puntos de 96")
    void premium() {
        assertThat(conUnaMuestra("99.0")).isEqualTo(new Recomendacion("PREMIUM", TEXTO_PREMIUM, false));
    }

    @Test
    @DisplayName("RF-12 · C2 · con 93 %, la categoría es Estándar, sin sugerencia: está a 3 puntos de 90 y de 96")
    void estandar() {
        assertThat(conUnaMuestra("93.0")).isEqualTo(new Recomendacion("ESTANDAR", TEXTO_ESTANDAR, false));
    }

    @Test
    @DisplayName("RF-12 · C3 · con 90,9 % es Estándar y sugiere otra muestra (a 0,9 de 90); con 91,0 % no la sugiere (a 1 punto): límite del margen")
    void limiteDelMargen() {
        assertThat(conUnaMuestra("90.9")).isEqualTo(new Recomendacion("ESTANDAR", TEXTO_ESTANDAR, true));
        assertThat(conUnaMuestra("91.0")).isEqualTo(new Recomendacion("ESTANDAR", TEXTO_ESTANDAR, false));
    }

    @Test
    @DisplayName("RF-12 · C4 · con 90,0 % la categoría es Estándar y con 89,9 % es Corriente; los dos sugieren otra muestra (prueba de límite)")
    void limiteDeLaCategoria() {
        assertThat(conUnaMuestra("90.0")).isEqualTo(new Recomendacion("ESTANDAR", TEXTO_ESTANDAR, true));
        assertThat(conUnaMuestra("89.9")).isEqualTo(new Recomendacion("CORRIENTE", TEXTO_CORRIENTE, true));
    }

    @Test
    @DisplayName("RF-12 · C5 · con 75 %, la categoría es Fuera de norma, sin sugerencia")
    void fueraDeNorma() {
        assertThat(conUnaMuestra("75.0")).isEqualTo(new Recomendacion("FUERA_DE_NORMA", TEXTO_FUERA, false));
    }

    @Test
    @DisplayName("RF-12 · el 0 de Fuera de norma no cuenta para sugerir otra muestra: 0,0 % y 0,5 % no la sugieren")
    void elCeroNoCuenta() {
        assertThat(conUnaMuestra("0.0")).isEqualTo(new Recomendacion("FUERA_DE_NORMA", TEXTO_FUERA, false));
        assertThat(conUnaMuestra("0.5")).isEqualTo(new Recomendacion("FUERA_DE_NORMA", TEXTO_FUERA, false));
    }

    @Test
    @DisplayName("RF-12 · también sugiere otra muestra por debajo y por encima de los límites de Premium y Corriente")
    void sugiereCercaDeCadaLimite() {
        assertThat(conUnaMuestra("95.1").sugerirOtraMuestra()).isTrue();
        assertThat(conUnaMuestra("96.9").sugerirOtraMuestra()).isTrue();
        assertThat(conUnaMuestra("97.0").sugerirOtraMuestra()).isFalse();
        assertThat(conUnaMuestra("81.1").sugerirOtraMuestra()).isTrue();
        assertThat(conUnaMuestra("81.0").sugerirOtraMuestra()).isFalse();
        assertThat(conUnaMuestra("100.0")).isEqualTo(new Recomendacion("PREMIUM", TEXTO_PREMIUM, false));
    }

    @Test
    @DisplayName("RF-12 · C9 · si la empresa cambió sus rangos, se usan los de la empresa: con Estándar desde 91 %, 90,6 % es Corriente y sugiere otra muestra")
    void rangosDeLaEmpresa() {
        List<RangoDeCategoria> propios = List.of(
                new RangoDeCategoria("PREMIUM", new BigDecimal("97.5"), "Exportación"),
                new RangoDeCategoria("ESTANDAR", new BigDecimal("91.0"), "Venta de la casa"),
                new RangoDeCategoria("CORRIENTE", new BigDecimal("80.0"), "Selección a mano"),
                new RangoDeCategoria("FUERA_DE_NORMA", new BigDecimal("0.0"), "Reproceso"));

        Recomendacion recomendacion = ReglasDeRecomendacion.recomendar(new BigDecimal("90.6"), propios, 1, UN_PUNTO);
        Recomendacion premium = ReglasDeRecomendacion.recomendar(new BigDecimal("97.0"), propios, 1, UN_PUNTO);

        assertThat(recomendacion).isEqualTo(new Recomendacion("CORRIENTE", "Selección a mano", true));
        assertThat(premium).isEqualTo(new Recomendacion("ESTANDAR", "Venta de la casa", true));
    }

    @Test
    @DisplayName("D-24 · a cuántos puntos se sugiere otra muestra es un parámetro: con 2 puntos, 91,9 % la sugiere y 92,0 % no, porque queda justo a 2 de 90")
    void puntosParaOtraMuestra() {
        Recomendacion dosPuntos = ReglasDeRecomendacion.recomendar(new BigDecimal("92.0"), POR_DEFECTO, 1,
                new BigDecimal("2"));

        assertThat(dosPuntos.sugerirOtraMuestra()).isFalse();
        assertThat(ReglasDeRecomendacion.recomendar(new BigDecimal("91.9"), POR_DEFECTO, 1, new BigDecimal("2"))
                .sugerirOtraMuestra()).isTrue();
    }

    @Test
    @DisplayName("RF-12 · con 2 muestras ya no se sugiere otra, aunque quede cerca de un límite (D-25)")
    void conDosMuestrasNoSugiere() {
        Recomendacion recomendacion = ReglasDeRecomendacion.recomendar(new BigDecimal("96.9"), POR_DEFECTO, 2,
                UN_PUNTO);

        assertThat(recomendacion).isEqualTo(new Recomendacion("PREMIUM", TEXTO_PREMIUM, false));
    }

    @Test
    @DisplayName("D-25 · un lote tiene 1 o 2 muestras: con 0 o 3 no hay recomendación")
    void unaODosMuestras() {
        BigDecimal porcentaje = new BigDecimal("90.0");

        assertThatThrownBy(() -> ReglasDeRecomendacion.recomendar(porcentaje, POR_DEFECTO, 0, UN_PUNTO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReglasDeRecomendacion.recomendar(porcentaje, POR_DEFECTO, 3, UN_PUNTO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RF-12 · si ningún rango cubre el porcentaje, no hay categoría")
    void sinRangoQueLoCubra() {
        List<RangoDeCategoria> sinFueraDeNorma = List.of(
                new RangoDeCategoria("ESTANDAR", new BigDecimal("90.0"), TEXTO_ESTANDAR));

        assertThatThrownBy(() -> ReglasDeRecomendacion.recomendar(new BigDecimal("89.9"), sinFueraDeNorma, 1,
                UN_PUNTO)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RF-13 · un rango tiene categoría y un límite de 0 a 100 (prueba de límite)")
    void rangoValido() {
        assertThat(new RangoDeCategoria("PREMIUM", new BigDecimal("100.0"), "x").limiteInferior())
                .isEqualByComparingTo("100");
        assertThat(new RangoDeCategoria("FUERA_DE_NORMA", new BigDecimal("0.0"), "x").limiteInferior())
                .isEqualByComparingTo("0");
        assertThatThrownBy(() -> new RangoDeCategoria(" ", BigDecimal.TEN, "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RangoDeCategoria("PREMIUM", new BigDecimal("100.1"), "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RangoDeCategoria("PREMIUM", new BigDecimal("-0.1"), "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
