/*
 * Nombre: OrientacionExif.java
 * Descripción: Lee la orientación EXIF de una foto JPG, para dibujarla girada como la ve la página y como la revisó el
 *              servicio de IA, que da las posiciones de los granos en la foto ya girada (D-45, RF-15).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.pdf;

/**
 * La orientación EXIF de una foto (D-45): de 1 (sin girar) a 8. El navegador gira la foto según ella, y el servicio
 * de IA da las posiciones de los granos en la foto ya girada; Java, en cambio, lee los píxeles sin girar. Solo se lee
 * de un JPG: del segmento APP1 «Exif», la etiqueta 0x0112 del primer directorio. Una foto sin EXIF, un PNG o un EXIF
 * dañado dan 1.
 */
final class OrientacionExif {

    /** Sin girar. */
    static final int NORMAL = 1;

    private static final int ETIQUETA_DE_ORIENTACION = 0x0112;

    private OrientacionExif() {
    }

    /**
     * La orientación de una foto.
     *
     * @param foto los bytes del archivo
     * @return de 1 a 8; 1 si no la tiene o no se puede leer
     */
    static int de(byte[] foto) {
        if (foto == null || foto.length < 4 || u8(foto, 0) != 0xFF || u8(foto, 1) != 0xD8) {
            return NORMAL;
        }
        int posicion = 2;
        while (posicion + 4 <= foto.length && u8(foto, posicion) == 0xFF) {
            int marcador = u8(foto, posicion + 1);
            if (marcador == 0xDA || marcador == 0xD9) {
                break;
            }
            int largo = (u8(foto, posicion + 2) << 8) | u8(foto, posicion + 3);
            int datos = posicion + 4;
            if (largo < 2 || datos + largo - 2 > foto.length) {
                break;
            }
            if (marcador == 0xE1 && largo >= 8 && esExif(foto, datos)) {
                return orientacion(foto, datos + 6, datos + largo - 2);
            }
            posicion = datos + largo - 2;
        }
        return NORMAL;
    }

    private static boolean esExif(byte[] foto, int inicio) {
        return foto[inicio] == 'E' && foto[inicio + 1] == 'x' && foto[inicio + 2] == 'i' && foto[inicio + 3] == 'f'
                && foto[inicio + 4] == 0 && foto[inicio + 5] == 0;
    }

    /** La orientación dentro del bloque TIFF que va de {@code tiff} a {@code fin}. */
    private static int orientacion(byte[] foto, int tiff, int fin) {
        if (tiff + 8 > fin) {
            return NORMAL;
        }
        boolean intel = foto[tiff] == 'I' && foto[tiff + 1] == 'I';
        boolean motorola = foto[tiff] == 'M' && foto[tiff + 1] == 'M';
        if (!intel && !motorola || u16(foto, tiff + 2, intel) != 42) {
            return NORMAL;
        }
        long directorio = tiff + u32(foto, tiff + 4, intel);
        if (directorio + 2 > fin) {
            return NORMAL;
        }
        int entradas = u16(foto, (int) directorio, intel);
        for (int i = 0; i < entradas; i++) {
            int entrada = (int) directorio + 2 + i * 12;
            if (entrada + 12 > fin) {
                return NORMAL;
            }
            if (u16(foto, entrada, intel) == ETIQUETA_DE_ORIENTACION) {
                int valor = u16(foto, entrada + 8, intel);
                return valor >= 1 && valor <= 8 ? valor : NORMAL;
            }
        }
        return NORMAL;
    }

    private static int u8(byte[] datos, int posicion) {
        return datos[posicion] & 0xFF;
    }

    private static int u16(byte[] datos, int posicion, boolean intel) {
        return intel ? u8(datos, posicion) | (u8(datos, posicion + 1) << 8)
                : (u8(datos, posicion) << 8) | u8(datos, posicion + 1);
    }

    private static long u32(byte[] datos, int posicion, boolean intel) {
        long bajo = u16(datos, intel ? posicion : posicion + 2, intel);
        long alto = u16(datos, intel ? posicion + 2 : posicion, intel);
        return (alto << 16) | bajo;
    }
}
