/*
 * Nombre: package-info.java (analisis.adaptadores.jpa)
 * Descripción: Adaptador de la base de datos del módulo de análisis, con JPA (D-21).
 * Fecha de creación: 10/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
/**
 * Adaptador de la base de datos del módulo de análisis (D-21): implementa los puertos de los repositorios
 * con JPA, sobre las tablas que genera {@code generador/} desde {@code specs/entidades} (V1): {@code lote},
 * {@code muestra}, {@code foto}, {@code grano} y {@code modelo}. También tiene la cola de análisis en la tabla
 * {@code muestra} ({@code ColaEnLaBase}, RNF-02 C3) y los rangos vigentes de cada empresa, desde el CRUD generado de
 * {@code rango_recomendacion} ({@code RangosDeLaEmpresaJpa}, RF-13).
 */
package co.edu.usco.inspectia.analisis.adaptadores.jpa;
