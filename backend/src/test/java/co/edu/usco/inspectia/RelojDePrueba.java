/*
 * Nombre: RelojDePrueba.java
 * Descripción: Un reloj que las pruebas fijan y atrasan, para revisar sin el reloj real lo que depende de la
 *              hora, como el corte de sesiones cuando el reloj del servidor retrocede (RNF-04 C6). Reemplaza al
 *              bean reloj de la aplicación con @TestBean.
 * Fecha de creación: 26/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

/** Un reloj en UTC que solo cambia cuando la prueba lo fija o lo atrasa: entre una y otra, no avanza. */
public final class RelojDePrueba extends Clock {

    private final AtomicReference<Instant> ahora;

    /**
     * Crea el reloj en una hora.
     *
     * @param inicial la hora en la que empieza
     */
    public RelojDePrueba(Instant inicial) {
        this.ahora = new AtomicReference<>(inicial);
    }

    /**
     * Pone el reloj en una hora.
     *
     * @param hora la hora
     */
    public void fijar(Instant hora) {
        ahora.set(hora);
    }

    /**
     * Atrasa el reloj, como cuando el sistema ajusta la hora hacia atrás.
     *
     * @param tiempo cuánto lo atrasa
     */
    public void atrasar(Duration tiempo) {
        ahora.updateAndGet(hora -> hora.minus(tiempo));
    }

    @Override
    public Instant instant() {
        return ahora.get();
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    /** El reloj de la aplicación es UTC (Clock.systemUTC()): este no se usa en otra zona. */
    @Override
    public Clock withZone(ZoneId zona) {
        if (ZoneOffset.UTC.equals(zona)) {
            return this;
        }
        throw new UnsupportedOperationException("El reloj de prueba solo está en UTC");
    }
}
