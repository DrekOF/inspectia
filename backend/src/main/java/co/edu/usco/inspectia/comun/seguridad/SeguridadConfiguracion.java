/*
 * Nombre: SeguridadConfiguracion.java
 * Descripción: Cadena de seguridad del servidor: todo /api exige sesión con un token JWT, cada
 *              operación su rol y las contraseñas se guardan con BCrypt (RNF-04, D-28).
 * Fecha de creación: 13/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.comun.seguridad;

import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cadena de seguridad del servidor (RNF-04, D-28):
 * <ul>
 *   <li>Cada petición trae su token en el encabezado {@code Authorization: Bearer}
 *       ({@link ConfiguracionJwt}). En cada una se lee de la base su usuario, con su rol y su
 *       empresa ({@link CargadorDeSesion}).</li>
 *   <li>Cada módulo aporta sus reglas por ruta ({@link ReglasDeAcceso}); lo demás exige sesión.</li>
 *   <li>Sin sesión responde 401 y con un rol sin permiso, 403, en JSON ({@link RespuestasDeSeguridad}).</li>
 *   <li>{@code @PreAuthorize} en los controladores es la segunda barrera.</li>
 *   <li>La documentación OpenAPI es pública en desarrollo; para producción se decide en el despliegue.</li>
 * </ul>
 *
 * <p>La API no usa CSRF: ese ataque aprovecha que el navegador envía solo las cookies, y aquí la
 * sesión no va en una cookie sino en un encabezado que el frontend agrega a cada petición (D-28).
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
     * @param cargador lee de la base el usuario de cada token
     * @return la cadena
     * @throws Exception si la configuración no es válida
     */
    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, ObjectProvider<ReglasDeAcceso> reglas,
            RespuestasDeSeguridad respuestas, CargadorDeSesion cargador) throws Exception {
        http.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(servidor -> servidor
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                token -> new SesionAutenticada(cargador.cargar(usuarioDe(token), token.getExpiresAt()))))
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
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

    /** El identificador del usuario que trae el token; si no es un UUID, el token no es de este servidor. */
    private static UUID usuarioDe(Jwt token) {
        String usuario = token.getSubject();
        try {
            return UUID.fromString(usuario == null ? "" : usuario);
        } catch (IllegalArgumentException error) {
            throw new SesionNoValidaExcepcion("error.sesion_invalida");
        }
    }
}
