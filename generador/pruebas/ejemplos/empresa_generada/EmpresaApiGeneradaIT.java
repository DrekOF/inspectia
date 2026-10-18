/*
 * Nombre: EmpresaApiGeneradaIT.java
 * Descripción: Pruebas generadas de la API de empresa: permisos, validaciones y datos únicos.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.4)
 * Versión: 1.4
 * Entidades: empresa 1.2, usuario 1.4, rango_recomendacion 1.1
 */
package co.edu.usco.inspectia.empresa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import co.edu.usco.inspectia.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pruebas generadas de la API de {@code empresa} (RP-13, RP-23): cada permiso de {@code operaciones}
 * y cada validación del JSON tiene un caso, además de los datos únicos. Las pruebas de los criterios de
 * las fichas van aparte, escritas a mano.
 *
 * <p>Los datos válidos salen de {@code ejemplo} de cada atributo. Las pruebas confirman sus transacciones
 * y vacían las tablas antes y después de cada una.
 */
@PruebaIntegracion
class EmpresaApiGeneradaIT {

    private static final String RUTA = "/api/v1/empresas";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void vaciarAntes() {
        vaciar();
    }

    @AfterEach
    void vaciarDespues() {
        vaciar();
    }

    @ParameterizedTest(name = "{0} · {1}")
    @DisplayName("RNF-04 · C2 · cada operación de empresa solo la usan los roles de su JSON")
    @MethodSource("permisos")
    void permisos(String operacion, String rol, boolean permitido) {
        MvcTestResult resultado = enviar(operacion, user("prueba@inspectia.test").roles(rol));

        if (permitido) {
            assertThat(resultado.getResponse().getStatus()).isNotIn(401, 403);
        } else {
            assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    static Stream<Arguments> permisos() {
        return Stream.of(
                Arguments.of("listar", "USUARIO_FINAL", false),
                Arguments.of("listar", "ADMIN_EMPRESA", false),
                Arguments.of("listar", "ADMIN_PLATAFORMA", true),
                Arguments.of("ver", "USUARIO_FINAL", false),
                Arguments.of("ver", "ADMIN_EMPRESA", false),
                Arguments.of("ver", "ADMIN_PLATAFORMA", true),
                Arguments.of("crear", "USUARIO_FINAL", false),
                Arguments.of("crear", "ADMIN_EMPRESA", false),
                Arguments.of("crear", "ADMIN_PLATAFORMA", true),
                Arguments.of("editar", "USUARIO_FINAL", false),
                Arguments.of("editar", "ADMIN_EMPRESA", false),
                Arguments.of("editar", "ADMIN_PLATAFORMA", true),
                Arguments.of("desactivar", "USUARIO_FINAL", false),
                Arguments.of("desactivar", "ADMIN_EMPRESA", false),
                Arguments.of("desactivar", "ADMIN_PLATAFORMA", true),
                Arguments.of("reactivar", "USUARIO_FINAL", false),
                Arguments.of("reactivar", "ADMIN_EMPRESA", false),
                Arguments.of("reactivar", "ADMIN_PLATAFORMA", true));
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("RNF-04 · C1 · sin sesión, cada operación de empresa responde 401")
    @MethodSource("operaciones")
    void sinSesion(String operacion) {
        assertThat(enviar(operacion, null)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    static Stream<String> operaciones() {
        return Stream.of("listar", "ver", "crear", "editar", "desactivar", "reactivar");
    }

    @ParameterizedTest(name = "{0}: {2}")
    @DisplayName("RF-02 · cada validación del JSON de empresa responde 400 con su mensaje al crear")
    @MethodSource("validacionesAlCrear")
    void validacionesAlCrear(String campo, Object valor, String mensaje) {
        Map<String, Object> datos = datosParaCrear();
        poner(datos, campo, valor);

        MvcTestResult resultado = enviar(HttpMethod.POST, RUTA, datos, conPermiso("crear"));

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).contains(tuple(campo, mensaje));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM empresa", Integer.class)).isZero();
    }

    static Stream<Arguments> validacionesAlCrear() {
        return Stream.of(
                Arguments.of("nombre", null, "Este dato es obligatorio."),
                Arguments.of("nombre", "x".repeat(121), "Puede tener como máximo 120 caracteres."),
                Arguments.of("nit", null, "Este dato es obligatorio."),
                Arguments.of("nit", "?", "Escribe el NIT sin puntos y con el dígito de verificación, por ejemplo 900123456-8."),
                Arguments.of("ciudad", null, "Este dato es obligatorio."),
                Arguments.of("ciudad", "x".repeat(81), "Puede tener como máximo 80 caracteres."),
                Arguments.of("administrador.nombre", null, "Este dato es obligatorio."),
                Arguments.of("administrador.nombre", "x".repeat(121), "Puede tener como máximo 120 caracteres."),
                Arguments.of("administrador.correo", null, "Este dato es obligatorio."),
                Arguments.of("administrador.correo", correoDeLargo(255), "Puede tener como máximo 254 caracteres."),
                Arguments.of("administrador.correo", "sin-arroba", "Escribe un correo válido."),
                Arguments.of("administrador.contrasena", null, "Este dato es obligatorio."),
                Arguments.of("administrador.contrasena", " ".repeat(8), "Este dato es obligatorio."),
                Arguments.of("administrador.contrasena", "x".repeat(65), "La contraseña puede tener como máximo 64 caracteres."),
                Arguments.of("administrador.contrasena", "x".repeat(7), "La contraseña debe tener al menos 8 caracteres."),
                Arguments.of("administrador.contrasena", "ñ".repeat(37), "La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes"));
    }

    @ParameterizedTest(name = "{0}: {2}")
    @DisplayName("RF-02 · cada validación del JSON de empresa responde 400 con su mensaje al editar")
    @MethodSource("validacionesAlEditar")
    void validacionesAlEditar(String campo, Object valor, String mensaje) {
        UUID id = crear();
        Map<String, Object> datos = datosParaEditar();
        poner(datos, campo, valor);

        MvcTestResult resultado = enviar(HttpMethod.PUT, RUTA + "/" + id, datos, conPermiso("editar"));

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).contains(tuple(campo, mensaje));
    }

    static Stream<Arguments> validacionesAlEditar() {
        return Stream.of(
                Arguments.of("nombre", null, "Este dato es obligatorio."),
                Arguments.of("nombre", "x".repeat(121), "Puede tener como máximo 120 caracteres."),
                Arguments.of("nit", null, "Este dato es obligatorio."),
                Arguments.of("nit", "?", "Escribe el NIT sin puntos y con el dígito de verificación, por ejemplo 900123456-8."),
                Arguments.of("ciudad", null, "Este dato es obligatorio."),
                Arguments.of("ciudad", "x".repeat(81), "Puede tener como máximo 80 caracteres."));
    }

    @Test
    @DisplayName("RF-02 · los datos únicos del JSON de empresa no se repiten: responde 409")
    void datosUnicos() {
        crear();

        MvcTestResult resultado = enviar(HttpMethod.POST, RUTA, datosParaCrear(), conPermiso("crear"));

        assertThat(resultado).hasStatus(HttpStatus.CONFLICT);
        assertThat(errores(resultado)).contains(
                tuple("nit", "Ya existe una empresa con este NIT."),
                tuple("administrador.correo", "Ya existe un usuario con este correo."));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM empresa", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("RF-02 · empresa guarda sin espacios al inicio ni al final los textos que su JSON pide recortar")
    void recortaLosTextos() {
        Map<String, Object> datos = datosParaCrear();
        datos.put("nombre", "  " + "Cacaos del Huila" + "  ");
        datos.put("nit", "  " + "900123456-8" + "  ");
        datos.put("ciudad", "  " + "Neiva" + "  ");

        MvcTestResult resultado = enviar(HttpMethod.POST, RUTA, datos, conPermiso("crear"));

        assertThat(resultado).hasStatus(HttpStatus.CREATED);
        assertThat(json(resultado)).containsEntry("nombre", "Cacaos del Huila");
        assertThat(json(resultado)).containsEntry("nit", "900123456-8");
        assertThat(json(resultado)).containsEntry("ciudad", "Neiva");
    }

    private void vaciar() {
        jdbc.execute("TRUNCATE empresa, usuario, rango_recomendacion CASCADE");
    }

    private static Map<String, Object> datosParaCrear() {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", "Cacaos del Huila");
        datos.put("nit", "900123456-8");
        datos.put("ciudad", "Neiva");
        Map<String, Object> administrador = new LinkedHashMap<>();
        administrador.put("nombre", "Ana Pérez");
        administrador.put("correo", "ana.perez@example.com");
        administrador.put("contrasena", "Prueba-Segura-2026");
        datos.put("administrador", administrador);
        return datos;
    }

    private UUID crear() {
        MvcTestResult resultado = enviar(HttpMethod.POST, RUTA, datosParaCrear(), conPermiso("crear"));
        assertThat(resultado).hasStatus(HttpStatus.CREATED);
        return UUID.fromString((String) json(resultado).get("id"));
    }

    private static Map<String, Object> datosParaEditar() {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", "Cacaos del Huila");
        datos.put("nit", "900123456-8");
        datos.put("ciudad", "Neiva");
        return datos;
    }

    /** Envía la operación con datos válidos; las que llevan un registro usan uno que no existe. */
    private MvcTestResult enviar(String operacion, RequestPostProcessor usuario) {
        String registro = RUTA + "/" + UUID.randomUUID();
        return switch (operacion) {
            case "listar" -> enviar(HttpMethod.GET, RUTA, null, usuario);
            case "ver" -> enviar(HttpMethod.GET, registro, null, usuario);
            case "crear" -> enviar(HttpMethod.POST, RUTA, datosParaCrear(), usuario);
            case "editar" -> enviar(HttpMethod.PUT, registro, datosParaEditar(), usuario);
            case "desactivar" -> enviar(HttpMethod.POST, registro + "/desactivar", null, usuario);
            case "reactivar" -> enviar(HttpMethod.POST, registro + "/reactivar", null, usuario);
            default -> throw new IllegalArgumentException(operacion);
        };
    }

    private MvcTestResult enviar(HttpMethod metodo, String uri, Object datos, RequestPostProcessor usuario) {
        MockMvcTester.MockMvcRequestBuilder peticion = mvc.method(metodo).uri(uri).with(csrf());
        if (datos != null) {
            peticion.contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(datos));
        }
        if (usuario != null) {
            peticion.with(usuario);
        }
        return peticion.exchange();
    }

    /** Un usuario con el primer rol que puede usar la operación. */
    private static RequestPostProcessor conPermiso(String operacion) {
        String rol = switch (operacion) {
            case "listar" -> "ADMIN_PLATAFORMA";
            case "ver" -> "ADMIN_PLATAFORMA";
            case "crear" -> "ADMIN_PLATAFORMA";
            case "editar" -> "ADMIN_PLATAFORMA";
            case "desactivar" -> "ADMIN_PLATAFORMA";
            case "reactivar" -> "ADMIN_PLATAFORMA";
            default -> throw new IllegalArgumentException(operacion);
        };
        return user("prueba@inspectia.test").roles(rol);
    }

    /** Cambia un dato; {@code administrador.correo} cambia el correo dentro de {@code administrador}. */
    @SuppressWarnings("unchecked")
    private static void poner(Map<String, Object> datos, String campo, Object valor) {
        String[] partes = campo.split("\\.", 2);
        if (partes.length == 2) {
            ((Map<String, Object>) datos.get(partes[0])).put(partes[1], valor);
        } else {
            datos.put(campo, valor);
        }
    }

    private static Map<String, Object> json(MvcTestResult resultado) {
        return JsonPath.read(new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8), "$");
    }

    @SuppressWarnings("unchecked")
    private static List<Tuple> errores(MvcTestResult resultado) {
        List<Map<String, Object>> errores = (List<Map<String, Object>>) json(resultado).get("errores");
        assertThat(errores).as("la respuesta trae la lista «errores»").isNotNull();
        return errores.stream().map(error -> tuple(error.get("campo"), error.get("mensaje"))).toList();
    }

    /** Un correo válido de {@code largo} caracteres: 64 en la parte local y etiquetas de hasta 63. */
    private static String correoDeLargo(int largo) {
        String local = "a".repeat(64);
        StringBuilder dominio = new StringBuilder("co");
        while (local.length() + 1 + dominio.length() < largo) {
            int falta = largo - local.length() - 1 - dominio.length() - 1;
            dominio.insert(0, "b".repeat(Math.max(1, Math.min(63, falta))) + ".");
        }
        return local + "@" + dominio;
    }
}
