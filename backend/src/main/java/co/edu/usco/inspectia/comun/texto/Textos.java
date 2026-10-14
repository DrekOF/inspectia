/*
 * Nombre: Textos.java
 * Descripción: Normalización de los textos que llegan a la API: quitar espacios y pasar a minúsculas.
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.texto;

import java.util.Locale;

/**
 * Normaliza los textos que llegan a la API antes de validarlos y guardarlos. La usan las solicitudes
 * generadas para las validaciones {@code recortar} y {@code minusculas} de {@code specs/entidades}.
 */
public final class Textos {

    private Textos() {
    }

    /**
     * Quita los espacios del inicio y del final.
     *
     * @param texto el texto, o {@code null}
     * @return el texto sin esos espacios, o {@code null}
     */
    public static String recortar(String texto) {
        return texto == null ? null : texto.strip();
    }

    /**
     * Pasa el texto a minúsculas, igual en cualquier idioma del servidor.
     *
     * @param texto el texto, o {@code null}
     * @return el texto en minúsculas, o {@code null}
     */
    public static String minusculas(String texto) {
        return texto == null ? null : texto.toLowerCase(Locale.ROOT);
    }
}
