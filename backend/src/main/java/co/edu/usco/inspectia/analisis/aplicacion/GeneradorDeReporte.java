/*
 * Nombre: GeneradorDeReporte.java
 * Descripción: Puerto que arma el reporte PDF de un lote en un idioma, con su resultado y el de cada muestra (RF-15,
 *              D-21).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

/**
 * Puerto del reporte PDF de un lote (RF-15, D-21). El caso de uso le pasa lo guardado y él solo lo dibuja: no
 * recalcula nada, así las cifras del PDF son las de la página (RF-15 C2). Lo implementa el adaptador de PDF.
 */
public interface GeneradorDeReporte {

    /**
     * Arma el reporte.
     *
     * @param contenido el lote, su empresa, su resultado y sus muestras terminadas, con sus fotos
     * @param idioma el idioma de los textos, los números y las fechas
     * @return los bytes del PDF
     */
    byte[] generar(ContenidoDelReporte contenido, IdiomaDelReporte idioma);
}
