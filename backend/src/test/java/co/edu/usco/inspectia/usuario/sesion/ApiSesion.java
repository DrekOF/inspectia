/*
 * Nombre: ApiSesion.java
 * Descripción: Datos y llamadas comunes de las pruebas del inicio de sesión (RF-01, RNF-04, D-28).
 * Fecha de creación: 14/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario.sesion;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

/**
 * Datos y llamadas comunes de las pruebas del inicio de sesión.
 *
 * <p>Contrato que fijan estas pruebas (D-28):
 * <ul>
 *   <li>{@code POST /api/v1/sesion} con {@code correo} y {@code contrasena} responde {@code token},
 *       {@code tipo} ({@code Bearer}), {@code vence} y {@code usuario}: {@code id}, {@code nombre},
 *       {@code correo}, {@code rol} y {@code empresa} ({@code id} y {@code nombre}, o {@code null}
 *       para el administrador de la plataforma).</li>
 *   <li>{@code GET /api/v1/sesion}, con el token en el encabezado {@code Authorization: Bearer},
 *       responde {@code vence} y {@code usuario}.</li>
 *   <li>Los rechazos responden 401 en {@code application/problem+json}; el mensaje va en
 *       {@code detail} y, en los intentos fallidos, la regla del bloqueo va en {@code ayuda}.</li>
 *   <li>La API no usa CSRF: estas pruebas no envían ese token.</li>
 * </ul>
 */
final class ApiSesion {

    static final String RUTA = "/api/v1/sesion";
    static final String CONTRASENA = "Clave-Segura-2026";

    static final String CREDENCIALES_INCORRECTAS = "Correo o contraseña incorrectos";
    static final String AYUDA_BLOQUEO = "Si fallas 5 veces seguidas, la cuenta se bloquea 15 minutos.";
    static final String USUARIO_DESACTIVADO =
            "Tu usuario está desactivado: pide al administrador de tu empresa que lo reactive.";
    static final String EMPRESA_DESACTIVADA = "Tu empresa está desactivada: comunícate con el equipo de InspectIA.";
    static final String SESION_VENCIDA = "Tu sesión venció: inicia sesión de nuevo.";
    static final String SESION_INVALIDA = "La sesión no es válida: inicia sesión de nuevo.";
    static final String SIN_SESION = "Inicia sesión para continuar.";

    /** Costo bajo, solo para crear rápido los usuarios de prueba: el backend acepta hashes de cualquier costo. */
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder(4);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final MockMvcTester mvc;
    private final JdbcTemplate jdbc;

    ApiSesion(MockMvcTester mvc, JdbcTemplate jdbc) {
        this.mvc = mvc;
        this.jdbc = jdbc;
    }

    /** Pide iniciar sesión. */
    MvcTestResult iniciar(String correo, String contrasena) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("correo", correo);
        datos.put("contrasena", contrasena);
        return enviar(HttpMethod.POST, RUTA, datos, null);
    }

    /** Inicia sesión con la contraseña correcta y devuelve el token; falla si no responde 200. */
    String token(String correo) {
        MvcTestResult resultado = iniciar(correo, CONTRASENA);
        assertThat(resultado).hasStatus(HttpStatus.OK);
        return (String) json(resultado).get("token");
    }

    /** Pide la sesión actual con el token; sin token si es {@code null}. */
    MvcTestResult sesion(String token) {
        return enviar(HttpMethod.GET, RUTA, null, token);
    }

    /**
     * Envía una petición con el token en {@code Authorization: Bearer}.
     *
     * @param datos el cuerpo, que se envía como JSON; {@code null} si no lleva
     * @param token el token; {@code null} para enviar la petición sin sesión
     */
    MvcTestResult enviar(HttpMethod metodo, String uri, Object datos, String token) {
        MockMvcTester.MockMvcRequestBuilder peticion = mvc.method(metodo).uri(uri);
        if (token != null) {
            peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (datos != null) {
            peticion.contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(datos));
        }
        return peticion.exchange();
    }

    /** Pide la sesión actual con el encabezado {@code Authorization} tal cual. */
    MvcTestResult conEncabezado(String autorizacion) {
        return mvc.get().uri(RUTA).header(HttpHeaders.AUTHORIZATION, autorizacion).exchange();
    }

    UUID crearEmpresa(String nombre, String nit) {
        return jdbc.queryForObject("INSERT INTO empresa (nombre, nit, ciudad) VALUES (?, ?, 'Neiva') RETURNING id",
                UUID.class, nombre, nit);
    }

    /**
     * Crea un usuario activo con la contraseña {@link #CONTRASENA}.
     *
     * @param empresa su empresa; {@code null} para el administrador de la plataforma
     */
    UUID crearUsuario(UUID empresa, String nombre, String correo, String rol) {
        String hash = BCRYPT.encode(CONTRASENA);
        if (empresa == null) {
            return jdbc.queryForObject("""
                    INSERT INTO usuario (nombre, correo, contrasena_hash, rol)
                    VALUES (?, ?, ?, ?) RETURNING id""", UUID.class, nombre, correo, hash, rol);
        }
        return jdbc.queryForObject("""
                INSERT INTO usuario (empresa_id, nombre, correo, contrasena_hash, rol)
                VALUES (?, ?, ?, ?, ?) RETURNING id""", UUID.class, empresa, nombre, correo, hash, rol);
    }

    /** El cuerpo de la respuesta, leído como un objeto JSON. */
    static Map<String, Object> json(MvcTestResult resultado) {
        String cuerpo = new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        return JsonPath.read(cuerpo, "$");
    }

    /** El usuario que deben responder el inicio de sesión y la sesión actual. */
    static Map<String, Object> usuario(UUID id, String nombre, String correo, String rol, UUID empresa,
            String nombreEmpresa) {
        Map<String, Object> usuario = new LinkedHashMap<>();
        usuario.put("id", id.toString());
        usuario.put("nombre", nombre);
        usuario.put("correo", correo);
        usuario.put("rol", rol);
        usuario.put("empresa", empresa == null ? null : Map.of("id", empresa.toString(), "nombre", nombreEmpresa));
        return usuario;
    }

    /** Borra lo que guardaron las pruebas, que sí confirman sus transacciones. */
    static void borrarDatos(JdbcTemplate jdbc) {
        jdbc.execute("TRUNCATE lote, rango_recomendacion, usuario, empresa CASCADE");
    }
}
