/*
 * Nombre: RegistroDeModelosDelServicio.java
 * Descripción: Registra en la tabla modelo los 5 modelos de la comparación que responde el servicio de IA y deja en uso
 *              el que carga el servicio; si el servicio todavía no responde al arrancar, lo reintenta (RF-08 C3, RF-17,
 *              D-18, D-48).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.ia;

import co.edu.usco.inspectia.analisis.aplicacion.MetricasDelModelo;
import co.edu.usco.inspectia.analisis.aplicacion.RepositorioDeModelos;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * El registro de los modelos del servicio de IA (contrato común del despliegue, sección 1). Al arrancar con
 * {@code inspectia.ia.clasificador=servicio}, pide {@code GET /modelos}, registra los 5 modelos que falten con todas sus
 * métricas ({@code INSERT … ON CONFLICT DO NOTHING}: uno ya registrado no cambia, así las muestras conservan los recall
 * con que se corrigieron, RF-10 C13) y deja en uso solo el que carga el servicio (RF-17).
 *
 * <p>El servicio de IA puede tardar en cargar el modelo más que el servidor en arrancar. Si no responde, el servidor
 * arranca igual: lo reintenta cada {@code inspectia.ia.tiempos.reintento-de-modelos} en un hilo aparte, y el
 * clasificador lo vuelve a intentar antes de cada análisis ({@link #asegurar()}).
 */
@Slf4j
public class RegistroDeModelosDelServicio implements SmartInitializingSingleton, DisposableBean {

    private final ClienteDelServicioDeIa cliente;
    private final RepositorioDeModelos modelos;
    private final Duration reintento;
    private volatile boolean registrados;
    private ScheduledExecutorService reintentos;
    private ScheduledFuture<?> tarea;

    /**
     * Crea el registro.
     *
     * @param cliente el cliente HTTP del servicio
     * @param modelos la tabla {@code modelo}
     * @param reintento cada cuánto lo reintenta si el servicio no responde al arrancar
     */
    public RegistroDeModelosDelServicio(ClienteDelServicioDeIa cliente, RepositorioDeModelos modelos,
            Duration reintento) {
        this.cliente = cliente;
        this.modelos = modelos;
        this.reintento = reintento;
    }

    /** Registra los modelos después de crear todos los beans; si el servicio no responde, programa los reintentos. */
    @Override
    public synchronized void afterSingletonsInstantiated() {
        if (asegurar()) {
            return;
        }
        reintentos = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("registro-de-modelos")
                .daemon(true).factory());
        tarea = reintentos.scheduleWithFixedDelay(() -> {
            if (asegurar()) {
                tarea.cancel(false);
            }
        }, reintento.toMillis(), reintento.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Registra los modelos si todavía no se registraron. No lanza excepciones: si falla, queda en el registro del
     * servidor y se vuelve a intentar.
     *
     * @return {@code true} si ya están registrados
     */
    public boolean asegurar() {
        if (registrados) {
            return true;
        }
        synchronized (this) {
            if (registrados) {
                return true;
            }
            try {
                ClienteDelServicioDeIa.ModelosDelServicio respuesta = cliente.modelos();
                int nuevos = 0;
                for (MetricasDelModelo modelo : respuesta.modelos()) {
                    nuevos += modelos.registrarSiFalta(modelo) ? 1 : 0;
                }
                modelos.ponerEnUso(respuesta.enUso().nombre(), respuesta.enUso().version());
                registrados = true;
                log.info("Modelos del servicio de IA: {} registrados ahora, {} en total; en uso {} {}", nuevos,
                        respuesta.modelos().size(), respuesta.enUso().nombre(), respuesta.enUso().version());
                return true;
            } catch (RuntimeException excepcion) {
                log.warn("No se pudieron registrar los modelos del servicio de IA; se reintenta en {}", reintento,
                        excepcion);
                return false;
            }
        }
    }

    /** Detiene los reintentos al apagar. */
    @Override
    public synchronized void destroy() {
        if (reintentos != null) {
            reintentos.shutdownNow();
        }
    }
}
