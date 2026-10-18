/*
 * Nombre: SesionAcceso.java
 * Descripción: Reglas de acceso de la sesión: iniciar sesión es público y no lee el token; ver la
 *              sesión exige uno válido (RF-01, RNF-04).
 * Fecha de creación: 14/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.usuario.sesion;

import co.edu.usco.inspectia.comun.seguridad.ReglasDeAcceso;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

/**
 * Reglas de acceso de la sesión (RNF-04): iniciar sesión es lo único público de la API y no lee el
 * token; ver la sesión actual lo puede cualquier rol con un token válido.
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

    /**
     * Iniciar sesión no lee el token: quien tiene la sesión vencida puede volver a entrar aunque el
     * frontend todavía envíe el token viejo (RF-01 C6). Ver la sesión actual sí lo exige.
     */
    @Override
    public List<RequestMatcher> sinToken() {
        return List.of(PathPatternRequestMatcher.pathPattern(HttpMethod.POST, SesionControlador.RUTA));
    }
}
