/*
 * Nombre: FormatoDeFoto.java
 * Descripción: Los formatos de foto que acepta la app, JPG y PNG (RF-05 C3), con su tipo, su extensión y los primeros
 *              bytes con que empieza un archivo de cada uno.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Un formato de foto que acepta la app (RF-05 C3). Se reconoce por sus primeros bytes, no por el nombre del archivo
 * ni por el tipo que dice el navegador: así un archivo que solo dice ser JPG no pasa.
 */
public enum FormatoDeFoto {

    /** JPG: empieza con {@code FF D8 FF}. */
    JPG("image/jpeg", "jpg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),

    /** PNG: empieza con {@code 89 50 4E 47 0D 0A 1A 0A}. */
    PNG("image/png", "png", new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});

    /** Cuántos primeros bytes hacen falta para reconocer cualquier formato. */
    public static final int BYTES_PARA_RECONOCER = 8;

    private final String tipo;
    private final String extension;
    private final byte[] comienzo;

    FormatoDeFoto(String tipo, String extension, byte[] comienzo) {
        this.tipo = tipo;
        this.extension = extension;
        this.comienzo = comienzo;
    }

    /**
     * El formato de un tipo de contenido, como {@code image/jpeg}, sin distinguir mayúsculas y sin sus parámetros.
     *
     * @param tipo el tipo; puede ser {@code null}
     * @return el formato, o vacío si no es JPG ni PNG
     */
    public static Optional<FormatoDeFoto> deTipo(String tipo) {
        if (tipo == null) {
            return Optional.empty();
        }
        String limpio = tipo.split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(formato -> formato.tipo.equals(limpio)).findFirst();
    }

    /**
     * El formato de una extensión de archivo, como {@code jpg}.
     *
     * @param extension la extensión, sin el punto
     * @return el formato, o vacío si no es de JPG ni de PNG
     */
    public static Optional<FormatoDeFoto> deExtension(String extension) {
        return Arrays.stream(values()).filter(formato -> formato.extension.equals(extension)).findFirst();
    }

    /**
     * Si un archivo empieza como uno de este formato.
     *
     * @param primeros los primeros bytes del archivo
     * @return {@code true} si empieza con los bytes del formato
     */
    public boolean empiezaComo(byte[] primeros) {
        return primeros != null && primeros.length >= comienzo.length
                && Arrays.equals(primeros, 0, comienzo.length, comienzo, 0, comienzo.length);
    }

    /** @return el tipo de contenido, como {@code image/jpeg} */
    public String getTipo() {
        return tipo;
    }

    /** @return la extensión de los archivos, sin el punto, como {@code jpg} */
    public String getExtension() {
        return extension;
    }
}
