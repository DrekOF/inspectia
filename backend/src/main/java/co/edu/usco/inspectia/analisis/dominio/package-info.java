/*
 * Nombre: package-info.java (analisis.dominio)
 * Descripción: Reglas del análisis de un lote, sin depender de Spring (D-21).
 * Fecha de creación: 10/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
/**
 * Reglas del análisis de un lote, en Java puro: no dependen de Spring, de la base ni de la API (D-21). Se
 * prueban con pruebas unitarias y de mutación, sin encender el servidor (RF-10 C7). Tienen el registro del lote
 * (RF-04), las fotos de la muestra (RF-05) y el resultado: el porcentaje contado y el corregido con los recall del
 * modelo de cada muestra, su margen ({@code PorcentajeDelLote}, D-39, P-19), la categoría y la sugerencia de otra
 * muestra ({@code ReglasDeRecomendacion}, RF-12, D-24, D-25), los granos dudosos ({@code ReglasDeConfianza}, P-15) y
 * el resultado del lote con 1 o 2 muestras ({@code ResultadoDelLote}).
 */
package co.edu.usco.inspectia.analisis.dominio;
