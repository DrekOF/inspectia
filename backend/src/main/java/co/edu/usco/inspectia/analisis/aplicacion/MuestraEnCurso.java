/*
 * Nombre: MuestraEnCurso.java
 * Descripción: La muestra de un lote cuyas fotos se están tomando o ya se enviaron, con lo que dejó la cola si la
 *              devolvió: su motivo, sus repeticiones y la copia de los parámetros de RF-09 (RF-05, RF-09, D-25, D-44).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeMuestra;
import co.edu.usco.inspectia.analisis.dominio.MotivoDeDevolucion;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * La muestra en curso de un lote (RF-05, D-25).
 *
 * @param id la muestra
 * @param numero su número: 1, o 2 para la segunda
 * @param estado su estado: solo una pendiente recibe fotos
 * @param motivoDevolucion por qué la cola la devolvió sin resultado, o {@code null} (D-44)
 * @param repeticiones las fotos que RF-09 pidió repetir por granos dudosos, de 0 a 2
 * @param umbralConfianzaBaja la copia del umbral que usó RF-09, o {@code null} si todavía no la evaluó
 * @param porcentajeMaxDudosos la copia del máximo de dudosos que usó RF-09, o {@code null}
 */
public record MuestraEnCurso(UUID id, int numero, EstadoDeMuestra estado, MotivoDeDevolucion motivoDevolucion,
        int repeticiones, BigDecimal umbralConfianzaBaja, BigDecimal porcentajeMaxDudosos) {

    /**
     * Una muestra que la cola no ha devuelto: sin motivo, sin repeticiones y sin la copia de los parámetros.
     *
     * @param id la muestra
     * @param numero su número
     * @param estado su estado
     */
    public MuestraEnCurso(UUID id, int numero, EstadoDeMuestra estado) {
        this(id, numero, estado, null, 0, null, null);
    }
}
