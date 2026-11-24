/*
 * Nombre: MetricasDelModeloAcceso.java
 * Descripción: Regla de acceso de la ruta del modelo de IA: solo el administrador de empresa (RF-17 C3, RNF-04).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.estadistica.modelo;

import co.edu.usco.inspectia.comun.seguridad.ReglasDeAcceso;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * Regla de acceso de la ruta del modelo de IA (RNF-04): solo la usa el administrador de empresa. El usuario final y el
 * administrador de la plataforma, que no tiene empresa, reciben 403 (RF-17 C3).
 */
@Component
public class MetricasDelModeloAcceso implements ReglasDeAcceso {

    @Override
    public void registrar(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry autorizaciones) {
        autorizaciones.requestMatchers(HttpMethod.GET, MetricasDelModeloControlador.RUTA).hasRole("ADMIN_EMPRESA");
    }
}
