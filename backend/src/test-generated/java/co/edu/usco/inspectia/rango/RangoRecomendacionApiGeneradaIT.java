/*
 * Nombre: RangoRecomendacionApiGeneradaIT.java
 * Descripción: Pruebas generadas de la API de rango_recomendacion, que se edita en bloque: permisos,
 *              validaciones, filas completas y empresas.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.8)
 * Versión: 1.8
 * Entidades: rango_recomendacion 1.2
 */
package co.edu.usco.inspectia.rango;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.comun.seguridad.SesionAutenticada;
import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
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
 * Pruebas generadas de la API de {@code rango_recomendacion}, que se edita en bloque (RP-13, RP-23): cada permiso
 * de {@code operaciones} y cada validación del JSON tiene un caso, además de las filas completas y la
 * separación por empresa (RNF-03). Las pruebas de los criterios de las fichas van aparte, escritas a mano.
 *
 * <p>Las filas válidas son las de {@code valores_iniciales}, una por cada categoría: cumplen las
 * validaciones del JSON y también las reglas que se programan a mano. Las empresas de prueba se crean
 * directamente en la base, sin filas: al editar se crean las que falten. Las pruebas confirman sus
 * transacciones y vacían las tablas antes y después de cada una.
 */
@PruebaIntegracion
class RangoRecomendacionApiGeneradaIT {

    private static final String RUTA = "/api/v1/rangos-recomendacion";
    private static final String RESTAURAR = RUTA + "/restaurar-por-defecto";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    /** La empresa de la sesión de las pruebas. */
    private UUID empresa;

    /** Otra empresa, para revisar que sus filas no se mezclen con las de la sesión (RNF-03). */
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
    @DisplayName("RNF-04 · C2 · cada operación de rango_recomendacion solo la usan los roles de su JSON")
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
                Arguments.of("editar", "USUARIO_FINAL", false),
                Arguments.of("editar", "ADMIN_EMPRESA", true),
                Arguments.of("editar", "ADMIN_PLATAFORMA", false),
                Arguments.of("restaurar_por_defecto", "USUARIO_FINAL", false),
                Arguments.of("restaurar_por_defecto", "ADMIN_EMPRESA", true),
                Arguments.of("restaurar_por_defecto", "ADMIN_PLATAFORMA", false));
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("RNF-04 · C1 · sin sesión, cada operación de rango_recomendacion responde 401")
    @MethodSource("operaciones")
    void sinSesion(String operacion) {
        assertThat(enviar(operacion, null)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    static Stream<String> operaciones() {
        return Stream.of("listar", "editar", "restaurar_por_defecto");
    }

    @ParameterizedTest(name = "{0}: {2}")
    @DisplayName("RF-13 · cada validación del JSON de rango_recomendacion responde 400 con su mensaje en el campo de la fila y no guarda nada")
    @MethodSource("validaciones")
    void validaciones(String campo, Object valor, String mensaje) {
        List<Map<String, Object>> filas = filasValidas();
        filas.getFirst().put(campo, valor);

        MvcTestResult resultado = editar(filas, empresa);

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).contains(tuple("filas[0]." + campo, mensaje));
        assertThat(filasDe(empresa)).isZero();
    }

    static Stream<Arguments> validaciones() {
        return Stream.of(
                Arguments.of("categoria", null, "Este dato es obligatorio."),
                Arguments.of("limiteInferior", null, "Este dato es obligatorio."),
                Arguments.of("limiteInferior", -1, "Debe ser mayor o igual a 0."),
                Arguments.of("limiteInferior", 101, "Debe ser menor o igual a 100."),
                Arguments.of("limiteInferior", 0.05, "Usa como máximo 1 decimal."),
                Arguments.of("recomendacion", null, "Este dato es obligatorio."),
                Arguments.of("recomendacion", "x".repeat(301), "Puede tener como máximo 300 caracteres."));
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("RF-13 · rango_recomendacion se edita con una fila por cada categoría: con una de menos, una de más o una repetida responde 400 en filas y no guarda nada")
    @MethodSource("filasIncompletas")
    void filasIncompletas(String caso, List<Map<String, Object>> filas) {
        MvcTestResult resultado = editar(filas, empresa);

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(tuple("filas", "Envía una fila por cada categoría, sin repetir: Premium o Especial, Estándar, Corriente y Fuera de norma."));
        assertThat(filasDe(empresa)).isZero();
    }

    static Stream<Arguments> filasIncompletas() {
        List<Map<String, Object>> deMenos = filasValidas();
        deMenos.removeLast();
        List<Map<String, Object>> deMas = filasValidas();
        deMas.add(filasValidas().getFirst());
        List<Map<String, Object>> repetida = filasValidas();
        repetida.set(repetida.size() - 1, filasValidas().getFirst());
        return Stream.of(Arguments.of("una de menos", deMenos), Arguments.of("una de más", deMas),
                Arguments.of("una repetida", repetida));
    }

    @Test
    @DisplayName("RP-03 · un código de categoría que no existe responde 400 en el campo de su fila")
    void codigoQueNoExiste() {
        List<Map<String, Object>> filas = filasValidas();
        filas.getLast().put("categoria", "NO_EXISTE");

        MvcTestResult resultado = editar(filas, empresa);

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(tuple("filas[3].categoria", "El valor no es válido."));
    }

    @Test
    @DisplayName("RF-13 · limiteInferior no cambia en la fila de FUERA_DE_NORMA: otro valor responde 400 en su campo y no guarda nada")
    void soloLectura1() {
        List<Map<String, Object>> filas = filasValidas();
        filas.get(3).put("limiteInferior", 0.1);

        MvcTestResult resultado = editar(filas, empresa);

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(tuple("filas[3].limiteInferior", "El límite de Fuera de norma es fijo: 0 %."));
        assertThat(filasDe(empresa)).isZero();
    }

    @Test
    @DisplayName("RF-13 · rango_recomendacion guarda sin espacios al inicio ni al final los textos que su JSON pide recortar")
    void recortaLosTextos() {
        List<Map<String, Object>> filas = filasValidas();
        filas.getFirst().put("recomendacion", "  " + "Apto para mercado especial o de exportación." + "  ");

        MvcTestResult resultado = editar(filas, empresa);

        assertThat(resultado).hasStatus(HttpStatus.OK);
        assertThat(lista(resultado).getFirst()).containsEntry("recomendacion", "Apto para mercado especial o de exportación.");
    }

    @Test
    @DisplayName("RF-13 · restaurar deja a la empresa una fila por cada categoría, con los valores por defecto del JSON")
    void restaurarDejaLosValoresPorDefecto() {
        MvcTestResult resultado = enviar(HttpMethod.POST, RESTAURAR, null, conPermiso("restaurar_por_defecto", empresa));

        assertThat(resultado).hasStatus(HttpStatus.OK);
        List<Map<String, Object>> filas = lista(resultado);
        assertThat(filas).hasSize(4);
        assertThat(filas.get(0))
                .containsEntry("categoria", "PREMIUM")
                .containsEntry("limiteInferior", 96.0)
                .containsEntry("recomendacion", "Apto para mercado especial o de exportación.");
        assertThat(filas.get(1))
                .containsEntry("categoria", "ESTANDAR")
                .containsEntry("limiteInferior", 90.0)
                .containsEntry("recomendacion", "Apto para venta estándar. Retirar a mano los granos defectuosos podría subirlo de categoría.");
        assertThat(filas.get(2))
                .containsEntry("categoria", "CORRIENTE")
                .containsEntry("limiteInferior", 82.0)
                .containsEntry("recomendacion", "Retirar los granos defectuosos antes de vender, y revisar la fermentación y el secado.");
        assertThat(filas.get(3))
                .containsEntry("categoria", "FUERA_DE_NORMA")
                .containsEntry("limiteInferior", 0.0)
                .containsEntry("recomendacion", "No venderlo como cacao de calidad: hacer una selección completa y revisar todo el proceso después de la cosecha.");
        assertThat(filasDe(empresa)).isEqualTo(4);
    }

    @Test
    @DisplayName("RNF-03 · C2 · el listado de rango_recomendacion solo trae las filas de la empresa de la sesión")
    void listadoDeLaEmpresa() {
        assertThat(editar(filasValidas(), empresa)).hasStatus(HttpStatus.OK);
        assertThat(editar(filasValidas(), otraEmpresa)).hasStatus(HttpStatus.OK);

        MvcTestResult resultado = enviar(HttpMethod.GET, RUTA, null, conPermiso("listar", empresa));

        assertThat(resultado).hasStatus(HttpStatus.OK);
        assertThat(lista(resultado)).extracting(fila -> UUID.fromString((String) fila.get("id")))
                .containsExactlyInAnyOrderElementsOf(idsDe(empresa));
    }

    @Test
    @DisplayName("RNF-03 · editar y restaurar las filas de rango_recomendacion no toca las de otra empresa")
    void noTocaLasDeOtraEmpresa() {
        assertThat(editar(filasValidas(), otraEmpresa)).hasStatus(HttpStatus.OK);
        List<Map<String, Object>> antes = filasEnLaBase(otraEmpresa);

        assertThat(editar(filasValidas(), empresa)).hasStatus(HttpStatus.OK);
        assertThat(enviar(HttpMethod.POST, RESTAURAR, null, conPermiso("restaurar_por_defecto", empresa)))
                .hasStatus(HttpStatus.OK);

        assertThat(filasEnLaBase(otraEmpresa)).isEqualTo(antes);
        assertThat(filasDe(empresa)).isEqualTo(4);
    }

    private void vaciar() {
        jdbc.execute("TRUNCATE rango_recomendacion, empresa CASCADE");
    }

    /** Una empresa con los ejemplos de su JSON, creada directamente en la base: no tiene filas de rango_recomendacion. */
    private UUID crearEmpresa(Object... valores) {
        return jdbc.queryForObject("INSERT INTO empresa (nombre, nit, ciudad) VALUES (?, ?, ?) RETURNING id",
                UUID.class, valores);
    }

    /** Una fila por cada categoría, con los datos de {@code valores_iniciales}, en el orden de la lista. */
    private static List<Map<String, Object>> filasValidas() {
        List<Map<String, Object>> filas = new ArrayList<>();
        filas.add(fila("PREMIUM", 96.0, "Apto para mercado especial o de exportación."));
        filas.add(fila("ESTANDAR", 90.0, "Apto para venta estándar. Retirar a mano los granos defectuosos podría subirlo de categoría."));
        filas.add(fila("CORRIENTE", 82.0, "Retirar los granos defectuosos antes de vender, y revisar la fermentación y el secado."));
        filas.add(fila("FUERA_DE_NORMA", 0.0, "No venderlo como cacao de calidad: hacer una selección completa y revisar todo el proceso después de la cosecha."));
        return filas;
    }

    private static Map<String, Object> fila(Object categoria, Object limiteInferior, Object recomendacion) {
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("categoria", categoria);
        fila.put("limiteInferior", limiteInferior);
        fila.put("recomendacion", recomendacion);
        return fila;
    }

    /** Edita las filas con una sesión de la empresa dada. */
    private MvcTestResult editar(List<Map<String, Object>> filas, UUID suEmpresa) {
        return enviar(HttpMethod.PUT, RUTA, Map.of("filas", filas), conPermiso("editar", suEmpresa));
    }

    /** Envía la operación con datos válidos. */
    private MvcTestResult enviar(String operacion, RequestPostProcessor usuario) {
        return switch (operacion) {
            case "listar" -> enviar(HttpMethod.GET, RUTA, null, usuario);
            case "editar" -> enviar(HttpMethod.PUT, RUTA, Map.of("filas", filasValidas()), usuario);
            case "restaurar_por_defecto" -> enviar(HttpMethod.POST, RESTAURAR, null, usuario);
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

    /** Una sesión en la empresa dada, con el primer rol que puede usar la operación. */
    private static RequestPostProcessor conPermiso(String operacion, UUID empresa) {
        String rol = switch (operacion) {
            case "listar" -> "ADMIN_EMPRESA";
            case "editar" -> "ADMIN_EMPRESA";
            case "restaurar_por_defecto" -> "ADMIN_EMPRESA";
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

    private int filasDe(UUID suEmpresa) {
        return jdbc.queryForObject("SELECT count(*) FROM rango_recomendacion WHERE empresa_id = ?", Integer.class, suEmpresa);
    }

    private List<UUID> idsDe(UUID suEmpresa) {
        return jdbc.queryForList("SELECT id FROM rango_recomendacion WHERE empresa_id = ?", UUID.class, suEmpresa);
    }

    /** Las filas de una empresa tal como están en la base, con sus fechas. */
    private List<Map<String, Object>> filasEnLaBase(UUID suEmpresa) {
        return jdbc.queryForList("SELECT * FROM rango_recomendacion WHERE empresa_id = ? ORDER BY id", suEmpresa);
    }

    private static List<Map<String, Object>> lista(MvcTestResult resultado) {
        return JsonPath.read(cuerpo(resultado), "$");
    }

    private static List<Tuple> errores(MvcTestResult resultado) {
        List<Map<String, Object>> errores = JsonPath.read(cuerpo(resultado), "$.errores");
        assertThat(errores).as("la respuesta trae la lista «errores»").isNotNull();
        return errores.stream().map(error -> tuple(error.get("campo"), error.get("mensaje"))).toList();
    }

    private static String cuerpo(MvcTestResult resultado) {
        return new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }
}
