/*
 * Nombre: package-info.java (comun.produccion)
 * Descripción: El perfil produccion, el del despliegue en AWS (D-16).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
/**
 * El perfil {@code produccion}, el del despliegue en AWS (D-16): no arranca sin el almacén en S3 y el servicio de IA, y
 * toma la IP y el protocolo del cliente de los encabezados de CloudFront. Su configuración está en
 * {@code application-produccion.yml}.
 */
package co.edu.usco.inspectia.comun.produccion;
