/*
 * Nombre: package-info.java (analisis.adaptadores.carpeta)
 * Descripción: El almacén de las fotos en una carpeta del servidor, con enlaces que firma el mismo servidor, mientras
 *              no haya despliegue en AWS (D-41).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
/**
 * El almacén de las fotos en una carpeta del servidor (D-41): el adaptador del puerto
 * {@link co.edu.usco.inspectia.analisis.aplicacion.AlmacenDeArchivos} mientras no haya despliegue en AWS, y en las
 * pruebas. Hace las veces de S3: firma sus propios enlaces temporales de subida y de vista, que no piden sesión, y los
 * atiende en {@code /api/v1/archivos}. Solo se carga con el almacén en carpeta, el de siempre por ahora
 * ({@code inspectia.archivos.almacen=carpeta}); con S3 no existen ni la carpeta ni esas rutas.
 */
package co.edu.usco.inspectia.analisis.adaptadores.carpeta;
