/*
 * Nombre: ParametrosYConteosTest.java
 * Descripción: Pruebas unitarias de los datos de entrada del cálculo: los recall del modelo, el conteo de una muestra y
 *              los parámetros del análisis, con sus límites (D-39, RF-10, P-15, P-19, D-24).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

import static co.edu.usco.inspectia.analisis.dominio.PorcentajeDelLoteTest.EFFICIENTNET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Los recall, los conteos y los parámetros, que el dominio revisa antes de calcular. */
class ParametrosYConteosTest {

    @Test
    @DisplayName("D-39 · con EfficientNet-B0, el modelo marca como sanos el 46,60 % de los no sanos y separa las clases por 44,60 puntos")
    void recallsDeEfficientNet() {
        assertThat(EFFICIENTNET.noSanosMarcadosComoSanos()).isEqualTo(new BigDecimal("46.60"));
        assertThat(EFFICIENTNET.separacion()).isEqualTo(new BigDecimal("44.60"));
    }

    @Test
    @DisplayName("D-39 · solo sirve un modelo mejor que el azar: los recall deben sumar más de 100 (prueba de límite)")
    void modeloMejorQueElAzar() {
        assertThat(new RecallsDelModelo(new BigDecimal("50.01"), new BigDecimal("50.00")).separacion())
                .isEqualTo(new BigDecimal("0.01"));
        assertThatThrownBy(() -> new RecallsDelModelo(new BigDecimal("50.00"), new BigDecimal("50.00")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecallsDelModelo(new BigDecimal("40.00"), new BigDecimal("50.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("D-39 · cada recall va de 0 a 100 (prueba de límite)")
    void recallsDeCeroACien() {
        assertThat(new RecallsDelModelo(new BigDecimal("100.00"), new BigDecimal("0.01")).recallSano())
                .isEqualTo(new BigDecimal("100.00"));
        assertThat(new RecallsDelModelo(new BigDecimal("0.01"), new BigDecimal("100.00")).recallNoSano())
                .isEqualTo(new BigDecimal("100.00"));
        assertThatThrownBy(() -> new RecallsDelModelo(new BigDecimal("100.01"), new BigDecimal("50.00")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecallsDelModelo(new BigDecimal("50.00"), new BigDecimal("100.01")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecallsDelModelo(new BigDecimal("-0.01"), new BigDecimal("100.00")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("recallSano debe ir de 0 a 100");
        assertThatThrownBy(() -> new RecallsDelModelo(new BigDecimal("100.00"), new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("recallNoSano debe ir de 0 a 100");
    }

    @Test
    @DisplayName("RF-10 · un conteo tiene al menos un grano y de 0 al total de sanos (prueba de límite)")
    void conteoValido() {
        assertThat(new ConteoDeMuestra(0, 1, EFFICIENTNET).granosSanos()).isZero();
        assertThat(new ConteoDeMuestra(1, 1, EFFICIENTNET).avisoConfianzaBaja()).isFalse();
        assertThatThrownBy(() -> new ConteoDeMuestra(0, 0, EFFICIENTNET)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConteoDeMuestra(-1, 10, EFFICIENTNET))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConteoDeMuestra(11, 10, EFFICIENTNET))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RF-07 · una muestra da resultado con 100 granos o más: 99 no alcanzan (prueba de límite)")
    void minimoDeGranos() {
        assertThat(ConteoDeMuestra.alcanzaElMinimo(99)).isFalse();
        assertThat(ConteoDeMuestra.alcanzaElMinimo(100)).isTrue();
        assertThat(ConteoDeMuestra.alcanzaElMinimo(150)).isTrue();
    }

    @Test
    @DisplayName("P-15 · los parámetros de hoy sirven, y sus reglas de confianza usan el umbral y el máximo de dudosos")
    void parametrosDeHoy() {
        ParametrosDelAnalisis parametros = new ParametrosDelAnalisis(new BigDecimal("60"), new BigDecimal("10"),
                BigDecimal.ONE, BigDecimal.ONE);

        assertThat(parametros.reglasDeConfianza())
                .isEqualTo(new ReglasDeConfianza(new BigDecimal("60"), new BigDecimal("10")));
    }

    @Test
    @DisplayName("P-15 · el umbral admite hasta 2 decimales y el máximo de dudosos hasta 1, como sus columnas (prueba de límite)")
    void decimalesDeLosParametros() {
        assertThat(new ParametrosDelAnalisis(new BigDecimal("59.99"), new BigDecimal("10.5"), BigDecimal.ONE,
                BigDecimal.ONE).umbralConfianzaBaja()).isEqualTo(new BigDecimal("59.99"));
        assertThat(new ParametrosDelAnalisis(new BigDecimal("60.000"), new BigDecimal("10.00"), BigDecimal.ONE,
                BigDecimal.ONE).porcentajeMaxDudosos()).isEqualTo(new BigDecimal("10.00"));
        assertThatThrownBy(() -> new ParametrosDelAnalisis(new BigDecimal("59.995"), BigDecimal.TEN, BigDecimal.ONE,
                BigDecimal.ONE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ParametrosDelAnalisis(BigDecimal.TEN, new BigDecimal("10.05"), BigDecimal.ONE,
                BigDecimal.ONE)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("P-15 · el umbral y el máximo de dudosos van de 0 a 100 (prueba de límite)")
    void porcentajesDeCeroACien() {
        assertThat(new ParametrosDelAnalisis(BigDecimal.ZERO, new BigDecimal("100"), BigDecimal.ONE, BigDecimal.ONE))
                .isNotNull();
        assertThat(new ParametrosDelAnalisis(new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE))
                .isNotNull();
        assertThatThrownBy(() -> new ParametrosDelAnalisis(new BigDecimal("-1"), BigDecimal.TEN, BigDecimal.ONE,
                BigDecimal.ONE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ParametrosDelAnalisis(new BigDecimal("101"), BigDecimal.TEN, BigDecimal.ONE,
                BigDecimal.ONE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ParametrosDelAnalisis(BigDecimal.TEN, new BigDecimal("-1"), BigDecimal.ONE,
                BigDecimal.ONE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ParametrosDelAnalisis(BigDecimal.TEN, new BigDecimal("101"), BigDecimal.ONE,
                BigDecimal.ONE)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("P-19 · el factor del margen y los puntos para otra muestra (D-24) deben ser mayores que 0")
    void factorYPuntosPositivos() {
        assertThat(new ParametrosDelAnalisis(BigDecimal.TEN, BigDecimal.TEN, new BigDecimal("0.1"),
                new BigDecimal("0.1")).factorDelMargen()).isEqualTo(new BigDecimal("0.1"));
        assertThatThrownBy(() -> new ParametrosDelAnalisis(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO,
                BigDecimal.ONE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ParametrosDelAnalisis(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE,
                BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
    }
}
