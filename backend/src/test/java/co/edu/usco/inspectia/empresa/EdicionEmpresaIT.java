/*
 * Nombre: EdicionEmpresaIT.java
 * Descripción: Pruebas de los criterios de RF-02 y D-27 sobre editar, desactivar y reactivar una
 *              empresa, por la API y con PostgreSQL 18.
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.empresa;

import static co.edu.usco.inspectia.empresa.ApiEmpresas.campos;
import static co.edu.usco.inspectia.empresa.ApiEmpresas.edicion;
import static co.edu.usco.inspectia.empresa.ApiEmpresas.errores;
import static co.edu.usco.inspectia.empresa.ApiEmpresas.json;
import static co.edu.usco.inspectia.empresa.ApiEmpresas.registro;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import co.edu.usco.inspectia.PruebaIntegracion;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Pruebas de RF-02 y D-27 sobre editar, desactivar y reactivar una empresa. Desactivar no borra nada:
 * cambia {@code activa}, y el inicio de sesión de sus usuarios depende de ese dato (se prueba en RF-01).
 */
@PruebaIntegracion
class EdicionEmpresaIT {

    private static final String NIT = "900123456-8";
    private static final String OTRO_NIT = "890903938-8";

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

    @Test
    @DisplayName("RF-02 · C9 · al desactivar una empresa no se borra nada: sus usuarios, rangos y lotes se conservan")
    void desactivarConservaLosDatos() {
        UUID empresa = api.registrarValida(registro(NIT, "ana@cacaos.test"));
        UUID admin = idDelAdministrador(empresa);
        jdbc.update("""
                INSERT INTO lote (empresa_id, codigo, fecha_recepcion, origen, usuario_id)
                VALUES (?, 'L-001', current_date, 'Finca La Esperanza', ?)""", empresa, admin);

        MvcTestResult resultado = api.desactivar(empresa);

        assertThat(resultado).hasStatus(HttpStatus.OK);
        assertThat(json(resultado)).containsEntry("id", empresa.toString()).containsEntry("activa", false);
        assertThat(activa(empresa)).isFalse();
        assertThat(activo(admin)).as("el usuario no se desactiva uno por uno").isTrue();
        assertThat(contarDe("lote", empresa)).isEqualTo(1);
        assertThat(contarDe("rango_recomendacion", empresa)).isEqualTo(4);
    }

    @Test
    @DisplayName("RF-02 · C10 · al reactivar una empresa queda activa otra vez y cada usuario conserva su estado")
    void reactivarRespetaElEstadoDeCadaUsuario() {
        UUID empresa = api.registrarValida(registro(NIT, "ana@cacaos.test"));
        UUID admin = idDelAdministrador(empresa);
        UUID inactivo = jdbc.queryForObject("""
                INSERT INTO usuario (empresa_id, nombre, correo, contrasena_hash, rol, activo)
                VALUES (?, 'Luis Gómez', 'luis@cacaos.test', 'x', 'USUARIO_FINAL', false) RETURNING id""",
                UUID.class, empresa);
        assertThat(api.desactivar(empresa)).hasStatus(HttpStatus.OK);

        MvcTestResult resultado = api.reactivar(empresa);

        assertThat(resultado).hasStatus(HttpStatus.OK);
        assertThat(json(resultado)).containsEntry("activa", true);
        assertThat(activa(empresa)).isTrue();
        assertThat(activo(admin)).isTrue();
        assertThat(activo(inactivo)).as("reactivar la empresa no reactiva a quien estaba desactivado").isFalse();
    }

    @Test
    @DisplayName("RF-02 · C11 · al editar, un NIT de otra empresa responde 409 y no cambia nada")
    void editarConElNitDeOtraEmpresa() {
        UUID empresa = api.registrarValida(registro(NIT, "ana@cacaos.test"));
        api.registrarValida(registro(OTRO_NIT, "luis@cafe.test"));

        MvcTestResult resultado = api.editar(empresa, edicion("Cacaos del Sur", OTRO_NIT, "Pitalito"));

        assertThat(resultado).hasStatus(HttpStatus.CONFLICT)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(errores(resultado)).containsExactly(tuple("nit", "Ya existe una empresa con este NIT."));
        assertThat(datosGuardados(empresa)).containsEntry("nombre", "Cacaos del Huila").containsEntry("nit", NIT);
    }

    @Test
    @DisplayName("RF-02 · C11 · al editar, un NIT con dígito de verificación inválido responde 400 y no cambia nada")
    void editarConUnDigitoDeVerificacionInvalido() {
        UUID empresa = api.registrarValida(registro(NIT, "ana@cacaos.test"));

        MvcTestResult resultado = api.editar(empresa, edicion("Cacaos del Sur", "890903938-9", "Pitalito"));

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(errores(resultado))
                .containsExactly(tuple("nit", "El dígito de verificación del NIT no corresponde."));
        assertThat(datosGuardados(empresa)).containsEntry("nombre", "Cacaos del Huila").containsEntry("nit", NIT);
    }

    @Test
    @DisplayName("RF-02 · C11 · al editar, la empresa puede conservar su propio NIT y guardar los demás cambios")
    void editarConservandoElNit() {
        UUID empresa = api.registrarValida(registro(NIT, "ana@cacaos.test"));

        MvcTestResult resultado = api.editar(empresa, edicion("  Cacaos del Sur ", NIT, " Pitalito "));

        assertThat(resultado).hasStatus(HttpStatus.OK);
        assertThat(json(resultado))
                .containsEntry("id", empresa.toString())
                .containsEntry("nombre", "Cacaos del Sur")
                .containsEntry("nit", NIT)
                .containsEntry("ciudad", "Pitalito")
                .containsEntry("activa", true);
        assertThat(datosGuardados(empresa))
                .containsEntry("nombre", "Cacaos del Sur")
                .containsEntry("ciudad", "Pitalito");
    }

    @Test
    @DisplayName("D-27 · al editar, los datos obligatorios se validan igual que en el registro")
    void editarSinLosDatosObligatorios() {
        UUID empresa = api.registrarValida(registro(NIT, "ana@cacaos.test"));

        MvcTestResult resultado = api.editar(empresa, edicion(" ", null, ""));

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(campos(resultado)).containsExactlyInAnyOrder("nombre", "nit", "ciudad");
        assertThat(datosGuardados(empresa)).containsEntry("nombre", "Cacaos del Huila").containsEntry("nit", NIT);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("RF-02 · ver, editar, desactivar o reactivar una empresa que no existe responde 404")
    @MethodSource("operacionesSobreUnaEmpresa")
    void unaEmpresaQueNoExiste(String operacion, BiFunction<ApiEmpresas, UUID, MvcTestResult> llamada) {
        MvcTestResult resultado = llamada.apply(api, UUID.randomUUID());

        assertThat(resultado).hasStatus(HttpStatus.NOT_FOUND)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    }

    static Stream<Arguments> operacionesSobreUnaEmpresa() {
        BiFunction<ApiEmpresas, UUID, MvcTestResult> ver = ApiEmpresas::ver;
        BiFunction<ApiEmpresas, UUID, MvcTestResult> editar =
                (api, id) -> api.editar(id, edicion("Cacaos del Huila", NIT, "Neiva"));
        BiFunction<ApiEmpresas, UUID, MvcTestResult> desactivar = ApiEmpresas::desactivar;
        BiFunction<ApiEmpresas, UUID, MvcTestResult> reactivar = ApiEmpresas::reactivar;
        return Stream.of(
                Arguments.of("ver", ver),
                Arguments.of("editar", editar),
                Arguments.of("desactivar", desactivar),
                Arguments.of("reactivar", reactivar));
    }

    private UUID idDelAdministrador(UUID empresa) {
        return jdbc.queryForObject(
                "SELECT id FROM usuario WHERE empresa_id = ? AND rol = 'ADMIN_EMPRESA'", UUID.class, empresa);
    }

    private boolean activa(UUID empresa) {
        return jdbc.queryForObject("SELECT activa FROM empresa WHERE id = ?", Boolean.class, empresa);
    }

    private boolean activo(UUID usuario) {
        return jdbc.queryForObject("SELECT activo FROM usuario WHERE id = ?", Boolean.class, usuario);
    }

    private int contarDe(String tabla, UUID empresa) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla + " WHERE empresa_id = ?", Integer.class, empresa);
    }

    private Map<String, Object> datosGuardados(UUID empresa) {
        return jdbc.queryForMap("SELECT nombre, nit, ciudad, activa FROM empresa WHERE id = ?", empresa);
    }
}
