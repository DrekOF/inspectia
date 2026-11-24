/*
 * Nombre: ConfirmarFotoTest.java
 * Descripción: Pruebas unitarias de confirmar una foto con su revisión rápida, con Mockito: la foto se confirma
 *              cargada, se revisa fuera de la transacción y queda aprobada o por repetir; sin respuesta del servicio o
 *              con el clasificador falso, queda cargada (RF-06, RF-07, D-46).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.FormatoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.MotivoDeFoto;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/** Confirmar una foto y revisarla (D-46), sin Spring, sin la base y sin el servicio de IA. */
class ConfirmarFotoTest {

    private static final UUID EMPRESA = UUID.fromString("0199f0a0-0000-7000-8000-000000000001");
    private static final UUID USUARIO = UUID.fromString("0199f0a0-0000-7000-8000-000000000003");
    private static final UUID LOTE = UUID.fromString("0199f0a0-0000-7000-8000-000000000002");
    private static final String LLAVE = EMPRESA + "/" + LOTE + "/m1-0199f0a0-0000-7000-8000-000000000009.jpg";
    private static final byte[] CONTENIDO = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final EnlaceTemporal ENLACE = new EnlaceTemporal("/api/v1/archivos/x", "GET", java.util.Map.of(),
            Instant.parse("2026-11-24T15:15:00Z"));
    private static final VistaDeFoto CARGADA = new VistaDeFoto(2, FormatoDeFoto.JPG, 3, EstadoDeFoto.CARGADA, ENLACE);

    private SubirFotos subir;
    private RevisorDeFotos revisor;
    private AlmacenDeArchivos almacen;
    private GuardarRevisionDeFoto revisiones;
    private ConfirmarFoto confirmar;

    @BeforeEach
    void preparar() {
        subir = mock(SubirFotos.class);
        revisor = mock(RevisorDeFotos.class);
        almacen = mock(AlmacenDeArchivos.class);
        revisiones = mock(GuardarRevisionDeFoto.class);
        confirmar = new ConfirmarFoto(subir, revisor, almacen, revisiones);
        when(subir.confirmar(EMPRESA, USUARIO, LOTE, 2, LLAVE)).thenReturn(CARGADA);
        when(revisor.revisa()).thenReturn(true);
        when(almacen.leer(LLAVE)).thenReturn(CONTENIDO);
    }

    private static VistaDeFoto revisada(RevisionDeFoto revision) {
        return new VistaDeFoto(2, FormatoDeFoto.JPG, 3, revision.estado(), ENLACE, revision.motivo(),
                revision.granosEncontrados(), revision.regionesPegadas(), List.of());
    }

    @Test
    @DisplayName("RF-06 · C3 · al confirmar, la foto se guarda cargada, se revisa con su contenido después de esa transacción y responde aprobada con sus granos")
    void revisaDespuesDeConfirmar() {
        RevisionDeFoto aprobada = RevisionDeFoto.aprobada(52);
        when(revisor.revisar(any())).thenReturn(aprobada);
        when(revisiones.guardar(EMPRESA, LOTE, 2, LLAVE, aprobada)).thenReturn(Optional.of(revisada(aprobada)));

        VistaDeFoto vista = confirmar.confirmar(EMPRESA, USUARIO, LOTE, 2, LLAVE);

        assertThat(vista.estado()).isEqualTo(EstadoDeFoto.APROBADA);
        assertThat(vista.granosEncontrados()).isEqualTo(52);
        ArgumentCaptor<FotoParaClasificar> foto = ArgumentCaptor.forClass(FotoParaClasificar.class);
        InOrder orden = inOrder(subir, almacen, revisor, revisiones);
        orden.verify(subir).confirmar(EMPRESA, USUARIO, LOTE, 2, LLAVE);
        orden.verify(almacen).leer(LLAVE);
        orden.verify(revisor).revisar(foto.capture());
        orden.verify(revisiones).guardar(EMPRESA, LOTE, 2, LLAVE, aprobada);
        assertThat(foto.getValue().numero()).isEqualTo(2);
        assertThat(foto.getValue().formato()).isEqualTo(FormatoDeFoto.JPG);
        assertThat(foto.getValue().contenido()).isEqualTo(CONTENIDO);
    }

    @Test
    @DisplayName("RF-07 · C2 · una foto con granos pegados responde por repetir, con sus regiones")
    void granosPegados() {
        RevisionDeFoto pegados = new RevisionDeFoto(EstadoDeFoto.POR_REPETIR, MotivoDeFoto.GRANOS_PEGADOS, 47,
                List.of(new Rectangulo(1210, 860, 240, 130)));
        when(revisor.revisar(any())).thenReturn(pegados);
        when(revisiones.guardar(EMPRESA, LOTE, 2, LLAVE, pegados)).thenReturn(Optional.of(revisada(pegados)));

        VistaDeFoto vista = confirmar.confirmar(EMPRESA, USUARIO, LOTE, 2, LLAVE);

        assertThat(vista.motivo()).isEqualTo(MotivoDeFoto.GRANOS_PEGADOS);
        assertThat(vista.regionesPegadas()).containsExactly(new Rectangulo(1210, 860, 240, 130));
    }

    @Test
    @DisplayName("D-46 · si el servicio de IA no responde, la foto queda «Cargada» y no se guarda ninguna revisión")
    void sinRespuestaQuedaCargada() {
        when(revisor.revisar(any())).thenThrow(new SinRespuestaDeLaIaExcepcion("tiempo agotado", null));

        assertThat(confirmar.confirmar(EMPRESA, USUARIO, LOTE, 2, LLAVE)).isEqualTo(CARGADA);
        verify(revisiones, never()).guardar(any(), any(), any(Integer.class), any(), any());
    }

    @Test
    @DisplayName("D-46 · si no se puede leer la foto o el servicio la rechaza, también queda «Cargada» y la revisa la cola")
    void otraFallaQuedaCargada() {
        when(almacen.leer(LLAVE)).thenThrow(new IllegalStateException("no está"));
        assertThat(confirmar.confirmar(EMPRESA, USUARIO, LOTE, 2, LLAVE)).isEqualTo(CARGADA);

        doReturn(CONTENIDO).when(almacen).leer(LLAVE);
        when(revisor.revisar(any())).thenThrow(new IllegalStateException("400"));
        assertThat(confirmar.confirmar(EMPRESA, USUARIO, LOTE, 2, LLAVE)).isEqualTo(CARGADA);
        verify(revisiones, never()).guardar(any(), any(), any(Integer.class), any(), any());
    }

    @Test
    @DisplayName("D-46 · si mientras se revisaba la foto la reemplazaron o enviaron la muestra, responde la foto como se confirmó")
    void laFotoYaNoEsLaMisma() {
        when(revisor.revisar(any())).thenReturn(RevisionDeFoto.aprobada(50));
        when(revisiones.guardar(any(), any(), any(Integer.class), any(), any())).thenReturn(Optional.empty());

        assertThat(confirmar.confirmar(EMPRESA, USUARIO, LOTE, 2, LLAVE)).isEqualTo(CARGADA);
    }

    @Test
    @DisplayName("D-46 · con el clasificador falso no hay revisión: la foto queda «Cargada», como antes, y ni se lee del almacén")
    void sinRevisor() {
        when(revisor.revisa()).thenReturn(false);

        assertThat(confirmar.confirmar(EMPRESA, USUARIO, LOTE, 2, LLAVE)).isEqualTo(CARGADA);
        verifyNoInteractions(almacen, revisiones);
        verify(revisor, never()).revisar(any());
    }

    @Test
    @DisplayName("RF-05 · confirmar otra vez la misma llave responde la foto ya revisada, sin pedir otra revisión")
    void mismaLlaveYaRevisada() {
        VistaDeFoto aprobada = revisada(RevisionDeFoto.aprobada(50));
        when(subir.confirmar(EMPRESA, USUARIO, LOTE, 2, LLAVE)).thenReturn(new VistaDeFoto(2, FormatoDeFoto.JPG, 3,
                EstadoDeFoto.APROBADA, ENLACE));
        when(revisiones.vista(EMPRESA, LOTE, 2, LLAVE)).thenReturn(Optional.of(aprobada));

        assertThat(confirmar.confirmar(EMPRESA, USUARIO, LOTE, 2, LLAVE)).isEqualTo(aprobada);
        verifyNoInteractions(almacen);
        verify(revisor, never()).revisar(any());
    }
}
