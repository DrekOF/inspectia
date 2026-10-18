/*
 * Nombre: SesionAutenticada.java
 * Descripción: La sesión de una petición con un token válido: su usuario y su rol como permiso
 *              (RNF-04, D-28).
 * Fecha de creación: 14/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.seguridad;

import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * La sesión de una petición con un token válido. Su rol queda como el permiso {@code ROLE_<rol>},
 * el que revisan las reglas de acceso por ruta y {@code @PreAuthorize} (RNF-04).
 */
public class SesionAutenticada extends AbstractAuthenticationToken {

    private final UsuarioEnSesion usuario;

    /**
     * Crea la sesión de un usuario ya revisado.
     *
     * @param usuario el usuario, leído de la base en esta petición
     */
    public SesionAutenticada(UsuarioEnSesion usuario) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol())));
        this.usuario = usuario;
        super.setAuthenticated(true);
    }

    /** El token ya se revisó y no se guarda: la sesión no tiene credenciales. */
    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public UsuarioEnSesion getPrincipal() {
        return usuario;
    }

    /** El identificador del usuario, para los registros del servidor; así no queda su correo. */
    @Override
    public String getName() {
        return usuario.id().toString();
    }
}
