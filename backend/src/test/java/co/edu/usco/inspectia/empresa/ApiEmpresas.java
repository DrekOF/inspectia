/*
 * Nombre: ApiEmpresas.java
 * Descripción: Datos y llamadas comunes de las pruebas de la API de empresas (RF-02).
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.empresa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.groups.Tuple;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;

/**
 * Datos y llamadas comunes de las pruebas de la API de empresas (RF-02).
 *
 * <p>Contrato que fijan estas pruebas:
 * <ul>
 *   <li>Rutas: {@code /api/v1/empresas}, {@code /{id}}, {@code /{id}/desactivar} y
 *       {@code /{id}/reactivar}.</li>
 *   <li>El registro lleva los datos de la empresa y, en {@code administrador}, los de su primer
 *       administrador ({@code crear_con} de {@code empresa.json}).</li>
 *   <li>Los errores responden en {@code application/problem+json} (RFC 9457), con la lista
 *       {@code errores}: un {@code campo} y un {@code mensaje} por cada dato que falla. Los mensajes
 *       salen en español si la petición no pide otro idioma.</li>
 * </ul>
 *
 * <p>Las peticiones que cambian datos llevan el token CSRF de prueba: así las pruebas no dependen de si
 * la API usa CSRF, algo que se decide con el inicio de sesión (RF-01).
 */
final class ApiEmpresas {

    static final String RUTA = "/api/v1/empresas";

    /** Contraseña inicial válida: 17 caracteres, solo ASCII. */
    static final String CONTRASENA = "Clave-Segura-2026";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final MockMvcTester mvc;

    ApiEmpresas(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    /** El administrador de la plataforma, el único rol que puede usar la API de empresas. */
    static RequestPostProcessor adminPlataforma() {
        return user("plataforma@inspectia.test").roles("ADMIN_PLATAFORMA");
    }

    /**
     * Datos válidos para registrar una empresa con su primer administrador. El mapa se puede cambiar
     * antes de enviarlo.
     */
    static Map<String, Object> registro(String nit, String correo) {
        Map<String, Object> administrador = new LinkedHashMap<>();
        administrador.put("nombre", "Ana Pérez");
        administrador.put("correo", correo);
        administrador.put("contrasena", CONTRASENA);
        Map<String, Object> empresa = new LinkedHashMap<>();
        empresa.put("nombre", "Cacaos del Huila");
        empresa.put("nit", nit);
        empresa.put("ciudad", "Neiva");
        empresa.put("administrador", administrador);
        return empresa;
    }

    /** El administrador dentro de los datos de registro, para cambiarlo. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> administrador(Map<String, Object> registro) {
        return (Map<String, Object>) registro.get("administrador");
    }

    /** Datos para editar una empresa: nombre, NIT y ciudad (D-27). */
    static Map<String, Object> edicion(String nombre, String nit, String ciudad) {
        Map<String, Object> empresa = new LinkedHashMap<>();
        empresa.put("nombre", nombre);
        empresa.put("nit", nit);
        empresa.put("ciudad", ciudad);
        return empresa;
    }

    MvcTestResult registrar(Map<String, Object> datos) {
        return enviar(HttpMethod.POST, RUTA, datos, adminPlataforma());
    }

    /** Registra una empresa que debe quedar creada y devuelve su id. */
    UUID registrarValida(Map<String, Object> datos) {
        MvcTestResult resultado = registrar(datos);
        assertThat(resultado).hasStatus(HttpStatus.CREATED);
        return UUID.fromString((String) json(resultado).get("id"));
    }

    MvcTestResult ver(UUID id) {
        return enviar(HttpMethod.GET, RUTA + "/" + id, null, adminPlataforma());
    }

    MvcTestResult editar(UUID id, Map<String, Object> datos) {
        return enviar(HttpMethod.PUT, RUTA + "/" + id, datos, adminPlataforma());
    }

    MvcTestResult desactivar(UUID id) {
        return enviar(HttpMethod.POST, RUTA + "/" + id + "/desactivar", null, adminPlataforma());
    }

    MvcTestResult reactivar(UUID id) {
        return enviar(HttpMethod.POST, RUTA + "/" + id + "/reactivar", null, adminPlataforma());
    }

    /**
     * Envía una petición a la API.
     *
     * @param datos el cuerpo, que se envía como JSON; {@code null} si no lleva
     * @param usuario quién hace la petición; {@code null} para hacerla sin sesión
     */
    MvcTestResult enviar(HttpMethod metodo, String uri, Object datos, RequestPostProcessor usuario) {
        MockMvcTester.MockMvcRequestBuilder peticion = mvc.method(metodo).uri(uri).with(csrf());
        if (datos != null) {
            peticion.contentType(MediaType.APPLICATION_JSON).content(aJson(datos));
        }
        if (usuario != null) {
            peticion.with(usuario);
        }
        return peticion.exchange();
    }

    static String aJson(Object datos) {
        return JSON.writeValueAsString(datos);
    }

    /** El cuerpo de la respuesta, leído como un objeto JSON. */
    static Map<String, Object> json(MvcTestResult resultado) {
        String cuerpo = new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        return JsonPath.read(cuerpo, "$");
    }

    /** Los errores de la respuesta como pares (campo, mensaje). */
    static List<Tuple> errores(MvcTestResult resultado) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> errores = (List<Map<String, Object>>) json(resultado).get("errores");
        assertThat(errores).as("la respuesta trae la lista «errores»").isNotNull();
        return errores.stream().map(error -> tuple(error.get("campo"), error.get("mensaje"))).toList();
    }

    /** Solo los campos de los errores de la respuesta. */
    static List<Object> campos(MvcTestResult resultado) {
        return errores(resultado).stream().map(error -> error.toList().getFirst()).toList();
    }

    /** Borra lo que guardaron las pruebas de la API, que sí confirman sus transacciones. */
    static void borrarDatos(JdbcTemplate jdbc) {
        jdbc.execute("TRUNCATE lote, rango_recomendacion, usuario, empresa CASCADE");
    }

    static int contar(JdbcTemplate jdbc, String tabla) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
    }
}
