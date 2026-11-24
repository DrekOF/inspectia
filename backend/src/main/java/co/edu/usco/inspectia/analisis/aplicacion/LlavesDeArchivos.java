/*
 * Nombre: LlavesDeArchivos.java
 * Descripción: Las dos formas de llave que aceptan los almacenes de archivos: la de una foto de una muestra y la de un
 *              reporte PDF (D-16, D-41, RF-15). Cualquier otra se rechaza.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.util.regex.Pattern;

/**
 * Las formas de llave de los archivos, que comparten la carpeta local (D-41) y S3 (D-16). Las arma siempre el backend:
 * <ul>
 *   <li>Una foto: {@code <empresa>/<lote>/m<muestra>-<uuid>.<jpg|png>}, con la muestra 1 o 2 (D-25).</li>
 *   <li>Un reporte PDF, uno por idioma (RF-15): {@code reportes/<empresa>/<lote>/<uuid>-<es|en>.pdf}.</li>
 * </ul>
 * Cualquier otra forma, como una llave que sale de su carpeta o una de los modelos o de los respaldos, no se acepta: un
 * almacén nunca lee, escribe ni firma un enlace fuera de esas dos.
 */
public final class LlavesDeArchivos {

    /** Qué guarda una llave. */
    public enum Clase {
        /** Una foto de una muestra. */
        FOTO,
        /** Un reporte PDF de un lote. */
        REPORTE
    }

    private static final String UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    private static final Pattern FOTO = Pattern.compile("^" + UUID + "/" + UUID + "/m[12]-" + UUID + "\\.(jpg|png)$");

    private static final Pattern REPORTE = Pattern.compile(
            "^reportes/" + UUID + "/" + UUID + "/" + UUID + "-(es|en)\\.pdf$");

    private LlavesDeArchivos() {
    }

    /**
     * Qué guarda una llave.
     *
     * @param llave la llave
     * @return la clase de archivo
     * @throws IllegalArgumentException si la llave no es de una foto ni de un reporte
     */
    public static Clase clase(String llave) {
        if (llave != null && FOTO.matcher(llave).matches()) {
            return Clase.FOTO;
        }
        if (llave != null && REPORTE.matcher(llave).matches()) {
            return Clase.REPORTE;
        }
        throw new IllegalArgumentException("La llave no tiene la forma <empresa>/<lote>/m<muestra>-<archivo> de una "
                + "foto ni la forma reportes/<empresa>/<lote>/<archivo>-<idioma>.pdf de un reporte");
    }

    /**
     * Si una llave es de una foto.
     *
     * @param llave la llave
     * @return {@code true} si tiene la forma de una foto
     */
    public static boolean esFoto(String llave) {
        return llave != null && FOTO.matcher(llave).matches();
    }
}
