/*
 * Nombre: ReglasDeRecomendacionTest.java
 * Descripción: Pruebas unitarias de la recomendación: la categoría probable con los rangos de la empresa, cuándo se
 *              sugiere otra muestra y de qué límite, el más cercano y en empate el más alto, con los ejemplos de RF-12
 *              y sus límites (RF-12, D-24, D-25, D-47).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La recomendación (RF-12), con los rangos por defecto de D-10 salvo donde la prueba dice otros. Cuando sugiere otra
 * muestra, dice de qué límite y de qué categoría es ese límite, para el aviso (D-47); si no, los dos van vacíos.
 */
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

    private static Recomendacion conUnaMuestra(String porcentaje, List<RangoDeCategoria> rangos) {
        return ReglasDeRecomendacion.recomendar(new BigDecimal(porcentaje), rangos, 1, UN_PUNTO);
    }

    /** Una recomendación sin sugerencia: sin límite ni categoría del límite (D-47). */
    private static Recomendacion sinSugerencia(String categoria, String texto) {
        return new Recomendacion(categoria, texto, false, null, null);
    }

    /** Una recomendación que sugiere otra muestra por el límite {@code limite}, de la categoría dada (D-47). */
    private static Recomendacion sugiere(String categoria, String texto, String limite, String categoriaDelLimite) {
        return new Recomendacion(categoria, texto, true, new BigDecimal(limite), categoriaDelLimite);
    }

    /** Rangos con dos límites a menos de 1 punto entre sí, como permite RF-13: Premium desde 91 y Estándar desde 90. */
    private static List<RangoDeCategoria> premiumDesde(String limite) {
        return List.of(
                new RangoDeCategoria("PREMIUM", new BigDecimal(limite), "Exportación"),
                new RangoDeCategoria("ESTANDAR", new BigDecimal("90.0"), "Venta de la casa"),
                new RangoDeCategoria("CORRIENTE", new BigDecimal("82.0"), "Selección a mano"),
                new RangoDeCategoria("FUERA_DE_NORMA", new BigDecimal("0.0"), "Reproceso"));
    }

    @Test
    @DisplayName("RF-12 · C1 · con 99 %, la categoría es Premium o Especial, sin sugerencia: está a 3 puntos de 96")
    void premium() {
        assertThat(conUnaMuestra("99.0")).isEqualTo(sinSugerencia("PREMIUM", TEXTO_PREMIUM));
    }

    @Test
    @DisplayName("RF-12 · C2 · con 93 %, la categoría es Estándar, sin sugerencia: está a 3 puntos de 90 y de 96")
    void estandar() {
        assertThat(conUnaMuestra("93.0")).isEqualTo(sinSugerencia("ESTANDAR", TEXTO_ESTANDAR));
    }

    @Test
    @DisplayName("RF-12 · C3 · con 90,9 % es Estándar y sugiere otra muestra por el límite de Estándar (90, a 0,9 puntos); con 91,0 % no la sugiere (a 1 punto): límite del margen")
    void limiteDelMargen() {
        assertThat(conUnaMuestra("90.9")).isEqualTo(sugiere("ESTANDAR", TEXTO_ESTANDAR, "90.0", "ESTANDAR"));
        assertThat(conUnaMuestra("91.0")).isEqualTo(sinSugerencia("ESTANDAR", TEXTO_ESTANDAR));
    }

    @Test
    @DisplayName("RF-12 · C4 · con 90,0 % la categoría es Estándar y con 89,9 % es Corriente; los dos sugieren otra muestra por el límite de Estándar, 90 (prueba de límite)")
    void limiteDeLaCategoria() {
        assertThat(conUnaMuestra("90.0")).isEqualTo(sugiere("ESTANDAR", TEXTO_ESTANDAR, "90.0", "ESTANDAR"));
        assertThat(conUnaMuestra("89.9")).isEqualTo(sugiere("CORRIENTE", TEXTO_CORRIENTE, "90.0", "ESTANDAR"));
    }

    @Test
    @DisplayName("RF-12 · C5 · con 75 %, la categoría es Fuera de norma, sin sugerencia")
    void fueraDeNorma() {
        assertThat(conUnaMuestra("75.0")).isEqualTo(sinSugerencia("FUERA_DE_NORMA", TEXTO_FUERA));
    }

    @Test
    @DisplayName("RF-12 · el 0 de Fuera de norma no cuenta para sugerir otra muestra: 0,0 % y 0,5 % no la sugieren")
    void elCeroNoCuenta() {
        assertThat(conUnaMuestra("0.0")).isEqualTo(sinSugerencia("FUERA_DE_NORMA", TEXTO_FUERA));
        assertThat(conUnaMuestra("0.5")).isEqualTo(sinSugerencia("FUERA_DE_NORMA", TEXTO_FUERA));
    }

    @Test
    @DisplayName("RF-12 · también sugiere otra muestra por debajo y por encima de los límites de Premium y Corriente, con ese límite")
    void sugiereCercaDeCadaLimite() {
        assertThat(conUnaMuestra("95.1")).isEqualTo(sugiere("ESTANDAR", TEXTO_ESTANDAR, "96.0", "PREMIUM"));
        assertThat(conUnaMuestra("96.9")).isEqualTo(sugiere("PREMIUM", TEXTO_PREMIUM, "96.0", "PREMIUM"));
        assertThat(conUnaMuestra("97.0")).isEqualTo(sinSugerencia("PREMIUM", TEXTO_PREMIUM));
        assertThat(conUnaMuestra("81.1")).isEqualTo(sugiere("FUERA_DE_NORMA", TEXTO_FUERA, "82.0", "CORRIENTE"));
        assertThat(conUnaMuestra("81.0")).isEqualTo(sinSugerencia("FUERA_DE_NORMA", TEXTO_FUERA));
        assertThat(conUnaMuestra("100.0")).isEqualTo(sinSugerencia("PREMIUM", TEXTO_PREMIUM));
    }

    @Test
    @DisplayName("RF-12 · C9 · si la empresa cambió sus rangos, se usan los de la empresa: con Estándar desde 91 %, 90,6 % es Corriente y sugiere otra muestra por el límite de Estándar (91)")
    void rangosDeLaEmpresa() {
        List<RangoDeCategoria> propios = List.of(
                new RangoDeCategoria("PREMIUM", new BigDecimal("97.5"), "Exportación"),
                new RangoDeCategoria("ESTANDAR", new BigDecimal("91.0"), "Venta de la casa"),
                new RangoDeCategoria("CORRIENTE", new BigDecimal("80.0"), "Selección a mano"),
                new RangoDeCategoria("FUERA_DE_NORMA", new BigDecimal("0.0"), "Reproceso"));

        Recomendacion recomendacion = ReglasDeRecomendacion.recomendar(new BigDecimal("90.6"), propios, 1, UN_PUNTO);
        Recomendacion premium = ReglasDeRecomendacion.recomendar(new BigDecimal("97.0"), propios, 1, UN_PUNTO);

        assertThat(recomendacion).isEqualTo(sugiere("CORRIENTE", "Selección a mano", "91.0", "ESTANDAR"));
        assertThat(premium).isEqualTo(sugiere("ESTANDAR", "Venta de la casa", "97.5", "PREMIUM"));
    }

    @Test
    @DisplayName("D-24 · a cuántos puntos se sugiere otra muestra es un parámetro: con 2 puntos, 91,9 % la sugiere y 92,0 % no, porque queda justo a 2 de 90")
    void puntosParaOtraMuestra() {
        Recomendacion dosPuntos = ReglasDeRecomendacion.recomendar(new BigDecimal("92.0"), POR_DEFECTO, 1,
                new BigDecimal("2"));

        assertThat(dosPuntos).isEqualTo(sinSugerencia("ESTANDAR", TEXTO_ESTANDAR));
        assertThat(ReglasDeRecomendacion.recomendar(new BigDecimal("91.9"), POR_DEFECTO, 1, new BigDecimal("2")))
                .isEqualTo(sugiere("ESTANDAR", TEXTO_ESTANDAR, "90.0", "ESTANDAR"));
    }

    @Test
    @DisplayName("RF-12 · con 2 muestras ya no se sugiere otra, aunque quede cerca de un límite (D-25)")
    void conDosMuestrasNoSugiere() {
        Recomendacion recomendacion = ReglasDeRecomendacion.recomendar(new BigDecimal("96.9"), POR_DEFECTO, 2,
                UN_PUNTO);

        assertThat(recomendacion).isEqualTo(sinSugerencia("PREMIUM", TEXTO_PREMIUM));
    }

    @Test
    @DisplayName("D-47 · si dos límites quedan a menos de 1 punto, cuenta el más cercano: con Premium desde 91 y Estándar desde 90, 90,6 % queda a 0,4 de Premium y 90,4 %, a 0,4 de Estándar")
    void elLimiteMasCercano() {
        List<RangoDeCategoria> rangos = premiumDesde("91.0");

        assertThat(conUnaMuestra("90.6", rangos))
                .isEqualTo(sugiere("ESTANDAR", "Venta de la casa", "91.0", "PREMIUM"));
        assertThat(conUnaMuestra("90.4", rangos))
                .isEqualTo(sugiere("ESTANDAR", "Venta de la casa", "90.0", "ESTANDAR"));
        assertThat(conUnaMuestra("89.8", rangos)).as("a 0,2 de 90 y a 1,2 de 91")
                .isEqualTo(sugiere("CORRIENTE", "Selección a mano", "90.0", "ESTANDAR"));
    }

    @Test
    @DisplayName("D-47 · si dos límites quedan a la misma distancia, cuenta el más alto, sin importar el orden de la lista: con Premium desde 90,4 y Estándar desde 90, 90,2 % sugiere por el de Premium")
    void enEmpateElMasAlto() {
        List<RangoDeCategoria> rangos = premiumDesde("90.4");
        List<RangoDeCategoria> alReves = rangos.reversed();

        assertThat(conUnaMuestra("90.2", rangos))
                .isEqualTo(sugiere("ESTANDAR", "Venta de la casa", "90.4", "PREMIUM"));
        assertThat(conUnaMuestra("90.2", alReves))
                .isEqualTo(sugiere("ESTANDAR", "Venta de la casa", "90.4", "PREMIUM"));
    }

    @Test
    @DisplayName("D-47 · un límite que queda justo a 1 punto no cuenta aunque otro quede igual de lejos: con 90,0 % entre límites de 89 y 91 no se sugiere otra muestra (prueba de límite)")
    void aLaDistanciaDeLosPuntosNoCuenta() {
        List<RangoDeCategoria> rangos = List.of(
                new RangoDeCategoria("PREMIUM", new BigDecimal("91.0"), "Exportación"),
                new RangoDeCategoria("ESTANDAR", new BigDecimal("89.0"), "Venta de la casa"),
                new RangoDeCategoria("CORRIENTE", new BigDecimal("82.0"), "Selección a mano"),
                new RangoDeCategoria("FUERA_DE_NORMA", new BigDecimal("0.0"), "Reproceso"));

        assertThat(conUnaMuestra("90.0", rangos)).isEqualTo(sinSugerencia("ESTANDAR", "Venta de la casa"));
        assertThat(conUnaMuestra("90.1", rangos)).as("a 0,9 de 91 y a 1,1 de 89")
                .isEqualTo(sugiere("ESTANDAR", "Venta de la casa", "91.0", "PREMIUM"));
    }

    @Test
    @DisplayName("D-47 · justo en un límite, a 0,0 puntos, se sugiere otra muestra por ese límite: 96,0 % por Premium y 82,0 % por Corriente (prueba de límite)")
    void justoEnElLimite() {
        assertThat(conUnaMuestra("96.0")).isEqualTo(sugiere("PREMIUM", TEXTO_PREMIUM, "96.0", "PREMIUM"));
        assertThat(conUnaMuestra("82.0")).isEqualTo(sugiere("CORRIENTE", TEXTO_CORRIENTE, "82.0", "CORRIENTE"));
    }

    @Test
    @DisplayName("D-47 · el límite 0 de Fuera de norma nunca cuenta, aunque sea el más cercano: con Corriente desde 1, 0,2 % sugiere por el límite de Corriente")
    void elCeroNuncaEsElLimite() {
        List<RangoDeCategoria> bajos = List.of(
                new RangoDeCategoria("PREMIUM", new BigDecimal("3.0"), "Exportación"),
                new RangoDeCategoria("ESTANDAR", new BigDecimal("2.0"), "Venta de la casa"),
                new RangoDeCategoria("CORRIENTE", new BigDecimal("1.0"), "Selección a mano"),
                new RangoDeCategoria("FUERA_DE_NORMA", new BigDecimal("0.0"), "Reproceso"));

        assertThat(conUnaMuestra("0.2", bajos)).isEqualTo(sugiere("FUERA_DE_NORMA", "Reproceso", "1.0", "CORRIENTE"));
        assertThat(ReglasDeRecomendacion.recomendar(new BigDecimal("0.0"), bajos, 1, new BigDecimal("0.5")))
                .as("a 0 del límite de Fuera de norma y a 1 de Corriente")
                .isEqualTo(sinSugerencia("FUERA_DE_NORMA", "Reproceso"));
    }

    @Test
    @DisplayName("D-47 · la categoría del límite puede ser distinta de la del lote: con Premium desde 97,5, 97,0 % es Estándar y sugiere otra muestra por el límite de Premium")
    void laCategoriaDelLimiteNoEsLaDelLote() {
        List<RangoDeCategoria> propios = List.of(
                new RangoDeCategoria("PREMIUM", new BigDecimal("97.5"), "Exportación"),
                new RangoDeCategoria("ESTANDAR", new BigDecimal("91.0"), "Venta de la casa"),
                new RangoDeCategoria("CORRIENTE", new BigDecimal("80.0"), "Selección a mano"),
                new RangoDeCategoria("FUERA_DE_NORMA", new BigDecimal("0.0"), "Reproceso"));

        Recomendacion recomendacion = conUnaMuestra("97.0", propios);

        assertThat(recomendacion.categoria()).isEqualTo("ESTANDAR");
        assertThat(recomendacion.sugerirOtraMuestra()).isTrue();
        assertThat(recomendacion.limiteSugerencia()).isEqualByComparingTo("97.5");
        assertThat(recomendacion.categoriaDelLimite()).isEqualTo("PREMIUM");
    }

    @Test
    @DisplayName("D-47 · con 2 muestras no hay sugerencia ni límite, ni siquiera justo en un límite (D-25)")
    void conDosMuestrasSinLimite() {
        Recomendacion recomendacion = ReglasDeRecomendacion.recomendar(new BigDecimal("90.0"), POR_DEFECTO, 2,
                UN_PUNTO);

        assertThat(recomendacion).isEqualTo(sinSugerencia("ESTANDAR", TEXTO_ESTANDAR));
        assertThat(recomendacion.limiteSugerencia()).isNull();
        assertThat(recomendacion.categoriaDelLimite()).isNull();
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
