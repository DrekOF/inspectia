/*
 * Nombre: Registrador.java
 * Descripción: Quien registró lotes de la empresa, con su identificador y su nombre y sin su correo, para el filtro
 *              «Usuario» del historial (RF-14 restricción 6, RNF-03).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.util.UUID;

/**
 * Quien registró al menos un lote de la empresa (RF-14 restricción 6). Sale también si después lo desactivaron
 * (RF-03 C6), y no lleva el correo: el filtro «Usuario» solo necesita el nombre (RNF-03).
 *
 * @param id el identificador del usuario
 * @param nombre su nombre
 */
public record Registrador(UUID id, String nombre) {
}
