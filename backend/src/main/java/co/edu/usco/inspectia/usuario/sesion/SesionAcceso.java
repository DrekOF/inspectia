/*
 * Nombre: SesionAcceso.java
 * Descripción: Reglas de acceso de la sesión: iniciar sesión es público y ver la sesión exige un
 *              token (RF-01, RNF-04).
 * Fecha de creación: 14/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario.sesion;

import co.edu.usco.inspectia.comun.seguridad.ReglasDeAcceso;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * Reglas de acceso de la sesión (RNF-04): iniciar sesión es lo único público de la API; ver la
 * sesión actual lo puede cualquier rol con un token válido.
 */
@Component
public class SesionAcceso implements ReglasDeAcceso {

    @Override
    public void registrar(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry autorizaciones) {
        autorizaciones
                .requestMatchers(HttpMethod.POST, SesionControlador.RUTA).permitAll()
                .requestMatchers(HttpMethod.GET, SesionControlador.RUTA).authenticated();
    }
}
