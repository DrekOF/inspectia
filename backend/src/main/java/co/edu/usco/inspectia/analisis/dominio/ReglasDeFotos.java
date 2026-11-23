/*
 * Nombre: ReglasDeFotos.java
 * Descripción: Las reglas de las fotos de una muestra (RF-05): de 1 a 3 por muestra, JPG o PNG, de hasta 10 MB, y un
 *              contenido que empieza como su formato.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

/**
 * Las reglas de las fotos de una muestra (RF-05), sin Spring. Los límites son los de la tabla {@code foto}
 * ({@code specs/entidades/foto.json}): número de 1 a 3 y tamaño de 1 a 10.485.760 bytes.
 */
public final class ReglasDeFotos {

    /** El tamaño máximo de una foto: 10 MB, 10.485.760 bytes (RF-05 C3). */
    public static final long MAXIMO_DE_BYTES = 10_485_760L;

    /** Cuántas fotos tiene como máximo una muestra (RF-05 C2). */
    public static final int MAXIMO_DE_FOTOS = 3;

    private ReglasDeFotos() {
    }

    /**
     * Revisa el tipo y el tamaño de una foto antes de subirla.
     *
     * @param tipo el tipo que declara, como {@code image/jpeg}
     * @param bytes cuánto pesa
     * @return su formato
     * @throws FormatoDeFotoNoPermitidoExcepcion si no es JPG ni PNG (RF-05 C3)
     * @throws FotoDemasiadoGrandeExcepcion si pesa más de 10 MB (RF-05 C3)
     */
    public static FormatoDeFoto revisarArchivo(String tipo, long bytes) {
        FormatoDeFoto formato = FormatoDeFoto.deTipo(tipo).orElseThrow(FormatoDeFotoNoPermitidoExcepcion::new);
        if (bytes > MAXIMO_DE_BYTES) {
            throw new FotoDemasiadoGrandeExcepcion(bytes);
        }
        return formato;
    }

    /**
     * Revisa que el contenido de una foto ya subida empiece como su formato: un archivo que solo dice ser JPG no
     * pasa.
     *
     * @param formato el formato que declaró
     * @param primeros sus primeros bytes
     * @throws FormatoDeFotoNoPermitidoExcepcion si no empieza como su formato (RF-05 C3)
     */
    public static void revisarContenido(FormatoDeFoto formato, byte[] primeros) {
        if (!formato.empiezaComo(primeros)) {
            throw new FormatoDeFotoNoPermitidoExcepcion();
        }
    }

    /**
     * Si un número de foto es válido: de 1 a 3 (RF-05 C2).
     *
     * @param numero el número de la foto en la muestra
     * @return {@code true} si va de 1 a 3
     */
    public static boolean esNumeroValido(int numero) {
        return numero >= 1 && numero <= MAXIMO_DE_FOTOS;
    }
}
