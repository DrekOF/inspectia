/*
 * Nombre: package-info.java (analisis.adaptadores.falso)
 * Descripción: El clasificador de granos falso de desarrollo y de las pruebas, mientras el servicio de IA no use el
 *              modelo de Julián (RF-08, D-40).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
/**
 * El clasificador de granos falso: el adaptador del puerto
 * {@link co.edu.usco.inspectia.analisis.aplicacion.ClasificadorDeGranos} con {@code inspectia.ia.clasificador=falso},
 * mientras el servicio de IA no use el modelo de Julián (D-40). Es determinista según el ancho y el alto de cada foto, y
 * registra su modelo, {@code clasificador-falso 1.0}, al arrancar.
 */
package co.edu.usco.inspectia.analisis.adaptadores.falso;
