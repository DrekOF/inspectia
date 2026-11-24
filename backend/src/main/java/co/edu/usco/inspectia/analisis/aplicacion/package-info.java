/*
 * Nombre: package-info.java (analisis.aplicacion)
 * Descripción: Casos de uso del módulo de análisis y sus puertos (D-21).
 * Fecha de creación: 10/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
/**
 * Casos de uso del módulo de análisis y sus puertos (D-21): las interfaces con que hablan con la base, el
 * almacenamiento de las fotos, el servicio de IA y el PDF, que implementan los adaptadores. Tiene registrar un lote
 * (RF-04), consultar el historial (RF-14), subir las fotos de la muestra (RF-05) y el análisis: enviar la muestra
 * ({@link co.edu.usco.inspectia.analisis.aplicacion.EnviarMuestra}), atender la cola
 * ({@link co.edu.usco.inspectia.analisis.aplicacion.AtenderCola} y
 * {@link co.edu.usco.inspectia.analisis.aplicacion.GuardarAnalisis}), y consultar el avance y el resultado
 * ({@link co.edu.usco.inspectia.analisis.aplicacion.ConsultarAnalisis} y
 * {@link co.edu.usco.inspectia.analisis.aplicacion.ConsultarResultado}). Sus puertos son
 * {@link co.edu.usco.inspectia.analisis.aplicacion.RepositorioDeLotes},
 * {@link co.edu.usco.inspectia.analisis.aplicacion.RepositorioDeMuestras},
 * {@link co.edu.usco.inspectia.analisis.aplicacion.AlmacenDeArchivos},
 * {@link co.edu.usco.inspectia.analisis.aplicacion.ClasificadorDeGranos},
 * {@link co.edu.usco.inspectia.analisis.aplicacion.RepositorioDeModelos},
 * {@link co.edu.usco.inspectia.analisis.aplicacion.RangosDeLaEmpresa} y
 * {@link co.edu.usco.inspectia.analisis.aplicacion.ColaDeAnalisis}.
 */
package co.edu.usco.inspectia.analisis.aplicacion;
