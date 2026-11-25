/*
 * Nombre: RevisorDeFotos.java
 * Descripción: Puerto de la revisión rápida de cada foto al confirmarla: nitidez, luz, granos pegados y cuántos granos
 *              tiene (RF-06, RF-07, D-46).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

/**
 * Puerto de la revisión rápida de una foto (D-46). Lo implementa el cliente del servicio de IA
 * ({@code inspectia.ia.clasificador=servicio}, {@code POST /revisar}) y, con el clasificador falso, uno que no revisa:
 * así la foto queda «Cargada», como antes. Se llama fuera de toda transacción, porque puede tardar hasta 10 s.
 */
public interface RevisorDeFotos {

    /**
     * Si este revisor revisa las fotos. El que no revisa responde {@code false}, así no se lee la foto del almacén.
     *
     * @return {@code true} si {@link #revisar} da una revisión
     */
    default boolean revisa() {
        return true;
    }

    /**
     * Revisa una foto.
     *
     * @param foto la foto, con su número, su formato y su contenido
     * @return la revisión
     * @throws SinRespuestaDeLaIaExcepcion si el servicio no responde a tiempo, no se puede conectar o falla
     * @throws RuntimeException si no la puede revisar por otra razón: la foto queda «Cargada» y la revisa la cola
     */
    RevisionDeFoto revisar(FotoParaClasificar foto);
}
