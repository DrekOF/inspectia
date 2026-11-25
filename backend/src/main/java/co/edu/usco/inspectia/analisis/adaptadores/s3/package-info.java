/*
 * Nombre: package-info.java (analisis.adaptadores.s3)
 * Descripción: El almacén de los archivos en Amazon S3, el de AWS (D-16).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
/**
 * El almacén de los archivos en Amazon S3 (D-16): el adaptador del puerto
 * {@link co.edu.usco.inspectia.analisis.aplicacion.AlmacenDeArchivos} en AWS, con el AWS SDK for Java v2. Las fotos van
 * en {@code fotos/} y los reportes PDF en {@code reportes/} del bucket {@code inspectia-archivos-<cuenta>}; la página
 * sube y ve las fotos con enlaces prefirmados. Solo se carga con {@code inspectia.archivos.almacen=s3}, el valor que
 * exige el perfil {@code produccion}.
 */
package co.edu.usco.inspectia.analisis.adaptadores.s3;
