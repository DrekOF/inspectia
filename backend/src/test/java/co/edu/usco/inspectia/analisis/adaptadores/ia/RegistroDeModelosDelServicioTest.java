/*
 * Nombre: RegistroDeModelosDelServicioTest.java
 * Descripción: Pruebas unitarias del registro de los modelos del servicio de IA, con Mockito: registra los que faltan y
 *              deja en uso el que carga el servicio; si el servicio no responde al arrancar, el servidor arranca igual
 *              y lo reintenta (RF-08, RF-17, D-18, D-48).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.usco.inspectia.analisis.aplicacion.MetricasDelModelo;
import co.edu.usco.inspectia.analisis.aplicacion.RepositorioDeModelos;
import co.edu.usco.inspectia.analisis.aplicacion.SinRespuestaDeLaIaExcepcion;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** El registro de los modelos del servicio de IA, sin Spring, sin la base y sin el servicio. */
class RegistroDeModelosDelServicioTest {

    private static final MetricasDelModelo EFFICIENTNET = metricas("EfficientNet-B0", "73.50");
    private static final MetricasDelModelo RESNET = metricas("ResNet-50", "69.90");

    private ClienteDelServicioDeIa cliente;
    private RepositorioDeModelos modelos;
    private RegistroDeModelosDelServicio registro;

    private static MetricasDelModelo metricas(String nombre, String f1) {
        return new MetricasDelModelo(nombre, "1.0", "CNN", new BigDecimal(f1), new BigDecimal("70"),
                new BigDecimal("70"), new BigDecimal("91.20"), new BigDecimal("53.40"), new BigDecimal("87.50"),
                new BigDecimal("62.90"), 238, 23, 34, 39, new BigDecimal("19.00"));
    }

    @BeforeEach
    void preparar() {
        cliente = mock(ClienteDelServicioDeIa.class);
        modelos = mock(RepositorioDeModelos.class);
        registro = new RegistroDeModelosDelServicio(cliente, modelos, Duration.ofMillis(50));
    }

    @AfterEach
    void apagar() {
        registro.destroy();
    }

    @Test
    @DisplayName("RF-08 · C3 · al arrancar con el servicio, registra los modelos que faltan y deja en uso solo el que carga el servicio")
    void registraYPoneEnUso() {
        when(cliente.modelos()).thenReturn(new ClienteDelServicioDeIa.ModelosDelServicio(EFFICIENTNET,
                List.of(EFFICIENTNET, RESNET)));
        when(modelos.registrarSiFalta(EFFICIENTNET)).thenReturn(false);
        when(modelos.registrarSiFalta(RESNET)).thenReturn(true);

        registro.afterSingletonsInstantiated();

        InOrder orden = inOrder(modelos);
        orden.verify(modelos).registrarSiFalta(EFFICIENTNET);
        orden.verify(modelos).registrarSiFalta(RESNET);
        orden.verify(modelos).ponerEnUso("EfficientNet-B0", "1.0");
        assertThat(registro.asegurar()).isTrue();
        verify(cliente, times(1)).modelos();
    }

    @Test
    @DisplayName("RF-08 · si el servicio todavía no responde al arrancar, el servidor arranca igual y lo reintenta hasta registrarlos")
    void reintentaSiElServicioNoResponde() throws InterruptedException {
        when(cliente.modelos())
                .thenThrow(new SinRespuestaDeLaIaExcepcion("todavía carga el modelo", null))
                .thenReturn(new ClienteDelServicioDeIa.ModelosDelServicio(EFFICIENTNET, List.of(EFFICIENTNET)));

        registro.afterSingletonsInstantiated();
        verify(modelos, never()).ponerEnUso(any(), any());

        for (int intento = 0; intento < 100 && !registro.asegurar(); intento++) {
            Thread.sleep(20);
        }
        verify(modelos).ponerEnUso("EfficientNet-B0", "1.0");
    }

    @Test
    @DisplayName("RF-08 · si poner en uso falla, no se da por registrado y se vuelve a intentar")
    void fallaAlPonerEnUso() {
        when(cliente.modelos()).thenReturn(new ClienteDelServicioDeIa.ModelosDelServicio(EFFICIENTNET,
                List.of(EFFICIENTNET)));
        org.mockito.Mockito.doThrow(new IllegalStateException("F1 menor que 65")).doNothing().when(modelos)
                .ponerEnUso("EfficientNet-B0", "1.0");

        assertThat(registro.asegurar()).isFalse();
        assertThat(registro.asegurar()).isTrue();
        verify(cliente, times(2)).modelos();
    }
}
