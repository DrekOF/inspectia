/*
 * Nombre: EstadisticasAcceso.java
 * Descripción: Regla de acceso de la ruta de las estadísticas: solo el administrador de empresa (RF-16 C3, RNF-04).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.estadistica.lotes;

import co.edu.usco.inspectia.comun.seguridad.ReglasDeAcceso;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * Regla de acceso de las estadísticas (RNF-04): solo las consulta el administrador de empresa (RF-16 C3). El usuario
 * final y el administrador de la plataforma reciben 403. Se revisa antes de leer la petición, así que un rol sin
 * permiso recibe 403 aunque envíe un periodo inválido.
 */
@Component
public class EstadisticasAcceso implements ReglasDeAcceso {

    @Override
    public void registrar(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry autorizaciones) {
        autorizaciones.requestMatchers(HttpMethod.GET, EstadisticasControlador.RUTA).hasRole("ADMIN_EMPRESA");
    }
}
