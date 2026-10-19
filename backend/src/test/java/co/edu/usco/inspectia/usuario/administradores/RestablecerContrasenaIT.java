/*
 * Nombre: RestablecerContrasenaIT.java
 * Descripción: Pruebas del restablecimiento de la contraseña de un administrador de empresa por el
 *              administrador de la plataforma, con PostgreSQL 18 y tokens reales (RF-02 C12, RNF-04, D-30).
 * Fecha de creación: 18/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.usuario.administradores;

import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.CONTRASENA;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.CONTRASENA_CAMBIADA;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.error;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.errores;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.lista;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import co.edu.usco.inspectia.usuario.sesion.TokensDePrueba;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Pruebas de las rutas del administrador de la plataforma sobre los administradores de una empresa
 * (RF-02 C12, D-30):
 * <ul>
 *   <li>{@code GET /api/v1/empresas/{id}/administradores} responde la lista de sus administradores:
 *       {@code id}, {@code nombre}, {@code correo} y {@code activo}, por nombre.</li>
 *   <li>{@code PUT /api/v1/empresas/{id}/administradores/{usuarioId}/contrasena} con
 *       {@code contrasena} la restablece y responde 204.</li>
 * </ul>
 * Si el usuario no es administrador de esa empresa, las dos responden que no existe (404).
 */
@PruebaIntegracion
class RestablecerContrasenaIT {

    private static final String PLATAFORMA = "plataforma@inspectia.test";
    private static final String ANA = "ana@cacaos.test";
    private static final String BEATRIZ = "beatriz@cacaos.test";
    private static final String LUIS = "luis@cacaos.test";
    private static final String MARTA = "marta@cacaosur.test";
    private static final String NUEVA = "Nueva-Clave-2026";
    private static final String NO_EXISTE = "No existe un registro con ese identificador.";
    private static final String OBLIGATORIO = "Este dato es obligatorio.";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiSesion api;
    private UUID empresa;
    private UUID otraEmpresa;
    private UUID ana;
    private UUID beatriz;
    private UUID luis;
    private UUID marta;
    private String token;

    @BeforeEach
    void preparar() {
        api = new ApiSesion(mvc, jdbc);
        ApiSesion.borrarDatos(jdbc);
        empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        beatriz = api.crearUsuario(empresa, "Beatriz Rojas", BEATRIZ, "ADMIN_EMPRESA");
        ana = api.crearUsuario(empresa, "Ana Pérez", ANA, "ADMIN_EMPRESA");
        luis = api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");
        otraEmpresa = api.crearEmpresa("Cacao del Sur", "800765432-1");
        marta = api.crearUsuario(otraEmpresa, "Marta Ruiz", MARTA, "ADMIN_EMPRESA");
        api.crearUsuario(null, "Equipo InspectIA", PLATAFORMA, "ADMIN_PLATAFORMA");
        token = api.token(PLATAFORMA);
    }

    @AfterEach
    void limpiar() {
        ApiSesion.borrarDatos(jdbc);
    }

    @Test
    @DisplayName("RF-02 · el administrador de la plataforma ve los administradores de una empresa, por nombre y sin sus contraseñas")
    void listaLosAdministradores() {
        MvcTestResult resultado = api.enviar(HttpMethod.GET, administradores(empresa), null, token);

        assertThat(resultado).hasStatus(HttpStatus.OK);
        List<Map<String, Object>> administradores = lista(resultado);
        assertThat(administradores).extracting(a -> a.get("correo")).as("sin el usuario final ni los de otra empresa")
                .containsExactly(ANA, BEATRIZ);
        assertThat(administradores.getFirst()).containsOnlyKeys("id", "nombre", "correo", "activo")
                .containsEntry("id", ana.toString()).containsEntry("activo", true);
    }

    @Test
    @DisplayName("RF-02 · C12 · el administrador de la plataforma restablece la contraseña de un administrador de empresa: entra con la nueva y no con la anterior")
    void restableceLaContrasena() {
        MvcTestResult resultado = restablecer(empresa, ana, NUEVA);

        assertThat(resultado).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(api.iniciar(ANA, NUEVA)).hasStatus(HttpStatus.OK);
        assertThat(api.iniciar(ANA, CONTRASENA)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @ParameterizedTest(name = "{0} caracteres con {1} eñes: {2}")
    @DisplayName("RF-02 · C12 · la contraseña nueva pasa por las mismas validaciones de la inicial (prueba de límite)")
    @CsvSource({
        "7, 0, 400, La contraseña debe tener al menos 8 caracteres.",
        "8, 0, 204, ''",
        "64, 0, 204, ''",
        "65, 0, 400, La contraseña puede tener como máximo 64 caracteres.",
        "64, 8, 204, ''",
        "64, 9, 400, 'La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes'"})
    void validaLaContrasena(int caracteres, int enes, int estado, String mensaje) {
        String nueva = "ñ".repeat(enes) + "x".repeat(caracteres - enes);

        MvcTestResult resultado = restablecer(empresa, ana, nueva);

        assertThat(resultado).hasStatus(estado);
        if (estado == 400) {
            assertThat(errores(resultado)).containsExactly(error("contrasena", mensaje));
            assertThat(api.iniciar(ANA, CONTRASENA)).as("no cambió").hasStatus(HttpStatus.OK);
        } else {
            assertThat(api.iniciar(ANA, nueva)).hasStatus(HttpStatus.OK);
        }
    }

    @ParameterizedTest(name = "«{0}»: {1}")
    @DisplayName("RF-02 · C12 · una contraseña nueva de solo espacios se rechaza como si faltara, igual que la inicial; con una letra se acepta (prueba de límite)")
    @CsvSource({"'        ', 400", "'       x', 204"})
    void contrasenaDeSoloEspacios(String nueva, int estado) {
        MvcTestResult resultado = restablecer(empresa, ana, nueva);

        assertThat(resultado).hasStatus(estado);
        if (estado == 400) {
            assertThat(errores(resultado)).containsExactly(error("contrasena", OBLIGATORIO));
            assertThat(api.iniciar(ANA, CONTRASENA)).as("no cambió").hasStatus(HttpStatus.OK);
        } else {
            assertThat(api.iniciar(ANA, nueva)).as("entra con ella, espacios incluidos").hasStatus(HttpStatus.OK);
        }
    }

    @Test
    @DisplayName("RF-02 · restablecer la contraseña de un administrador de empresa desbloquea su cuenta")
    void restablecerDesbloquea() {
        jdbc.update("UPDATE usuario SET intentos_fallidos = 3, bloqueado_hasta = now() + interval '10 minutes' WHERE id = ?",
                ana);

        assertThat(restablecer(empresa, ana, NUEVA)).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(api.iniciar(ANA, NUEVA)).hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("RNF-04 · C6 · al restablecer la contraseña de un administrador de empresa, sus sesiones abiertas dejan de servir")
    void restablecerCortaLasSesiones() {
        String abierta = TokensDePrueba.token(ana, Instant.now().minusSeconds(5));
        assertThat(api.sesion(abierta)).hasStatus(HttpStatus.OK);

        assertThat(restablecer(empresa, ana, NUEVA)).hasStatus(HttpStatus.NO_CONTENT);

        MvcTestResult cortada = api.sesion(abierta);
        assertThat(cortada).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(json(cortada)).containsEntry("detail", CONTRASENA_CAMBIADA);
        assertThat(api.sesion(token)).as("la del administrador de la plataforma sigue").hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("RF-02 · si el usuario no es administrador de esa empresa, la API responde que no existe y no cambia nada")
    void soloAdministradoresDeEsaEmpresa() {
        List<MvcTestResult> resultados = List.of(
                restablecer(empresa, luis, NUEVA),
                restablecer(empresa, marta, NUEVA),
                restablecer(otraEmpresa, ana, NUEVA),
                restablecer(empresa, UUID.randomUUID(), NUEVA),
                restablecer(UUID.randomUUID(), ana, NUEVA),
                api.enviar(HttpMethod.GET, administradores(UUID.randomUUID()), null, token));

        for (MvcTestResult resultado : resultados) {
            assertThat(resultado).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(json(resultado)).containsEntry("detail", NO_EXISTE);
        }
        for (String correo : List.of(ANA, LUIS, MARTA)) {
            assertThat(api.iniciar(correo, CONTRASENA)).as(correo + " no cambió").hasStatus(HttpStatus.OK);
        }
    }

    @Test
    @DisplayName("RNF-04 · C2 · solo el administrador de la plataforma lista y restablece a los administradores de empresa: los demás roles reciben 403")
    void soloLaPlataforma() {
        for (String otro : List.of(api.token(BEATRIZ), api.token(LUIS))) {
            assertThat(restablecer(empresa, ana, NUEVA, otro)).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(api.enviar(HttpMethod.GET, administradores(empresa), null, otro)).hasStatus(HttpStatus.FORBIDDEN);
        }
        assertThat(restablecer(empresa, ana, NUEVA, null)).as("sin sesión").hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(api.iniciar(ANA, CONTRASENA)).as("no cambió").hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("RF-02 · también restablece la contraseña de un administrador desactivado o de una empresa desactivada, sin reactivarlos")
    void noReactivaNada() {
        jdbc.update("UPDATE usuario SET activo = false WHERE id = ?", beatriz);
        jdbc.update("UPDATE empresa SET activa = false WHERE id = ?", otraEmpresa);

        assertThat(restablecer(empresa, beatriz, NUEVA)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(restablecer(otraEmpresa, marta, NUEVA)).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(jdbc.queryForObject("SELECT activo FROM usuario WHERE id = ?", Boolean.class, beatriz)).isFalse();
        assertThat(jdbc.queryForObject("SELECT activa FROM empresa WHERE id = ?", Boolean.class, otraEmpresa)).isFalse();
    }

    private MvcTestResult restablecer(UUID empresa, UUID usuario, String nueva) {
        return restablecer(empresa, usuario, nueva, token);
    }

    private MvcTestResult restablecer(UUID empresa, UUID usuario, String nueva, String token) {
        return api.enviar(HttpMethod.PUT, administradores(empresa) + "/" + usuario + "/contrasena",
                Map.of("contrasena", nueva), token);
    }

    private static String administradores(UUID empresa) {
        return "/api/v1/empresas/" + empresa + "/administradores";
    }
}
