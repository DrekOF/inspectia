/*
 * Nombre: SinRevisionDeFotos.java
 * Descripción: El puerto RevisorDeFotos del clasificador falso: no revisa, y la foto queda «Cargada» como antes de la
 *              revisión rápida (D-46).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.falso;

import co.edu.usco.inspectia.analisis.aplicacion.FotoParaClasificar;
import co.edu.usco.inspectia.analisis.aplicacion.RevisionDeFoto;
import co.edu.usco.inspectia.analisis.aplicacion.RevisorDeFotos;

/**
 * La revisión de las fotos con el clasificador falso ({@code inspectia.ia.clasificador=falso}): no hay revisión rápida
 * (D-46), así que la foto confirmada queda «Cargada», sin conteo ni regiones, como antes. Así las pruebas de punta a
 * punta siguen sin cambios.
 */
public class SinRevisionDeFotos implements RevisorDeFotos {

    @Override
    public boolean revisa() {
        return false;
    }

    /**
     * No se llama: {@link #revisa()} dice que no revisa.
     *
     * @throws UnsupportedOperationException siempre
     */
    @Override
    public RevisionDeFoto revisar(FotoParaClasificar foto) {
        throw new UnsupportedOperationException("El clasificador falso no revisa las fotos");
    }
}
