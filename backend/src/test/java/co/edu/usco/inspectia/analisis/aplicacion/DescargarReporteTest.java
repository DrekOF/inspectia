/*
 * Nombre: DescargarReporteTest.java
 * Descripción: Pruebas unitarias del caso de uso que descarga el reporte PDF de un lote, con Mockito: el lote de otra
 *              empresa o pendiente, la primera descarga que lo arma en los dos idiomas y lo guarda, las siguientes que
 *              entregan el guardado, los dos archivos que faltan, uno solo que falta (se rehace solo ese y se
 *              conserva el otro), el contenido con las muestras terminadas y sus fotos, y el
 *              nombre del archivo (RF-15 C1, C4 y C5, RNF-03, D-50).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.usco.inspectia.analisis.dominio.ClaseDeGrano;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeLote;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeMuestra;
import co.edu.usco.inspectia.analisis.dominio.FormatoDeFoto;
import co.edu.usco.inspectia.comun.errores.ConflictoExcepcion;
import co.edu.usco.inspectia.comun.errores.NoEncontradoExcepcion;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;

/** Descargar el reporte PDF de un lote (RF-15), sin Spring ni la base. */
class DescargarReporteTest {

    private static final UUID EMPRESA = UUID.fromString("0199f0a0-0000-7000-8000-000000000001");
    private static final UUID LOTE = UUID.fromString("0199f0a0-0000-7000-8000-000000000002");
    private static final UUID MUESTRA_1 = UUID.fromString("0199f0a0-0000-7000-8000-000000000005");
    private static final UUID MUESTRA_2 = UUID.fromString("0199f0a0-0000-7000-8000-000000000006");
    private static final UUID FOTO_1 = UUID.fromString("0199f0a0-0000-7000-8000-000000000011");
    private static final UUID FOTO_2 = UUID.fromString("0199f0a0-0000-7000-8000-000000000012");
    private static final Instant AHORA = Instant.parse("2026-11-24T20:00:00Z");
    private static final String BASE = "reportes/" + EMPRESA + "/" + LOTE + "/0199f0a0-0000-7000-8000-000000000099";
    private static final byte[] PDF_ES = {'%', 'P', 'D', 'F', 'e', 's'};
    private static final byte[] PDF_EN = {'%', 'P', 'D', 'F', 'e', 'n'};
    private static final ResultadoGuardado RESULTADO = new ResultadoGuardado(LOTE, "L-0143", LocalDate.of(2026, 10, 20),
            "La Esperanza", EstadoDeLote.ANALIZADO, OffsetDateTime.parse("2026-11-23T15:00:03Z"),
            new BigDecimal("90.6"), new BigDecimal("7.5"), "ESTANDAR", "Venta estándar", true, new BigDecimal("90.0"),
            "ESTANDAR", false);

    private RepositorioDeLotes lotes;
    private RepositorioDeMuestras muestras;
    private AlmacenDeArchivos almacen;
    private GeneradorDeReporte generador;
    private ApplicationEventPublisher eventos;
    private DescargarReporte descargar;

    @BeforeEach
    void preparar() {
        lotes = mock(RepositorioDeLotes.class);
        muestras = mock(RepositorioDeMuestras.class);
        almacen = mock(AlmacenDeArchivos.class);
        generador = mock(GeneradorDeReporte.class);
        eventos = mock(ApplicationEventPublisher.class);
        descargar = new DescargarReporte(lotes, muestras, almacen, generador, eventos,
                Clock.fixed(AHORA, ZoneOffset.UTC));
        when(lotes.estadoParaActualizar(EMPRESA, LOTE)).thenReturn(Optional.of(EstadoDeLote.ANALIZADO));
        when(lotes.resultado(EMPRESA, LOTE)).thenReturn(Optional.of(RESULTADO));
        when(lotes.datosDelReporte(EMPRESA, LOTE))
                .thenReturn(Optional.of(new DatosDelReporte("Cacaos del Huila", "900123456-8", null)));
        when(muestras.delLote(EMPRESA, LOTE)).thenReturn(List.of(terminada(MUESTRA_1, 1, "Luis Gómez", 87)));
        when(muestras.fotos(EMPRESA, MUESTRA_1)).thenReturn(List.of(
                new FotoGuardada(FOTO_1, 1, "llave-1", FormatoDeFoto.JPG, 3, EstadoDeFoto.APROBADA),
                new FotoGuardada(FOTO_2, 2, "llave-2", FormatoDeFoto.PNG, 3, EstadoDeFoto.APROBADA)));
        when(muestras.granos(eq(EMPRESA), any())).thenReturn(List.of(
                new GranoDeFoto(FOTO_1, 1, 2, 3, 4, ClaseDeGrano.SANO, new BigDecimal("90.00")),
                new GranoDeFoto(FOTO_1, 5, 6, 7, 8, ClaseDeGrano.NO_SANO, new BigDecimal("59.99")),
                new GranoDeFoto(FOTO_2, 9, 9, 9, 9, ClaseDeGrano.SANO, new BigDecimal("60.00"))));
        when(almacen.leer("llave-1")).thenReturn(new byte[] {1});
        when(almacen.leer("llave-2")).thenReturn(new byte[] {2});
        when(generador.generar(any(), eq(IdiomaDelReporte.ES))).thenReturn(PDF_ES);
        when(generador.generar(any(), eq(IdiomaDelReporte.EN))).thenReturn(PDF_EN);
    }

    private static MuestraGuardada terminada(UUID id, int numero, String analista, int sanos) {
        return new MuestraGuardada(id, numero, EstadoDeMuestra.TERMINADA, UUID.randomUUID(), analista,
                "EfficientNet-B0", "1.0", 100, sanos, new BigDecimal("60.00"), new BigDecimal("10.0"),
                AHORA.minusSeconds(60), AHORA.minusSeconds(30));
    }

    @Test
    @DisplayName("RNF-03 · el reporte de un lote de otra empresa, o que no existe, responde 404 sin armar nada")
    void deOtraEmpresa() {
        when(lotes.estadoParaActualizar(EMPRESA, LOTE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> descargar.descargar(EMPRESA, LOTE, IdiomaDelReporte.ES))
                .isInstanceOf(NoEncontradoExcepcion.class);
        verify(generador, never()).generar(any(), any());
    }

    @Test
    @DisplayName("RF-15 · un lote pendiente todavía no tiene reporte: 409, sin armar nada")
    void pendiente() {
        when(lotes.estadoParaActualizar(EMPRESA, LOTE)).thenReturn(Optional.of(EstadoDeLote.PENDIENTE));

        assertThatThrownBy(() -> descargar.descargar(EMPRESA, LOTE, IdiomaDelReporte.ES))
                .isInstanceOf(ConflictoExcepcion.class).hasMessage("reporte.lote_pendiente");
        verify(generador, never()).generar(any(), any());
    }

    @Test
    @DisplayName("RF-15 · la primera descarga bloquea el lote, arma el reporte en español y en inglés, guarda los dos archivos con la llave reportes/<empresa>/<lote>/<uuid>-<es|en>.pdf y la base en el lote (D-50)")
    void primeraDescarga() {
        ReporteDelLote reporte = descargar.descargar(EMPRESA, LOTE, IdiomaDelReporte.EN);

        assertThat(reporte.nombre()).isEqualTo("InspectIA-L-0143-en.pdf");
        assertThat(reporte.contenido()).isEqualTo(PDF_EN);
        ArgumentCaptor<String> base = ArgumentCaptor.forClass(String.class);
        InOrder orden = inOrder(lotes, almacen, eventos);
        orden.verify(lotes).estadoParaActualizar(EMPRESA, LOTE);
        orden.verify(eventos).publishEvent(any(ReporteNuevo.class));
        orden.verify(lotes).guardarRutaDelReporte(eq(EMPRESA), eq(LOTE), base.capture());
        assertThat(base.getValue()).matches("reportes/" + EMPRESA + "/" + LOTE + "/[0-9a-f-]{36}");
        verify(almacen).guardar(base.getValue() + "-es.pdf", PDF_ES, "application/pdf");
        verify(almacen).guardar(base.getValue() + "-en.pdf", PDF_EN, "application/pdf");
        verify(eventos).publishEvent(new ReporteNuevo(base.getValue()));
    }

    @Test
    @DisplayName("RF-15 · C4 · al descargarlo de nuevo se entrega el archivo guardado, sin armarlo otra vez: es el mismo reporte aunque la empresa haya cambiado sus rangos")
    void segundaDescarga() {
        when(lotes.datosDelReporte(EMPRESA, LOTE))
                .thenReturn(Optional.of(new DatosDelReporte("Cacaos del Huila", "900123456-8", BASE)));
        when(almacen.describir(BASE + "-es.pdf")).thenReturn(Optional.of(new ArchivoGuardado(6, PDF_ES)));
        when(almacen.leer(BASE + "-es.pdf")).thenReturn(PDF_ES);

        ReporteDelLote reporte = descargar.descargar(EMPRESA, LOTE, IdiomaDelReporte.ES);

        assertThat(reporte.contenido()).isEqualTo(PDF_ES);
        assertThat(reporte.nombre()).isEqualTo("InspectIA-L-0143-es.pdf");
        verify(generador, never()).generar(any(), any());
        verify(almacen, never()).guardar(anyString(), any(), anyString());
        verify(lotes, never()).guardarRutaDelReporte(any(), any(), any());
    }

    @Test
    @DisplayName("RF-15 · si faltan los archivos de los dos idiomas, el reporte se arma de nuevo con otra base y el incompleto se descarta")
    void faltaElArchivo() {
        when(lotes.datosDelReporte(EMPRESA, LOTE))
                .thenReturn(Optional.of(new DatosDelReporte("Cacaos del Huila", "900123456-8", BASE)));
        when(almacen.describir(BASE + "-es.pdf")).thenReturn(Optional.empty());
        when(almacen.describir(BASE + "-en.pdf")).thenReturn(Optional.empty());

        ReporteDelLote reporte = descargar.descargar(EMPRESA, LOTE, IdiomaDelReporte.ES);

        assertThat(reporte.contenido()).isEqualTo(PDF_ES);
        verify(eventos).publishEvent(new ReporteDescartado(BASE));
        ArgumentCaptor<String> base = ArgumentCaptor.forClass(String.class);
        verify(lotes).guardarRutaDelReporte(eq(EMPRESA), eq(LOTE), base.capture());
        assertThat(base.getValue()).isNotEqualTo(BASE);
    }

    @Test
    @DisplayName("RF-15 · si falta solo el archivo del idioma pedido, se rehace solo ese en la misma base y el del otro idioma se conserva tal cual, así sigue siendo el mismo reporte al descargarlo de nuevo (C4)")
    void faltaUnSoloIdioma() {
        when(lotes.datosDelReporte(EMPRESA, LOTE))
                .thenReturn(Optional.of(new DatosDelReporte("Cacaos del Huila", "900123456-8", BASE)));
        when(almacen.describir(BASE + "-en.pdf")).thenReturn(Optional.empty());
        when(almacen.describir(BASE + "-es.pdf")).thenReturn(Optional.of(new ArchivoGuardado(6, PDF_ES)));

        ReporteDelLote reporte = descargar.descargar(EMPRESA, LOTE, IdiomaDelReporte.EN);

        assertThat(reporte.contenido()).isEqualTo(PDF_EN);
        assertThat(reporte.nombre()).isEqualTo("InspectIA-L-0143-en.pdf");
        verify(generador).generar(any(), eq(IdiomaDelReporte.EN));
        verify(generador, never()).generar(any(), eq(IdiomaDelReporte.ES));
        verify(almacen).guardar(BASE + "-en.pdf", PDF_EN, "application/pdf");
        verify(almacen, never()).guardar(eq(BASE + "-es.pdf"), any(), anyString());
        verify(almacen, never()).borrar(anyString());
        verify(eventos, never()).publishEvent(any());
        verify(lotes, never()).guardarRutaDelReporte(any(), any(), any());
    }

    @Test
    @DisplayName("RF-15 · C1 · el reporte lleva la empresa, el resultado guardado, los granos de las muestras terminadas, cada una con sus fotos y sus granos marcados con el umbral que copió (P-15), y la fecha en que se armó")
    void contenido() {
        ArgumentCaptor<ContenidoDelReporte> contenido = ArgumentCaptor.forClass(ContenidoDelReporte.class);

        descargar.descargar(EMPRESA, LOTE, IdiomaDelReporte.ES);

        verify(generador).generar(contenido.capture(), eq(IdiomaDelReporte.ES));
        ContenidoDelReporte armado = contenido.getValue();
        assertThat(armado.empresa()).isEqualTo("Cacaos del Huila");
        assertThat(armado.nit()).isEqualTo("900123456-8");
        assertThat(armado.lote()).isEqualTo(RESULTADO);
        assertThat(armado.totalGranos()).isEqualTo(100);
        assertThat(armado.granosSanos()).isEqualTo(87);
        assertThat(armado.porcentajeContado()).isEqualByComparingTo("87.0");
        assertThat(armado.generado()).isEqualTo(AHORA);
        assertThat(armado.muestras()).hasSize(1);
        ContenidoDelReporte.MuestraDelReporte muestra = armado.muestras().getFirst();
        assertThat(muestra.muestra().usuarioNombre()).isEqualTo("Luis Gómez");
        assertThat(muestra.fotos()).extracting(ContenidoDelReporte.FotoDelReporte::numero).containsExactly(1, 2);
        assertThat(muestra.fotos().getFirst().contenido()).containsExactly(1);
        assertThat(muestra.fotos().getFirst().granos()).containsExactly(
                new GranoMarcado(1, 2, 3, 4, ClaseDeGrano.SANO, false),
                new GranoMarcado(5, 6, 7, 8, ClaseDeGrano.NO_SANO, true));
        assertThat(muestra.fotos().get(1).granos()).containsExactly(
                new GranoMarcado(9, 9, 9, 9, ClaseDeGrano.SANO, false));
    }

    @Test
    @DisplayName("RF-15 · C5 · con 2 muestras terminadas el reporte trae la suma y las dos; una muestra 2 que todavía no termina no entra (D-25)")
    void dosMuestras() {
        when(muestras.delLote(EMPRESA, LOTE)).thenReturn(List.of(terminada(MUESTRA_1, 1, "Luis Gómez", 87),
                terminada(MUESTRA_2, 2, "Pedro Díaz", 86)));
        ArgumentCaptor<ContenidoDelReporte> contenido = ArgumentCaptor.forClass(ContenidoDelReporte.class);

        descargar.descargar(EMPRESA, LOTE, IdiomaDelReporte.ES);

        verify(generador).generar(contenido.capture(), eq(IdiomaDelReporte.ES));
        assertThat(contenido.getValue().totalGranos()).isEqualTo(200);
        assertThat(contenido.getValue().granosSanos()).isEqualTo(173);
        assertThat(contenido.getValue().porcentajeContado()).isEqualByComparingTo("86.5");
        assertThat(contenido.getValue().muestras()).extracting(m -> m.muestra().usuarioNombre())
                .containsExactly("Luis Gómez", "Pedro Díaz");

        MuestraGuardada enEspera = new MuestraGuardada(MUESTRA_2, 2, EstadoDeMuestra.EN_ESPERA, UUID.randomUUID(),
                "Pedro Díaz", null, null, null, null, null, null, AHORA, null);
        when(muestras.delLote(EMPRESA, LOTE)).thenReturn(List.of(terminada(MUESTRA_1, 1, "Luis Gómez", 87),
                enEspera));
        ArgumentCaptor<ContenidoDelReporte> conEspera = ArgumentCaptor.forClass(ContenidoDelReporte.class);
        descargar.descargar(EMPRESA, LOTE, IdiomaDelReporte.EN);
        verify(generador, org.mockito.Mockito.times(2)).generar(conEspera.capture(), eq(IdiomaDelReporte.EN));
        assertThat(conEspera.getValue().muestras()).hasSize(1);
        assertThat(conEspera.getValue().totalGranos()).isEqualTo(100);
    }

    @Test
    @DisplayName("RF-15 · una foto que el almacén no puede leer va sin contenido, y el reporte sale igual")
    void fotoQueNoSeLee() {
        when(almacen.leer("llave-2")).thenThrow(new UncheckedIOException(new java.io.IOException("no está")));
        ArgumentCaptor<ContenidoDelReporte> contenido = ArgumentCaptor.forClass(ContenidoDelReporte.class);

        descargar.descargar(EMPRESA, LOTE, IdiomaDelReporte.ES);

        verify(generador).generar(contenido.capture(), eq(IdiomaDelReporte.ES));
        assertThat(contenido.getValue().muestras().getFirst().fotos().get(1).contenido()).isNull();
    }

    @Test
    @DisplayName("RF-15 · el nombre del archivo es InspectIA-<código>-<idioma>.pdf, sin los caracteres que no van en un nombre de archivo")
    void nombreDelArchivo() {
        assertThat(DescargarReporte.nombreDelArchivo("L-0143", IdiomaDelReporte.ES)).isEqualTo("InspectIA-L-0143-es.pdf");
        assertThat(DescargarReporte.nombreDelArchivo(" Lote 7/B:ñ ", IdiomaDelReporte.EN))
                .isEqualTo("InspectIA-Lote_7_B_ñ-en.pdf");
    }

    @Test
    @DisplayName("RF-15 · el idioma del reporte se elige por su código, sin distinguir mayúsculas, o por la región de la petición: el inglés si es inglesa y si no el español")
    void idiomas() {
        assertThat(IdiomaDelReporte.de("EN")).contains(IdiomaDelReporte.EN);
        assertThat(IdiomaDelReporte.de(" es ")).contains(IdiomaDelReporte.ES);
        assertThat(IdiomaDelReporte.de("fr")).isEmpty();
        assertThat(IdiomaDelReporte.de((String) null)).isEmpty();
        assertThat(IdiomaDelReporte.de(java.util.Locale.UK)).isEqualTo(IdiomaDelReporte.EN);
        assertThat(IdiomaDelReporte.de(java.util.Locale.FRANCE)).isEqualTo(IdiomaDelReporte.ES);
        assertThat(IdiomaDelReporte.de((java.util.Locale) null)).isEqualTo(IdiomaDelReporte.ES);
    }
}
