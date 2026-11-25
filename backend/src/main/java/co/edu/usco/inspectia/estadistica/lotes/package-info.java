/*
 * Nombre: package-info.java (estadistica.lotes)
 * Descripción: Las estadísticas de los lotes de la empresa (RF-16): por capas y a mano (D-21).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
/**
 * Las estadísticas de los lotes de la empresa (RF-16), por capas: {@link EstadisticasControlador} (la API),
 * {@link EstadisticasServicio} (valida el periodo), {@link EstadisticasRepositorio} (lee los lotes analizados de la
 * empresa en el periodo) y {@link CalculoDeEstadisticas} (arma las cifras, sin Spring).
 *
 * <p>Solo las ve el administrador de empresa (RF-16 C3, RNF-04), y solo de su empresa (RNF-03). El periodo es la fecha
 * del resultado en días de Colombia, con las mismas reglas de los filtros del historial (RF-14), así las cifras
 * coinciden con los lotes del historial en el mismo periodo (RF-16 C1). Las fincas y los proveedores se agrupan en Java
 * con {@code Textos.claveDeBusqueda}, como las sugerencias de origen (RF-04), sin tocar la base (RF-16 C4).
 */
package co.edu.usco.inspectia.estadistica.lotes;
