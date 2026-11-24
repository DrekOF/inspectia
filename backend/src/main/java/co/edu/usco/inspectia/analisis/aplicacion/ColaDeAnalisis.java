/*
 * Nombre: ColaDeAnalisis.java
 * Descripción: Puerto de la cola de análisis: las muestras «en espera», que se atienden en el orden en que se enviaron
 *              (RNF-02 C3, D-21).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.time.Instant;
import java.util.Optional;

/**
 * Puerto de la cola de análisis (RNF-02 C3). La cola está en la base: son las muestras {@code EN_ESPERA}, por fecha de
 * envío. Así ninguna se pierde si el servidor se reinicia. Es de toda la plataforma; la empresa de cada muestra sale de
 * su fila (RNF-03).
 */
public interface ColaDeAnalisis {

    /**
     * Toma la muestra que lleva más tiempo en espera y la deja «en proceso», en su propia transacción. Si otra
     * ejecución tiene bloqueada una muestra, la salta.
     *
     * @param ahora la hora de inicio, al microsegundo
     * @return la muestra tomada, o vacío si la cola está vacía
     */
    Optional<MuestraTomada> tomarLaSiguiente(Instant ahora);

    /**
     * Devuelve a la cola las muestras que quedaron «en proceso», como cuando el servidor se apagó en medio de un
     * análisis. Conservan su fecha de envío, así se atienden primero. Supone una sola instancia del servidor (D-16).
     *
     * @return cuántas devolvió
     */
    int devolverLasInterrumpidas();
}
