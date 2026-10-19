/*
 * Nombre: ReglasEmpresa.java
 * Descripción: Reglas de empresa que se programan a mano (RF-02).
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.5)
 * Versión: 1.5
 * Entidades: empresa 1.2, usuario 1.5, rango_recomendacion 1.1
 */
package co.edu.usco.inspectia.empresa;

/**
 * Reglas de {@code empresa} que no se pueden declarar en el JSON y se programan a mano (RP-23). Las
 * implementa una clase del paquete {@code co.edu.usco.inspectia.empresa.reglas}; si falta, la aplicación no arranca.
 */
public interface ReglasEmpresa {

    /**
     * nit_digito_verificacion: El dígito de verificación del NIT debe coincidir con el que calcula el
     * algoritmo de la DIAN (ver specs/modelo-datos.md). Se revisa al crear y al editar (RF-02, D-27).
     *
     * @param empresa el registro con los datos que se van a guardar
     * @return {@code true} si se cumple; si no, la API responde 400 en el campo nit con el mensaje de la regla
     */
    boolean cumpleNitDigitoVerificacion(Empresa empresa);
}
