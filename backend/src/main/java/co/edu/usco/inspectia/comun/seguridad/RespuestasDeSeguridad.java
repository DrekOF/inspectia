/*
 * Nombre: RespuestasDeSeguridad.java
 * Descripción: Respuestas 401 y 403 de la API en application/problem+json, en el idioma de la
 *              petición; el 401 explica por qué no hay sesión (RNF-04, RP-11, D-28).
 * Fecha de creación: 13/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.3
 */
package co.edu.usco.inspectia.comun.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;
import tools.jackson.databind.json.JsonMapper;

/**
 * Respuestas de la cadena de seguridad en {@code application/problem+json}, con el mismo formato
 * que los demás errores de la API (RNF-04): 401 sin sesión y 403 con un rol sin permiso. El 401 dice
 * por qué no hay sesión, para que el frontend lo muestre y pida iniciar sesión de nuevo.
 */
@Component
public class RespuestasDeSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final MessageSource mensajes;
    private final LocaleResolver idiomas;
    private final JsonMapper json;

    /**
     * Crea las respuestas; Spring les pasa los mensajes, el idioma de cada petición y el JSON.
     *
     * @param mensajes los textos en español y en inglés
     * @param idiomas el idioma de cada petición, según {@code Accept-Language}
     * @param json el convertidor a JSON
     */
    public RespuestasDeSeguridad(MessageSource mensajes, LocaleResolver idiomas, JsonMapper json) {
        this.mensajes = mensajes;
        this.idiomas = idiomas;
        this.json = json;
    }

    @Override
    public void commence(HttpServletRequest peticion, HttpServletResponse respuesta,
            AuthenticationException excepcion) throws IOException {
        escribir(peticion, respuesta, HttpStatus.UNAUTHORIZED, motivo(excepcion));
    }

    /**
     * El mensaje de un 401 según su causa (D-28):
     * <ul>
     *   <li>El usuario del token no puede usar la sesión: su motivo; por ejemplo, que está desactivado o
     *       que su contraseña cambió después de emitir el token («Tu contraseña cambió»).</li>
     *   <li>El token venció: «Tu sesión venció».</li>
     *   <li>El token no sirve (alterado, de otra clave, sin firma, de otro emisor o mal formado):
     *       «La sesión no es válida».</li>
     *   <li>No hay token: «Inicia sesión para continuar».</li>
     * </ul>
     */
    static String motivo(AuthenticationException excepcion) {
        if (excepcion instanceof SesionNoValidaExcepcion sesion) {
            return sesion.getClave();
        }
        if (excepcion instanceof OAuth2AuthenticationException) {
            return vencida(excepcion) ? "error.sesion_vencida" : "error.sesion_invalida";
        }
        return "error.no_autenticado";
    }

    /** Si el único problema del token es que venció: su firma y su emisor estaban bien. */
    private static boolean vencida(Throwable excepcion) {
        for (Throwable causa = excepcion; causa != null; causa = causa.getCause()) {
            if (causa instanceof JwtValidationException validacion) {
                return !validacion.getErrors().isEmpty() && validacion.getErrors().stream()
                        .allMatch(error -> ValidadorDeVigencia.SESION_VENCIDA.equals(error.getErrorCode()));
            }
        }
        return false;
    }

    @Override
    public void handle(HttpServletRequest peticion, HttpServletResponse respuesta,
            AccessDeniedException excepcion) throws IOException {
        escribir(peticion, respuesta, HttpStatus.FORBIDDEN, "error.prohibido");
    }

    private void escribir(HttpServletRequest peticion, HttpServletResponse respuesta, HttpStatus estado,
            String clave) throws IOException {
        Locale idioma = idiomas.resolveLocale(peticion);
        Map<String, Object> problema = new LinkedHashMap<>();
        problema.put("type", "about:blank");
        problema.put("title", estado.getReasonPhrase());
        problema.put("status", estado.value());
        problema.put("detail", mensajes.getMessage(clave, null, clave, idioma));
        problema.put("instance", peticion.getRequestURI());
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8);
        json.writeValue(respuesta.getOutputStream(), problema);
    }
}
