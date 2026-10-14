/*
 * Nombre: ReglasDeAcceso.java
 * Descripción: Reglas de acceso por ruta que aporta cada módulo a la cadena de seguridad (RNF-04).
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.seguridad;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

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
}
