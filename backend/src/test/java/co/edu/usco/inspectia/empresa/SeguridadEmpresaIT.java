/*
 * Nombre: SeguridadEmpresaIT.java
 * Descripción: Pruebas de seguridad de la API de empresas (RNF-04, RP-12): sin sesión responde 401 y
 *              con un rol sin permiso, 403.
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.empresa;

import static co.edu.usco.inspectia.empresa.ApiEmpresas.RUTA;
import static co.edu.usco.inspectia.empresa.ApiEmpresas.contar;
import static co.edu.usco.inspectia.empresa.ApiEmpresas.edicion;
import static co.edu.usco.inspectia.empresa.ApiEmpresas.json;
import static co.edu.usco.inspectia.empresa.ApiEmpresas.registro;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import co.edu.usco.inspectia.PruebaIntegracion;
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
 * Pruebas de seguridad de la API de empresas (RNF-04, RP-12). Solo el administrador de la plataforma
 * la usa (D-13): sin sesión responde 401 y con otro rol, 403, siempre en JSON y sin guardar nada.
 * Las operaciones sobre una empresa usan un id que no existe: el permiso se revisa antes de buscarla.
 */
@PruebaIntegracion
class SeguridadEmpresaIT {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiEmpresas api;

    @BeforeEach
    void preparar() {
        api = new ApiEmpresas(mvc);
        ApiEmpresas.borrarDatos(jdbc);
    }

    @AfterEach
    void limpiar() {
        ApiEmpresas.borrarDatos(jdbc);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("RNF-04 · C1 · sin sesión, cada operación de la API de empresas responde 401 en JSON")
    @MethodSource("operaciones")
    void sinSesionResponde401(String operacion, HttpMethod metodo, String uri, Object datos) {
        MvcTestResult resultado = api.enviar(metodo, uri, datos, null);

        assertThat(resultado).hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(json(resultado)).containsEntry("status", 401);
        assertThat(contar(jdbc, "empresa")).isZero();
    }

    @ParameterizedTest(name = "{0} · {1}")
    @DisplayName("RNF-04 · C2 · un administrador de empresa o un usuario final recibe 403 en cada operación de la API de empresas")
    @MethodSource("operacionesConRolesSinPermiso")
    void otrosRolesReciben403(String rol, String operacion, HttpMethod metodo, String uri, Object datos) {
        MvcTestResult resultado = api.enviar(metodo, uri, datos, user("usuario@cacaos.test").roles(rol));

        assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(json(resultado)).containsEntry("status", 403);
        assertThat(contar(jdbc, "empresa")).isZero();
    }

    @Test
    @DisplayName("RNF-04 · C2 · el permiso se revisa antes que los datos: un usuario final recibe 403 aunque envíe datos inválidos")
    void elPermisoSeRevisaAntesQueLosDatos() {
        MvcTestResult resultado = api.enviar(HttpMethod.POST, RUTA, Map.of(),
                user("usuario@cacaos.test").roles("USUARIO_FINAL"));

        assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN);
    }

    /** Las 6 operaciones de la API de empresas, con datos válidos cuando llevan cuerpo. */
    static Stream<Arguments> operaciones() {
        String empresa = RUTA + "/" + UUID.randomUUID();
        return Stream.of(
                Arguments.of("listar", HttpMethod.GET, RUTA, null),
                Arguments.of("ver", HttpMethod.GET, empresa, null),
                Arguments.of("crear", HttpMethod.POST, RUTA, registro("900123456-8", "ana@cacaos.test")),
                Arguments.of("editar", HttpMethod.PUT, empresa, edicion("Cacaos del Huila", "900123456-8", "Neiva")),
                Arguments.of("desactivar", HttpMethod.POST, empresa + "/desactivar", null),
                Arguments.of("reactivar", HttpMethod.POST, empresa + "/reactivar", null));
    }

    static Stream<Arguments> operacionesConRolesSinPermiso() {
        return Stream.of("ADMIN_EMPRESA", "USUARIO_FINAL").flatMap(rol -> operaciones().map(operacion -> {
            Object[] datos = operacion.get();
            return Arguments.of(rol, datos[0], datos[1], datos[2], datos[3]);
        }));
    }
}
