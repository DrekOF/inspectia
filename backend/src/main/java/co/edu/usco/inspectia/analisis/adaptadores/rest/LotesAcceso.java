/*
 * Nombre: LotesAcceso.java
 * Descripción: Reglas de acceso de las rutas de lotes: registrar, el usuario final; consultar, también el
 *              administrador de empresa (RF-04, RF-14, RNF-04).
 * Fecha de creación: 10/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.adaptadores.rest;

import co.edu.usco.inspectia.comun.seguridad.ReglasDeAcceso;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * Reglas de acceso de las rutas de lotes (RNF-04). El usuario final registra los lotes (RF-04) y sube sus fotos
 * (RF-05; RF-14, restricción 4); él y el administrador de empresa consultan el historial (RF-14). El administrador
 * de la plataforma, que no tiene empresa, no usa estas rutas. Se revisan antes de leer la petición, así que un rol
 * sin permiso recibe 403 aunque envíe datos inválidos. Las de las fotos van primero: gana la primera regla que
 * coincide, y la de consultar abarca todo lo que está debajo de los lotes.
 */
@Component
public class LotesAcceso implements ReglasDeAcceso {

    @Override
    public void registrar(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry autorizaciones) {
        String fotos = LotesControlador.RUTA + "/*/fotos";
        autorizaciones
                .requestMatchers(HttpMethod.GET, fotos).hasRole("USUARIO_FINAL")
                .requestMatchers(HttpMethod.POST, fotos + "/*/subida").hasRole("USUARIO_FINAL")
                .requestMatchers(HttpMethod.PUT, fotos + "/*").hasRole("USUARIO_FINAL")
                .requestMatchers(HttpMethod.POST, LotesControlador.RUTA).hasRole("USUARIO_FINAL")
                .requestMatchers(HttpMethod.GET, LotesControlador.RUTA, LotesControlador.RUTA + "/**")
                .hasAnyRole("USUARIO_FINAL", "ADMIN_EMPRESA");
    }
}
