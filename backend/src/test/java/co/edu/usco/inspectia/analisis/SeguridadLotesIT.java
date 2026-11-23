/*
 * Nombre: SeguridadLotesIT.java
 * Descripción: Pruebas de seguridad de la API de lotes (RNF-04, RP-12): sin sesión responde 401; el
 *              administrador de empresa no registra lotes y el de la plataforma no usa la API, con 403.
 * Fecha de creación: 10/11/2026
 * Actualización: 10/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis;

import static co.edu.usco.inspectia.analisis.ApiLotes.ORIGENES;
import static co.edu.usco.inspectia.analisis.ApiLotes.RECEPCION;
import static co.edu.usco.inspectia.analisis.ApiLotes.RUTA;
import static co.edu.usco.inspectia.analisis.ApiLotes.datos;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import java.util.Map;
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
 * Pruebas de seguridad de la API de lotes (RNF-04, RP-12). El usuario final registra los lotes (RF-04);
 * el administrador de empresa solo consulta el historial (RF-14), y el administrador de la plataforma, que
 * no tiene empresa, no usa esta API. Sin sesión responde 401 y con un rol sin permiso, 403, siempre en JSON
 * y sin guardar nada. Las operaciones sobre un lote usan un id que no existe: el permiso se revisa antes
 * de buscarlo.
 */
@PruebaIntegracion
class SeguridadLotesIT {

    private static final String ANA = "admin@cacaos.test";
    private static final String PLATAFORMA = "plataforma@inspectia.test";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiSesion api;

    @BeforeEach
    void preparar() {
        api = new ApiSesion(mvc, jdbc);
        ApiSesion.borrarDatos(jdbc);
        UUID empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        api.crearUsuario(empresa, "Ana Pérez", ANA, "ADMIN_EMPRESA");
        api.crearUsuario(null, "Equipo InspectIA", PLATAFORMA, "ADMIN_PLATAFORMA");
    }

    @AfterEach
    void limpiar() {
        ApiSesion.borrarDatos(jdbc);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("RNF-04 · C1 · sin sesión, cada operación de la API de lotes responde 401 en JSON")
    @MethodSource("operaciones")
    void sinSesionResponde401(String operacion, HttpMethod metodo, String uri, Object datos) {
        MvcTestResult resultado = api.enviar(metodo, uri, datos, null);

        assertThat(resultado).hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(json(resultado)).containsEntry("status", 401);
        assertThat(contarLotes()).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("RNF-04 · C2 · el administrador de la plataforma recibe 403 en cada operación de la API de lotes")
    @MethodSource("operaciones")
    void laPlataformaRecibe403(String operacion, HttpMethod metodo, String uri, Object datos) {
        MvcTestResult resultado = api.enviar(metodo, uri, datos, api.token(PLATAFORMA));

        assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(json(resultado)).containsEntry("status", 403);
        assertThat(contarLotes()).isZero();
    }

    @Test
    @DisplayName("RNF-04 · C2 · el administrador de empresa no registra lotes: recibe 403 y no se guarda nada")
    void elAdministradorDeEmpresaNoRegistra() {
        MvcTestResult resultado = api.enviar(HttpMethod.POST, RUTA,
                datos("L-001", RECEPCION, "Finca La Esperanza", null), api.token(ANA));

        assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(json(resultado)).containsEntry("status", 403);
        assertThat(contarLotes()).isZero();
    }

    @Test
    @DisplayName("RNF-04 · C2 · el permiso se revisa antes que los datos: el administrador de empresa recibe 403 aunque envíe datos inválidos")
    void elPermisoSeRevisaAntesQueLosDatos() {
        MvcTestResult resultado = api.enviar(HttpMethod.POST, RUTA, Map.of(), api.token(ANA));

        assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN);
    }

    /** Las 4 operaciones de la API de lotes, con datos válidos cuando llevan cuerpo. */
    static Stream<Arguments> operaciones() {
        return Stream.of(
                Arguments.of("historial", HttpMethod.GET, RUTA, null),
                Arguments.of("detalle", HttpMethod.GET, RUTA + "/" + UUID.randomUUID(), null),
                Arguments.of("registrar", HttpMethod.POST, RUTA, datos("L-001", RECEPCION, "Finca La Esperanza", null)),
                Arguments.of("sugerencias de finca o proveedor", HttpMethod.GET, ORIGENES + "?texto=finca", null));
    }

    private int contarLotes() {
        return jdbc.queryForObject("SELECT count(*) FROM lote", Integer.class);
    }
}
