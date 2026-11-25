/*
 * Nombre: AlmacenDeArchivos.java
 * Descripción: Puerto del almacén de los archivos (D-21, D-41): los enlaces temporales para subir y ver una foto, y
 *              revisar, leer, guardar o borrar un archivo, como una foto o un reporte PDF. Lo implementan la carpeta
 *              local y S3 (D-16).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.3
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.time.Duration;
import java.util.Optional;

/**
 * Puerto del almacén de los archivos (D-21). En AWS es S3 (D-16); en el PC de desarrollo y en las pruebas, una carpeta
 * del servidor que firma sus propios enlaces (D-41). La página sube y ve las fotos directo con los enlaces temporales,
 * sin pasar el archivo por los casos de uso. Las llaves las arma siempre el backend y tienen una de las formas de
 * {@link LlavesDeArchivos}: los dos almacenes rechazan cualquier otra. Los enlaces temporales son solo de fotos: un
 * reporte se descarga por el backend, con la sesión.
 */
public interface AlmacenDeArchivos {

    /**
     * El enlace para subir un archivo con {@code PUT}, sin la sesión. Solo sirve con ese tipo y ese tamaño.
     *
     * @param llave dónde queda el archivo
     * @param tipo el tipo del contenido, como {@code image/jpeg}
     * @param bytes cuánto pesa
     * @param vigencia cuánto dura el enlace
     * @return el enlace
     */
    EnlaceTemporal enlaceDeSubida(String llave, String tipo, long bytes, Duration vigencia);

    /**
     * El enlace para ver un archivo con {@code GET}, sin la sesión, como en un {@code <img>}.
     *
     * @param llave el archivo
     * @param vigencia cuánto dura el enlace
     * @return el enlace
     */
    EnlaceTemporal enlaceDeVista(String llave, Duration vigencia);

    /**
     * Lo que se necesita para revisar un archivo ya subido: cuánto pesa y sus primeros bytes.
     *
     * @param llave el archivo
     * @return el archivo, o vacío si no se subió
     */
    Optional<ArchivoGuardado> describir(String llave);

    /**
     * El contenido de un archivo ya subido, para mandárselo al clasificador de granos: el servidor le manda las fotos,
     * no sus enlaces (specs/arquitectura.md).
     *
     * @param llave el archivo
     * @return sus bytes
     * @throws java.io.UncheckedIOException si el archivo no está o no se puede leer
     */
    byte[] leer(String llave);

    /**
     * Guarda un archivo que arma el propio servidor, como el reporte PDF de un lote (RF-15).
     *
     * @param llave la llave del archivo
     * @param contenido sus bytes
     * @param tipo su tipo, como {@code application/pdf}
     */
    void guardar(String llave, byte[] contenido, String tipo);

    /**
     * Borra un archivo. Si ya no está, no hace nada.
     *
     * @param llave el archivo
     */
    void borrar(String llave);
}
