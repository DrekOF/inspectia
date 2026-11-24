/*
 * Nombre: BorrarReportesDescartadosTest.java
 * Descripción: Pruebas unitarias del borrado de los archivos de un reporte PDF descartado o de uno nuevo cuya
 *              transacción se deshizo: los dos idiomas, aunque uno falle (RF-15 C5, D-25, D-50).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Borrar los archivos de un reporte (RF-15). */
class BorrarReportesDescartadosTest {

    private static final String BASE = "reportes/e/l/r";

    private final AlmacenDeArchivos almacen = mock(AlmacenDeArchivos.class);
    private final BorrarReportesDescartados borrar = new BorrarReportesDescartados(almacen);

    @Test
    @DisplayName("RF-15 · C5 · un reporte descartado borra sus dos archivos, el de español y el de inglés, aunque borrar el primero falle (D-25, D-50)")
    void borraLosDosIdiomas() {
        doThrow(new IllegalStateException("antivirus")).when(almacen).borrar(BASE + "-es.pdf");

        borrar.borrar(new ReporteDescartado(BASE));

        verify(almacen).borrar(BASE + "-es.pdf");
        verify(almacen).borrar(BASE + "-en.pdf");
    }

    @Test
    @DisplayName("RF-15 · si la transacción que guardaba un reporte nuevo se deshace, sus archivos se borran")
    void deshaceElNuevo() {
        borrar.deshacer(new ReporteNuevo(BASE));

        verify(almacen).borrar(BASE + "-es.pdf");
        verify(almacen).borrar(BASE + "-en.pdf");
    }
}
