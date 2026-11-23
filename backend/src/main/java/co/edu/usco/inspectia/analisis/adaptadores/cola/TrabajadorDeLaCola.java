/*
 * Nombre: TrabajadorDeLaCola.java
 * Descripción: El hilo que atiende la cola de análisis, una muestra a la vez: al arrancar devuelve a la cola las que
 *              quedaron en proceso, se despierta al enviar una muestra y revisa la tabla cada espera-maxima (RNF-02 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.cola;

import co.edu.usco.inspectia.analisis.adaptadores.configuracion.PropiedadesDelAnalisis;
import co.edu.usco.inspectia.analisis.aplicacion.AtenderCola;
import co.edu.usco.inspectia.analisis.aplicacion.ColaDeAnalisis;
import co.edu.usco.inspectia.analisis.aplicacion.MuestraEnviada;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * El trabajador de la cola de análisis (RNF-02 C3): un solo hilo, así las muestras se atienden de a una y en el orden
 * en que se enviaron. Todo corre en una sola instancia del servidor (D-16).
 *
 * <ul>
 *   <li><b>Al arrancar</b>, devuelve a la cola las muestras que quedaron «en proceso», como cuando el servidor se apagó
 *       en medio de un análisis, y atiende las que esperan.</li>
 *   <li><b>Al enviar una muestra</b>, el evento {@link MuestraEnviada} lo despierta cuando la transacción se confirma.
 *       Si ese aviso se pierde, igual revisa la tabla cada {@code inspectia.analisis.cola.espera-maxima}.</li>
 *   <li><b>Al apagar</b>, deja de tomar muestras y espera unos segundos la que está atendiendo; si no termina, queda «en
 *       proceso» y vuelve a la cola al arrancar.</li>
 * </ul>
 *
 * <p>Con {@code inspectia.analisis.cola.automatica=false} no arranca: las pruebas de integración atienden la cola a
 * mano con {@link AtenderCola#atenderUna()}, así no corre un hilo mientras otra prueba borra los datos.
 */
@Slf4j
@Component
public class TrabajadorDeLaCola implements SmartLifecycle {

    /** Cuánto espera, al apagar, la muestra que está atendiendo. */
    static final Duration ESPERA_AL_APAGAR = Duration.ofSeconds(10);

    private final AtenderCola atender;
    private final ColaDeAnalisis cola;
    private final boolean automatica;
    private final Duration esperaMaxima;
    private final Semaphore aviso = new Semaphore(0);
    private volatile boolean encendido;
    private Thread hilo;

    /**
     * Crea el trabajador; Spring le pasa sus dependencias.
     *
     * @param atender el caso de uso que atiende una muestra
     * @param cola la cola, para devolver las que quedaron en proceso
     * @param propiedades si la cola es automática y cada cuánto revisa la tabla
     */
    public TrabajadorDeLaCola(AtenderCola atender, ColaDeAnalisis cola, PropiedadesDelAnalisis propiedades) {
        this.atender = atender;
        this.cola = cola;
        this.automatica = propiedades.cola().automatica();
        this.esperaMaxima = propiedades.cola().esperaMaxima();
    }

    /** Devuelve a la cola las muestras que quedaron en proceso y arranca el hilo, si la cola es automática. */
    @Override
    public synchronized void start() {
        if (!automatica) {
            log.info("La cola de análisis no es automática (inspectia.analisis.cola.automatica=false)");
            return;
        }
        if (encendido) {
            return;
        }
        int devueltas = cola.devolverLasInterrumpidas();
        if (devueltas > 0) {
            log.warn("{} muestras quedaron en proceso al apagar el servidor: vuelven a la cola", devueltas);
        }
        encendido = true;
        hilo = Thread.ofPlatform().name("cola-de-analisis").daemon(true).start(this::trabajar);
    }

    /** Deja de tomar muestras y espera unos segundos la que está atendiendo. */
    @Override
    public void stop() {
        Thread actual;
        synchronized (this) {
            if (!encendido) {
                return;
            }
            encendido = false;
            actual = hilo;
        }
        aviso.release();
        try {
            actual.join(ESPERA_AL_APAGAR);
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
        }
        if (actual.isAlive()) {
            log.warn("El análisis en curso no terminó al apagar: su muestra vuelve a la cola al arrancar");
        }
    }

    @Override
    public boolean isRunning() {
        return encendido;
    }

    /**
     * Despierta al hilo cuando se confirma el envío de una muestra.
     *
     * @param evento la muestra enviada
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alEnviar(MuestraEnviada evento) {
        despertar();
    }

    /** Despierta al hilo para que revise la cola en seguida. */
    public void despertar() {
        aviso.release();
    }

    /** El bucle del hilo: atiende hasta vaciar la cola y espera un aviso o la espera máxima. */
    void trabajar() {
        while (encendido) {
            try {
                while (encendido && atender.atenderUna()) {
                    // Atiende la siguiente sin esperar.
                }
            } catch (RuntimeException excepcion) {
                log.error("La cola de análisis falló; vuelve a revisarla en {}", esperaMaxima, excepcion);
            }
            esperar();
        }
    }

    private void esperar() {
        try {
            if (aviso.tryAcquire(esperaMaxima.toMillis(), TimeUnit.MILLISECONDS)) {
                aviso.drainPermits();
            }
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
            encendido = false;
        }
    }
}
