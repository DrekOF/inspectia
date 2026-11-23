/*
 * Nombre: AlmacenDeArchivos.java
 * Descripción: Puerto del almacén de los archivos de las muestras (D-21, D-41): los enlaces temporales para subir y
 *              ver una foto, y revisar o borrar un archivo. Lo implementan la carpeta local y, con el despliegue, S3.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.time.Duration;
import java.util.Optional;

/**
 * Puerto del almacén de los archivos (D-21). En AWS es S3 (D-16); mientras tanto, y en las pruebas, una carpeta del
 * servidor que firma sus propios enlaces (D-41). La página sube y ve las fotos directo con los enlaces temporales,
 * sin pasar el archivo por los casos de uso. Las llaves las arma siempre el backend.
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
     * Borra un archivo. Si ya no está, no hace nada.
     *
     * @param llave el archivo
     */
    void borrar(String llave);
}
