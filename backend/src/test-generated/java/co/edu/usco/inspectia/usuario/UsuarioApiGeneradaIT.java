/*
 * Nombre: UsuarioApiGeneradaIT.java
 * Descripción: Pruebas generadas de la API de usuario: permisos, validaciones y datos únicos.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.11)
 * Versión: 1.11
 * Entidades: usuario 1.8
 */
package co.edu.usco.inspectia.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.comun.api.Pagina;
import co.edu.usco.inspectia.comun.seguridad.SesionAutenticada;
import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
 * Pruebas generadas de la API de {@code usuario} (RP-13, RP-23): cada permiso de {@code operaciones}
 * y cada validación del JSON tiene un caso, además de los datos únicos. Las pruebas de los criterios de
 * las fichas van aparte, escritas a mano.
 *
 * <p>Los datos válidos salen de {@code ejemplo} de cada atributo. Las pruebas confirman sus transacciones
 * y vacían las tablas antes y después de cada una.
 *
 * <p>{@code usuario} es por empresa: las pruebas usan una sesión como la del inicio de sesión, con
 * una empresa de prueba, y revisan que un registro de otra empresa no exista para ella (RNF-03).
 */
@PruebaIntegracion
class UsuarioApiGeneradaIT {

    private static final String RUTA = "/api/v1/usuarios";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    /** La empresa de la sesión de las pruebas. */
    private UUID empresa;

    /** Otra empresa, para revisar que sus datos no se mezclen con los de la sesión (RNF-03). */
    private UUID otraEmpresa;

    @BeforeEach
    void vaciarAntes() {
        vaciar();
        empresa = crearEmpresa("Cacaos del Huila", "900123456-8", "Neiva");
        otraEmpresa = crearEmpresa("Cacaos del Huila", "011234567-9", "Neiva");
    }

    @AfterEach
    void vaciarDespues() {
        vaciar();
    }

    @ParameterizedTest(name = "{0} · {1}")
    @DisplayName("RNF-04 · C2 · cada operación de usuario solo la usan los roles de su JSON")
    @MethodSource("permisos")
    void permisos(String operacion, String rol, boolean permitido) {
        MvcTestResult resultado = enviar(operacion, sesion(rol, empresa));

        if (permitido) {
            assertThat(resultado.getResponse().getStatus()).isNotIn(401, 403);
        } else {
            assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    static Stream<Arguments> permisos() {
        return Stream.of(
                Arguments.of("listar", "USUARIO_FINAL", false),
                Arguments.of("listar", "ADMIN_EMPRESA", true),
                Arguments.of("listar", "ADMIN_PLATAFORMA", false),
                Arguments.of("ver", "USUARIO_FINAL", false),
                Arguments.of("ver", "ADMIN_EMPRESA", true),
                Arguments.of("ver", "ADMIN_PLATAFORMA", false),
                Arguments.of("crear", "USUARIO_FINAL", false),
                Arguments.of("crear", "ADMIN_EMPRESA", true),
                Arguments.of("crear", "ADMIN_PLATAFORMA", false),
                Arguments.of("editar", "USUARIO_FINAL", false),
                Arguments.of("editar", "ADMIN_EMPRESA", true),
                Arguments.of("editar", "ADMIN_PLATAFORMA", false),
                Arguments.of("desactivar", "USUARIO_FINAL", false),
                Arguments.of("desactivar", "ADMIN_EMPRESA", true),
                Arguments.of("desactivar", "ADMIN_PLATAFORMA", false),
                Arguments.of("reactivar", "USUARIO_FINAL", false),
                Arguments.of("reactivar", "ADMIN_EMPRESA", true),
                Arguments.of("reactivar", "ADMIN_PLATAFORMA", false));
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("RNF-04 · C1 · sin sesión, cada operación de usuario responde 401")
    @MethodSource("operaciones")
    void sinSesion(String operacion) {
        assertThat(enviar(operacion, null)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    static Stream<String> operaciones() {
        return Stream.of("listar", "ver", "crear", "editar", "desactivar", "reactivar");
    }

    @ParameterizedTest(name = "{0}: {2}")
    @DisplayName("RF-03 · cada validación del JSON de usuario responde 400 con su mensaje al crear")
    @MethodSource("validacionesAlCrear")
    void validacionesAlCrear(String campo, Object valor, String mensaje) {
        Map<String, Object> datos = datosParaCrear();
        poner(datos, campo, valor);

        MvcTestResult resultado = enviar(HttpMethod.POST, RUTA, datos, conPermiso("crear"));

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).contains(tuple(campo, mensaje));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario", Integer.class)).isZero();
    }

    static Stream<Arguments> validacionesAlCrear() {
        return Stream.of(
                Arguments.of("nombre", null, "Este dato es obligatorio."),
                Arguments.of("nombre", "x".repeat(121), "Puede tener como máximo 120 caracteres."),
                Arguments.of("correo", null, "Este dato es obligatorio."),
                Arguments.of("correo", correoDeLargo(255), "Puede tener como máximo 254 caracteres."),
                Arguments.of("correo", "sin-arroba", "Escribe un correo válido."),
                Arguments.of("contrasena", null, "Este dato es obligatorio."),
                Arguments.of("contrasena", " ".repeat(8), "Este dato es obligatorio."),
                Arguments.of("contrasena", "x".repeat(65), "La contraseña puede tener como máximo 64 caracteres."),
                Arguments.of("contrasena", "x".repeat(7), "La contraseña debe tener al menos 8 caracteres."),
                Arguments.of("contrasena", "ñ".repeat(37), "La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes"),
                Arguments.of("contrasena", "ñ".repeat(65), "La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes"),
                Arguments.of("rol", null, "Este dato es obligatorio."));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("RF-03 · cada validación del JSON de usuario acepta su valor del borde al crear")
    @MethodSource("bordesAlCrear")
    void bordesAlCrear(String campo, Object valor) {
        Map<String, Object> datos = datosParaCrear();
        poner(datos, campo, valor);

        MvcTestResult resultado = enviar(HttpMethod.POST, RUTA, datos, conPermiso("crear"));

        assertThat(resultado).hasStatus(HttpStatus.CREATED);
    }

    static Stream<Arguments> bordesAlCrear() {
        return Stream.of(
                Arguments.of("nombre", "x".repeat(120)),
                Arguments.of("correo", correoDeLargo(254)),
                Arguments.of("contrasena", "x".repeat(64)),
                Arguments.of("contrasena", "x".repeat(8)),
                Arguments.of("contrasena", "ñ".repeat(36)));
    }

    @ParameterizedTest(name = "{0}: {2}")
    @DisplayName("RF-03 · cada validación del JSON de usuario responde 400 con su mensaje al editar")
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
                Arguments.of("correo", null, "Este dato es obligatorio."),
                Arguments.of("correo", correoDeLargo(255), "Puede tener como máximo 254 caracteres."),
                Arguments.of("correo", "sin-arroba", "Escribe un correo válido."),
                Arguments.of("contrasena", " ".repeat(8), "Este dato es obligatorio."),
                Arguments.of("contrasena", "x".repeat(65), "La contraseña puede tener como máximo 64 caracteres."),
                Arguments.of("contrasena", "x".repeat(7), "La contraseña debe tener al menos 8 caracteres."),
                Arguments.of("contrasena", "ñ".repeat(37), "La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes"),
                Arguments.of("contrasena", "ñ".repeat(65), "La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes"),
                Arguments.of("rol", null, "Este dato es obligatorio."));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("RF-03 · cada validación del JSON de usuario acepta su valor del borde al editar")
    @MethodSource("bordesAlEditar")
    void bordesAlEditar(String campo, Object valor) {
        UUID id = crear();
        Map<String, Object> datos = datosParaEditar();
        poner(datos, campo, valor);

        MvcTestResult resultado = enviar(HttpMethod.PUT, RUTA + "/" + id, datos, conPermiso("editar"));

        assertThat(resultado).hasStatus(HttpStatus.OK);
    }

    static Stream<Arguments> bordesAlEditar() {
        return Stream.of(
                Arguments.of("nombre", "x".repeat(120)),
                Arguments.of("correo", correoDeLargo(254)),
                Arguments.of("contrasena", "x".repeat(64)),
                Arguments.of("contrasena", "x".repeat(8)),
                Arguments.of("contrasena", "ñ".repeat(36)));
    }

    @ParameterizedTest(name = "{0}: {1}")
    @DisplayName("RNF-04 · un valor de una lista de usuario que existe pero no se puede asignar responde 403 y no guarda nada")
    @MethodSource("valoresNoPermitidos")
    void valoresNoPermitidos(String campo, Object valor, String mensaje) {
        Map<String, Object> datos = datosParaCrear();
        poner(datos, campo, valor);

        MvcTestResult resultado = enviar(HttpMethod.POST, RUTA, datos, conPermiso("crear"));

        assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(errores(resultado)).containsExactly(tuple(campo, mensaje));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario", Integer.class)).isZero();
    }

    static Stream<Arguments> valoresNoPermitidos() {
        return Stream.of(
                Arguments.of("rol", "ADMIN_PLATAFORMA", "Solo puedes asignar los roles Usuario final o Administrador de empresa."));
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("RP-03 · un código que no existe en una lista de usuario responde 400 con su campo")
    @MethodSource("listas")
    void codigoQueNoExiste(String campo) {
        Map<String, Object> datos = datosParaCrear();
        poner(datos, campo, "NO_EXISTE");

        MvcTestResult resultado = enviar(HttpMethod.POST, RUTA, datos, conPermiso("crear"));

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(tuple(campo, "El valor no es válido."));
    }

    static Stream<String> listas() {
        return Stream.of("rol");
    }

    @Test
    @DisplayName("RF-03 · los datos únicos del JSON de usuario no se repiten: responde 409")
    void datosUnicos() {
        crear();

        MvcTestResult resultado = enviar(HttpMethod.POST, RUTA, datosParaCrear(), conPermiso("crear"));

        assertThat(resultado).hasStatus(HttpStatus.CONFLICT);
        assertThat(errores(resultado)).contains(
                tuple("correo", "Ya existe un usuario con este correo."));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("RF-03 · usuario guarda sin espacios al inicio ni al final los textos que su JSON pide recortar")
    void recortaLosTextos() {
        Map<String, Object> datos = datosParaCrear();
        datos.put("nombre", "  " + "Ana Pérez" + "  ");
        datos.put("correo", "  " + "ana.perez@example.com" + "  ");

        MvcTestResult resultado = enviar(HttpMethod.POST, RUTA, datos, conPermiso("crear"));

        assertThat(resultado).hasStatus(HttpStatus.CREATED);
        assertThat(json(resultado)).containsEntry("nombre", "Ana Pérez");
        assertThat(json(resultado)).containsEntry("correo", "ana.perez@example.com");
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("RNF-03 · un registro de usuario de otra empresa responde 404 en cada operación que lo usa")
    @MethodSource("operacionesConRegistro")
    void deOtraEmpresaNoExiste(String operacion) {
        UUID id = crear();

        MvcTestResult resultado = enviar(operacion, id, conPermiso(operacion, otraEmpresa));

        assertThat(resultado).hasStatus(HttpStatus.NOT_FOUND);
    }

    static Stream<String> operacionesConRegistro() {
        return Stream.of("ver", "editar", "desactivar", "reactivar");
    }

    @Test
    @DisplayName("RNF-03 · C2 · el listado de usuario solo trae los registros de la empresa de la sesión")
    void listadoDeLaEmpresa() {
        UUID id = crear();

        MvcTestResult deLaEmpresa = enviar(HttpMethod.GET, RUTA, null, conPermiso("listar"));
        MvcTestResult deOtra = enviar(HttpMethod.GET, RUTA, null, conPermiso("listar", otraEmpresa));

        assertThat(identificadores(deLaEmpresa)).containsExactly(id.toString());
        assertThat(identificadores(deOtra)).isEmpty();
    }

    @Test
    @DisplayName("RF-03 · el listado de usuario filtra por activo: con true trae solo los activos y con false, los desactivados")
    void listadoFiltraPorActivo() {
        String id = crear().toString();

        assertThat(identificadores(listarConActivo("true"))).containsExactly(id);
        assertThat(identificadores(listarConActivo("false"))).isEmpty();

        assertThat(enviar(HttpMethod.POST, RUTA + "/" + id + "/desactivar", null, conPermiso("desactivar")))
                .hasStatus(HttpStatus.OK);

        assertThat(identificadores(listarConActivo("true"))).isEmpty();
        assertThat(identificadores(listarConActivo("false"))).containsExactly(id);
        assertThat(identificadores(enviar(HttpMethod.GET, RUTA, null, conPermiso("listar")))).containsExactly(id);
    }

    @Test
    @DisplayName("RF-03 · las páginas del listado de usuario van de 0 a la más alta que la consulta puede pedir y traen de 1 a 100 filas; fuera de esos límites responde 400 y no un error del servidor (prueba de límite)")
    void limitesDeLaPagina() {
        String ultima = "?pagina=" + Pagina.PAGINA_MAXIMA + "&tamano=" + Pagina.TAMANO_MAXIMO;
        MvcTestResult negativa = enviar(HttpMethod.GET, RUTA + "?pagina=-1", null, conPermiso("listar"));
        MvcTestResult laUltima = enviar(HttpMethod.GET, RUTA + ultima, null, conPermiso("listar"));
        MvcTestResult pasada = enviar(HttpMethod.GET, RUTA + "?pagina=" + (Pagina.PAGINA_MAXIMA + 1), null,
                conPermiso("listar"));
        MvcTestResult sinFilas = enviar(HttpMethod.GET, RUTA + "?tamano=0", null, conPermiso("listar"));
        MvcTestResult grande = enviar(HttpMethod.GET, RUTA + "?tamano=" + (Pagina.TAMANO_MAXIMO + 1), null,
                conPermiso("listar"));

        assertThat(negativa).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(negativa)).containsExactly(tuple("pagina", "Debe ser mayor o igual a 0."));
        assertThat(laUltima).hasStatus(HttpStatus.OK);
        assertThat(pasada).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(pasada)).containsExactly(
                tuple("pagina", "Debe ser menor o igual a " + Pagina.PAGINA_MAXIMA + "."));
        assertThat(sinFilas).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(sinFilas)).containsExactly(tuple("tamano", "Debe ser mayor o igual a 1."));
        assertThat(grande).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(grande)).containsExactly(
                tuple("tamano", "Debe ser menor o igual a " + Pagina.TAMANO_MAXIMO + "."));
    }

    @Test
    @DisplayName("RNF-03 · el registro nuevo de usuario queda en la empresa de la sesión")
    void quedaEnLaEmpresaDeLaSesion() {
        UUID id = crear();

        assertThat(jdbc.queryForObject("SELECT empresa_id FROM usuario WHERE id = ?", UUID.class, id))
                .isEqualTo(empresa);
    }

    private void vaciar() {
        jdbc.execute("TRUNCATE usuario, empresa CASCADE");
    }

    /** Una empresa con los ejemplos de su JSON, creada directamente en la base. */
    private UUID crearEmpresa(Object... valores) {
        return jdbc.queryForObject("INSERT INTO empresa (nombre, nit, ciudad) VALUES (?, ?, ?) RETURNING id",
                UUID.class, valores);
    }

    private static Map<String, Object> datosParaCrear() {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", "Ana Pérez");
        datos.put("correo", "ana.perez@example.com");
        datos.put("contrasena", "Prueba-Segura-2026");
        datos.put("rol", "USUARIO_FINAL");
        return datos;
    }

    private UUID crear() {
        MvcTestResult resultado = enviar(HttpMethod.POST, RUTA, datosParaCrear(), conPermiso("crear"));
        assertThat(resultado).hasStatus(HttpStatus.CREATED);
        return UUID.fromString((String) json(resultado).get("id"));
    }

    private static Map<String, Object> datosParaEditar() {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", "Ana Pérez");
        datos.put("correo", "ana.perez@example.com");
        datos.put("contrasena", "Prueba-Segura-2026");
        datos.put("rol", "USUARIO_FINAL");
        return datos;
    }

    /** Envía la operación con datos válidos; las que llevan un registro usan uno que no existe. */
    private MvcTestResult enviar(String operacion, RequestPostProcessor usuario) {
        return enviar(operacion, UUID.randomUUID(), usuario);
    }

    /** Envía la operación con datos válidos sobre el registro {@code id}. */
    private MvcTestResult enviar(String operacion, UUID id, RequestPostProcessor usuario) {
        String registro = RUTA + "/" + id;
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

    /** Una sesión en la empresa de prueba, con el primer rol que puede usar la operación. */
    private RequestPostProcessor conPermiso(String operacion) {
        return conPermiso(operacion, empresa);
    }

    /** Una sesión en la empresa dada, con el primer rol que puede usar la operación. */
    private static RequestPostProcessor conPermiso(String operacion, UUID empresa) {
        String rol = switch (operacion) {
            case "listar" -> "ADMIN_EMPRESA";
            case "ver" -> "ADMIN_EMPRESA";
            case "crear" -> "ADMIN_EMPRESA";
            case "editar" -> "ADMIN_EMPRESA";
            case "desactivar" -> "ADMIN_EMPRESA";
            case "reactivar" -> "ADMIN_EMPRESA";
            default -> throw new IllegalArgumentException(operacion);
        };
        return sesion(rol, empresa);
    }

    /**
     * Una sesión como la que arma el inicio de sesión (D-28): un usuario del rol en la empresa dada. El
     * administrador de la plataforma no tiene empresa.
     */
    private static RequestPostProcessor sesion(String rol, UUID empresa) {
        UUID suEmpresa = "ADMIN_PLATAFORMA".equals(rol) ? null : empresa;
        return authentication(new SesionAutenticada(new UsuarioEnSesion(UUID.randomUUID(), "Usuario de prueba",
                "prueba@inspectia.test", rol, suEmpresa, suEmpresa == null ? null : "Empresa de prueba",
                Instant.now().plusSeconds(3600))));
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

    /** Los identificadores de los registros de una página del listado. */
    private static List<String> identificadores(MvcTestResult resultado) {
        return JsonPath.read(new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8),
                "$.contenido[*].id");
    }

    /** El listado con el filtro {@code ?activo=} (D-33). */
    private MvcTestResult listarConActivo(String activo) {
        return enviar(HttpMethod.GET, RUTA + "?activo=" + activo, null, conPermiso("listar"));
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
