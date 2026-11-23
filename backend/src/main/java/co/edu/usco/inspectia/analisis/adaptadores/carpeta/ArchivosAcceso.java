/*
 * Nombre: ArchivosAcceso.java
 * Descripción: Reglas de acceso de las rutas firmadas de la carpeta de las fotos: no piden sesión ni leen el token,
 *              porque las autoriza la firma del enlace (D-41, excepción a RNF-04).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.carpeta;

import co.edu.usco.inspectia.comun.seguridad.ReglasDeAcceso;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

/**
 * Reglas de acceso de las rutas firmadas de la carpeta de las fotos (D-41). Son la excepción a RNF-04: no piden sesión,
 * porque un {@code <img>} no manda el token y con S3 la página tampoco lo manda, y las autoriza la firma de cada enlace
 * ({@link ArchivosControlador}). Tampoco leen el token: uno vencido en el encabezado no las rechaza. Solo existen con
 * el almacén en carpeta.
 */
@Component
@ConditionalOnProperty(prefix = "inspectia.archivos", name = "almacen", havingValue = "carpeta",
        matchIfMissing = true)
public class ArchivosAcceso implements ReglasDeAcceso {

    private static final String ARCHIVOS = AlmacenEnCarpeta.RUTA + "/**";

    @Override
    public void registrar(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry autorizaciones) {
        autorizaciones
                .requestMatchers(HttpMethod.GET, ARCHIVOS).permitAll()
                .requestMatchers(HttpMethod.PUT, ARCHIVOS).permitAll();
    }

    @Override
    public List<RequestMatcher> sinToken() {
        return List.of(PathPatternRequestMatcher.pathPattern(HttpMethod.GET, ARCHIVOS),
                PathPatternRequestMatcher.pathPattern(HttpMethod.PUT, ARCHIVOS));
    }
}
