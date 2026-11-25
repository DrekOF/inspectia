/*
 * Nombre: ContenidoDelReporte.java
 * Descripción: Lo que lleva el reporte PDF de un lote: su empresa, su resultado guardado, lo que contó el modelo y sus
 *              muestras terminadas con sus fotos marcadas (RF-15, D-25, D-39, D-43, D-47).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * El contenido del reporte PDF de un lote analizado (RF-15). Las cifras son las guardadas, como en la página (RF-15
 * C2); con 2 muestras, los granos del lote son la suma de las dos (D-25). Los nombres de la empresa y de quien analizó
 * cada muestra se leen al armar el reporte: el lote no los copia.
 *
 * @param empresa el nombre de la empresa
 * @param nit su NIT
 * @param lote el lote y su resultado guardado
 * @param totalGranos el total de granos de sus muestras terminadas
 * @param granosSanos los que el modelo marcó como sanos
 * @param porcentajeContado el porcentaje que contó el modelo, con 1 decimal
 * @param muestras sus muestras terminadas, por número
 * @param generado cuándo se armó el reporte
 */
public record ContenidoDelReporte(String empresa, String nit, ResultadoGuardado lote, int totalGranos,
        int granosSanos, BigDecimal porcentajeContado, List<MuestraDelReporte> muestras, Instant generado) {

    /**
     * Una muestra terminada del lote.
     *
     * @param muestra sus datos guardados: quién la analizó, su modelo y sus conteos
     * @param porcentajeContado su porcentaje contado, con 1 decimal
     * @param fotos sus fotos, por número, con sus granos
     */
    public record MuestraDelReporte(MuestraGuardada muestra, BigDecimal porcentajeContado,
            List<FotoDelReporte> fotos) {
    }

    /**
     * Una foto de una muestra, con sus granos marcados.
     *
     * @param numero su número en la muestra, de 1 a 3
     * @param contenido los bytes del archivo, JPG o PNG, o {@code null} si no se pudo leer del almacén
     * @param granos sus granos, en píxeles de la foto ya girada según su EXIF (D-45), con su clase y si son dudosos
     */
    public record FotoDelReporte(int numero, byte[] contenido, List<GranoMarcado> granos) {
    }
}
