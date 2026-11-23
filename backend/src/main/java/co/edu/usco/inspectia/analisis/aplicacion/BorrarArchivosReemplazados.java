/*
 * Nombre: BorrarArchivosReemplazados.java
 * Descripción: Borra del almacén el archivo de una foto reemplazada, después de confirmar la transacción (RF-05 C5).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Borra el archivo de una foto reemplazada (RF-05 C5), después de confirmar la transacción que guardó la nueva. Si
 * el borrado falla, por ejemplo porque el antivirus tiene el archivo abierto, lo registra y sigue: la foto ya no está
 * en la base, y el archivo huérfano no se puede ver sin un enlace firmado.
 */
@Slf4j
@Component
public class BorrarArchivosReemplazados {

    private final AlmacenDeArchivos almacen;

    /**
     * Crea el borrador; Spring le pasa el almacén.
     *
     * @param almacen el almacén de los archivos
     */
    public BorrarArchivosReemplazados(AlmacenDeArchivos almacen) {
        this.almacen = almacen;
    }

    /**
     * Borra el archivo de la foto anterior.
     *
     * @param evento la foto reemplazada
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void borrar(ArchivoReemplazado evento) {
        try {
            almacen.borrar(evento.llave());
        } catch (RuntimeException excepcion) {
            log.warn("No se pudo borrar el archivo de una foto reemplazada: {}", evento.llave(), excepcion);
        }
    }
}
