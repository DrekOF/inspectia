/*
 * Nombre: FotoGuardada.java
 * Descripción: Una foto de una muestra, como está en la base (RF-05).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.FormatoDeFoto;
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
 */
public record FotoGuardada(UUID id, int numero, String llave, FormatoDeFoto formato, long tamanoBytes,
        EstadoDeFoto estado) {
}
