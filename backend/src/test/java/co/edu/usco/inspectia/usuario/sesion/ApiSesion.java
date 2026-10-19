/*
 * Nombre: ApiSesion.java
 * Descripción: Datos y llamadas comunes de las pruebas que usan la sesión: inicio de sesión, usuarios y
 *              restablecimiento de contraseñas (RF-01, RF-02, RF-03, RNF-04, D-28).
 * Fecha de creación: 14/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia.usuario.sesion;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
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
 * Datos y llamadas comunes de las pruebas que usan la sesión. La usan las pruebas del inicio de sesión
 * y las de otros módulos que necesitan un token real, como la gestión de usuarios.
 *
 * <p>Contrato que fijan las pruebas del inicio de sesión (D-28):
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
public final class ApiSesion {

    public static final String RUTA = "/api/v1/sesion";
    public static final String CONTRASENA = "Clave-Segura-2026";

    public static final String CREDENCIALES_INCORRECTAS = "Correo o contraseña incorrectos";
    public static final String AYUDA_BLOQUEO = "Si fallas 5 veces seguidas, la cuenta se bloquea 15 minutos.";
    public static final String USUARIO_DESACTIVADO =
            "Tu usuario está desactivado: pide al administrador de tu empresa que lo reactive.";
    public static final String EMPRESA_DESACTIVADA =
            "Tu empresa está desactivada: comunícate con el equipo de InspectIA.";
    public static final String SESION_VENCIDA = "Tu sesión venció: inicia sesión de nuevo.";
    public static final String SESION_INVALIDA = "La sesión no es válida: inicia sesión de nuevo.";
    public static final String SIN_SESION = "Inicia sesión para continuar.";
    public static final String CONTRASENA_CAMBIADA = "Tu contraseña cambió: inicia sesión de nuevo.";

    /** Costo bajo, solo para crear rápido los usuarios de prueba: el backend acepta hashes de cualquier costo. */
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder(4);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final MockMvcTester mvc;
    private final JdbcTemplate jdbc;

    /**
     * Crea las llamadas de una prueba.
     *
     * @param mvc el cliente de la API
     * @param jdbc la base de la prueba
     */
    public ApiSesion(MockMvcTester mvc, JdbcTemplate jdbc) {
        this.mvc = mvc;
        this.jdbc = jdbc;
    }

    /** Pide iniciar sesión. */
    public MvcTestResult iniciar(String correo, String contrasena) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("correo", correo);
        datos.put("contrasena", contrasena);
        return enviar(HttpMethod.POST, RUTA, datos, null);
    }

    /** Inicia sesión con la contraseña {@link #CONTRASENA} y devuelve el token; falla si no responde 200. */
    public String token(String correo) {
        return token(correo, CONTRASENA);
    }

    /** Inicia sesión con la contraseña dada y devuelve el token; falla si no responde 200. */
    public String token(String correo, String contrasena) {
        MvcTestResult resultado = iniciar(correo, contrasena);
        assertThat(resultado).hasStatus(HttpStatus.OK);
        return (String) json(resultado).get("token");
    }

    /** Pide la sesión actual con el token; sin token si es {@code null}. */
    public MvcTestResult sesion(String token) {
        return enviar(HttpMethod.GET, RUTA, null, token);
    }

    /**
     * Envía una petición con el token en {@code Authorization: Bearer}.
     *
     * @param datos el cuerpo, que se envía como JSON; {@code null} si no lleva
     * @param token el token; {@code null} para enviar la petición sin sesión
     */
    public MvcTestResult enviar(HttpMethod metodo, String uri, Object datos, String token) {
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
    public MvcTestResult conEncabezado(String autorizacion) {
        return mvc.get().uri(RUTA).header(HttpHeaders.AUTHORIZATION, autorizacion).exchange();
    }

    /** Crea una empresa activa en Neiva. */
    public UUID crearEmpresa(String nombre, String nit) {
        return jdbc.queryForObject("INSERT INTO empresa (nombre, nit, ciudad) VALUES (?, ?, 'Neiva') RETURNING id",
                UUID.class, nombre, nit);
    }

    /**
     * Crea un usuario activo con la contraseña {@link #CONTRASENA}.
     *
     * @param empresa su empresa; {@code null} para el administrador de la plataforma
     */
    public UUID crearUsuario(UUID empresa, String nombre, String correo, String rol) {
        return crearUsuario(empresa, nombre, correo, rol, CONTRASENA);
    }

    /**
     * Crea un usuario activo con la contraseña dada.
     *
     * @param empresa su empresa; {@code null} para el administrador de la plataforma
     */
    public UUID crearUsuario(UUID empresa, String nombre, String correo, String rol, String contrasena) {
        String hash = BCRYPT.encode(contrasena);
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
    public static Map<String, Object> json(MvcTestResult resultado) {
        return JsonPath.read(cuerpo(resultado), "$");
    }

    /** El cuerpo de la respuesta, leído como una lista JSON de objetos. */
    public static List<Map<String, Object>> lista(MvcTestResult resultado) {
        return JsonPath.read(cuerpo(resultado), "$");
    }

    /** Los errores de una respuesta 400, 403 o 409: un {@code campo} y un {@code mensaje} por cada uno. */
    public static List<Map<String, Object>> errores(MvcTestResult resultado) {
        return JsonPath.read(cuerpo(resultado), "$.errores");
    }

    /**
     * Un error como lo responde la API.
     *
     * @param campo el campo de la petición; {@code null} si el error no es de un campo, y entonces no
     *              aparece en la respuesta
     */
    public static Map<String, Object> error(String campo, String mensaje) {
        Map<String, Object> error = new LinkedHashMap<>();
        if (campo != null) {
            error.put("campo", campo);
        }
        error.put("mensaje", mensaje);
        return error;
    }

    /** El usuario que deben responder el inicio de sesión y la sesión actual. */
    public static Map<String, Object> usuario(UUID id, String nombre, String correo, String rol, UUID empresa,
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
    public static void borrarDatos(JdbcTemplate jdbc) {
        jdbc.execute("TRUNCATE lote, rango_recomendacion, usuario, empresa CASCADE");
    }

    private static String cuerpo(MvcTestResult resultado) {
        return new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }
}
