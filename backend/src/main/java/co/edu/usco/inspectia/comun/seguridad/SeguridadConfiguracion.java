/*
 * Nombre: SeguridadConfiguracion.java
 * Descripción: Cadena de seguridad del servidor: todo /api exige sesión, cada operación su rol y las
 *              contraseñas se guardan con BCrypt (RNF-04).
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.seguridad;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cadena de seguridad del servidor (RNF-04):
 * <ul>
 *   <li>Cada módulo aporta sus reglas por ruta ({@link ReglasDeAcceso}); lo demás exige sesión.</li>
 *   <li>Sin sesión responde 401 y con un rol sin permiso, 403, en JSON ({@link RespuestasDeSeguridad}).</li>
 *   <li>{@code @PreAuthorize} en los controladores es la segunda barrera.</li>
 *   <li>La documentación OpenAPI es pública en desarrollo; para producción se decide en el despliegue.</li>
 * </ul>
 *
 * <p>Todavía nadie puede iniciar sesión: el inicio de sesión llega con RF-01, que también decide si
 * la API usa CSRF. Mientras tanto queda el CSRF de Spring Security por defecto.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SeguridadConfiguracion {

    /** Rutas de la documentación OpenAPI (RP-04). */
    private static final String[] DOCUMENTACION = {"/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"};

    /**
     * La cadena de seguridad.
     *
     * @param http configuración de Spring Security
     * @param reglas las reglas de acceso de cada módulo
     * @param respuestas las respuestas 401 y 403 en JSON
     * @return la cadena
     * @throws Exception si la configuración no es válida
     */
    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, ObjectProvider<ReglasDeAcceso> reglas,
            RespuestasDeSeguridad respuestas) throws Exception {
        http.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                .authorizeHttpRequests(autorizaciones -> {
                    autorizaciones.requestMatchers(DOCUMENTACION).permitAll();
                    autorizaciones.requestMatchers("/error").permitAll();
                    reglas.orderedStream().forEach(regla -> regla.registrar(autorizaciones));
                    autorizaciones.anyRequest().authenticated();
                });
        return http.build();
    }

    /**
     * Codifica las contraseñas con BCrypt, costo 10 (RNF-04). Es el mismo formato que usa pgcrypto en
     * {@code infra/bd/crear-admin-plataforma.sql}.
     *
     * @return el codificador
     */
    @Bean
    PasswordEncoder codificadorDeContrasenas() {
        return new BCryptPasswordEncoder();
    }
}
