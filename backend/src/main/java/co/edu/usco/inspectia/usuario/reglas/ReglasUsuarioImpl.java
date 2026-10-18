/*
 * Nombre: ReglasUsuarioImpl.java
 * Descripción: Reglas de usuario escritas a mano: implementa la interfaz generada ReglasUsuario (RP-23,
 *              RF-03), con el corte de sesiones al cambiar una contraseña (RNF-04, D-30).
 * Fecha de creación: 18/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia.usuario.reglas;

import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import co.edu.usco.inspectia.usuario.ReglasUsuario;
import co.edu.usco.inspectia.usuario.Usuario;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/**
 * Reglas de {@code specs/entidades/usuario.json} que se programan a mano. El servicio generado las
 * llama con el usuario de la sesión, que es el administrador de la empresa (RF-03). El restablecimiento
 * de RF-02 C12 también usa el efecto que corta las sesiones.
 */
@Component
public class ReglasUsuarioImpl implements ReglasUsuario {

    private final UsuariosActivosRepositorio activos;
    private final Clock reloj;

    /**
     * Crea las reglas; Spring les pasa el conteo de activos y la hora del servidor.
     *
     * @param activos cuenta los usuarios activos de una empresa
     * @param reloj la hora del servidor
     */
    public ReglasUsuarioImpl(UsuariosActivosRepositorio activos, Clock reloj) {
        this.activos = activos;
        this.reloj = reloj;
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

    /**
     * Al restablecer o cambiar una contraseña, las sesiones abiertas antes del cambio dejan de servir y la
     * cuenta se desbloquea (RNF-04 C6, D-30). {@code sesiones_desde} toma la hora del cambio en segundos
     * enteros, como el {@code iat} de los tokens: en cada petición, un token emitido antes de ese segundo
     * responde 401 «Tu contraseña cambió». Uno emitido en el mismo segundo sigue sirviendo; es una
     * ventana de menos de un segundo que se aceptó en el plan de RF-03.
     */
    @Override
    public void cortarSesionesAlCambiarContrasena(Usuario usuario, UsuarioEnSesion sesion) {
        usuario.setSesionesDesde(OffsetDateTime.ofInstant(reloj.instant().truncatedTo(ChronoUnit.SECONDS),
                ZoneOffset.UTC));
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
    }
}
