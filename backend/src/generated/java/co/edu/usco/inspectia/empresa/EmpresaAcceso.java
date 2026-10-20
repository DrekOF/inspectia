/*
 * Nombre: EmpresaAcceso.java
 * Descripción: Roles que pueden usar cada operación de la API de empresa (RNF-04).
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.6)
 * Versión: 1.6
 * Entidades: empresa 1.2, usuario 1.5, rango_recomendacion 1.2
 */
package co.edu.usco.inspectia.empresa;

import co.edu.usco.inspectia.comun.seguridad.ReglasDeAcceso;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * Roles que pueden usar cada operación de la API de {@code empresa}, según {@code operaciones} del
 * JSON (RNF-04). La cadena de seguridad los revisa antes de leer la petición: un rol sin permiso
 * recibe 403 aunque envíe datos inválidos.
 */
@Component
public class EmpresaAcceso implements ReglasDeAcceso {

    @Override
    public void registrar(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry autorizaciones) {
        autorizaciones
                .requestMatchers(HttpMethod.GET, "/api/v1/empresas").hasRole("ADMIN_PLATAFORMA")
                .requestMatchers(HttpMethod.GET, "/api/v1/empresas/{id}").hasRole("ADMIN_PLATAFORMA")
                .requestMatchers(HttpMethod.POST, "/api/v1/empresas").hasRole("ADMIN_PLATAFORMA")
                .requestMatchers(HttpMethod.PUT, "/api/v1/empresas/{id}").hasRole("ADMIN_PLATAFORMA")
                .requestMatchers(HttpMethod.POST, "/api/v1/empresas/{id}/desactivar").hasRole("ADMIN_PLATAFORMA")
                .requestMatchers(HttpMethod.POST, "/api/v1/empresas/{id}/reactivar").hasRole("ADMIN_PLATAFORMA");
    }
}
