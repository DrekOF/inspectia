/*
 * Nombre: ArchivoReemplazado.java
 * Descripción: Evento de una foto reemplazada: su archivo se borra del almacén después de guardar la nueva (RF-05 C5).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

/**
 * Una foto se reemplazó (RF-05 C5). Su archivo se borra del almacén solo cuando la nueva quedó guardada: si la
 * transacción no se confirma, la anterior sigue en su lugar.
 *
 * @param llave el archivo de la foto anterior
 */
public record ArchivoReemplazado(String llave) {
}
