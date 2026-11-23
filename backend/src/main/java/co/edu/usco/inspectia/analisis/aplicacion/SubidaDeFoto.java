/*
 * Nombre: SubidaDeFoto.java
 * Descripción: El enlace para subir una foto y la llave con que después se confirma (RF-05, D-41).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

/**
 * Una subida de foto pedida (RF-05, D-41): la página sube el archivo con el enlace y después confirma la foto con la
 * llave.
 *
 * @param llave dónde queda el archivo en el almacén
 * @param enlace el enlace para subirlo con {@code PUT}
 */
public record SubidaDeFoto(String llave, EnlaceTemporal enlace) {
}
