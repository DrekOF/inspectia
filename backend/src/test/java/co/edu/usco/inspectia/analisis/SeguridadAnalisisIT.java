/*
 * Nombre: SeguridadAnalisisIT.java
 * Descripción: Pruebas de seguridad de la API del análisis (RNF-04, RP-12): sin sesión responde 401; la administradora
 *              de empresa no envía a analizar (403) pero sí ve el avance y el resultado; el administrador de la
 *              plataforma recibe 403 en todo.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis;

import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.ConfiguracionContenedores;
import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import java.util.UUID;
import java.util.stream.Stream;
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

/**
 * Pruebas de seguridad de la API del análisis (RNF-04, RP-12). Solo el usuario final envía a analizar; él y la
 * administradora de empresa ven el avance y el resultado; el administrador de la plataforma, que no tiene empresa, no
 * usa estas rutas. Sin sesión responde 401 y con un rol sin permiso, 403, siempre en JSON y sin cambiar nada.
 */
@PruebaIntegracion
class SeguridadAnalisisIT {

    private static final String LUIS = "luis@cacaos.test";
    private static final String ANA = "admin@cacaos.test";
    private static final String PLATAFORMA = "plataforma@inspectia.test";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiSesion api;
    private ApiAnalisis analisis;
    private UUID lote;

    @BeforeEach
    void preparar() {
        api = new ApiSesion(mvc, jdbc);
        analisis = new ApiAnalisis(api, new ApiFotos(api, mvc));
        ApiSesion.borrarDatos(jdbc);
        UUID empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");
        api.crearUsuario(empresa, "Ana Pérez", ANA, "ADMIN_EMPRESA");
        api.crearUsuario(null, "Equipo InspectIA", PLATAFORMA, "ADMIN_PLATAFORMA");
        String tokenDeLuis = api.token(LUIS);
        lote = new ApiLotes(api).registrarOk(tokenDeLuis, "L-001");
        analisis.subirFotos(tokenDeLuis, lote, new int[] {100, 100}, new int[] {100, 100});
    }

    @AfterEach
    void limpiar() {
        ApiSesion.borrarDatos(jdbc);
        ConfiguracionContenedores.vaciarCarpetaDeFotos();
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("RNF-04 · C1 · sin sesión, cada operación de la API del análisis responde 401 en JSON")
    @MethodSource("operaciones")
    void sinSesionResponde401(String operacion, HttpMethod metodo, String ruta) {
        MvcTestResult resultado = api.enviar(metodo, ruta.replace("{id}", lote.toString()), null, null);

        assertThat(resultado).hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(json(resultado)).containsEntry("status", 401);
    }

    @Test
    @DisplayName("RNF-04 · C2 · la administradora de empresa no envía a analizar: recibe 403 y la muestra sigue pendiente")
    void laAdministradoraNoEnvia() {
        MvcTestResult resultado = analisis.enviar(api.token(ANA), lote);

        assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(jdbc.queryForObject("SELECT estado FROM muestra WHERE lote_id = ?", String.class, lote))
                .isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("RNF-04 · C2 · el permiso se revisa antes de buscar el lote: la administradora recibe 403 aunque el lote no exista")
    void elPermisoSeRevisaAntes() {
        assertThat(analisis.enviar(api.token(ANA), UUID.randomUUID())).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("RNF-04 · la administradora de empresa sí ve el avance y el resultado de los lotes de su empresa")
    void laAdministradoraVe() {
        String token = api.token(ANA);

        assertThat(analisis.avance(token, lote)).hasStatus(HttpStatus.OK);
        assertThat(analisis.resultado(token, lote)).hasStatus(HttpStatus.OK);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("RNF-04 · C2 · el administrador de la plataforma recibe 403 en cada operación de la API del análisis")
    @MethodSource("operaciones")
    void laPlataformaRecibe403(String operacion, HttpMethod metodo, String ruta) {
        MvcTestResult resultado = api.enviar(metodo, ruta.replace("{id}", lote.toString()), null,
                api.token(PLATAFORMA));

        assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(json(resultado)).containsEntry("status", 403);
        assertThat(jdbc.queryForObject("SELECT estado FROM muestra WHERE lote_id = ?", String.class, lote))
                .isEqualTo("PENDIENTE");
    }

    /** Las 3 operaciones de la API del análisis; {@code {id}} es el lote. */
    static Stream<Arguments> operaciones() {
        String lote = ApiLotes.RUTA + "/{id}";
        return Stream.of(
                Arguments.of("analizar la muestra", HttpMethod.POST, lote + "/analisis"),
                Arguments.of("ver el avance", HttpMethod.GET, lote + "/analisis"),
                Arguments.of("ver el resultado", HttpMethod.GET, lote + "/resultado"));
    }
}
