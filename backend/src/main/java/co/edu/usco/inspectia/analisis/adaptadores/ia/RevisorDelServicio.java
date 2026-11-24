/*
 * Nombre: RevisorDelServicio.java
 * Descripción: El puerto RevisorDeFotos con el servicio de IA: la revisión rápida de una foto al confirmarla, por HTTP
 *              (RF-06, RF-07, D-46).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.ia;

import co.edu.usco.inspectia.analisis.aplicacion.FotoParaClasificar;
import co.edu.usco.inspectia.analisis.aplicacion.RevisionDeFoto;
import co.edu.usco.inspectia.analisis.aplicacion.RevisorDeFotos;

/**
 * La revisión rápida con el servicio de IA ({@code POST /revisar}, D-46): nitidez, luz, granos pegados y cuántos granos
 * tiene la foto. Espera hasta 10 s.
 */
public class RevisorDelServicio implements RevisorDeFotos {

    private final ClienteDelServicioDeIa cliente;

    /**
     * Crea el revisor.
     *
     * @param cliente el cliente HTTP del servicio
     */
    public RevisorDelServicio(ClienteDelServicioDeIa cliente) {
        this.cliente = cliente;
    }

    @Override
    public RevisionDeFoto revisar(FotoParaClasificar foto) {
        return cliente.revisar(foto);
    }
}
