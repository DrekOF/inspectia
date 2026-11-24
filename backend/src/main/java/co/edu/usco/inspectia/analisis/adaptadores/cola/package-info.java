/*
 * Nombre: package-info.java (analisis.adaptadores.cola)
 * Descripción: El hilo que atiende la cola de análisis (RNF-02 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
/**
 * El trabajador de la cola de análisis (RNF-02 C3): un solo hilo del servidor que atiende las muestras «en espera» en el
 * orden en que se enviaron, con el caso de uso {@code AtenderCola}. La cola está en la base ({@code ColaEnLaBase}), así
 * que ninguna muestra se pierde si el servidor se reinicia.
 */
package co.edu.usco.inspectia.analisis.adaptadores.cola;
