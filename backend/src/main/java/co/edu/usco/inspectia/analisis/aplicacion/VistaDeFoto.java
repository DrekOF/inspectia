/*
 * Nombre: VistaDeFoto.java
 * Descripción: Una foto de la muestra como la ve la página: sus datos y un enlace nuevo para verla (RF-05, RF-14 C5).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.FormatoDeFoto;

/**
 * Una foto de la muestra como la ve la página (RF-05, RF-14 C5). El enlace de vista es nuevo en cada respuesta y
 * vence en minutos (RNF-03 C3).
 *
 * @param numero su número en la muestra, de 1 a 3
 * @param formato JPG o PNG
 * @param tamanoBytes cuánto pesa
 * @param estado cargada, aprobada o por repetir
 * @param vista el enlace para verla
 */
public record VistaDeFoto(int numero, FormatoDeFoto formato, long tamanoBytes, EstadoDeFoto estado,
        EnlaceTemporal vista) {
}
