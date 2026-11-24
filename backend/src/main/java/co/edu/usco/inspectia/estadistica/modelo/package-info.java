/*
 * Nombre: package-info.java (estadistica.modelo)
 * Descripción: La pantalla «Modelo de IA» del administrador de empresa: las métricas de prueba de los modelos de la
 *              comparación, cuál está en uso y la confianza promedio de los análisis de la empresa (RF-17, D-14, D-18,
 *              D-48).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
/**
 * Las métricas del modelo de IA para el administrador de empresa (RF-17). Va por capas y se escribe a mano (D-21):
 * <ul>
 *   <li>{@link co.edu.usco.inspectia.estadistica.modelo.MetricasDelModeloControlador}: {@code GET /api/v1/modelo}, solo
 *       para el administrador de empresa (RNF-04).</li>
 *   <li>{@link co.edu.usco.inspectia.estadistica.modelo.MetricasDelModeloServicio}: arma la respuesta.</li>
 *   <li>{@link co.edu.usco.inspectia.estadistica.modelo.MetricasDelModeloRepositorio}: lee la tabla {@code modelo},
 *       común a todas las empresas, y los granos de la empresa de la sesión (RNF-03).</li>
 * </ul>
 *
 * <p>Solo lee: las filas de {@code modelo} las registra el backend al arrancar (el clasificador falso o el servicio de
 * IA) y los granos los guarda el análisis (módulo {@code analisis}).
 */
package co.edu.usco.inspectia.estadistica.modelo;
