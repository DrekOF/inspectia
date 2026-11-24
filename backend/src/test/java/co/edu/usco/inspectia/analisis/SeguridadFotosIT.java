/*
 * Nombre: SeguridadFotosIT.java
 * Descripción: Pruebas de seguridad de la API de las fotos de un lote (RNF-04, RP-12): sin sesión responde 401, y
 *              la administradora de empresa y el administrador de la plataforma, que no suben fotos, 403.
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis;

import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.ConfiguracionContenedores;
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
 * Pruebas de seguridad de la API de las fotos (RNF-04, RP-12). Solo el usuario final sube fotos (RF-05) y retoma
 * un lote pendiente (RF-14, restricción 4). Sin sesión responde 401 y con otro rol, 403, siempre en JSON y sin
 * guardar nada. Las operaciones usan un lote que no existe: el permiso se revisa antes de buscarlo.
 */
@PruebaIntegracion
class SeguridadFotosIT {

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
        ConfiguracionContenedores.vaciarCarpetaDeFotos();
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("RNF-04 · C1 · sin sesión, cada operación de la API de las fotos responde 401 en JSON")
    @MethodSource("operaciones")
    void sinSesionResponde401(String operacion, HttpMethod metodo, String uri, Object datos) {
        MvcTestResult resultado = api.enviar(metodo, uri, datos, null);

        assertThat(resultado).hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(json(resultado)).containsEntry("status", 401);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("RNF-04 · C2 · la administradora de empresa recibe 403 en cada operación de la API de las fotos: no sube fotos ni retoma lotes")
    @MethodSource("operaciones")
    void laAdministradoraRecibe403(String operacion, HttpMethod metodo, String uri, Object datos) {
        MvcTestResult resultado = api.enviar(metodo, uri, datos, api.token(ANA));

        assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(json(resultado)).containsEntry("status", 403);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("RNF-04 · C2 · el administrador de la plataforma recibe 403 en cada operación de la API de las fotos")
    @MethodSource("operaciones")
    void laPlataformaRecibe403(String operacion, HttpMethod metodo, String uri, Object datos) {
        MvcTestResult resultado = api.enviar(metodo, uri, datos, api.token(PLATAFORMA));

        assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(json(resultado)).containsEntry("status", 403);
    }

    @Test
    @DisplayName("RNF-04 · C2 · el permiso se revisa antes que los datos: la administradora recibe 403 aunque envíe datos inválidos")
    void elPermisoSeRevisaAntesQueLosDatos() {
        String ruta = ApiFotos.ruta(UUID.randomUUID());

        MvcTestResult subida = api.enviar(HttpMethod.POST, ruta + "/9/subida", Map.of(), api.token(ANA));
        MvcTestResult confirmacion = api.enviar(HttpMethod.PUT, ruta + "/9", Map.of(), api.token(ANA));

        assertThat(subida).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(confirmacion).hasStatus(HttpStatus.FORBIDDEN);
    }

    /** Las 3 operaciones de la API de las fotos, con datos válidos cuando llevan cuerpo. */
    static Stream<Arguments> operaciones() {
        String ruta = ApiFotos.ruta(UUID.randomUUID());
        return Stream.of(
                Arguments.of("listar las fotos", HttpMethod.GET, ruta, null),
                Arguments.of("pedir el enlace de subida", HttpMethod.POST, ruta + "/1/subida",
                        Map.of("muestra", 1, "tipo", ApiFotos.JPG, "tamanoBytes", 1_000)),
                Arguments.of("confirmar la foto", HttpMethod.PUT, ruta + "/1",
                        Map.of("llave", ApiFotos.llave(UUID.randomUUID(), UUID.randomUUID(), 1, "jpg"))));
    }
}
