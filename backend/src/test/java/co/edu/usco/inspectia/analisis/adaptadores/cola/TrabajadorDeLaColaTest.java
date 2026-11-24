/*
 * Nombre: TrabajadorDeLaColaTest.java
 * Descripción: Pruebas unitarias del trabajador de la cola de análisis, con Mockito: al arrancar devuelve las muestras
 *              en proceso, atiende hasta vaciar la cola, despierta con el aviso de un envío, revisa la tabla cada
 *              espera-maxima, sigue después de un error y no arranca con la cola a mano (RNF-02 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.cola;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.usco.inspectia.analisis.adaptadores.configuracion.PropiedadesDelAnalisis;
import co.edu.usco.inspectia.analisis.aplicacion.AtenderCola;
import co.edu.usco.inspectia.analisis.aplicacion.ColaDeAnalisis;
import co.edu.usco.inspectia.analisis.aplicacion.MuestraEnviada;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** El hilo de la cola de análisis (RNF-02 C3), con un caso de uso falso: sin la base ni Spring. */
class TrabajadorDeLaColaTest {

    /** Más que lo que tarda el hilo en reaccionar a un aviso, y mucho menos que la espera máxima larga. */
    private static final long PACIENCIA_MS = 2_000;

    private AtenderCola atender;
    private ColaDeAnalisis cola;
    private TrabajadorDeLaCola trabajador;

    @BeforeEach
    void preparar() {
        atender = mock(AtenderCola.class);
        cola = mock(ColaDeAnalisis.class);
    }

    @AfterEach
    void apagar() {
        if (trabajador != null) {
            trabajador.stop();
        }
    }

    private static PropiedadesDelAnalisis propiedades(boolean automatica, Duration espera) {
        return new PropiedadesDelAnalisis(new BigDecimal("60"), new BigDecimal("10"), BigDecimal.ONE, BigDecimal.ONE,
                new PropiedadesDelAnalisis.Cola(automatica, espera));
    }

    @Test
    @DisplayName("RNF-02 · C3 · al arrancar, devuelve a la cola las muestras que quedaron en proceso y después atiende hasta vaciarla")
    void alArrancar() {
        when(cola.devolverLasInterrumpidas()).thenReturn(1);
        when(atender.atenderUna()).thenReturn(true, true, false);
        trabajador = new TrabajadorDeLaCola(atender, cola, propiedades(true, Duration.ofMinutes(1)));

        trabajador.start();

        assertThat(trabajador.isRunning()).isTrue();
        verify(atender, timeout(PACIENCIA_MS).times(3)).atenderUna();
        InOrder orden = inOrder(cola, atender);
        orden.verify(cola).devolverLasInterrumpidas();
        orden.verify(atender, times(3)).atenderUna();
    }

    @Test
    @DisplayName("RNF-02 · C3 · el aviso de un envío despierta al trabajador sin esperar la revisión de la tabla")
    void elAvisoLoDespierta() {
        when(atender.atenderUna()).thenReturn(false);
        trabajador = new TrabajadorDeLaCola(atender, cola, propiedades(true, Duration.ofMinutes(1)));
        trabajador.start();
        verify(atender, timeout(PACIENCIA_MS).times(1)).atenderUna();

        trabajador.alEnviar(new MuestraEnviada(UUID.randomUUID()));

        verify(atender, timeout(PACIENCIA_MS).times(2)).atenderUna();
    }

    @Test
    @DisplayName("RNF-02 · C3 · sin aviso, revisa la tabla cada espera-maxima: así un envío cuyo aviso se perdió no se queda esperando")
    void revisaCadaEsperaMaxima() {
        when(atender.atenderUna()).thenReturn(false);
        trabajador = new TrabajadorDeLaCola(atender, cola, propiedades(true, Duration.ofMillis(50)));

        trabajador.start();

        verify(atender, timeout(PACIENCIA_MS).atLeast(4)).atenderUna();
    }

    @Test
    @DisplayName("RNF-02 · si atender la cola falla, el trabajador lo registra y sigue revisándola")
    void sigueDespuesDeUnError() {
        when(atender.atenderUna()).thenThrow(new IllegalStateException("La base no responde")).thenReturn(false);
        trabajador = new TrabajadorDeLaCola(atender, cola, propiedades(true, Duration.ofMillis(50)));

        trabajador.start();

        verify(atender, timeout(PACIENCIA_MS).atLeast(2)).atenderUna();
        assertThat(trabajador.isRunning()).isTrue();
    }

    @Test
    @DisplayName("RNF-02 · con la cola a mano (automatica=false) no arranca el hilo ni toca la cola: las pruebas la atienden una muestra a la vez")
    void colaAMano() throws InterruptedException {
        trabajador = new TrabajadorDeLaCola(atender, cola, propiedades(false, Duration.ofMillis(50)));

        trabajador.start();
        trabajador.despertar();
        Thread.sleep(200);

        assertThat(trabajador.isRunning()).isFalse();
        verify(cola, never()).devolverLasInterrumpidas();
        verify(atender, never()).atenderUna();
    }

    @Test
    @DisplayName("RNF-02 · al apagar deja de tomar muestras; arrancar dos veces no crea otro hilo")
    void alApagar() throws InterruptedException {
        when(atender.atenderUna()).thenReturn(false);
        trabajador = new TrabajadorDeLaCola(atender, cola, propiedades(true, Duration.ofMillis(50)));
        trabajador.start();
        trabajador.start();
        verify(cola, times(1)).devolverLasInterrumpidas();

        trabajador.stop();
        int atendidas = mockingDetails(atender).getInvocations().size();
        Thread.sleep(300);

        assertThat(trabajador.isRunning()).isFalse();
        assertThat(mockingDetails(atender).getInvocations()).hasSize(atendidas);
    }
}
