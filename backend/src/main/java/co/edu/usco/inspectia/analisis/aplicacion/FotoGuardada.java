/*
 * Nombre: FotoGuardada.java
 * Descripción: Una foto guardada de una muestra: su número, su llave, su formato, su tamaño, su estado y lo que dijo su
 *              revisión (RF-05, RF-06, RF-07, D-46).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.FormatoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.MotivoDeFoto;
import java.util.UUID;

/**
 * Una foto de una muestra (RF-05).
 *
 * @param id la foto
 * @param numero su número en la muestra, de 1 a 3
 * @param llave dónde está en el almacén
 * @param formato JPG o PNG
 * @param tamanoBytes cuánto pesa
 * @param estado cargada, aprobada o por repetir
 * @param motivo por qué hay que repetirla, o {@code null} si no está por repetir (RF-06, RF-07, RF-09)
 * @param granosEncontrados los granos válidos que encontró su revisión, o {@code null} mientras está cargada (D-46)
 */
public record FotoGuardada(UUID id, int numero, String llave, FormatoDeFoto formato, long tamanoBytes,
        EstadoDeFoto estado, MotivoDeFoto motivo, Integer granosEncontrados) {

    /**
     * Una foto sin revisión: sin motivo ni granos encontrados.
     *
     * @param id la foto
     * @param numero su número en la muestra
     * @param llave dónde está en el almacén
     * @param formato JPG o PNG
     * @param tamanoBytes cuánto pesa
     * @param estado su estado
     */
    public FotoGuardada(UUID id, int numero, String llave, FormatoDeFoto formato, long tamanoBytes,
            EstadoDeFoto estado) {
        this(id, numero, llave, formato, tamanoBytes, estado, null, null);
    }
}
