/*
 * Nombre: MetricasDelModeloServicioTest.java
 * Descripción: Pruebas unitarias del servicio del modelo de IA con Mockito (RF-17): cada modelo pasa a la respuesta con
 *              sus métricas, su matriz de confusión y si está en uso, y la confianza promedio se pide solo con la
 *              empresa de la sesión (RNF-03) y, si se pide, con el periodo en días de Colombia, que se valida como
 *              el de las estadísticas.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.estadistica.modelo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.usco.inspectia.comun.errores.CampoInvalido;
import co.edu.usco.inspectia.comun.errores.ReglaIncumplidaExcepcion;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas del servicio del modelo de IA, sin base de datos: el repositorio y sus filas son simulados. La prueba de
 * integración ({@code MetricasDelModeloIT}) prueba lo mismo con PostgreSQL.
 */
@ExtendWith(MockitoExtension.class)
class MetricasDelModeloServicioTest {

    private static final UUID EMPRESA = UUID.fromString("0199e2a0-0000-7000-8000-000000000001");
    private static final UUID EFFICIENTNET = UUID.fromString("0199e2a0-0000-7000-8000-0000000000e0");
    private static final UUID FALSO = UUID.fromString("0199e2a0-0000-7000-8000-0000000000f0");

    @Mock
    private MetricasDelModeloRepositorio repositorio;

    @Mock
    private ConfianzaDeLosAnalisisRepositorio confianzas;

    @InjectMocks
    private MetricasDelModeloServicio servicio;

    @Test
    @DisplayName("RF-17 · C1 · cada modelo pasa a la respuesta con sus métricas de prueba, su matriz de confusión y si está en uso, en el orden del repositorio")
    void pasaLasMetricasDeCadaModelo() {
        List<ModeloComparado> modelos = List.of(efficientNet(), falso());
        ConfianzaDeLaEmpresa sinAnalisis = confianza(null, 0, 0);
        when(repositorio.comparados()).thenReturn(modelos);
        when(confianzas.confianzaDe(EMPRESA, null, null)).thenReturn(sinAnalisis);

        MetricasDelModeloRespuesta respuesta = servicio.consultar(EMPRESA, null, null);

        assertThat(respuesta.modelos()).hasSize(2);
        ModeloMedidoRespuesta modelo = respuesta.modelos().get(0);
        assertThat(modelo).isEqualTo(new ModeloMedidoRespuesta(EFFICIENTNET, "EfficientNet-B0", "1.0", "CNN", true,
                new BigDecimal("73.50"), new BigDecimal("75.20"), new BigDecimal("72.31"), new BigDecimal("91.20"),
                new BigDecimal("53.40"), new BigDecimal("87.50"), new BigDecimal("62.90"),
                new MatrizDeConfusionRespuesta(238, 23, 34, 39), new BigDecimal("19.00")));
    }

    @Test
    @DisplayName("RF-17 · C1 · un modelo sin las métricas de la comparación, como el del clasificador falso, va con sus recall, sin matriz y sin las demás")
    void elModeloSinMetricasVaSinMatriz() {
        List<ModeloComparado> modelos = List.of(falso());
        ConfianzaDeLaEmpresa sinAnalisis = confianza(null, 0, 0);
        when(repositorio.comparados()).thenReturn(modelos);
        when(confianzas.confianzaDe(EMPRESA, null, null)).thenReturn(sinAnalisis);

        ModeloMedidoRespuesta modelo = servicio.consultar(EMPRESA, null, null).modelos().get(0);

        assertThat(modelo).isEqualTo(new ModeloMedidoRespuesta(FALSO, "clasificador-falso", "1.0", null, false, null,
                null, null, new BigDecimal("91.20"), new BigDecimal("53.40"), null, null, null, null));
    }

    @Test
    @DisplayName("RF-17 · C2 · la confianza promedio se pide con la empresa de la sesión y pasa a la respuesta con cuántos granos y muestras la dan")
    void laConfianzaEsDeLaEmpresa() {
        ConfianzaDeLaEmpresa conAnalisis = confianza(new BigDecimal("88.60"), 200, 2);
        when(repositorio.comparados()).thenReturn(List.of());
        when(confianzas.confianzaDe(EMPRESA, null, null)).thenReturn(conAnalisis);

        MetricasDelModeloRespuesta respuesta = servicio.consultar(EMPRESA, null, null);

        verify(confianzas).confianzaDe(EMPRESA, null, null);
        assertThat(respuesta.confianza()).isEqualTo(new ConfianzaPromedioRespuesta(new BigDecimal("88.60"), 200, 2));
    }

    @Test
    @DisplayName("RF-17 · C2 · sin análisis en la empresa, la confianza promedio va vacía, con 0 granos y 0 muestras")
    void sinAnalisisLaConfianzaVaVacia() {
        ConfianzaDeLaEmpresa sinAnalisis = confianza(null, 0, 0);
        when(repositorio.comparados()).thenReturn(List.of());
        when(confianzas.confianzaDe(EMPRESA, null, null)).thenReturn(sinAnalisis);

        assertThat(servicio.consultar(EMPRESA, null, null).confianza()).isEqualTo(new ConfianzaPromedioRespuesta(null, 0, 0));
    }

    @Test
    @DisplayName("RF-17 · la confianza promedio de un periodo se pide desde las 00:00 del primer día en Colombia hasta las 00:00 del día siguiente al último, como el historial")
    void laConfianzaDeUnPeriodo() {
        ConfianzaDeLaEmpresa conAnalisis = confianza(new BigDecimal("88.60"), 100, 1);
        Instant inicio = Instant.parse("2026-11-01T05:00:00Z");
        Instant fin = Instant.parse("2026-12-01T05:00:00Z");
        when(repositorio.comparados()).thenReturn(List.of());
        when(confianzas.confianzaDe(EMPRESA, inicio, fin)).thenReturn(conAnalisis);

        MetricasDelModeloRespuesta respuesta = servicio.consultar(EMPRESA, LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30));

        assertThat(respuesta.confianza()).isEqualTo(new ConfianzaPromedioRespuesta(new BigDecimal("88.60"), 100, 1));
    }

    @Test
    @DisplayName("RF-17 · el periodo de la confianza es opcional: solo «Desde» cuenta desde ese día, y solo «Hasta», hasta ese día incluido")
    void periodoConUnaSolaFecha() {
        ConfianzaDeLaEmpresa sinAnalisis = confianza(null, 0, 0);
        when(repositorio.comparados()).thenReturn(List.of());
        when(confianzas.confianzaDe(EMPRESA, Instant.parse("2026-11-20T05:00:00Z"), null)).thenReturn(sinAnalisis);
        when(confianzas.confianzaDe(EMPRESA, null, Instant.parse("2026-11-21T05:00:00Z"))).thenReturn(sinAnalisis);

        servicio.consultar(EMPRESA, LocalDate.of(2026, 11, 20), null);
        servicio.consultar(EMPRESA, null, LocalDate.of(2026, 11, 20));

        verify(confianzas).confianzaDe(EMPRESA, Instant.parse("2026-11-20T05:00:00Z"), null);
        verify(confianzas).confianzaDe(EMPRESA, null, Instant.parse("2026-11-21T05:00:00Z"));
    }

    @Test
    @DisplayName("RF-17 · si «Hasta» es anterior a «Desde», o una fecha se sale de los años 0001 a 9999, se rechaza en su campo sin consultar (prueba de límite)")
    void periodoQueNoSirve() {
        assertThatThrownBy(() -> servicio.consultar(EMPRESA, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 19)))
                .isInstanceOfSatisfying(ReglaIncumplidaExcepcion.class, error -> assertThat(error.getCampos())
                        .containsExactly(new CampoInvalido("hasta", "estadistica.hasta_antes_de_desde")));
        assertThatThrownBy(() -> servicio.consultar(EMPRESA, LocalDate.of(0, 12, 31), LocalDate.of(10000, 1, 1)))
                .isInstanceOfSatisfying(ReglaIncumplidaExcepcion.class, error -> assertThat(error.getCampos())
                        .containsExactly(new CampoInvalido("desde", "error.valor_invalido"),
                                new CampoInvalido("hasta", "error.valor_invalido")));
        verify(confianzas, never()).confianzaDe(any(), any(), any());
    }

    /** EfficientNet-B0, en uso, con las métricas de D-18 y D-48. */
    private static ModeloComparado efficientNet() {
        ModeloComparado modelo = mock(ModeloComparado.class);
        lenient().when(modelo.getId()).thenReturn(EFFICIENTNET);
        lenient().when(modelo.getNombre()).thenReturn("EfficientNet-B0");
        lenient().when(modelo.getVersion()).thenReturn("1.0");
        lenient().when(modelo.getTipo()).thenReturn("CNN");
        lenient().when(modelo.getEnUso()).thenReturn(true);
        lenient().when(modelo.getF1Macro()).thenReturn(new BigDecimal("73.50"));
        lenient().when(modelo.getPrecisionMacro()).thenReturn(new BigDecimal("75.20"));
        lenient().when(modelo.getRecallMacro()).thenReturn(new BigDecimal("72.31"));
        lenient().when(modelo.getRecallSano()).thenReturn(new BigDecimal("91.20"));
        lenient().when(modelo.getRecallNoSano()).thenReturn(new BigDecimal("53.40"));
        lenient().when(modelo.getPrecisionSano()).thenReturn(new BigDecimal("87.50"));
        lenient().when(modelo.getPrecisionNoSano()).thenReturn(new BigDecimal("62.90"));
        lenient().when(modelo.getSanosComoSanos()).thenReturn(238);
        lenient().when(modelo.getSanosComoNoSanos()).thenReturn(23);
        lenient().when(modelo.getNoSanosComoSanos()).thenReturn(34);
        lenient().when(modelo.getNoSanosComoNoSanos()).thenReturn(39);
        lenient().when(modelo.getTiempoPorGranoMs()).thenReturn(new BigDecimal("19.00"));
        return modelo;
    }

    /** El modelo del clasificador falso: solo sus recall, sin las demás métricas y sin estar en uso. */
    private static ModeloComparado falso() {
        ModeloComparado modelo = mock(ModeloComparado.class);
        lenient().when(modelo.getId()).thenReturn(FALSO);
        lenient().when(modelo.getNombre()).thenReturn("clasificador-falso");
        lenient().when(modelo.getVersion()).thenReturn("1.0");
        lenient().when(modelo.getEnUso()).thenReturn(false);
        lenient().when(modelo.getRecallSano()).thenReturn(new BigDecimal("91.20"));
        lenient().when(modelo.getRecallNoSano()).thenReturn(new BigDecimal("53.40"));
        // Mockito responde 0 en los Integer sin simular: la base los deja vacíos.
        lenient().when(modelo.getSanosComoSanos()).thenReturn(null);
        lenient().when(modelo.getSanosComoNoSanos()).thenReturn(null);
        lenient().when(modelo.getNoSanosComoSanos()).thenReturn(null);
        lenient().when(modelo.getNoSanosComoNoSanos()).thenReturn(null);
        return modelo;
    }

    /** La confianza de una empresa, como la responde el repositorio. */
    private static ConfianzaDeLaEmpresa confianza(BigDecimal promedio, long granos, long muestras) {
        ConfianzaDeLaEmpresa confianza = mock(ConfianzaDeLaEmpresa.class);
        lenient().when(confianza.getPromedio()).thenReturn(promedio);
        lenient().when(confianza.getGranos()).thenReturn(granos);
        lenient().when(confianza.getMuestras()).thenReturn(muestras);
        return confianza;
    }
}
