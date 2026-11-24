/*
 * Nombre: EnviarMuestraTest.java
 * Descripción: Pruebas unitarias del caso de uso «Analizar la muestra», con Mockito: al menos 2 fotos, la muestra en
 *              espera con su fecha y quien la envía, el reintento seguro, el lote ya analizado y otra empresa
 *              (RF-05 C2, RNF-02 C3, RNF-03).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeLote;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeMuestra;
import co.edu.usco.inspectia.analisis.dominio.FormatoDeFoto;
import co.edu.usco.inspectia.comun.errores.CampoInvalido;
import co.edu.usco.inspectia.comun.errores.ConflictoExcepcion;
import co.edu.usco.inspectia.comun.errores.NoEncontradoExcepcion;
import co.edu.usco.inspectia.comun.errores.ReglaIncumplidaExcepcion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;

/** «Analizar la muestra» (RF-05 C2, RNF-02 C3), sin Spring ni la base. */
class EnviarMuestraTest {

    private static final UUID EMPRESA = UUID.fromString("0199f0a0-0000-7000-8000-000000000001");
    private static final UUID LOTE = UUID.fromString("0199f0a0-0000-7000-8000-000000000002");
    private static final UUID MUESTRA = UUID.fromString("0199f0a0-0000-7000-8000-000000000005");
    private static final UUID CARLOS = UUID.fromString("0199f0a0-0000-7000-8000-000000000009");
    /** Con nanosegundos: lo que se guarda y se responde queda al microsegundo, como en PostgreSQL. */
    private static final Instant AHORA = Instant.parse("2026-11-23T15:00:00.123456789Z");
    private static final Instant AHORA_EN_MICROS = Instant.parse("2026-11-23T15:00:00.123456Z");

    private RepositorioDeLotes lotes;
    private RepositorioDeMuestras muestras;
    private ApplicationEventPublisher eventos;
    private EnviarMuestra enviar;

    @BeforeEach
    void preparar() {
        lotes = mock(RepositorioDeLotes.class);
        muestras = mock(RepositorioDeMuestras.class);
        eventos = mock(ApplicationEventPublisher.class);
        enviar = new EnviarMuestra(lotes, muestras, eventos, Clock.fixed(AHORA, ZoneOffset.UTC));
        when(lotes.estadoParaActualizar(EMPRESA, LOTE)).thenReturn(Optional.of(EstadoDeLote.PENDIENTE));
    }

    private void conMuestra(EstadoDeMuestra estado, int fotos) {
        when(muestras.muestraEnCurso(EMPRESA, LOTE)).thenReturn(Optional.of(new MuestraEnCurso(MUESTRA, 1, estado)));
        when(muestras.fotos(EMPRESA, MUESTRA)).thenReturn(IntStream.rangeClosed(1, fotos)
                .mapToObj(numero -> new FotoGuardada(UUID.randomUUID(), numero, "llave-" + numero, FormatoDeFoto.JPG,
                        1_000, EstadoDeFoto.CARGADA))
                .toList());
    }

    private static List<CampoInvalido> campos(ReglaIncumplidaExcepcion excepcion) {
        return excepcion.getCampos();
    }

    @Test
    @DisplayName("RF-05 · C2 · con 1 foto, «Analizar la muestra» responde 400 en «fotos» y no cambia nada (prueba de límite)")
    void conUnaFotoNoSeEnvia() {
        conMuestra(EstadoDeMuestra.PENDIENTE, 1);

        assertThatThrownBy(() -> enviar.enviar(EMPRESA, CARLOS, LOTE))
                .isInstanceOfSatisfying(ReglaIncumplidaExcepcion.class, excepcion -> assertThat(campos(excepcion))
                        .containsExactly(new CampoInvalido("fotos", "analisis.fotos.minimo")));
        verify(muestras, never()).enviar(any(), any(), any(), any());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("RF-05 · C2 · un lote sin muestra, que todavía no tiene fotos, responde 400 en «fotos»")
    void sinMuestraNoSeEnvia() {
        when(muestras.muestraEnCurso(EMPRESA, LOTE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enviar.enviar(EMPRESA, CARLOS, LOTE))
                .isInstanceOfSatisfying(ReglaIncumplidaExcepcion.class, excepcion -> assertThat(campos(excepcion))
                        .containsExactly(new CampoInvalido("fotos", "analisis.fotos.minimo")));
        verify(muestras, never()).enviar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("RF-05 · C2 · con 2 fotos, la muestra queda en espera con la fecha de envío al microsegundo y con quien la envía, y avisa a la cola (prueba de límite)")
    void conDosFotosQuedaEnEspera() {
        conMuestra(EstadoDeMuestra.PENDIENTE, 2);

        AvanceDelAnalisis avance = enviar.enviar(EMPRESA, CARLOS, LOTE);

        assertThat(avance).isEqualTo(new AvanceDelAnalisis(1, EstadoDeMuestra.EN_ESPERA, AHORA_EN_MICROS, null, null,
                false));
        InOrder orden = inOrder(lotes, muestras, eventos);
        orden.verify(lotes).estadoParaActualizar(EMPRESA, LOTE);
        orden.verify(muestras).enviar(EMPRESA, MUESTRA, CARLOS, AHORA_EN_MICROS);
        orden.verify(eventos).publishEvent(new MuestraEnviada(MUESTRA));
    }

    @Test
    @DisplayName("RF-05 · C2 · con 3 fotos, el máximo, la muestra también se envía")
    void conTresFotosSeEnvia() {
        conMuestra(EstadoDeMuestra.PENDIENTE, 3);

        assertThat(enviar.enviar(EMPRESA, CARLOS, LOTE).estado()).isEqualTo(EstadoDeMuestra.EN_ESPERA);
        verify(muestras).enviar(EMPRESA, MUESTRA, CARLOS, AHORA_EN_MICROS);
    }

    @Test
    @DisplayName("RNF-02 · enviar otra vez una muestra en espera o en proceso responde su avance sin cambiar nada: el reintento es seguro")
    void reintentoSeguro() {
        AvanceDelAnalisis enEspera = AvanceDelAnalisis.de(1, EstadoDeMuestra.EN_ESPERA,
                Instant.parse("2026-11-23T14:59:00Z"), null, null);
        when(muestras.avance(EMPRESA, LOTE)).thenReturn(Optional.of(enEspera));

        for (EstadoDeMuestra estado : List.of(EstadoDeMuestra.EN_ESPERA, EstadoDeMuestra.EN_PROCESO)) {
            conMuestra(estado, 2);
            assertThat(enviar.enviar(EMPRESA, CARLOS, LOTE)).isEqualTo(enEspera);
        }
        verify(muestras, never()).enviar(any(), any(), any(), any());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("RF-12 · un lote ya analizado no se vuelve a analizar: 409 con el motivo")
    void loteAnalizado() {
        when(lotes.estadoParaActualizar(EMPRESA, LOTE)).thenReturn(Optional.of(EstadoDeLote.ANALIZADO));

        assertThatThrownBy(() -> enviar.enviar(EMPRESA, CARLOS, LOTE))
                .isInstanceOfSatisfying(ConflictoExcepcion.class,
                        excepcion -> assertThat(excepcion.getClave()).isEqualTo("analisis.lote_analizado"));
        verify(muestras, never()).enviar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("RF-12 · una muestra ya terminada no se vuelve a enviar: 409 con el motivo")
    void muestraTerminada() {
        conMuestra(EstadoDeMuestra.TERMINADA, 2);

        assertThatThrownBy(() -> enviar.enviar(EMPRESA, CARLOS, LOTE)).isInstanceOf(ConflictoExcepcion.class);
        verify(muestras, never()).enviar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("RNF-03 · C1 · un lote de otra empresa no existe para la sesión: 404, sin buscar su muestra")
    void otraEmpresa() {
        when(lotes.estadoParaActualizar(EMPRESA, LOTE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enviar.enviar(EMPRESA, CARLOS, LOTE)).isInstanceOf(NoEncontradoExcepcion.class);
        verify(muestras, never()).muestraEnCurso(any(), any());
        verify(muestras, never()).enviar(any(), any(), any(), any());
        verify(eventos, never()).publishEvent(any(Object.class));
    }
}
