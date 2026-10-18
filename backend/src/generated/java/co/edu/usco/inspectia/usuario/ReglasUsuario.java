/*
 * Nombre: ReglasUsuario.java
 * Descripción: Reglas de usuario que se programan a mano (RF-03).
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.4)
 * Versión: 1.4
 * Entidades: usuario 1.4
 */
package co.edu.usco.inspectia.usuario;

import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;

/**
 * Reglas de {@code usuario} que no se pueden declarar en el JSON y se programan a mano (RP-23). Las
 * implementa una clase del paquete {@code co.edu.usco.inspectia.usuario.reglas}; si falta, la aplicación no arranca.
 *
 * <p>Los parámetros de las reglas salen del JSON como constantes: el valor solo vive allí.
 */
public interface ReglasUsuario {

    /**
     * Parámetro maximo de la regla maximo_usuarios_activos: Una empresa tiene como máximo 25 usuarios
     * activos, contando a sus administradores.
     */
    int MAXIMO_USUARIOS_ACTIVOS = 25;

    /**
     * maximo_usuarios_activos: Una empresa tiene como máximo 25 usuarios activos, contando a sus
     * administradores. Se revisa al crear y al reactivar (RF-03, RNF-02).
     *
     * @param usuario el registro con los datos que se van a guardar
     * @param sesion el usuario de la sesión, con su empresa
     * @return {@code true} si se cumple; si no, la API responde 400 con el mensaje de la regla
     */
    boolean cumpleMaximoUsuariosActivos(Usuario usuario, UsuarioEnSesion sesion);

    /**
     * no_desactivarse_a_si_mismo: El administrador no puede desactivar su propio usuario. Se revisa al
     * desactivar (RF-03).
     *
     * @param usuario el registro con los datos que se van a guardar
     * @param sesion el usuario de la sesión, con su empresa
     * @return {@code true} si se cumple; si no, la API responde 400 con el mensaje de la regla
     */
    boolean cumpleNoDesactivarseASiMismo(Usuario usuario, UsuarioEnSesion sesion);

    /**
     * no_cambiar_su_propio_rol: El administrador no puede cambiar su propio rol: si fuera el único
     * administrador, la empresa quedaría sin quien la gestione, porque el administrador de la
     * plataforma no edita usuarios. Se revisa al editar (RF-03).
     *
     * @param usuario el registro con los datos que se van a guardar
     * @param sesion el usuario de la sesión, con su empresa
     * @return {@code true} si se cumple; si no, la API responde 400 en el campo rol con el mensaje de la regla
     */
    boolean cumpleNoCambiarSuPropioRol(Usuario usuario, UsuarioEnSesion sesion);

    /**
     * no_cambiar_su_propia_contrasena: El administrador no puede cambiar su propia contraseña desde la
     * gestión de usuarios: la cambia en «Cambiar contraseña» (RF-18), escribiendo la actual. Sin esta
     * regla, una sesión robada bastaría para cambiarla. Se revisa al editar, solo si llega una
     * contraseña nueva (RF-03, RF-18, D-30).
     *
     * @param usuario el registro con los datos que se van a guardar
     * @param sesion el usuario de la sesión, con su empresa
     * @return {@code true} si se cumple; si no, la API responde 400 en el campo contrasena con el mensaje de la regla
     */
    boolean cumpleNoCambiarSuPropiaContrasena(Usuario usuario, UsuarioEnSesion sesion);
}
