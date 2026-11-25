/*
 * Nombre: GuardarAnalisisTest.java
 * Descripción: Pruebas unitarias del caso de uso que guarda el análisis de una muestra, con Mockito: el lote bloqueado
 *              primero, la muestra terminada con sus conteos y su modelo, los granos, las fotos aprobadas y el
 *              resultado con los rangos vigentes y el límite de la sugerencia; la muestra 2, que recalcula el lote
 *              con la suma de las dos y descarta el reporte PDF de la 1; la revisión de las fotos en la cola, el mínimo
 *              de granos y la foto que RF-09 pide repetir; y los casos que no guardan nada (RF-06, RF-07, RF-09,
 *              RF-10, RF-12, RF-13 C2, RF-15 C5, D-25, D-44, D-46, D-47, RNF-02).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.3
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
import co.edu.usco.inspectia.analisis.dominio.MotivoDeDevolucion;
import co.edu.usco.inspectia.analisis.dominio.MotivoDeFoto;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;

/** Guardar el análisis de una muestra (RF-10, RF-12), sin Spring ni la base. */
class GuardarAnalisisTest {

    private static final UUID EMPRESA = UUID.fromString("0199f0a0-0000-7000-8000-000000000001");
    private static final UUID LOTE = UUID.fromString("0199f0a0-0000-7000-8000-000000000002");
    private static final UUID MUESTRA = UUID.fromString("0199f0a0-0000-7000-8000-000000000005");
    private static final UUID MUESTRA_2 = UUID.fromString("0199f0a0-0000-7000-8000-000000000015");
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
    private ApplicationEventPublisher eventos;
    private GuardarAnalisis guardar;

    @BeforeEach
    void preparar() {
        lotes = mock(RepositorioDeLotes.class);
        muestras = mock(RepositorioDeMuestras.class);
        modelos = mock(RepositorioDeModelos.class);
        rangos = mock(RangosDeLaEmpresa.class);
        eventos = mock(ApplicationEventPublisher.class);
        guardar = new GuardarAnalisis(lotes, muestras, modelos, rangos, PARAMETROS, eventos,
                Clock.fixed(AHORA, ZoneOffset.UTC));
        when(lotes.estadoParaActualizar(EMPRESA, LOTE)).thenReturn(Optional.of(EstadoDeLote.PENDIENTE));
        when(modelos.buscar("clasificador-falso", "1.0"))
                .thenReturn(Optional.of(new ModeloRegistrado(MODELO, "clasificador-falso", "1.0", EFFICIENTNET)));
        when(muestras.fotos(EMPRESA, MUESTRA)).thenReturn(List.of(
                new FotoGuardada(FOTO_1, 1, "llave-1", FormatoDeFoto.JPG, 3, EstadoDeFoto.CARGADA),
                new FotoGuardada(FOTO_2, 2, "llave-2", FormatoDeFoto.JPG, 3, EstadoDeFoto.CARGADA)));
        when(muestras.terminar(any(), any())).thenReturn(true);
        when(muestras.devolver(TOMADA)).thenReturn(true);
        when(muestras.devolver(eq(TOMADA), any())).thenReturn(true);
        when(muestras.pedirRepeticion(any(), any(), any())).thenReturn(true);
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
    @DisplayName("RF-10 · C6 · con 87 sanos de 100, termina la muestra con sus conteos, su modelo y la copia de los parámetros, guarda sus granos, aprueba sus fotos y deja el lote con 90,6 %, margen 7,5 y la sugerencia por el límite de Estándar")
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
                new BigDecimal("7.5"), new BigDecimal("87.0"), 100, 87, "ESTANDAR", "Venta estándar", true,
                new BigDecimal("90.0"), "ESTANDAR", false), FIN);
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
        assertThat(resultado.getValue().limiteSugerencia()).as("el límite de Estándar de la empresa")
                .isEqualByComparingTo("91.0");
        assertThat(resultado.getValue().categoriaDelLimite()).isEqualTo("ESTANDAR");
    }

    @Test
    @DisplayName("D-25 · al terminar la muestra 2 de un lote ya analizado, aprueba solo sus fotos y guarda el resultado de la suma de las dos con los rangos vigentes: 174 de 200, 90,6 %, sin sugerencia ni límite (D-47)")
    void laMuestraDosRecalculaElLote() {
        MuestraTomada segunda = new MuestraTomada(MUESTRA_2, EMPRESA, LOTE, 2, 0, INICIO);
        when(lotes.estadoParaActualizar(EMPRESA, LOTE)).thenReturn(Optional.of(EstadoDeLote.ANALIZADO));
        when(muestras.fotos(EMPRESA, MUESTRA_2)).thenReturn(List.of(
                new FotoGuardada(FOTO_1, 1, "llave-1", FormatoDeFoto.JPG, 3, EstadoDeFoto.CARGADA),
                new FotoGuardada(FOTO_2, 2, "llave-2", FormatoDeFoto.JPG, 3, EstadoDeFoto.CARGADA)));
        when(muestras.conteosTerminados(EMPRESA, LOTE)).thenReturn(List.of(new ConteoDeMuestra(87, 100, EFFICIENTNET),
                new ConteoDeMuestra(87, 100, EFFICIENTNET)));

        GuardarAnalisis.Desenlace desenlace = guardar.guardar(segunda, new Clasificacion(FALSO, granos(100, 13, 0)));

        assertThat(desenlace).isEqualTo(GuardarAnalisis.Desenlace.TERMINADA);
        InOrder orden = inOrder(lotes, muestras, rangos);
        orden.verify(lotes).estadoParaActualizar(EMPRESA, LOTE);
        orden.verify(muestras).terminar(segunda, new TerminoDeMuestra(MODELO, 100, 87, false, new BigDecimal("60"),
                new BigDecimal("10"), FIN));
        orden.verify(muestras).aprobarFotos(EMPRESA, MUESTRA_2);
        orden.verify(rangos).vigentes(EMPRESA);
        orden.verify(lotes).guardarResultado(EMPRESA, LOTE, new ResultadoDelLote(new BigDecimal("90.6"),
                new BigDecimal("5.3"), new BigDecimal("87.0"), 200, 174, "ESTANDAR", "Venta estándar", false, null,
                null, false), FIN);
        verify(muestras, never()).aprobarFotos(EMPRESA, MUESTRA);
    }

    @Test
    @DisplayName("RF-15 · C5 · al terminar la muestra 2, el reporte PDF de la 1 se descarta: sus archivos se borran después de confirmar, y el siguiente trae la suma (D-25)")
    void laMuestraDosDescartaElReporte() {
        String base = "reportes/" + EMPRESA + "/" + LOTE + "/0199f0a0-0000-7000-8000-000000000099";
        MuestraTomada segunda = new MuestraTomada(MUESTRA_2, EMPRESA, LOTE, 2, 0, INICIO);
        when(lotes.estadoParaActualizar(EMPRESA, LOTE)).thenReturn(Optional.of(EstadoDeLote.ANALIZADO));
        when(lotes.datosDelReporte(EMPRESA, LOTE)).thenReturn(Optional.of(new DatosDelReporte("Cacaos", "9", base)));
        when(muestras.fotos(EMPRESA, MUESTRA_2)).thenReturn(List.of(
                new FotoGuardada(FOTO_1, 1, "llave-1", FormatoDeFoto.JPG, 3, EstadoDeFoto.CARGADA),
                new FotoGuardada(FOTO_2, 2, "llave-2", FormatoDeFoto.JPG, 3, EstadoDeFoto.CARGADA)));
        when(muestras.conteosTerminados(EMPRESA, LOTE)).thenReturn(List.of(new ConteoDeMuestra(87, 100, EFFICIENTNET),
                new ConteoDeMuestra(87, 100, EFFICIENTNET)));

        guardar.guardar(segunda, new Clasificacion(FALSO, granos(100, 13, 0)));

        InOrder orden = inOrder(lotes, eventos);
        orden.verify(lotes).guardarResultado(eq(EMPRESA), eq(LOTE), any(), eq(FIN));
        orden.verify(eventos).publishEvent(new ReporteDescartado(base));
    }

    @Test
    @DisplayName("RF-15 · un lote sin reporte no descarta nada al guardar su resultado")
    void sinReporteNoDescartaNada() {
        when(muestras.conteosTerminados(EMPRESA, LOTE)).thenReturn(List.of(new ConteoDeMuestra(87, 100, EFFICIENTNET)));
        when(lotes.datosDelReporte(EMPRESA, LOTE)).thenReturn(Optional.of(new DatosDelReporte("Cacaos", "9", null)));

        guardar.guardar(TOMADA, new Clasificacion(FALSO, granos(100, 13, 0)));

        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("RF-07 · C3 · con 99 granos no calcula el resultado: la muestra vuelve a pendiente con el motivo «Menos de 100 granos», sin granos ni resultado (prueba de límite)")
    void menosDeCienGranos() {
        GuardarAnalisis.Desenlace desenlace = guardar.guardar(TOMADA, new Clasificacion(FALSO, granos(99, 0, 0)));

        assertThat(desenlace).isEqualTo(GuardarAnalisis.Desenlace.DEVUELTA_POR_POCOS_GRANOS);
        InOrder orden = inOrder(lotes, muestras);
        orden.verify(lotes).estadoParaActualizar(EMPRESA, LOTE);
        orden.verify(muestras).devolver(TOMADA, MotivoDeDevolucion.POCOS_GRANOS);
        verify(muestras, never()).guardarRevision(any(), any(), any());
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
    @DisplayName("P-15 · la confianza se guarda con 2 decimales y el grano se compara con el umbral con ese valor: 59,995 queda en 60,00 y no es dudoso; 59,994 queda en 59,99 y sí")
    void confianzaComoLaGuardaLaBase() {
        when(muestras.conteosTerminados(EMPRESA, LOTE)).thenReturn(List.of(new ConteoDeMuestra(100, 100, EFFICIENTNET)));
        List<GranoClasificado> granos = new ArrayList<>(granos(100, 0, 0));
        granos.set(0, new GranoClasificado(1, 0, 0, 1, 1, ClaseDeGrano.SANO, new BigDecimal("59.995")));

        guardar.guardar(TOMADA, new Clasificacion(FALSO, granos));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GranoDeFoto>> guardados = ArgumentCaptor.forClass(List.class);
        verify(muestras).guardarGranos(eq(EMPRESA), guardados.capture());
        assertThat(guardados.getValue().get(0).confianza()).isEqualTo(new BigDecimal("60.00"));

        // Que sea dudoso solo cambia el aviso, que pide 2 repeticiones y más del 10 % de dudosos (RF-09): con 11 granos
        // de 59,995 no hay aviso, porque se guardan en 60,00; con 11 de 59,994, que se guardan en 59,99, sí.
        MuestraTomada repetida = new MuestraTomada(MUESTRA, EMPRESA, LOTE, 1, 2, INICIO);
        guardar.guardar(repetida, new Clasificacion(FALSO, conConfianza(11, "59.995")));
        verify(muestras).terminar(repetida, new TerminoDeMuestra(MODELO, 100, 100, false, new BigDecimal("60"),
                new BigDecimal("10"), FIN));
        MuestraTomada otraRepetida = new MuestraTomada(MUESTRA, EMPRESA, LOTE, 1, 2, INICIO.plusSeconds(1));
        guardar.guardar(otraRepetida, new Clasificacion(FALSO, conConfianza(11, "59.994")));
        verify(muestras).terminar(otraRepetida, new TerminoDeMuestra(MODELO, 100, 100, true, new BigDecimal("60"),
                new BigDecimal("10"), FIN));
    }

    /**
     * 100 granos sanos, con los primeros con una confianza dada y los demás con 90,5.
     *
     * @param cuantos cuántos granos llevan esa confianza
     * @param confianza la confianza, como 59.995
     * @return los granos
     */
    private static List<GranoClasificado> conConfianza(int cuantos, String confianza) {
        List<GranoClasificado> granos = new ArrayList<>(granos(100, 0, 0));
        for (int i = 0; i < cuantos; i++) {
            GranoClasificado grano = granos.get(i);
            granos.set(i, new GranoClasificado(grano.numeroDeFoto(), grano.x(), grano.y(), grano.ancho(), grano.alto(),
                    grano.clase(), new BigDecimal(confianza)));
        }
        return granos;
    }

    @Test
    @DisplayName("RF-09 · C4 · después de 2 repeticiones, una muestra con más del 10 % de dudosos termina con el aviso de confianza baja; sin repeticiones, pide repetir una foto")
    void avisoDeConfianzaBaja() {
        when(muestras.conteosTerminados(EMPRESA, LOTE)).thenReturn(List.of(new ConteoDeMuestra(100, 100, EFFICIENTNET)));
        MuestraTomada repetida = new MuestraTomada(MUESTRA, EMPRESA, LOTE, 1, 2, INICIO);

        assertThat(guardar.guardar(repetida, new Clasificacion(FALSO, granos(100, 0, 11))))
                .isEqualTo(GuardarAnalisis.Desenlace.TERMINADA);
        assertThat(guardar.guardar(TOMADA, new Clasificacion(FALSO, granos(100, 0, 11))))
                .isEqualTo(GuardarAnalisis.Desenlace.DEVUELTA_POR_DUDOSOS);

        verify(muestras).terminar(repetida, new TerminoDeMuestra(MODELO, 100, 100, true, new BigDecimal("60"),
                new BigDecimal("10"), FIN));
        verify(muestras, never()).terminar(eq(TOMADA), any());
        verify(muestras, never()).pedirRepeticion(eq(repetida), any(), any());
    }

    @Test
    @DisplayName("RF-09 · C1 · con 11 dudosos de 100, pide repetir la foto con más dudosos: la muestra vuelve con una repetición y la copia del umbral, la foto queda por repetir con sus granos guardados para mostrar cuáles son, y no hay resultado")
    void pideRepetirLaFotoConMasDudosos() {
        // granos(100, 0, 11) reparte los granos entre las fotos 1 y 2: la 1 tiene 6 dudosos y la 2, 5.
        GuardarAnalisis.Desenlace desenlace = guardar.guardar(TOMADA, new Clasificacion(FALSO, granos(100, 0, 11)));

        assertThat(desenlace).isEqualTo(GuardarAnalisis.Desenlace.DEVUELTA_POR_DUDOSOS);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GranoDeFoto>> guardados = ArgumentCaptor.forClass(List.class);
        InOrder orden = inOrder(lotes, muestras);
        orden.verify(lotes).estadoParaActualizar(EMPRESA, LOTE);
        orden.verify(muestras).pedirRepeticion(TOMADA, new BigDecimal("60"), new BigDecimal("10"));
        orden.verify(muestras).guardarRevision(EMPRESA, FOTO_1, RevisionDeFoto.porRepetir(MotivoDeFoto.GRANOS_DUDOSOS,
                50));
        orden.verify(muestras).borrarGranos(EMPRESA, List.of(FOTO_1));
        orden.verify(muestras).guardarGranos(eq(EMPRESA), guardados.capture());
        assertThat(guardados.getValue()).hasSize(50).allSatisfy(grano -> assertThat(grano.fotoId()).isEqualTo(FOTO_1));
        verify(muestras, never()).guardarRevision(eq(EMPRESA), eq(FOTO_2), any());
        verify(muestras, never()).terminar(any(), any());
        verify(muestras, never()).aprobarFotos(any(), any());
        verify(lotes, never()).guardarResultado(any(), any(), any(), any());
    }

    @Test
    @DisplayName("RF-09 · C1 · la foto que se repite es la de más dudosos; en empate, la de número menor; una sola por vez")
    void cualFotoSeRepite() {
        List<GranoClasificado> enLaDos = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            enLaDos.add(new GranoClasificado(i < 50 ? 1 : 2, i, 0, 5, 8, ClaseDeGrano.SANO,
                    new BigDecimal(i >= 85 ? "40" : "90")));
        }
        guardar.guardar(TOMADA, new Clasificacion(FALSO, enLaDos));
        verify(muestras).guardarRevision(EMPRESA, FOTO_2, RevisionDeFoto.porRepetir(MotivoDeFoto.GRANOS_DUDOSOS, 50));
        verify(muestras, never()).guardarRevision(eq(EMPRESA), eq(FOTO_1), any());

        MuestraTomada otra = new MuestraTomada(MUESTRA, EMPRESA, LOTE, 1, 1, INICIO.plusSeconds(1));
        guardar.guardar(otra, new Clasificacion(FALSO, granos(100, 0, 12)));
        verify(muestras).pedirRepeticion(otra, new BigDecimal("60"), new BigDecimal("10"));
        verify(muestras).guardarRevision(EMPRESA, FOTO_1, RevisionDeFoto.porRepetir(MotivoDeFoto.GRANOS_DUDOSOS, 50));
    }

    @Test
    @DisplayName("RF-09 · C2 · con 10 dudosos de 100 el análisis sigue sin pedir nada: termina sin aviso (prueba de límite)")
    void conDiezPorCientoTermina() {
        when(muestras.conteosTerminados(EMPRESA, LOTE)).thenReturn(List.of(new ConteoDeMuestra(100, 100, EFFICIENTNET)));

        assertThat(guardar.guardar(TOMADA, new Clasificacion(FALSO, granos(100, 0, 10))))
                .isEqualTo(GuardarAnalisis.Desenlace.TERMINADA);

        verify(muestras).terminar(TOMADA, new TerminoDeMuestra(MODELO, 100, 100, false, new BigDecimal("60"),
                new BigDecimal("10"), FIN));
        verify(muestras, never()).pedirRepeticion(any(), any(), any());
    }

    @Test
    @DisplayName("RF-06 · C1 · si una foto que llegó «Cargada» no pasa la revisión de la cola, la muestra vuelve a pendiente sin motivo propio, con la revisión de cada foto guardada y sin granos ni resultado")
    void fotoQueNoPasaLaRevisionDeLaCola() {
        Map<Integer, RevisionDeFoto> revisiones = Map.of(1, RevisionDeFoto.aprobada(50),
                2, RevisionDeFoto.porRepetir(MotivoDeFoto.BORROSA, 3));

        GuardarAnalisis.Desenlace desenlace = guardar.guardar(TOMADA, new Clasificacion(FALSO, granos(50, 0, 0),
                revisiones));

        assertThat(desenlace).isEqualTo(GuardarAnalisis.Desenlace.DEVUELTA_POR_FOTOS_POR_REPETIR);
        verify(muestras).devolver(TOMADA, null);
        verify(muestras).guardarRevision(EMPRESA, FOTO_1, RevisionDeFoto.aprobada(50));
        verify(muestras).guardarRevision(EMPRESA, FOTO_2, RevisionDeFoto.porRepetir(MotivoDeFoto.BORROSA, 3));
        verify(muestras, never()).guardarGranos(any(), anyList());
        verify(muestras, never()).terminar(any(), any());
        verify(muestras, never()).pedirRepeticion(any(), any(), any());
    }

    @Test
    @DisplayName("RF-07 · C3 · con el servicio de IA y menos de 100 granos, la muestra vuelve con «Menos de 100 granos» y cada foto queda aprobada con su conteo")
    void pocosGranosConElServicio() {
        Map<Integer, RevisionDeFoto> revisiones = Map.of(1, RevisionDeFoto.aprobada(49), 2,
                RevisionDeFoto.aprobada(50));

        GuardarAnalisis.Desenlace desenlace = guardar.guardar(TOMADA, new Clasificacion(FALSO, granos(99, 0, 0),
                revisiones));

        assertThat(desenlace).isEqualTo(GuardarAnalisis.Desenlace.DEVUELTA_POR_POCOS_GRANOS);
        verify(muestras).devolver(TOMADA, MotivoDeDevolucion.POCOS_GRANOS);
        verify(muestras).guardarRevision(EMPRESA, FOTO_1, RevisionDeFoto.aprobada(49));
        verify(muestras).guardarRevision(EMPRESA, FOTO_2, RevisionDeFoto.aprobada(50));
        verify(muestras, never()).guardarGranos(any(), anyList());
    }

    @Test
    @DisplayName("D-46 · con el servicio de IA, al terminar borra los granos que hubiera en sus fotos antes de guardar los nuevos y cada foto queda aprobada con los granos que la cola clasificó en ella")
    void terminaConLaRevisionDeLaCola() {
        when(muestras.conteosTerminados(EMPRESA, LOTE)).thenReturn(List.of(new ConteoDeMuestra(100, 100, EFFICIENTNET)));
        Map<Integer, RevisionDeFoto> revisiones = Map.of(1, RevisionDeFoto.aprobada(50), 2,
                RevisionDeFoto.aprobada(50));

        assertThat(guardar.guardar(TOMADA, new Clasificacion(FALSO, granos(100, 0, 0), revisiones)))
                .isEqualTo(GuardarAnalisis.Desenlace.TERMINADA);

        InOrder orden = inOrder(muestras);
        orden.verify(muestras).terminar(eq(TOMADA), any());
        orden.verify(muestras).borrarGranos(eq(EMPRESA), any());
        orden.verify(muestras).guardarGranos(eq(EMPRESA), anyList());
        orden.verify(muestras).aprobarFotos(EMPRESA, MUESTRA);
        verify(muestras).guardarRevision(EMPRESA, FOTO_1, RevisionDeFoto.aprobada(50));
        verify(muestras).guardarRevision(EMPRESA, FOTO_2, RevisionDeFoto.aprobada(50));
    }

    @Test
    @DisplayName("RF-08 · una revisión de una foto que la muestra no tiene no deja guardar nada")
    void revisionDeOtraFoto() {
        Clasificacion clasificacion = new Clasificacion(FALSO, granos(100, 0, 0),
                Map.of(3, RevisionDeFoto.aprobada(50)));

        assertThatThrownBy(() -> guardar.guardar(TOMADA, clasificacion)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("3");
        verify(muestras, never()).devolver(any(), any());
        verify(muestras, never()).terminar(any(), any());
    }

    @Test
    @DisplayName("RF-08 · C6 · devolver una muestra sin respuesta del servicio de IA bloquea primero el lote y guarda el motivo")
    void devolverConMotivo() {
        assertThat(guardar.devolver(TOMADA, MotivoDeDevolucion.SIN_RESPUESTA_DE_LA_IA)).isTrue();

        InOrder orden = inOrder(lotes, muestras);
        orden.verify(lotes).estadoParaActualizar(EMPRESA, LOTE);
        orden.verify(muestras).devolver(TOMADA, MotivoDeDevolucion.SIN_RESPUESTA_DE_LA_IA);
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
