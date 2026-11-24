/*
 * Nombre: PorcentajeDelLoteTest.java
 * Descripción: Pruebas unitarias del porcentaje del lote: el contado, el corregido con los recall de cada muestra y su
 *              margen, con los ejemplos de RF-10, D-39 y P-19 y sus límites (RF-10, P-19).
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

/**
 * El porcentaje del lote (RF-10), sin Spring ni el servicio de IA. Cada cifra se compara con su escala: un decimal.
 * Los recall son los de EfficientNet-B0 (91,20 y 53,40) y los de ResNet-50 (87,40 y 52,10), como en las fichas.
 */
class PorcentajeDelLoteTest {

    static final RecallsDelModelo EFFICIENTNET = new RecallsDelModelo(new BigDecimal("91.20"),
            new BigDecimal("53.40"));
    static final RecallsDelModelo RESNET = new RecallsDelModelo(new BigDecimal("87.40"), new BigDecimal("52.10"));
    static final BigDecimal K_1 = BigDecimal.ONE;

    private static PorcentajeDelLote de(ConteoDeMuestra... muestras) {
        return PorcentajeDelLote.de(List.of(muestras), K_1);
    }

    private static ConteoDeMuestra eff(int sanos, int total) {
        return new ConteoDeMuestra(sanos, total, EFFICIENTNET);
    }

    private static BigDecimal cifra(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    @DisplayName("RF-10 · C1 · con 93 granos sanos de 100, el porcentaje contado es 93,0 %")
    void contado93De100() {
        assertThat(de(eff(93, 100)).getContado()).isEqualTo(cifra("93.0"));
    }

    @Test
    @DisplayName("RF-10 · C2 · con 141 granos sanos de 150, el porcentaje contado es 94,0 %")
    void contado141De150() {
        assertThat(de(eff(141, 150)).getContado()).isEqualTo(cifra("94.0"));
    }

    @Test
    @DisplayName("RF-10 · C3 · con 100 granos sanos de 107, el porcentaje contado es 93,5 % (redondeo a un decimal)")
    void contado100De107() {
        assertThat(de(eff(100, 107)).getContado()).isEqualTo(cifra("93.5"));
    }

    @Test
    @DisplayName("RF-10 · C4 · con 2 muestras, 107 sanos de 120 y 113 de 125 dan 220 de 245 = 89,8 % contado")
    void contadoConDosMuestras() {
        PorcentajeDelLote porcentaje = de(eff(107, 120), eff(113, 125));

        assertThat(porcentaje.getGranosSanos()).isEqualTo(220);
        assertThat(porcentaje.getTotalGranos()).isEqualTo(245);
        assertThat(porcentaje.getContado()).isEqualTo(cifra("89.8"));
    }

    @Test
    @DisplayName("RF-10 · C5 · con 2 muestras de distinto tamaño se suman los granos: 109 de 120 y 120 de 150 dan 229 de 270 = 84,8 %, no el promedio de 85,4 %")
    void contadoSumaLosGranos() {
        PorcentajeDelLote porcentaje = de(eff(109, 120), eff(120, 150));

        assertThat(porcentaje.getGranosSanos()).isEqualTo(229);
        assertThat(porcentaje.getTotalGranos()).isEqualTo(270);
        assertThat(porcentaje.getContado()).isEqualTo(cifra("84.8")).isNotEqualTo(cifra("85.4"));
    }

    @Test
    @DisplayName("RF-10 · los porcentajes se redondean hacia arriba en el empate: 377 de 400 da 94,3 % contado")
    void empateHaciaArriba() {
        assertThat(de(eff(377, 400)).getContado()).isEqualTo(cifra("94.3"));
        assertThat(PorcentajeDelLote.contado(377, 400)).isEqualTo(cifra("94.3"));
    }

    @Test
    @DisplayName("RF-10 · C8 · con EfficientNet-B0, 87 sanos de 100 (87,0 % contado) dan 90,6 % corregido")
    void corregido87De100() {
        PorcentajeDelLote porcentaje = de(eff(87, 100));

        assertThat(porcentaje.getContado()).isEqualTo(cifra("87.0"));
        assertThat(porcentaje.getCorregido()).isEqualTo(cifra("90.6"));
    }

    @Test
    @DisplayName("RF-10 · C9 · con EfficientNet-B0, 93 sanos de 100 dan 100,0 % corregido: se limita a 100 % (prueba de límite)")
    void corregidoSeLimitaA100() {
        assertThat(de(eff(93, 100)).getCorregido()).isEqualTo(cifra("100.0"));
        assertThat(de(eff(141, 150)).getCorregido()).isEqualTo(cifra("100.0"));
        assertThat(de(eff(100, 107)).getCorregido()).isEqualTo(cifra("100.0"));
    }

    @Test
    @DisplayName("RF-10 · C10 · con EfficientNet-B0, 40 sanos de 100 dan 0,0 % corregido: se limita a 0 % (prueba de límite)")
    void corregidoSeLimitaA0() {
        assertThat(de(eff(40, 100)).getCorregido()).isEqualTo(cifra("0.0"));
        assertThat(de(eff(0, 100)).getCorregido()).isEqualTo(cifra("0.0"));
    }

    @Test
    @DisplayName("RF-10 · el corregido justo en los extremos: sin la corrección exacta daría 0 o 100 (prueba de límite)")
    void corregidoJustoEnLosExtremos() {
        // 233 de 500: 100·233 = 500·46,60, el numerador es 0. 114 de 125: 100·114 − 125·46,60 = 125·44,60, el
        // numerador es igual al denominador.
        assertThat(de(eff(233, 500)).getCorregido()).isEqualTo(cifra("0.0"));
        assertThat(de(eff(234, 500)).getCorregido()).isEqualTo(cifra("0.4"));
        assertThat(de(eff(114, 125)).getCorregido()).isEqualTo(cifra("100.0"));
        assertThat(de(eff(113, 125)).getCorregido()).isEqualTo(cifra("98.2"));
    }

    @Test
    @DisplayName("RF-10 · C11 · con EfficientNet-B0 y 2 muestras, 220 sanos de 245 (89,8 % contado) dan 96,9 % corregido")
    void corregidoConDosMuestras() {
        PorcentajeDelLote porcentaje = de(eff(107, 120), eff(113, 125));

        assertThat(porcentaje.getContado()).isEqualTo(cifra("89.8"));
        assertThat(porcentaje.getCorregido()).isEqualTo(cifra("96.9"));
    }

    @Test
    @DisplayName("RF-10 · C12 · con EfficientNet-B0, 84 sanos de 101 (83,2 % contado) dan 82,0 % corregido: se corrige con los conteos y se redondea al final, no 82,1 %")
    void corregidoConLosConteos() {
        PorcentajeDelLote porcentaje = de(eff(84, 101));

        assertThat(porcentaje.getContado()).isEqualTo(cifra("83.2"));
        assertThat(porcentaje.getCorregido()).isEqualTo(cifra("82.0")).isNotEqualTo(cifra("82.1"));
    }

    @Test
    @DisplayName("RF-10 · C14 · con 2 muestras de modelos distintos, 100 de 120 con EfficientNet-B0 y 105 de 125 con ResNet-50 dan 86,7 %; con EfficientNet-B0 en las dos, 83,1 %")
    void corregidoConDosModelos() {
        PorcentajeDelLote mezclado = de(eff(100, 120), new ConteoDeMuestra(105, 125, RESNET));
        PorcentajeDelLote unModelo = de(eff(100, 120), eff(105, 125));

        assertThat(mezclado.getCorregido()).isEqualTo(cifra("86.7"));
        assertThat(unModelo.getCorregido()).isEqualTo(cifra("83.1"));
        assertThat(mezclado.getContado()).isEqualTo(unModelo.getContado()).isEqualTo(cifra("83.7"));
    }

    @Test
    @DisplayName("RF-10 · el corregido usa los recall del modelo de la muestra: los mismos conteos dan otro porcentaje con ResNet-50")
    void corregidoConOtroModelo() {
        assertThat(de(new ConteoDeMuestra(87, 100, RESNET)).getCorregido()).isEqualTo(cifra("99.0"));
    }

    @Test
    @DisplayName("P-19 · el margen provisional, con k = 1: 87 de 100 da 7,5 puntos; 220 de 245, 4,3; 93 de 100, 5,7; 40 de 100, 11,0; 100 de 100, 0,0")
    void margenProvisional() {
        assertThat(de(eff(87, 100)).getMargen()).isEqualTo(cifra("7.5"));
        assertThat(de(eff(107, 120), eff(113, 125)).getMargen()).isEqualTo(cifra("4.3"));
        assertThat(de(eff(93, 100)).getMargen()).isEqualTo(cifra("5.7"));
        assertThat(de(eff(40, 100)).getMargen()).isEqualTo(cifra("11.0"));
        assertThat(de(eff(100, 100)).getMargen()).isEqualTo(cifra("0.0"));
        assertThat(de(eff(0, 100)).getMargen()).isEqualTo(cifra("0.0"));
    }

    @Test
    @DisplayName("P-19 · el margen con 2 modelos distintos usa la separación de cada muestra: 100 de 120 con EfficientNet-B0 y 105 de 125 con ResNet-50 dan 5,6 puntos")
    void margenConDosModelos() {
        assertThat(de(eff(100, 120), new ConteoDeMuestra(105, 125, RESNET)).getMargen()).isEqualTo(cifra("5.6"));
    }

    @Test
    @DisplayName("P-19 · el factor k multiplica el margen: con k = 2, 87 de 100 da 15,1 puntos")
    void margenConOtroFactor() {
        PorcentajeDelLote porcentaje = PorcentajeDelLote.de(List.of(eff(87, 100)), new BigDecimal("2"));

        assertThat(porcentaje.getMargen()).isEqualTo(cifra("15.1"));
        assertThat(porcentaje.getCorregido()).isEqualTo(cifra("90.6"));
    }

    @Test
    @DisplayName("P-19 · un margen de más de 100 puntos, de un modelo casi al azar, se deja en 100,0 (prueba de límite)")
    void margenHastaCien() {
        RecallsDelModelo casiAlAzar = new RecallsDelModelo(new BigDecimal("50.10"), new BigDecimal("50.00"));

        assertThat(de(new ConteoDeMuestra(50, 100, casiAlAzar)).getMargen()).isEqualTo(cifra("100.0"));
    }

    @Test
    @DisplayName("RF-10 · sin muestras, o con un factor del margen que no es mayor que 0, no hay porcentaje")
    void sinMuestrasNoHayPorcentaje() {
        assertThatThrownBy(() -> PorcentajeDelLote.de(List.of(), K_1)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos una muestra terminada");
        assertThatThrownBy(() -> PorcentajeDelLote.de(List.of(eff(87, 100)), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PorcentajeDelLote.de(List.of(eff(87, 100)), new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RF-10 · el porcentaje contado necesita un total positivo y sanos del 0 al total (prueba de límite)")
    void contadoConConteosValidos() {
        assertThat(PorcentajeDelLote.contado(0, 1)).isEqualTo(cifra("0.0"));
        assertThat(PorcentajeDelLote.contado(1, 1)).isEqualTo(cifra("100.0"));
        assertThatThrownBy(() -> PorcentajeDelLote.contado(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PorcentajeDelLote.contado(-1, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PorcentajeDelLote.contado(11, 10)).isInstanceOf(IllegalArgumentException.class);
    }
}
