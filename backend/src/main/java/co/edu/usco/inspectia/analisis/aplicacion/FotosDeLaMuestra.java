/*
 * Nombre: FotosDeLaMuestra.java
 * Descripción: Las fotos de la muestra en curso de un lote, para retomarlo, también las de la muestra 2, con lo que
 *              dejó la cola si la devolvió (RF-05, RF-09, RF-14 C5 y C6, D-25, D-44).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeMuestra;
import co.edu.usco.inspectia.analisis.dominio.MotivoDeDevolucion;
import java.math.BigDecimal;
import java.util.List;

/**
 * Las fotos de la muestra en curso de un lote (RF-05, RF-14 C5).
 *
 * @param muestra el número de la muestra, o {@code null} si todavía no tiene fotos, también la muestra 2 de un lote
 *     que sugiere otra muestra (D-25)
 * @param estado el estado de la muestra, o {@code null} si todavía no tiene fotos
 * @param fotos las fotos, por número
 * @param motivoDevolucion por qué la cola la devolvió sin resultado, o {@code null} (D-44)
 * @param repeticiones las fotos que RF-09 pidió repetir en esta muestra; 0 sin muestra
 * @param porcentajeMaxDudosos la copia del máximo de dudosos que usó RF-09, o {@code null} si no la evaluó (P-15)
 */
public record FotosDeLaMuestra(Integer muestra, EstadoDeMuestra estado, List<VistaDeFoto> fotos,
        MotivoDeDevolucion motivoDevolucion, int repeticiones, BigDecimal porcentajeMaxDudosos) {

    /**
     * Las fotos de una muestra que la cola no ha devuelto.
     *
     * @param muestra el número de la muestra, o {@code null}
     * @param estado su estado, o {@code null}
     * @param fotos las fotos, por número
     */
    public FotosDeLaMuestra(Integer muestra, EstadoDeMuestra estado, List<VistaDeFoto> fotos) {
        this(muestra, estado, fotos, null, 0, null);
    }
}
