/*
 * Nombre: AdministradoresAcceso.java
 * Descripción: Reglas de acceso de las rutas de los administradores de una empresa: solo el
 *              administrador de la plataforma (RF-02 C12, RNF-04).
 * Fecha de creación: 18/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario.administradores;

import co.edu.usco.inspectia.comun.seguridad.ReglasDeAcceso;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * Reglas de acceso de las rutas de los administradores de una empresa (RNF-04): solo las usa el
 * administrador de la plataforma. Se revisan antes de leer la petición, así que otro rol recibe 403
 * aunque envíe datos inválidos.
 */
@Component
public class AdministradoresAcceso implements ReglasDeAcceso {

    @Override
    public void registrar(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry autorizaciones) {
        autorizaciones
                .requestMatchers(HttpMethod.GET, AdministradoresControlador.RUTA).hasRole("ADMIN_PLATAFORMA")
                .requestMatchers(HttpMethod.PUT, AdministradoresControlador.CONTRASENA).hasRole("ADMIN_PLATAFORMA");
    }
}
