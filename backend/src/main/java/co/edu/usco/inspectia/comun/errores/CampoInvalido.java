/*
 * Nombre: CampoInvalido.java
 * Descripción: Un dato que no se puede guardar, con la clave de su mensaje en los textos de i18n.
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.errores;

/**
 * Un dato que no se puede guardar. Los servicios lo reportan con la clave del mensaje, y
 * {@link ManejadorDeErrores} lo traduce al idioma de la petición (RP-11).
 *
 * @param campo el campo de la petición, como {@code nit} o {@code administrador.correo};
 *              {@code null} si el error no es de un campo
 * @param clave la clave del mensaje en {@code i18n/generado.properties} o {@code i18n/mensajes.properties}
 */
public record CampoInvalido(String campo, String clave) {
}
