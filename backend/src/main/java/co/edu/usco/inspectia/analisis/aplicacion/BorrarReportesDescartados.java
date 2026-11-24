/*
 * Nombre: BorrarReportesDescartados.java
 * Descripción: Borra del almacén los archivos de un reporte PDF descartado, después de confirmar la transacción, y los
 *              de uno nuevo cuya transacción se deshizo (RF-15, D-25, D-50).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Borra los archivos de un reporte, uno por idioma (D-50), como {@link BorrarArchivosReemplazados} con las fotos: el
 * de la muestra 1 cuando la 2 termina (RF-15 C5, D-25), después de confirmar la transacción, y el que se acaba de
 * armar si la transacción que guardaba su base se deshizo. Si un borrado falla, lo registra y sigue: el lote ya no
 * apunta a ese reporte, y sin la sesión de su empresa nadie lo puede descargar.
 */
@Slf4j
@Component
public class BorrarReportesDescartados {

    private final AlmacenDeArchivos almacen;

    /**
     * Crea el borrador; Spring le pasa el almacén.
     *
     * @param almacen el almacén de los archivos
     */
    public BorrarReportesDescartados(AlmacenDeArchivos almacen) {
        this.almacen = almacen;
    }

    /**
     * Borra los archivos de un reporte que ya no sirve, cuando la transacción se confirma.
     *
     * @param evento el reporte descartado
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void borrar(ReporteDescartado evento) {
        borrarArchivos(evento.base());
    }

    /**
     * Borra los archivos de un reporte nuevo, si la transacción que lo guardaba se deshizo.
     *
     * @param evento el reporte nuevo
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void deshacer(ReporteNuevo evento) {
        borrarArchivos(evento.base());
    }

    private void borrarArchivos(String base) {
        for (IdiomaDelReporte idioma : IdiomaDelReporte.values()) {
            String llave = DescargarReporte.llave(base, idioma);
            try {
                almacen.borrar(llave);
            } catch (RuntimeException excepcion) {
                log.warn("No se pudo borrar el archivo de un reporte descartado: {}", llave, excepcion);
            }
        }
    }
}
