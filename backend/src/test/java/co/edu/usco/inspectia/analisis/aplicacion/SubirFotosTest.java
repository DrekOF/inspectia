/*
 * Nombre: SubirFotosTest.java
 * Descripción: Pruebas unitarias del caso de uso de subir las fotos, con Mockito (RF-05): el reemplazo borra la fila
 *              anterior y deja el archivo para un evento, la revisión del archivo al confirmarlo, el número de la foto,
 *              la forma de la llave, un lote que no está pendiente no recibe fotos, y la segunda muestra de un lote
 *              con otra muestra sugerida, que se crea con su primera foto sin tocar la muestra 1 (RF-12 C8,
 *              RF-14 C6, D-25).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import co.edu.usco.inspectia.comun.errores.ReglaIncumplidaExcepcion;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;

/**
 * El caso de uso de subir las fotos (RF-05), sin Spring ni la base. Un lote analizado solo recibe fotos si sugiere otra
 * muestra ({@link RepositorioDeLotes#sugiereOtraMuestra}) y su muestra en curso es la 1 terminada, que da paso a la
 * 2, o la 2 pendiente (D-25).
 */
class SubirFotosTest {

    private static final UUID EMPRESA = UUID.fromString("0199f0a0-0000-7000-8000-000000000001");
    private static final UUID LOTE = UUID.fromString("0199f0a0-0000-7000-8000-000000000002");
    private static final UUID USUARIO = UUID.fromString("0199f0a0-0000-7000-8000-000000000009");
    private static final UUID MUESTRA = UUID.fromString("0199f0a0-0000-7000-8000-000000000005");
    private static final UUID MUESTRA_2 = UUID.fromString("0199f0a0-0000-7000-8000-000000000015");
    private static final String ANTERIOR = EMPRESA + "/" + LOTE + "/0199f0a0-0000-7000-8000-000000000006.jpg";
    private static final String NUEVA = EMPRESA + "/" + LOTE + "/0199f0a0-0000-7000-8000-000000000007.png";
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final EnlaceTemporal VISTA = new EnlaceTemporal("/api/v1/archivos/x", "GET", Map.of(),
            Instant.parse("2026-11-23T15:15:00Z"));

    private RepositorioDeLotes lotes;
    private RepositorioDeMuestras muestras;
    private AlmacenDeArchivos almacen;
    private ApplicationEventPublisher eventos;
    private SubirFotos subir;

    @BeforeEach
    void preparar() {
        lotes = mock(RepositorioDeLotes.class);
        muestras = mock(RepositorioDeMuestras.class);
        almacen = mock(AlmacenDeArchivos.class);
        eventos = mock(ApplicationEventPublisher.class);
        subir = new SubirFotos(lotes, muestras, almacen, new VistasDeFotos(almacen), eventos);
        when(almacen.enlaceDeVista(anyString(), any(Duration.class))).thenReturn(VISTA);
    }

    @Test
    @DisplayName("RF-05 · C5 · al reemplazar una foto, la anterior se borra de la base antes de guardar la nueva, y su archivo no lo borra el caso de uso: lo deja a un evento, que espera a que se confirme (ver ArchivosReemplazadosIT)")
    void reemplazarBorraDespues() {
        MuestraEnCurso muestra = new MuestraEnCurso(MUESTRA, 1, EstadoDeMuestra.PENDIENTE);
        FotoGuardada anterior = new FotoGuardada(UUID.randomUUID(), 2, ANTERIOR, FormatoDeFoto.JPG, 1_000,
                EstadoDeFoto.CARGADA);
        FotoGuardada nueva = new FotoGuardada(UUID.randomUUID(), 2, NUEVA, FormatoDeFoto.PNG, PNG.length,
                EstadoDeFoto.CARGADA);
        when(lotes.estadoParaActualizar(EMPRESA, LOTE)).thenReturn(Optional.of(EstadoDeLote.PENDIENTE));
        when(muestras.muestraEnCurso(EMPRESA, LOTE)).thenReturn(Optional.of(muestra));
        when(muestras.foto(EMPRESA, MUESTRA, 2)).thenReturn(Optional.of(anterior));
        when(almacen.describir(NUEVA)).thenReturn(Optional.of(new ArchivoGuardado(PNG.length, PNG)));
        when(muestras.guardarFoto(EMPRESA, MUESTRA, 2, NUEVA, FormatoDeFoto.PNG, PNG.length)).thenReturn(nueva);

        VistaDeFoto vista = subir.confirmar(EMPRESA, USUARIO, LOTE, 2, NUEVA);

        assertThat(vista.numero()).isEqualTo(2);
        assertThat(vista.formato()).isEqualTo(FormatoDeFoto.PNG);
        InOrder orden = inOrder(muestras, eventos);
        orden.verify(muestras).borrarFoto(EMPRESA, anterior.id());
        orden.verify(eventos).publishEvent(new ArchivoReemplazado(ANTERIOR));
        orden.verify(muestras).guardarFoto(EMPRESA, MUESTRA, 2, NUEVA, FormatoDeFoto.PNG, PNG.length);
        verify(almacen, never()).borrar(anyString());
    }

    @Test
    @DisplayName("RF-05 · la primera foto confirmada crea la muestra 1 con quien la subió")
    void laPrimeraCreaLaMuestra() {
        MuestraEnCurso muestra = new MuestraEnCurso(MUESTRA, 1, EstadoDeMuestra.PENDIENTE);
        when(lotes.estadoParaActualizar(EMPRESA, LOTE)).thenReturn(Optional.of(EstadoDeLote.PENDIENTE));
        when(muestras.muestraEnCurso(EMPRESA, LOTE)).thenReturn(Optional.empty());
        when(almacen.describir(NUEVA)).thenReturn(Optional.of(new ArchivoGuardado(PNG.length, PNG)));
        when(muestras.crear(EMPRESA, LOTE, 1, USUARIO)).thenReturn(muestra);
        when(muestras.foto(EMPRESA, MUESTRA, 1)).thenReturn(Optional.empty());
        when(muestras.guardarFoto(EMPRESA, MUESTRA, 1, NUEVA, FormatoDeFoto.PNG, PNG.length)).thenReturn(
                new FotoGuardada(UUID.randomUUID(), 1, NUEVA, FormatoDeFoto.PNG, PNG.length, EstadoDeFoto.CARGADA));

        subir.confirmar(EMPRESA, USUARIO, LOTE, 1, NUEVA);

        verify(muestras).crear(EMPRESA, LOTE, 1, USUARIO);
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("RF-12 · C8 · un lote analizado sin sugerencia de otra muestra no recibe fotos: 409 con el motivo, sin pedir ningún enlace")
    void soloLotesPendientes() {
        when(lotes.estado(EMPRESA, LOTE)).thenReturn(Optional.of(EstadoDeLote.ANALIZADO));
        when(lotes.sugiereOtraMuestra(EMPRESA, LOTE)).thenReturn(false);
        when(muestras.muestraEnCurso(EMPRESA, LOTE))
                .thenReturn(Optional.of(new MuestraEnCurso(MUESTRA, 1, EstadoDeMuestra.TERMINADA)));

        assertThatThrownBy(() -> subir.pedirSubida(EMPRESA, LOTE, 1, "image/jpeg", 1_000))
                .isInstanceOf(ConflictoExcepcion.class).hasMessage("analisis.sin_otra_muestra");
        verify(almacen, never()).enlaceDeSubida(anyString(), anyString(), anyLong(), any(Duration.class));
    }

    @Test
    @DisplayName("RF-12 · C8 · un lote que ya tiene 2 muestras no recibe fotos para otra: 409 con el motivo, aunque la sugerencia siguiera guardada")
    void conDosMuestrasNoRecibeFotos() {
        when(lotes.estado(EMPRESA, LOTE)).thenReturn(Optional.of(EstadoDeLote.ANALIZADO));
        when(lotes.estadoParaActualizar(EMPRESA, LOTE)).thenReturn(Optional.of(EstadoDeLote.ANALIZADO));
        when(muestras.muestraEnCurso(EMPRESA, LOTE))
                .thenReturn(Optional.of(new MuestraEnCurso(MUESTRA_2, 2, EstadoDeMuestra.TERMINADA)));

        for (boolean sugerencia : new boolean[] {false, true}) {
            when(lotes.sugiereOtraMuestra(EMPRESA, LOTE)).thenReturn(sugerencia);
            assertThatThrownBy(() -> subir.pedirSubida(EMPRESA, LOTE, 1, "image/jpeg", 1_000))
                    .as("pedir, sugerencia %s", sugerencia)
                    .isInstanceOf(ConflictoExcepcion.class).hasMessage("analisis.sin_otra_muestra");
            assertThatThrownBy(() -> subir.confirmar(EMPRESA, USUARIO, LOTE, 1, NUEVA))
                    .as("confirmar, sugerencia %s", sugerencia)
                    .isInstanceOf(ConflictoExcepcion.class).hasMessage("analisis.sin_otra_muestra");
        }
        verify(almacen, never()).enlaceDeSubida(anyString(), anyString(), anyLong(), any(Duration.class));
        verify(almacen, never()).describir(anyString());
        verify(muestras, never()).crear(any(), any(), anyInt(), any());
        verify(muestras, never()).guardarFoto(any(), any(), anyInt(), anyString(), any(), anyLong());
    }

    @Test
    @DisplayName("RF-14 · C6 · con otra muestra sugerida y la muestra 1 terminada, el lote analizado da el enlace para subir la primera foto de la muestra 2")
    void conSugerenciaDaElEnlace() {
        EnlaceTemporal subida = new EnlaceTemporal("/api/v1/archivos/y", "PUT", Map.of("Content-Type", "image/jpeg"),
                Instant.parse("2026-11-24T15:10:00Z"));
        when(lotes.estado(EMPRESA, LOTE)).thenReturn(Optional.of(EstadoDeLote.ANALIZADO));
        when(lotes.sugiereOtraMuestra(EMPRESA, LOTE)).thenReturn(true);
        when(muestras.muestraEnCurso(EMPRESA, LOTE))
                .thenReturn(Optional.of(new MuestraEnCurso(MUESTRA, 1, EstadoDeMuestra.TERMINADA)));
        when(almacen.enlaceDeSubida(anyString(), anyString(), anyLong(), any(Duration.class))).thenReturn(subida);

        SubidaDeFoto pedida = subir.pedirSubida(EMPRESA, LOTE, 1, "image/jpeg", 1_000);

        assertThat(pedida.enlace()).isEqualTo(subida);
        assertThat(pedida.llave()).startsWith(EMPRESA + "/" + LOTE + "/").endsWith(".jpg");
        verify(muestras, never()).crear(any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("RF-12 · la primera foto de la muestra 2 crea la muestra 2 con quien la subió y no borra ni reemplaza la foto con ese número de la muestra 1 (D-25)")
    void laPrimeraFotoDeLaMuestraDos() {
        FotoGuardada deLaMuestraUno = new FotoGuardada(UUID.randomUUID(), 1, ANTERIOR, FormatoDeFoto.JPG, 1_000,
                EstadoDeFoto.APROBADA);
        when(lotes.estadoParaActualizar(EMPRESA, LOTE)).thenReturn(Optional.of(EstadoDeLote.ANALIZADO));
        when(lotes.sugiereOtraMuestra(EMPRESA, LOTE)).thenReturn(true);
        when(muestras.muestraEnCurso(EMPRESA, LOTE))
                .thenReturn(Optional.of(new MuestraEnCurso(MUESTRA, 1, EstadoDeMuestra.TERMINADA)));
        when(muestras.foto(EMPRESA, MUESTRA, 1)).thenReturn(Optional.of(deLaMuestraUno));
        when(almacen.describir(NUEVA)).thenReturn(Optional.of(new ArchivoGuardado(PNG.length, PNG)));
        when(muestras.crear(EMPRESA, LOTE, 2, USUARIO))
                .thenReturn(new MuestraEnCurso(MUESTRA_2, 2, EstadoDeMuestra.PENDIENTE));
        when(muestras.foto(EMPRESA, MUESTRA_2, 1)).thenReturn(Optional.empty());
        when(muestras.guardarFoto(EMPRESA, MUESTRA_2, 1, NUEVA, FormatoDeFoto.PNG, PNG.length)).thenReturn(
                new FotoGuardada(UUID.randomUUID(), 1, NUEVA, FormatoDeFoto.PNG, PNG.length, EstadoDeFoto.CARGADA));

        VistaDeFoto vista = subir.confirmar(EMPRESA, USUARIO, LOTE, 1, NUEVA);

        assertThat(vista.numero()).isEqualTo(1);
        verify(muestras).crear(EMPRESA, LOTE, 2, USUARIO);
        verify(muestras).guardarFoto(EMPRESA, MUESTRA_2, 1, NUEVA, FormatoDeFoto.PNG, PNG.length);
        verify(muestras, never()).crear(EMPRESA, LOTE, 1, USUARIO);
        verify(muestras, never()).borrarFoto(any(), any());
        verify(muestras, never()).guardarFoto(any(), eq(MUESTRA), anyInt(), anyString(), any(), anyLong());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("RF-14 · C6 · con la muestra 2 pendiente, una foto nueva va a la muestra 2, sin crear otra muestra")
    void conLaMuestraDosPendiente() {
        when(lotes.estadoParaActualizar(EMPRESA, LOTE)).thenReturn(Optional.of(EstadoDeLote.ANALIZADO));
        when(lotes.sugiereOtraMuestra(EMPRESA, LOTE)).thenReturn(true);
        when(muestras.muestraEnCurso(EMPRESA, LOTE))
                .thenReturn(Optional.of(new MuestraEnCurso(MUESTRA_2, 2, EstadoDeMuestra.PENDIENTE)));
        when(almacen.describir(NUEVA)).thenReturn(Optional.of(new ArchivoGuardado(PNG.length, PNG)));
        when(muestras.foto(EMPRESA, MUESTRA_2, 2)).thenReturn(Optional.empty());
        when(muestras.guardarFoto(EMPRESA, MUESTRA_2, 2, NUEVA, FormatoDeFoto.PNG, PNG.length)).thenReturn(
                new FotoGuardada(UUID.randomUUID(), 2, NUEVA, FormatoDeFoto.PNG, PNG.length, EstadoDeFoto.CARGADA));

        subir.confirmar(EMPRESA, USUARIO, LOTE, 2, NUEVA);

        verify(muestras).guardarFoto(EMPRESA, MUESTRA_2, 2, NUEVA, FormatoDeFoto.PNG, PNG.length);
        verify(muestras, never()).crear(any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("RF-12 · la muestra 2 enviada, en espera o en proceso, ya no recibe fotos: 409 porque ya no está pendiente")
    void laMuestraDosEnviada() {
        when(lotes.estado(EMPRESA, LOTE)).thenReturn(Optional.of(EstadoDeLote.ANALIZADO));
        when(lotes.sugiereOtraMuestra(EMPRESA, LOTE)).thenReturn(true);

        for (EstadoDeMuestra estado : List.of(EstadoDeMuestra.EN_ESPERA, EstadoDeMuestra.EN_PROCESO)) {
            when(muestras.muestraEnCurso(EMPRESA, LOTE))
                    .thenReturn(Optional.of(new MuestraEnCurso(MUESTRA_2, 2, estado)));
            assertThatThrownBy(() -> subir.pedirSubida(EMPRESA, LOTE, 3, "image/jpeg", 1_000)).as(estado.name())
                    .isInstanceOf(ConflictoExcepcion.class).hasMessage("foto.lote_no_pendiente");
        }
        verify(almacen, never()).enlaceDeSubida(anyString(), anyString(), anyLong(), any(Duration.class));
    }

    @Test
    @DisplayName("RF-05 · una muestra que ya se envió no recibe fotos, aunque el lote siga pendiente")
    void soloMuestrasPendientes() {
        when(lotes.estado(EMPRESA, LOTE)).thenReturn(Optional.of(EstadoDeLote.PENDIENTE));
        when(muestras.muestraEnCurso(EMPRESA, LOTE))
                .thenReturn(Optional.of(new MuestraEnCurso(MUESTRA, 1, EstadoDeMuestra.EN_ESPERA)));

        assertThatThrownBy(() -> subir.pedirSubida(EMPRESA, LOTE, 1, "image/jpeg", 1_000))
                .isInstanceOf(ConflictoExcepcion.class);
    }

    @Test
    @DisplayName("RF-05 · C3 · al confirmar, un archivo que no empieza como su formato, uno de más de 10 MB y uno vacío se rechazan en la llave, sin guardar la foto ni crear la muestra")
    void alConfirmarRevisaElArchivo() {
        when(lotes.estadoParaActualizar(EMPRESA, LOTE)).thenReturn(Optional.of(EstadoDeLote.PENDIENTE));
        Map<ArchivoGuardado, String> casos = Map.of(
                new ArchivoGuardado(8, "%PDF-1.7".getBytes(StandardCharsets.US_ASCII)), "foto.formato.no_permitido",
                new ArchivoGuardado(10_485_761, PNG), "foto.tamano_bytes.demasiado_grande",
                new ArchivoGuardado(0, new byte[0]), "foto.no_llego");

        casos.forEach((archivo, clave) -> {
            when(almacen.describir(NUEVA)).thenReturn(Optional.of(archivo));
            assertThatThrownBy(() -> subir.confirmar(EMPRESA, USUARIO, LOTE, 1, NUEVA)).as(clave)
                    .isInstanceOfSatisfying(ReglaIncumplidaExcepcion.class, excepcion -> assertThat(excepcion.getCampos())
                            .containsExactly(new CampoInvalido("llave", clave)));
        });
        verify(muestras, never()).crear(any(), any(), anyInt(), any());
        verify(muestras, never()).guardarFoto(any(), any(), anyInt(), anyString(), any(), anyLong());
    }

    @Test
    @DisplayName("RF-12 · C8 · al confirmar, un lote analizado sin sugerencia de otra muestra responde 409 sin mirar el almacén ni guardar nada")
    void alConfirmarSoloLotesPendientes() {
        when(lotes.estadoParaActualizar(EMPRESA, LOTE)).thenReturn(Optional.of(EstadoDeLote.ANALIZADO));
        when(lotes.sugiereOtraMuestra(EMPRESA, LOTE)).thenReturn(false);
        when(muestras.muestraEnCurso(EMPRESA, LOTE))
                .thenReturn(Optional.of(new MuestraEnCurso(MUESTRA, 1, EstadoDeMuestra.TERMINADA)));

        assertThatThrownBy(() -> subir.confirmar(EMPRESA, USUARIO, LOTE, 1, NUEVA))
                .isInstanceOf(ConflictoExcepcion.class).hasMessage("analisis.sin_otra_muestra");
        verify(almacen, never()).describir(anyString());
        verify(muestras, never()).guardarFoto(any(), any(), anyInt(), anyString(), any(), anyLong());
    }

    @Test
    @DisplayName("RF-05 · C2 · el caso de uso también limita el número de la foto de 1 a 3: el 0 y el 4 se rechazan sin pedir enlaces ni guardar nada (prueba de límite)")
    void numeroDeUnoATres() {
        for (int numero : new int[] {0, 4}) {
            assertThatThrownBy(() -> subir.pedirSubida(EMPRESA, LOTE, numero, "image/jpeg", 1_000)).as("pedir %d", numero)
                    .isInstanceOfSatisfying(ReglaIncumplidaExcepcion.class, excepcion -> assertThat(excepcion.getCampos())
                            .containsExactly(new CampoInvalido("numero", "error.valor_invalido")));
            assertThatThrownBy(() -> subir.confirmar(EMPRESA, USUARIO, LOTE, numero, NUEVA)).as("confirmar %d", numero)
                    .isInstanceOfSatisfying(ReglaIncumplidaExcepcion.class, excepcion -> assertThat(excepcion.getCampos())
                            .containsExactly(new CampoInvalido("numero", "error.valor_invalido")));
        }
        verify(lotes, never()).estado(any(), any());
        verify(lotes, never()).estadoParaActualizar(any(), any());
        verify(almacen, never()).enlaceDeSubida(anyString(), anyString(), anyLong(), any(Duration.class));
        verify(muestras, never()).guardarFoto(any(), any(), anyInt(), anyString(), any(), anyLong());
    }

    @Test
    @DisplayName("RF-05 · una llave de este lote sin la forma que arma el backend, como 36 guiones o 36 ceros en lugar del UUID, se rechaza en la llave sin mirar el almacén")
    void llaveSinLaFormaDelBackend() {
        when(lotes.estadoParaActualizar(EMPRESA, LOTE)).thenReturn(Optional.of(EstadoDeLote.PENDIENTE));
        String prefijo = EMPRESA + "/" + LOTE + "/";

        for (String llave : List.of(prefijo + "-".repeat(36) + ".jpg", prefijo + "0".repeat(36) + ".jpg",
                prefijo + "0199F0A0-0000-7000-8000-000000000007.png")) {
            assertThatThrownBy(() -> subir.confirmar(EMPRESA, USUARIO, LOTE, 1, llave)).as(llave)
                    .isInstanceOfSatisfying(ReglaIncumplidaExcepcion.class, excepcion -> assertThat(excepcion.getCampos())
                            .containsExactly(new CampoInvalido("llave", "error.valor_invalido")));
        }
        verify(almacen, never()).describir(anyString());
    }
}
