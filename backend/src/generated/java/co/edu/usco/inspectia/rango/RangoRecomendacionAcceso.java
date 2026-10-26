/*
 * Nombre: RangoRecomendacionAcceso.java
 * Descripción: Roles que pueden usar cada operación de la API de rango_recomendacion (RNF-04).
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.7)
 * Versión: 1.7
 * Entidades: rango_recomendacion 1.2
 */
package co.edu.usco.inspectia.rango;

import co.edu.usco.inspectia.comun.seguridad.ReglasDeAcceso;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * Roles que pueden usar cada operación de la API de {@code rango_recomendacion}, según {@code operaciones} del
 * JSON (RNF-04). La cadena de seguridad los revisa antes de leer la petición: un rol sin permiso
 * recibe 403 aunque envíe datos inválidos.
 */
@Component
public class RangoRecomendacionAcceso implements ReglasDeAcceso {

    @Override
    public void registrar(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry autorizaciones) {
        autorizaciones
                .requestMatchers(HttpMethod.GET, "/api/v1/rangos-recomendacion").hasRole("ADMIN_EMPRESA")
                .requestMatchers(HttpMethod.PUT, "/api/v1/rangos-recomendacion").hasRole("ADMIN_EMPRESA")
                .requestMatchers(HttpMethod.POST, "/api/v1/rangos-recomendacion/restaurar-por-defecto").hasRole("ADMIN_EMPRESA");
    }
}
