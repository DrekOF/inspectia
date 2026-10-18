/*
 * Nombre: ReglasUsuarioImpl.java
 * Descripción: Reglas de usuario escritas a mano: implementa la interfaz generada ReglasUsuario (RP-23,
 *              RF-03).
 * Fecha de creación: 18/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.usuario.reglas;

import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import co.edu.usco.inspectia.usuario.ReglasUsuario;
import co.edu.usco.inspectia.usuario.Usuario;
import org.springframework.stereotype.Component;

/**
 * Reglas de {@code specs/entidades/usuario.json} que se programan a mano. El servicio generado las
 * llama con el usuario de la sesión, que es el administrador de la empresa (RF-03).
 */
@Component
public class ReglasUsuarioImpl implements ReglasUsuario {

    private final UsuariosActivosRepositorio activos;

    /**
     * Crea las reglas; Spring les pasa el conteo de activos.
     *
     * @param activos cuenta los usuarios activos de una empresa
     */
    public ReglasUsuarioImpl(UsuariosActivosRepositorio activos) {
        this.activos = activos;
    }

    /**
     * Cuenta los activos de la empresa de la sesión: el usuario que se crea o se reactiva todavía no
     * está entre ellos. Reactivar a uno que ya está activo no suma otro (RF-03 C5).
     *
     * <p>Antes de contar bloquea la fila de la empresa hasta que termine la transacción. Si dos
     * administradores crean o reactivan un usuario a la vez, el segundo espera a que el primero guarde
     * y cuenta con ese usuario incluido; sin el bloqueo, los dos contarían 24 y quedarían 26.
     */
    @Override
    public boolean cumpleMaximoUsuariosActivos(Usuario usuario, UsuarioEnSesion sesion) {
        if (usuario.getId() != null && Boolean.TRUE.equals(usuario.getActivo())) {
            return true;
        }
        activos.bloquearEmpresa(sesion.empresaId());
        return activos.countByEmpresaIdAndActivoTrue(sesion.empresaId()) < MAXIMO_USUARIOS_ACTIVOS;
    }

    /** El administrador no puede desactivar su propio usuario (RF-03 C7). */
    @Override
    public boolean cumpleNoDesactivarseASiMismo(Usuario usuario, UsuarioEnSesion sesion) {
        return !usuario.getId().equals(sesion.id());
    }

    /**
     * El administrador no puede cambiar su propio rol (RF-03 C8). El rol de la sesión es el de la base
     * en esta petición, así que basta compararlo con el que se quiere guardar.
     */
    @Override
    public boolean cumpleNoCambiarSuPropioRol(Usuario usuario, UsuarioEnSesion sesion) {
        return !usuario.getId().equals(sesion.id()) || usuario.getRol().name().equals(sesion.rol());
    }

    /**
     * El administrador no puede cambiar su propia contraseña desde la gestión de usuarios: la cambia en
     * «Cambiar contraseña», escribiendo la actual (RF-03 C11, RF-18). El servicio solo la revisa si
     * llega una contraseña nueva.
     */
    @Override
    public boolean cumpleNoCambiarSuPropiaContrasena(Usuario usuario, UsuarioEnSesion sesion) {
        return !usuario.getId().equals(sesion.id());
    }
}
