/*
 * Nombre: ReglasDeAcceso.java
 * Descripción: Reglas de acceso por ruta que aporta cada módulo a la cadena de seguridad (RNF-04).
 * Fecha de creación: 13/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.comun.seguridad;

import java.util.List;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Reglas de acceso por ruta de un módulo: qué roles pueden usar cada operación de su API (RNF-04).
 * Los CRUD las generan desde {@code operaciones} de su JSON (RP-23).
 *
 * <p>Se revisan en la cadena de seguridad, antes de leer la petición: un rol sin permiso recibe 403
 * aunque envíe datos inválidos. {@code @PreAuthorize} en cada controlador es la segunda barrera.
 */
@FunctionalInterface
public interface ReglasDeAcceso {

    /**
     * Agrega las reglas del módulo.
     *
     * @param autorizaciones las reglas de la cadena de seguridad
     */
    void registrar(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry autorizaciones);

    /**
     * Rutas públicas del módulo que no leen el token: una petición a ellas no se rechaza por traer en
     * el encabezado {@code Authorization} un token vencido o que no sirve. Por ejemplo, iniciar
     * sesión cuando el frontend todavía envía el token de una sesión vencida (RF-01 C6). Por defecto,
     * ninguna.
     *
     * @return las rutas
     */
    default List<RequestMatcher> sinToken() {
        return List.of();
    }
}
