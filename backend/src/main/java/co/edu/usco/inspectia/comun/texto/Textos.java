/*
 * Nombre: Textos.java
 * Descripción: Normalización de los textos que llegan a la API: quitar espacios, pasar a minúsculas y, para
 *              buscar, quitar las tildes.
 * Fecha de creación: 13/10/2026
 * Actualización: 10/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.comun.texto;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normaliza los textos que llegan a la API antes de validarlos y guardarlos. La usan las solicitudes
 * generadas para las validaciones {@code recortar} y {@code minusculas} de {@code specs/entidades}, y las
 * búsquedas que no distinguen tildes, como las sugerencias de finca o proveedor (RF-04).
 */
public final class Textos {

    /** Las marcas que se separan de su letra al descomponer el texto: tildes, diéresis y la virgulilla de la ñ. */
    private static final Pattern MARCAS = Pattern.compile("\\p{M}+");

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

    /**
     * Quita las tildes y las demás marcas de las letras: «Paraíso» queda «Paraiso»; «Peña», «Pena», y
     * «Güiza», «Guiza». Sirve para buscar sin distinguirlas; los textos se guardan como llegan.
     *
     * @param texto el texto, o {@code null}
     * @return el texto sin marcas, o {@code null}
     */
    public static String sinTildes(String texto) {
        return texto == null ? null : MARCAS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
    }

    /**
     * La clave con que se busca un texto sin distinguir mayúsculas, tildes ni los espacios del inicio y del
     * final: «  Finca El PARAÍSO » y «finca el paraiso» tienen la misma.
     *
     * @param texto el texto, o {@code null}
     * @return su clave, o {@code null}
     */
    public static String claveDeBusqueda(String texto) {
        return minusculas(sinTildes(recortar(texto)));
    }
}
