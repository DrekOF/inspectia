/*
 * Nombre: ConsultarResultadoTest.java
 * Descripción: Pruebas unitarias de las consultas del análisis, con Mockito: el avance de la muestra en curso y el
 *              resultado guardado, sin recalcular, con el límite de la sugerencia y con los granos dudosos según el
 *              umbral que copió su muestra (RF-09 C5, RF-11 C4, RF-14 C3, RNF-01 C4, RNF-03, D-47).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.usco.inspectia.analisis.dominio.ClaseDeGrano;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeLote;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeMuestra;
import co.edu.usco.inspectia.analisis.dominio.FormatoDeFoto;
import co.edu.usco.inspectia.comun.errores.NoEncontradoExcepcion;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Las consultas del análisis: el avance y el resultado (RF-14 C3, RNF-01 C4), sin Spring ni la base. */
class ConsultarResultadoTest {

    private static final UUID EMPRESA = UUID.fromString("0199f0a0-0000-7000-8000-000000000001");
    private static final UUID LOTE = UUID.fromString("0199f0a0-0000-7000-8000-000000000002");
    private static final UUID MUESTRA = UUID.fromString("0199f0a0-0000-7000-8000-000000000005");
    private static final UUID CARLOS = UUID.fromString("0199f0a0-0000-7000-8000-000000000009");
    private static final UUID FOTO_1 = UUID.fromString("0199f0a0-0000-7000-8000-000000000011");
    private static final UUID FOTO_2 = UUID.fromString("0199f0a0-0000-7000-8000-000000000012");
    private static final Instant ENVIO = Instant.parse("2026-11-23T15:00:00Z");
    private static final Instant FIN = Instant.parse("2026-11-23T15:00:03Z");
    private static final EnlaceTemporal VISTA = new EnlaceTemporal("/api/v1/archivos/x", "GET", Map.of(),
            Instant.parse("2026-11-23T15:15:00Z"));

    private RepositorioDeLotes lotes;
    private RepositorioDeMuestras muestras;
    private AlmacenDeArchivos almacen;
    private ConsultarResultado resultado;
    private ConsultarAnalisis analisis;

    @BeforeEach
    void preparar() {
        lotes = mock(RepositorioDeLotes.class);
        muestras = mock(RepositorioDeMuestras.class);
        almacen = mock(AlmacenDeArchivos.class);
        resultado = new ConsultarResultado(lotes, muestras, new VistasDeFotos(almacen));
        analisis = new ConsultarAnalisis(lotes, muestras);
        when(almacen.enlaceDeVista(anyString(), any(Duration.class))).thenReturn(VISTA);
        when(muestras.fotos(EMPRESA, MUESTRA)).thenReturn(List.of(
                new FotoGuardada(FOTO_1, 1, "llave-1", FormatoDeFoto.JPG, 3, EstadoDeFoto.APROBADA),
                new FotoGuardada(FOTO_2, 2, "llave-2", FormatoDeFoto.PNG, 3, EstadoDeFoto.APROBADA)));
    }

    private static ResultadoGuardado analizado() {
        return new ResultadoGuardado(LOTE, "L-0143", LocalDate.parse("2026-10-20"), "La Esperanza",
                EstadoDeLote.ANALIZADO, OffsetDateTime.ofInstant(FIN, ZoneOffset.UTC), new BigDecimal("90.6"),
                new BigDecimal("7.5"), "ESTANDAR", "Apto para venta estándar.", true, new BigDecimal("90.0"),
                "ESTANDAR", false);
    }

    private static MuestraGuardada terminada(String umbral) {
        return new MuestraGuardada(MUESTRA, 1, EstadoDeMuestra.TERMINADA, CARLOS, "Carlos Ramírez",
                "clasificador-falso", "1.0", 100, 87, new BigDecimal(umbral), new BigDecimal("10.0"), ENVIO, FIN);
    }

    @Test
    @DisplayName("RF-14 · C3 · el detalle trae las cifras guardadas del lote, sin recalcularlas, con el límite de la sugerencia, y las de la muestra: 87 de 100, 87,0 % contado")
    void cifrasGuardadas() {
        when(lotes.resultado(EMPRESA, LOTE)).thenReturn(Optional.of(analizado()));
        when(muestras.delLote(EMPRESA, LOTE)).thenReturn(List.of(terminada("60.00")));
        when(muestras.granos(EMPRESA, List.of(FOTO_1, FOTO_2))).thenReturn(List.of());

        DetalleDelResultado detalle = resultado.detalle(EMPRESA, LOTE);

        assertThat(detalle.lote()).isEqualTo(analizado());
        assertThat(detalle.lote().limiteSugerencia()).isEqualByComparingTo("90.0");
        assertThat(detalle.lote().categoriaDelLimite()).isEqualTo("ESTANDAR");
        assertThat(detalle.totalGranos()).isEqualTo(100);
        assertThat(detalle.granosSanos()).isEqualTo(87);
        assertThat(detalle.porcentajeContado()).isEqualTo(new BigDecimal("87.0"));
        assertThat(detalle.muestras()).singleElement().satisfies(muestra -> {
            assertThat(muestra.muestra()).isEqualTo(terminada("60.00"));
            assertThat(muestra.porcentajeContado()).isEqualTo(new BigDecimal("87.0"));
            assertThat(muestra.fotos()).extracting(FotoMarcada::numero).containsExactly(1, 2);
            assertThat(muestra.fotos()).extracting(FotoMarcada::vista).containsOnly(VISTA);
        });
        verify(lotes, never()).guardarResultado(any(), any(), any(), any());
    }

    @Test
    @DisplayName("RF-09 · C5 · un grano es dudoso con el umbral que copió su muestra, no con el de hoy: con 50,00, uno de 55,00 no es dudoso; con 60,00, sí")
    void dudososConElUmbralDeLaMuestra() {
        List<GranoDeFoto> granos = List.of(
                new GranoDeFoto(FOTO_1, 1, 2, 3, 4, ClaseDeGrano.SANO, new BigDecimal("55.00")),
                new GranoDeFoto(FOTO_2, 5, 6, 7, 8, ClaseDeGrano.NO_SANO, new BigDecimal("49.99")),
                new GranoDeFoto(FOTO_1, 9, 10, 11, 12, ClaseDeGrano.NO_SANO, new BigDecimal("60.00")));
        when(lotes.resultado(EMPRESA, LOTE)).thenReturn(Optional.of(analizado()));
        when(muestras.granos(EMPRESA, List.of(FOTO_1, FOTO_2))).thenReturn(granos);

        when(muestras.delLote(EMPRESA, LOTE)).thenReturn(List.of(terminada("50.00")));
        List<FotoMarcada> conCincuenta = resultado.detalle(EMPRESA, LOTE).muestras().get(0).fotos();
        when(muestras.delLote(EMPRESA, LOTE)).thenReturn(List.of(terminada("60.00")));
        List<FotoMarcada> conSesenta = resultado.detalle(EMPRESA, LOTE).muestras().get(0).fotos();

        assertThat(conCincuenta.get(0).granos()).containsExactly(
                new GranoMarcado(1, 2, 3, 4, ClaseDeGrano.SANO, false),
                new GranoMarcado(9, 10, 11, 12, ClaseDeGrano.NO_SANO, false));
        assertThat(conCincuenta.get(1).granos()).containsExactly(
                new GranoMarcado(5, 6, 7, 8, ClaseDeGrano.NO_SANO, true));
        assertThat(conSesenta.get(0).granos()).extracting(GranoMarcado::dudoso).containsExactly(true, false);
    }

    @Test
    @DisplayName("RF-14 · con el lote pendiente, el resultado va vacío y las fotos de la muestra en espera van sin granos")
    void lotePendiente() {
        ResultadoGuardado pendiente = new ResultadoGuardado(LOTE, "L-0143", LocalDate.parse("2026-10-20"),
                "La Esperanza", EstadoDeLote.PENDIENTE, null, null, null, null, null, null, null, null, null);
        MuestraGuardada enEspera = new MuestraGuardada(MUESTRA, 1, EstadoDeMuestra.EN_ESPERA, CARLOS, "Carlos Ramírez",
                null, null, null, null, null, null, ENVIO, null);
        when(lotes.resultado(EMPRESA, LOTE)).thenReturn(Optional.of(pendiente));
        when(muestras.delLote(EMPRESA, LOTE)).thenReturn(List.of(enEspera));

        DetalleDelResultado detalle = resultado.detalle(EMPRESA, LOTE);

        assertThat(detalle.totalGranos()).isNull();
        assertThat(detalle.granosSanos()).isNull();
        assertThat(detalle.porcentajeContado()).isNull();
        assertThat(detalle.muestras().get(0).porcentajeContado()).isNull();
        assertThat(detalle.muestras().get(0).fotos()).extracting(FotoMarcada::granos).containsOnly(List.of());
        verify(muestras, never()).granos(any(), any());
    }

    @Test
    @DisplayName("RNF-03 · C1 · el resultado y el avance de un lote de otra empresa no existen para la sesión: 404")
    void otraEmpresa() {
        when(lotes.resultado(EMPRESA, LOTE)).thenReturn(Optional.empty());
        when(lotes.estado(EMPRESA, LOTE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resultado.detalle(EMPRESA, LOTE)).isInstanceOf(NoEncontradoExcepcion.class);
        assertThatThrownBy(() -> analisis.avance(EMPRESA, LOTE)).isInstanceOf(NoEncontradoExcepcion.class);
        verify(muestras, never()).delLote(any(), any());
        verify(muestras, never()).avance(any(), any());
    }

    @Test
    @DisplayName("RNF-01 · el avance de un lote sin muestra va vacío; el de una muestra en proceso trae su estado y sus fechas")
    void avance() {
        when(lotes.estado(EMPRESA, LOTE)).thenReturn(Optional.of(EstadoDeLote.PENDIENTE));
        when(muestras.avance(EMPRESA, LOTE)).thenReturn(Optional.empty());
        AvanceDelAnalisis sinMuestra = analisis.avance(EMPRESA, LOTE);
        AvanceDelAnalisis enProceso = AvanceDelAnalisis.de(1, EstadoDeMuestra.EN_PROCESO, ENVIO, FIN, null);
        when(muestras.avance(EMPRESA, LOTE)).thenReturn(Optional.of(enProceso));

        assertThat(sinMuestra).isEqualTo(new AvanceDelAnalisis(null, null, null, null, null, false));
        assertThat(analisis.avance(EMPRESA, LOTE)).isEqualTo(enProceso);
    }

    @Test
    @DisplayName("RF-08 · el avance dice que el último envío falló solo si la muestra quedó pendiente con fecha de envío")
    void falloDelUltimoEnvio() {
        assertThat(AvanceDelAnalisis.de(1, EstadoDeMuestra.PENDIENTE, ENVIO, FIN, null).fallo()).isTrue();
        assertThat(AvanceDelAnalisis.de(1, EstadoDeMuestra.PENDIENTE, null, null, null).fallo()).isFalse();
        assertThat(AvanceDelAnalisis.de(1, EstadoDeMuestra.EN_ESPERA, ENVIO, null, null).fallo()).isFalse();
        assertThat(AvanceDelAnalisis.de(1, EstadoDeMuestra.TERMINADA, ENVIO, FIN, FIN).fallo()).isFalse();
    }

    @Test
    @DisplayName("RNF-02 · el detalle lee el lote y sus muestras en una misma foto de la base (REPEATABLE READ): un análisis que se guarda entre las dos lecturas no lo deja con el lote pendiente y su muestra terminada")
    void unaMismaFotoDeLaBase() {
        // Con READ COMMITTED, cada SELECT ve lo confirmado hasta ese momento: si GuardarAnalisis confirma entre la
        // lectura del lote y la de sus muestras, la página recibiría el lote PENDIENTE con su muestra TERMINADA.
        Transactional transaccion = ConsultarResultado.class.getAnnotation(Transactional.class);

        assertThat(transaccion.readOnly()).isTrue();
        assertThat(transaccion.isolation()).isEqualTo(Isolation.REPEATABLE_READ);
    }
}
