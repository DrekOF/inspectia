/*
 * Nombre: ArchivoGuardado.java
 * Descripción: Lo que el almacén dice de un archivo ya subido: cuánto pesa y sus primeros bytes (RF-05 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

/**
 * Un archivo ya subido al almacén, para revisarlo al confirmar la foto (RF-05 C3).
 *
 * @param bytes cuánto pesa
 * @param primeros sus primeros bytes, para reconocer el formato
 */
public record ArchivoGuardado(long bytes, byte[] primeros) {
}
