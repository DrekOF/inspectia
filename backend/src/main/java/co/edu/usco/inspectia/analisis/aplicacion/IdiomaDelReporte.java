/*
 * Nombre: IdiomaDelReporte.java
 * Descripción: Los idiomas del reporte PDF del lote: español e inglés, uno por archivo (RF-15, RP-11, D-50).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.util.Locale;
import java.util.Optional;

/**
 * Un idioma del reporte PDF (RF-15, RP-11). Cada reporte se arma una vez en cada idioma y se guarda en un archivo por
 * idioma, con el código al final del nombre: {@code <base>-es.pdf} y {@code <base>-en.pdf} (D-50).
 */
public enum IdiomaDelReporte {

    /** Español de Colombia, el idioma por defecto. */
    ES("es", Locale.forLanguageTag("es-CO")),

    /** Inglés de Estados Unidos, como la página en inglés. */
    EN("en", Locale.US);

    private final String codigo;
    private final Locale region;

    IdiomaDelReporte(String codigo, Locale region) {
        this.codigo = codigo;
        this.region = region;
    }

    /**
     * El código del idioma, como en el parámetro {@code idioma} y en el nombre de los archivos.
     *
     * @return {@code es} o {@code en}
     */
    public String codigo() {
        return codigo;
    }

    /**
     * La región del idioma, para los textos, los números y las fechas: {@code es-CO} o {@code en-US}, como la página.
     *
     * @return la región
     */
    public Locale region() {
        return region;
    }

    /**
     * El idioma de un código, sin distinguir mayúsculas ni los espacios del inicio y del final.
     *
     * @param codigo {@code es} o {@code en}
     * @return el idioma, o vacío si no es uno de los dos
     */
    public static Optional<IdiomaDelReporte> de(String codigo) {
        String limpio = codigo == null ? "" : codigo.strip();
        for (IdiomaDelReporte idioma : values()) {
            if (idioma.codigo.equalsIgnoreCase(limpio)) {
                return Optional.of(idioma);
            }
        }
        return Optional.empty();
    }

    /**
     * El idioma de una región: el inglés si la región es inglesa y, si no, el español, el idioma por defecto (RP-11).
     *
     * @param region la región de la petición
     * @return el idioma
     */
    public static IdiomaDelReporte de(Locale region) {
        return region != null && EN.codigo.equals(region.getLanguage()) ? EN : ES;
    }
}
