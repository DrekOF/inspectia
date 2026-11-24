/*
 * Nombre: ResultadoDelLoteTest.java
 * Descripción: Pruebas unitarias del resultado del lote con 1 o 2 muestras: porcentajes, margen, categoría,
 *              recomendación, sugerencia con su límite y la categoría del límite, y aviso de confianza baja (RF-10,
 *              RF-12, D-25, D-47, RF-09 C6).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.dominio;

import static co.edu.usco.inspectia.analisis.dominio.PorcentajeDelLoteTest.EFFICIENTNET;
import static co.edu.usco.inspectia.analisis.dominio.ReglasDeRecomendacionTest.POR_DEFECTO;
import static co.edu.usco.inspectia.analisis.dominio.ReglasDeRecomendacionTest.TEXTO_CORRIENTE;
import static co.edu.usco.inspectia.analisis.dominio.ReglasDeRecomendacionTest.TEXTO_ESTANDAR;
import static co.edu.usco.inspectia.analisis.dominio.ReglasDeRecomendacionTest.TEXTO_PREMIUM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El resultado del lote (RF-10, RF-12), con los parámetros de hoy: umbral 60, 10 %, k = 1 y 1 punto. Si sugiere otra
 * muestra, lleva el límite más cercano y su categoría; si no, los dos van vacíos (D-47).
 */
class ResultadoDelLoteTest {

    static final ParametrosDelAnalisis PARAMETROS = new ParametrosDelAnalisis(new BigDecimal("60"),
            new BigDecimal("10"), BigDecimal.ONE, BigDecimal.ONE);

    private static ResultadoDelLote calcular(ConteoDeMuestra... muestras) {
        return ResultadoDelLote.calcular(List.of(muestras), POR_DEFECTO, PARAMETROS);
    }

    @Test
    @DisplayName("RF-10 · 87 sanos de 100 con EfficientNet-B0 dan un lote de 90,6 % con margen 7,5, Estándar y con sugerencia de otra muestra por el límite de Estándar (90)")
    void unaMuestra() {
        ResultadoDelLote resultado = calcular(new ConteoDeMuestra(87, 100, EFFICIENTNET));

        assertThat(resultado).isEqualTo(new ResultadoDelLote(new BigDecimal("90.6"), new BigDecimal("7.5"),
                new BigDecimal("87.0"), 100, 87, "ESTANDAR", TEXTO_ESTANDAR, true, new BigDecimal("90.0"), "ESTANDAR",
                false));
    }

    @Test
    @DisplayName("RF-12 · C11 · con EfficientNet-B0, 118 sanos de 132 (89,4 % contado) dan 95,95 %, que se redondea a 96,0 %: Premium o Especial (prueba de límite del redondeo)")
    void limiteDelRedondeo() {
        ResultadoDelLote resultado = calcular(new ConteoDeMuestra(118, 132, EFFICIENTNET));

        assertThat(resultado.porcentajeContado()).isEqualTo(new BigDecimal("89.4"));
        assertThat(resultado.porcentajeSanos()).isEqualTo(new BigDecimal("96.0"));
        assertThat(resultado.categoria()).isEqualTo("PREMIUM");
        assertThat(resultado.recomendacion()).isEqualTo(TEXTO_PREMIUM);
        assertThat(resultado.sugerirOtraMuestra()).as("queda a 0 puntos de 96").isTrue();
        assertThat(resultado.limiteSugerencia()).as("justo en el límite de Premium").isEqualTo(new BigDecimal("96.0"));
        assertThat(resultado.categoriaDelLimite()).isEqualTo("PREMIUM");
        assertThat(resultado.margen()).isEqualTo(new BigDecimal("6.0"));
    }

    @Test
    @DisplayName("RF-12 · C12 · con 2 muestras la categoría sale de la suma de sus granos: 220 sanos de 245 con EfficientNet-B0 (96,9 %) es Premium, sin sugerencia aunque quede a 0,9 de 96")
    void dosMuestras() {
        ResultadoDelLote resultado = calcular(new ConteoDeMuestra(107, 120, EFFICIENTNET),
                new ConteoDeMuestra(113, 125, EFFICIENTNET));

        assertThat(resultado).isEqualTo(new ResultadoDelLote(new BigDecimal("96.9"), new BigDecimal("4.3"),
                new BigDecimal("89.8"), 245, 220, "PREMIUM", TEXTO_PREMIUM, false, null, null, false));
    }

    @Test
    @DisplayName("D-47 · con 1 muestra, 107 sanos de 120 con EfficientNet-B0 dan 95,4 %, Estándar, y sugieren otra muestra por el límite de Premium (96), de otra categoría que la del lote")
    void elLimiteDeLaPrimeraMuestra() {
        ResultadoDelLote resultado = calcular(new ConteoDeMuestra(107, 120, EFFICIENTNET));

        assertThat(resultado).isEqualTo(new ResultadoDelLote(new BigDecimal("95.4"), new BigDecimal("6.4"),
                new BigDecimal("89.2"), 120, 107, "ESTANDAR", TEXTO_ESTANDAR, true, new BigDecimal("96.0"), "PREMIUM",
                false));
    }

    @Test
    @DisplayName("D-25 · con 2 muestras de 87 sanos de 100, la suma da 174 de 200: 90,6 % con margen 5,3, Estándar, sin sugerencia ni límite aunque quede a 0,6 de 90 (D-47)")
    void dosMuestrasSinLimite() {
        ResultadoDelLote igual = calcular(new ConteoDeMuestra(87, 100, EFFICIENTNET),
                new ConteoDeMuestra(87, 100, EFFICIENTNET));
        ResultadoDelLote peor = calcular(new ConteoDeMuestra(87, 100, EFFICIENTNET),
                new ConteoDeMuestra(85, 100, EFFICIENTNET));

        assertThat(igual).isEqualTo(new ResultadoDelLote(new BigDecimal("90.6"), new BigDecimal("5.3"),
                new BigDecimal("87.0"), 200, 174, "ESTANDAR", TEXTO_ESTANDAR, false, null, null, false));
        assertThat(peor).as("172 de 200 bajan a Corriente").isEqualTo(new ResultadoDelLote(new BigDecimal("88.3"),
                new BigDecimal("5.5"), new BigDecimal("86.0"), 200, 172, "CORRIENTE", TEXTO_CORRIENTE, false, null,
                null, false));
    }

    @Test
    @DisplayName("RF-09 · C6 · si una de las 2 muestras del lote salió con aviso de confianza baja, el resultado del lote también lo tiene (D-25)")
    void avisoDeAlgunaMuestra() {
        ResultadoDelLote conAviso = calcular(new ConteoDeMuestra(107, 120, EFFICIENTNET, false),
                new ConteoDeMuestra(113, 125, EFFICIENTNET, true));
        ResultadoDelLote sinAviso = calcular(new ConteoDeMuestra(107, 120, EFFICIENTNET, false),
                new ConteoDeMuestra(113, 125, EFFICIENTNET, false));

        assertThat(conAviso.avisoConfianzaBaja()).isTrue();
        assertThat(sinAviso.avisoConfianzaBaja()).isFalse();
        assertThat(calcular(new ConteoDeMuestra(87, 100, EFFICIENTNET, true)).avisoConfianzaBaja()).isTrue();
    }

    @Test
    @DisplayName("D-25 · el resultado necesita 1 o 2 muestras terminadas: con ninguna o con 3 no se calcula")
    void unaODosMuestras() {
        ConteoDeMuestra muestra = new ConteoDeMuestra(87, 100, EFFICIENTNET);

        assertThatThrownBy(() -> ResultadoDelLote.calcular(List.of(), POR_DEFECTO, PARAMETROS))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("1 o 2 muestras terminadas, no 0");
        assertThatThrownBy(() -> ResultadoDelLote.calcular(List.of(muestra, muestra, muestra), POR_DEFECTO,
                PARAMETROS)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 o 2 muestras terminadas, no 3");
    }

    @Test
    @DisplayName("P-19 · el factor del margen y los puntos para otra muestra salen de los parámetros")
    void usaLosParametros() {
        ParametrosDelAnalisis otros = new ParametrosDelAnalisis(new BigDecimal("60"), new BigDecimal("10"),
                new BigDecimal("2"), new BigDecimal("0.5"));

        ResultadoDelLote resultado = ResultadoDelLote.calcular(List.of(new ConteoDeMuestra(87, 100, EFFICIENTNET)),
                POR_DEFECTO, otros);

        assertThat(resultado.margen()).isEqualTo(new BigDecimal("15.1"));
        assertThat(resultado.sugerirOtraMuestra()).as("90,6 queda a 0,6 de 90, más de medio punto").isFalse();
        assertThat(resultado.limiteSugerencia()).isNull();
        assertThat(resultado.categoriaDelLimite()).isNull();
    }
}
