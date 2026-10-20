/*
 * Nombre: ReglasRangoRecomendacion.java
 * Descripción: Reglas de rango_recomendacion que se programan a mano (RF-13).
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.6)
 * Versión: 1.6
 * Entidades: rango_recomendacion 1.2
 */
package co.edu.usco.inspectia.rango;

import java.util.List;
import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;

/**
 * Reglas de {@code rango_recomendacion} que no se pueden declarar en el JSON y se programan a mano (RP-23). Las
 * implementa una clase del paquete {@code co.edu.usco.inspectia.rango.reglas}; si falta, la aplicación no arranca.
 */
public interface ReglasRangoRecomendacion {

    /**
     * limites_en_orden: Los límites deben bajar en orden: Premium o Especial > Estándar > Corriente >
     * Fuera de norma (0 %). Se revisa al editar (RF-13).
     *
     * @param filas las filas que se van a guardar, una por cada categoría, en el orden de la lista
     * @param sesion el usuario de la sesión, con su empresa
     * @return {@code true} si se cumple; si no, la API responde 400 con el mensaje de la regla
     */
    boolean cumpleLimitesEnOrden(List<RangoRecomendacion> filas, UsuarioEnSesion sesion);
}
