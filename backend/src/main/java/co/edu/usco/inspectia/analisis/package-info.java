/*
 * Nombre: package-info.java (analisis)
 * Descripción: Módulo analisis del servidor de InspectIA (D-21).
 * Fecha de creación: 13/10/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
/**
 * Módulo analisis, con arquitectura hexagonal (D-21). Maneja lote, muestra, foto, grano y modelo
 * (RF-04 a RF-12, RF-14 y RF-15) y se escribe a mano.
 *
 * <ul>
 *   <li>{@code dominio}: las reglas del lote (% de granos sanos, categoría y sugerencia), sin
 *       depender de Spring.</li>
 *   <li>{@code aplicacion}: los casos de uso, como enviar la muestra ({@code EnviarMuestra}), atender la cola
 *       ({@code AtenderCola} y {@code GuardarAnalisis}) y consultar el resultado, y sus puertos (interfaces).</li>
 *   <li>{@code adaptadores}: REST, la base con JPA y la cola de análisis, la carpeta de las fotos, el clasificador
 *       falso y, más adelante, el servicio de IA, S3 y el PDF.</li>
 * </ul>
 */
package co.edu.usco.inspectia.analisis;
