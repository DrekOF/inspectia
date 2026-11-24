/*
 * Nombre: AtenderColaTest.java
 * Descripción: Pruebas unitarias del caso de uso que atiende la cola de análisis, con Mockito: la cola vacía, el
 *              clasificador que recibe el contenido de las fotos, y los fallos que devuelven la muestra a pendiente
 *              (RNF-02 C3, RF-08).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.FormatoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.MotivoDeDevolucion;
import java.io.UncheckedIOException;
import java.nio.file.NoSuchFileException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/** Atender la cola de análisis (RNF-02 C3), sin Spring ni la base. */
class AtenderColaTest {

    private static final UUID EMPRESA = UUID.fromString("0199f0a0-0000-7000-8000-000000000001");
    private static final UUID LOTE = UUID.fromString("0199f0a0-0000-7000-8000-000000000002");
    private static final UUID MUESTRA = UUID.fromString("0199f0a0-0000-7000-8000-000000000005");
    private static final Instant AHORA = Instant.parse("2026-11-23T15:00:00.987654321Z");
    private static final MuestraTomada TOMADA = new MuestraTomada(MUESTRA, EMPRESA, LOTE, 1, 0,
            Instant.parse("2026-11-23T15:00:00.987654Z"));
    private static final byte[] PRIMERA = {1, 2, 3};
    private static final byte[] SEGUNDA = {4, 5};
    private static final Clasificacion CLASIFICACION = new Clasificacion(new ModeloUsado("clasificador-falso", "1.0"),
            List.of());

    private ColaDeAnalisis cola;
    private RepositorioDeMuestras muestras;
    private AlmacenDeArchivos almacen;
    private ClasificadorDeGranos clasificador;
    private GuardarAnalisis guardar;
    private AtenderCola atender;

    @BeforeEach
    void preparar() {
        cola = mock(ColaDeAnalisis.class);
        muestras = mock(RepositorioDeMuestras.class);
        almacen = mock(AlmacenDeArchivos.class);
        clasificador = mock(ClasificadorDeGranos.class);
        guardar = mock(GuardarAnalisis.class);
        atender = new AtenderCola(cola, muestras, almacen, clasificador, guardar, Clock.fixed(AHORA, ZoneOffset.UTC));
        when(muestras.fotos(EMPRESA, MUESTRA)).thenReturn(List.of(
                new FotoGuardada(UUID.randomUUID(), 1, "llave-1", FormatoDeFoto.JPG, 3, EstadoDeFoto.CARGADA),
                new FotoGuardada(UUID.randomUUID(), 2, "llave-2", FormatoDeFoto.PNG, 2, EstadoDeFoto.CARGADA)));
        when(almacen.leer("llave-1")).thenReturn(PRIMERA);
        when(almacen.leer("llave-2")).thenReturn(SEGUNDA);
    }

    @Test
    @DisplayName("RNF-02 · C3 · con la cola vacía no atiende nada y lo dice")
    void colaVacia() {
        when(cola.tomarLaSiguiente(any())).thenReturn(Optional.empty());

        assertThat(atender.atenderUna()).isFalse();
        verifyNoInteractions(clasificador, guardar, almacen);
    }

    @Test
    @DisplayName("RNF-02 · C3 · toma la siguiente muestra al microsegundo, le manda al clasificador el contenido de sus fotos y guarda lo que responde")
    void atiendeLaSiguiente() {
        when(cola.tomarLaSiguiente(Instant.parse("2026-11-23T15:00:00.987654Z"))).thenReturn(Optional.of(TOMADA));
        when(clasificador.clasificar(any())).thenReturn(CLASIFICACION);

        assertThat(atender.atenderUna()).isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FotoParaClasificar>> fotos = ArgumentCaptor.forClass(List.class);
        InOrder orden = inOrder(cola, clasificador, guardar);
        orden.verify(cola).tomarLaSiguiente(Instant.parse("2026-11-23T15:00:00.987654Z"));
        orden.verify(clasificador).clasificar(fotos.capture());
        orden.verify(guardar).guardar(TOMADA, CLASIFICACION);
        assertThat(fotos.getValue()).extracting(FotoParaClasificar::numero, FotoParaClasificar::formato,
                FotoParaClasificar::contenido)
                .containsExactly(tuple(1, FormatoDeFoto.JPG, PRIMERA), tuple(2, FormatoDeFoto.PNG, SEGUNDA));
        verify(guardar, never()).devolver(any());
    }

    @Test
    @DisplayName("RF-08 · si el clasificador falla, la muestra vuelve a pendiente sin resultado y la cola sigue")
    void clasificadorFalla() {
        when(cola.tomarLaSiguiente(any())).thenReturn(Optional.of(TOMADA));
        when(clasificador.clasificar(any())).thenThrow(new IllegalStateException("El servicio de IA no responde"));

        assertThat(atender.atenderUna()).isTrue();

        verify(guardar, never()).guardar(any(), any());
        verify(guardar).devolver(TOMADA);
    }

    @Test
    @DisplayName("RF-08 · C6 · si el servicio de IA no responde, la muestra vuelve a pendiente con el motivo «Sin respuesta del servicio de IA» y sin resultado")
    void servicioSinRespuesta() {
        when(cola.tomarLaSiguiente(any())).thenReturn(Optional.of(TOMADA));
        when(clasificador.clasificar(any())).thenThrow(new SinRespuestaDeLaIaExcepcion("tiempo agotado", null));

        assertThat(atender.atenderUna()).isTrue();

        verify(guardar, never()).guardar(any(), any());
        verify(guardar).devolver(TOMADA, MotivoDeDevolucion.SIN_RESPUESTA_DE_LA_IA);
        verify(guardar, never()).devolver(TOMADA);
    }

    @Test
    @DisplayName("RF-08 · si una foto no se puede leer del almacén, la muestra vuelve a pendiente sin clasificarla")
    void fotoQueNoSeLee() {
        when(cola.tomarLaSiguiente(any())).thenReturn(Optional.of(TOMADA));
        when(almacen.leer("llave-2")).thenThrow(new UncheckedIOException(new NoSuchFileException("llave-2")));

        assertThat(atender.atenderUna()).isTrue();

        verifyNoInteractions(clasificador);
        verify(guardar).devolver(TOMADA);
    }

    @Test
    @DisplayName("RF-08 · si guardar el resultado falla, la muestra vuelve a pendiente; si tampoco se puede devolver, la cola sigue y la muestra vuelve al arrancar")
    void guardarFalla() {
        when(cola.tomarLaSiguiente(any())).thenReturn(Optional.of(TOMADA));
        when(clasificador.clasificar(any())).thenReturn(CLASIFICACION);
        when(guardar.guardar(TOMADA, CLASIFICACION)).thenThrow(new IllegalStateException("La base rechazó el lote"));
        doThrow(new IllegalStateException("La base no responde")).when(guardar).devolver(TOMADA);

        assertThat(atender.atenderUna()).isTrue();

        verify(guardar).devolver(TOMADA);
    }
}
