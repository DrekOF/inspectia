/*
 * Nombre: GuardarAnalisisTest.java
 * Descripción: Pruebas unitarias del caso de uso que guarda el análisis de una muestra, con Mockito: el lote bloqueado
 *              primero, la muestra terminada con sus conteos y su modelo, los granos, las fotos aprobadas y el
 *              resultado con los rangos vigentes; y los casos que no guardan nada (RF-10, RF-12, RF-13 C2, RNF-02).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.usco.inspectia.analisis.dominio.ClaseDeGrano;
import co.edu.usco.inspectia.analisis.dominio.ConteoDeMuestra;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeLote;
import co.edu.usco.inspectia.analisis.dominio.FormatoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.ParametrosDelAnalisis;
import co.edu.usco.inspectia.analisis.dominio.RangoDeCategoria;
import co.edu.usco.inspectia.analisis.dominio.RecallsDelModelo;
import co.edu.usco.inspectia.analisis.dominio.ResultadoDelLote;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/** Guardar el análisis de una muestra (RF-10, RF-12), sin Spring ni la base. */
class GuardarAnalisisTest {

    private static final UUID EMPRESA = UUID.fromString("0199f0a0-0000-7000-8000-000000000001");
    private static final UUID LOTE = UUID.fromString("0199f0a0-0000-7000-8000-000000000002");
    private static final UUID MUESTRA = UUID.fromString("0199f0a0-0000-7000-8000-000000000005");
    private static final UUID FOTO_1 = UUID.fromString("0199f0a0-0000-7000-8000-000000000011");
    private static final UUID FOTO_2 = UUID.fromString("0199f0a0-0000-7000-8000-000000000012");
    private static final UUID MODELO = UUID.fromString("0199f0a0-0000-7000-8000-000000000020");
    private static final Instant AHORA = Instant.parse("2026-11-23T15:00:03.000000999Z");
    private static final Instant FIN = Instant.parse("2026-11-23T15:00:03Z");
    private static final Instant INICIO = Instant.parse("2026-11-23T15:00:01.250000Z");
    private static final MuestraTomada TOMADA = new MuestraTomada(MUESTRA, EMPRESA, LOTE, 1, 0, INICIO);
    private static final RecallsDelModelo EFFICIENTNET = new RecallsDelModelo(new BigDecimal("91.20"),
            new BigDecimal("53.40"));
    private static final ModeloUsado FALSO = new ModeloUsado("clasificador-falso", "1.0");
    private static final ParametrosDelAnalisis PARAMETROS = new ParametrosDelAnalisis(new BigDecimal("60"),
            new BigDecimal("10"), BigDecimal.ONE, BigDecimal.ONE);
    private static final List<RangoDeCategoria> RANGOS = List.of(
            new RangoDeCategoria("PREMIUM", new BigDecimal("96.0"), "Exportación"),
            new RangoDeCategoria("ESTANDAR", new BigDecimal("90.0"), "Venta estándar"),
            new RangoDeCategoria("CORRIENTE", new BigDecimal("82.0"), "Seleccionar"),
            new RangoDeCategoria("FUERA_DE_NORMA", new BigDecimal("0.0"), "Reprocesar"));

    private RepositorioDeLotes lotes;
    private RepositorioDeMuestras muestras;
    private RepositorioDeModelos modelos;
    private RangosDeLaEmpresa rangos;
    private GuardarAnalisis guardar;

    @BeforeEach
    void preparar() {
        lotes = mock(RepositorioDeLotes.class);
        muestras = mock(RepositorioDeMuestras.class);
        modelos = mock(RepositorioDeModelos.class);
        rangos = mock(RangosDeLaEmpresa.class);
        guardar = new GuardarAnalisis(lotes, muestras, modelos, rangos, PARAMETROS, Clock.fixed(AHORA, ZoneOffset.UTC));
        when(lotes.estadoParaActualizar(EMPRESA, LOTE)).thenReturn(Optional.of(EstadoDeLote.PENDIENTE));
        when(modelos.buscar("clasificador-falso", "1.0"))
                .thenReturn(Optional.of(new ModeloRegistrado(MODELO, "clasificador-falso", "1.0", EFFICIENTNET)));
        when(muestras.fotos(EMPRESA, MUESTRA)).thenReturn(List.of(
                new FotoGuardada(FOTO_1, 1, "llave-1", FormatoDeFoto.JPG, 3, EstadoDeFoto.CARGADA),
                new FotoGuardada(FOTO_2, 2, "llave-2", FormatoDeFoto.JPG, 3, EstadoDeFoto.CARGADA)));
        when(muestras.terminar(any(), any())).thenReturn(true);
        when(muestras.devolver(TOMADA)).thenReturn(true);
        when(rangos.vigentes(EMPRESA)).thenReturn(RANGOS);
    }

    /**
     * Granos repartidos entre las fotos 1 y 2: los primeros {@code noSanos} no sanos y los primeros {@code dudosos} con
     * confianza 55,00.
     */
    private static List<GranoClasificado> granos(int total, int noSanos, int dudosos) {
        List<GranoClasificado> granos = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            granos.add(new GranoClasificado(i % 2 + 1, i, 2 * i, 5, 8,
                    i < noSanos ? ClaseDeGrano.NO_SANO : ClaseDeGrano.SANO,
                    new BigDecimal(i < dudosos ? "55" : "90.5")));
        }
        return granos;
    }

    @Test
    @DisplayName("RF-10 · C6 · con 87 sanos de 100, termina la muestra con sus conteos, su modelo y la copia de los parámetros, guarda sus granos, aprueba sus fotos y deja el lote con 90,6 % y margen 7,5")
    void guardaElAnalisis() {
        when(muestras.conteosTerminados(EMPRESA, LOTE)).thenReturn(List.of(new ConteoDeMuestra(87, 100, EFFICIENTNET)));

        GuardarAnalisis.Desenlace desenlace = guardar.guardar(TOMADA, new Clasificacion(FALSO, granos(100, 13, 0)));

        assertThat(desenlace).isEqualTo(GuardarAnalisis.Desenlace.TERMINADA);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GranoDeFoto>> guardados = ArgumentCaptor.forClass(List.class);
        InOrder orden = inOrder(lotes, muestras, rangos);
        orden.verify(lotes).estadoParaActualizar(EMPRESA, LOTE);
        orden.verify(muestras).terminar(TOMADA, new TerminoDeMuestra(MODELO, 100, 87, false, new BigDecimal("60"),
                new BigDecimal("10"), FIN));
        orden.verify(muestras).guardarGranos(eq(EMPRESA), guardados.capture());
        orden.verify(muestras).aprobarFotos(EMPRESA, MUESTRA);
        orden.verify(muestras).conteosTerminados(EMPRESA, LOTE);
        orden.verify(rangos).vigentes(EMPRESA);
        orden.verify(lotes).guardarResultado(EMPRESA, LOTE, new ResultadoDelLote(new BigDecimal("90.6"),
                new BigDecimal("7.5"), new BigDecimal("87.0"), 100, 87, "ESTANDAR", "Venta estándar", true, false),
                FIN);
        assertThat(guardados.getValue()).hasSize(100);
        assertThat(guardados.getValue().get(0)).isEqualTo(new GranoDeFoto(FOTO_1, 0, 0, 5, 8, ClaseDeGrano.NO_SANO,
                new BigDecimal("90.50")));
        assertThat(guardados.getValue().get(1).fotoId()).isEqualTo(FOTO_2);
    }

    @Test
    @DisplayName("RF-13 · C2 · el resultado se calcula con los rangos vigentes de la empresa al guardar: con Estándar desde 91 %, 90,6 % es Corriente")
    void usaLosRangosVigentes() {
        when(muestras.conteosTerminados(EMPRESA, LOTE)).thenReturn(List.of(new ConteoDeMuestra(87, 100, EFFICIENTNET)));
        when(rangos.vigentes(EMPRESA)).thenReturn(List.of(
                new RangoDeCategoria("PREMIUM", new BigDecimal("96.0"), "Exportación"),
                new RangoDeCategoria("ESTANDAR", new BigDecimal("91.0"), "Venta de la casa"),
                new RangoDeCategoria("CORRIENTE", new BigDecimal("82.0"), "Seleccionar"),
                new RangoDeCategoria("FUERA_DE_NORMA", new BigDecimal("0.0"), "Reprocesar")));

        guardar.guardar(TOMADA, new Clasificacion(FALSO, granos(100, 13, 0)));

        ArgumentCaptor<ResultadoDelLote> resultado = ArgumentCaptor.forClass(ResultadoDelLote.class);
        verify(lotes).guardarResultado(eq(EMPRESA), eq(LOTE),
                resultado.capture(), eq(FIN));
        assertThat(resultado.getValue().categoria()).isEqualTo("CORRIENTE");
        assertThat(resultado.getValue().recomendacion()).isEqualTo("Seleccionar");
        assertThat(resultado.getValue().sugerirOtraMuestra()).isTrue();
    }

    @Test
    @DisplayName("RF-07 · con 99 granos no calcula el resultado: la muestra vuelve a pendiente, sin granos ni resultado (prueba de límite)")
    void menosDeCienGranos() {
        GuardarAnalisis.Desenlace desenlace = guardar.guardar(TOMADA, new Clasificacion(FALSO, granos(99, 0, 0)));

        assertThat(desenlace).isEqualTo(GuardarAnalisis.Desenlace.DEVUELTA_POR_POCOS_GRANOS);
        InOrder orden = inOrder(lotes, muestras);
        orden.verify(lotes).estadoParaActualizar(EMPRESA, LOTE);
        orden.verify(muestras).devolver(TOMADA);
        verify(muestras, never()).terminar(any(), any());
        verify(muestras, never()).guardarGranos(any(), anyList());
        verify(lotes, never()).guardarResultado(any(), any(), any(), any());
    }

    @Test
    @DisplayName("RNF-02 · si la muestra ya no está en proceso con su hora de inicio, otra ejecución la tomó: no guarda granos ni resultado")
    void yaNoEstabaEnProceso() {
        when(muestras.terminar(any(), any())).thenReturn(false);

        GuardarAnalisis.Desenlace desenlace = guardar.guardar(TOMADA, new Clasificacion(FALSO, granos(100, 13, 0)));

        assertThat(desenlace).isEqualTo(GuardarAnalisis.Desenlace.YA_NO_ESTABA_EN_PROCESO);
        verify(muestras, never()).guardarGranos(any(), anyList());
        verify(muestras, never()).aprobarFotos(any(), any());
        verify(lotes, never()).guardarResultado(any(), any(), any(), any());
    }

    @Test
    @DisplayName("RF-08 · un modelo que no está registrado no deja guardar nada: la transacción se deshace y la cola devuelve la muestra")
    void modeloNoRegistrado() {
        Clasificacion clasificacion = new Clasificacion(new ModeloUsado("EfficientNet-B0", "9.9"), granos(100, 0, 0));

        assertThatThrownBy(() -> guardar.guardar(TOMADA, clasificacion)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EfficientNet-B0 9.9");
        verify(muestras, never()).terminar(any(), any());
    }

    @Test
    @DisplayName("RF-08 · un grano de una foto que la muestra no tiene no deja guardar nada")
    void granoDeOtraFoto() {
        List<GranoClasificado> granos = new ArrayList<>(granos(100, 0, 0));
        granos.set(50, new GranoClasificado(3, 0, 0, 1, 1, ClaseDeGrano.SANO, new BigDecimal("90")));

        assertThatThrownBy(() -> guardar.guardar(TOMADA, new Clasificacion(FALSO, granos)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("foto 3");
        verify(muestras, never()).terminar(any(), any());
    }

    @Test
    @DisplayName("RF-07 · un grano con una posición o una confianza que no sirven no deja guardar nada")
    void granoQueNoSirve() {
        List<GranoClasificado> fueraDeLaFoto = new ArrayList<>(granos(100, 0, 0));
        fueraDeLaFoto.set(0, new GranoClasificado(1, -1, 0, 1, 1, ClaseDeGrano.SANO, new BigDecimal("90")));
        List<GranoClasificado> sinAncho = new ArrayList<>(granos(100, 0, 0));
        sinAncho.set(0, new GranoClasificado(1, 0, 0, 0, 1, ClaseDeGrano.SANO, new BigDecimal("90")));
        List<GranoClasificado> confianzaAlta = new ArrayList<>(granos(100, 0, 0));
        confianzaAlta.set(0, new GranoClasificado(1, 0, 0, 1, 1, ClaseDeGrano.SANO, new BigDecimal("100.01")));

        for (List<GranoClasificado> granos : List.of(fueraDeLaFoto, sinAncho, confianzaAlta)) {
            assertThatThrownBy(() -> guardar.guardar(TOMADA, new Clasificacion(FALSO, granos)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        verify(muestras, never()).terminar(any(), any());
    }

    @Test
    @DisplayName("P-15 · la confianza se guarda con 2 decimales y el grano se compara con el umbral con ese valor: 59,995 queda en 60,00 y no es dudoso")
    void confianzaComoLaGuardaLaBase() {
        when(muestras.conteosTerminados(EMPRESA, LOTE)).thenReturn(List.of(new ConteoDeMuestra(100, 100, EFFICIENTNET)));
        List<GranoClasificado> granos = new ArrayList<>(granos(100, 0, 0));
        granos.set(0, new GranoClasificado(1, 0, 0, 1, 1, ClaseDeGrano.SANO, new BigDecimal("59.995")));

        guardar.guardar(TOMADA, new Clasificacion(FALSO, granos));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GranoDeFoto>> guardados = ArgumentCaptor.forClass(List.class);
        verify(muestras).guardarGranos(eq(EMPRESA), guardados.capture());
        assertThat(guardados.getValue().get(0).confianza()).isEqualTo(new BigDecimal("60.00"));
    }

    @Test
    @DisplayName("RF-09 · después de 2 repeticiones, una muestra con más del 10 % de dudosos termina con el aviso de confianza baja; sin repeticiones, no")
    void avisoDeConfianzaBaja() {
        when(muestras.conteosTerminados(EMPRESA, LOTE)).thenReturn(List.of(new ConteoDeMuestra(100, 100, EFFICIENTNET)));
        MuestraTomada repetida = new MuestraTomada(MUESTRA, EMPRESA, LOTE, 1, 2, INICIO);

        guardar.guardar(repetida, new Clasificacion(FALSO, granos(100, 0, 11)));
        guardar.guardar(TOMADA, new Clasificacion(FALSO, granos(100, 0, 11)));

        verify(muestras).terminar(repetida, new TerminoDeMuestra(MODELO, 100, 100, true, new BigDecimal("60"),
                new BigDecimal("10"), FIN));
        verify(muestras).terminar(TOMADA, new TerminoDeMuestra(MODELO, 100, 100, false, new BigDecimal("60"),
                new BigDecimal("10"), FIN));
    }

    @Test
    @DisplayName("RF-08 · devolver una muestra cuyo análisis falló bloquea primero el lote, como al guardar")
    void devolverBloqueaElLote() {
        assertThat(guardar.devolver(TOMADA)).isTrue();

        InOrder orden = inOrder(lotes, muestras);
        orden.verify(lotes).estadoParaActualizar(EMPRESA, LOTE);
        orden.verify(muestras).devolver(TOMADA);
    }
}
