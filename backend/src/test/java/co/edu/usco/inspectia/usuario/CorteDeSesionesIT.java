/*
 * Nombre: CorteDeSesionesIT.java
 * Descripción: Pruebas del corte de sesiones al cambiar o restablecer una contraseña (RNF-04 C6) con un reloj
 *              de prueba que se fija y se atrasa: el corte no puede depender de la hora, ni de que el reloj del
 *              servidor retroceda ni del segundo en que se cambia. Revisa los tres caminos que cambian una
 *              contraseña: la gestión de usuarios (RF-03), el restablecimiento de RF-02 C12 y el cambio de la
 *              propia (RF-18).
 * Fecha de creación: 26/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario;

import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.CONTRASENA;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.CONTRASENA_CAMBIADA;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.RelojDePrueba;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Pruebas del corte de sesiones con el reloj del backend en manos de la prueba. Empieza a mitad de un segundo,
 * y la prueba lo atrasa o lo mueve dentro del mismo segundo, como cuando Windows ajustó la hora en medio de
 * RF-18 C5 (ver la bitácora del 26/10/2026).
 */
@PruebaIntegracion
class CorteDeSesionesIT {

    /** Una hora con milisegundos: el token guarda solo los segundos. */
    private static final Instant SEGUNDO_CINCO = Instant.parse("2026-10-26T16:14:05.100Z");
    private static final RelojDePrueba RELOJ = new RelojDePrueba(SEGUNDO_CINCO);

    private static final String ANA = "ana@cacaos.test";
    private static final String LUIS = "luis@cacaos.test";
    private static final String PLATAFORMA = "plataforma@inspectia.test";
    private static final String NUEVA = "Nueva-Clave-2026";

    /** Los caminos que cambian una contraseña. */
    enum Camino {
        GESTION_DE_USUARIOS,
        RESTABLECIMIENTO_DE_UN_ADMINISTRADOR,
        CAMBIO_DE_LA_PROPIA
    }

    /** El reloj del backend en estas pruebas: reemplaza al bean {@code reloj} de la aplicación. */
    @TestBean(name = "reloj", methodName = "relojDePrueba")
    private Clock reloj;

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiSesion api;
    private ApiUsuarios usuarios;
    private UUID empresa;
    private UUID ana;
    private UUID luis;

    static Clock relojDePrueba() {
        return RELOJ;
    }

    @BeforeEach
    void preparar() {
        RELOJ.fijar(SEGUNDO_CINCO);
        api = new ApiSesion(mvc, jdbc);
        usuarios = new ApiUsuarios(api);
        ApiSesion.borrarDatos(jdbc);
        empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        ana = api.crearUsuario(empresa, "Ana Pérez", ANA, "ADMIN_EMPRESA");
        luis = api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");
        api.crearUsuario(null, "Equipo InspectIA", PLATAFORMA, "ADMIN_PLATAFORMA");
    }

    @Test
    @DisplayName("RNF-04 · C6 · si el reloj del servidor se atrasa entre un inicio de sesión y el cambio de contraseña, esa sesión deja de servir, y una nueva sí sirve")
    void relojAtrasadoAntesDelCambio() {
        String deLuis = api.token(LUIS);
        RELOJ.atrasar(Duration.ofSeconds(2));

        assertThat(restablecerDesdeLaGestion()).hasStatus(HttpStatus.OK);

        sesionCortada(deLuis);
        assertThat(api.sesion(api.token(LUIS, NUEVA))).hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("RNF-04 · C6 · si el reloj del servidor se atrasa después del cambio de contraseña, un inicio de sesión nuevo sirve")
    void relojAtrasadoDespuesDelCambio() {
        assertThat(restablecerDesdeLaGestion()).hasStatus(HttpStatus.OK);
        RELOJ.atrasar(Duration.ofSeconds(2));

        String nueva = api.token(LUIS, NUEVA);

        assertThat(api.sesion(nueva)).hasStatus(HttpStatus.OK);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Camino.class)
    @DisplayName("RNF-04 · C6 · una sesión abierta en el mismo segundo del cambio de contraseña, pero antes de él, deja de servir, en los tres caminos que la cambian")
    void mismoSegundoAntesDelCambio(Camino camino) {
        String abierta = api.token(camino == Camino.RESTABLECIMIENTO_DE_UN_ADMINISTRADOR ? ANA : LUIS);
        RELOJ.fijar(SEGUNDO_CINCO.plusMillis(800));

        MvcTestResult cambio = cambiar(camino);

        assertThat(cambio.getResponse().getStatus()).isIn(HttpStatus.OK.value(), HttpStatus.NO_CONTENT.value());
        sesionCortada(abierta);
    }

    /** Cambia la contraseña por un camino: Luis en la gestión de usuarios y en la suya, Ana como administradora. */
    private MvcTestResult cambiar(Camino camino) {
        return switch (camino) {
            case GESTION_DE_USUARIOS -> restablecerDesdeLaGestion();
            case RESTABLECIMIENTO_DE_UN_ADMINISTRADOR -> api.enviar(HttpMethod.PUT,
                    "/api/v1/empresas/" + empresa + "/administradores/" + ana + "/contrasena",
                    Map.of("contrasena", NUEVA), api.token(PLATAFORMA));
            case CAMBIO_DE_LA_PROPIA -> api.enviar(HttpMethod.PUT, "/api/v1/sesion/contrasena",
                    cambioDeLaPropia(CONTRASENA, NUEVA), api.token(LUIS));
        };
    }

    /** Ana, su administradora, le restablece la contraseña a Luis desde la gestión de usuarios. */
    private MvcTestResult restablecerDesdeLaGestion() {
        return usuarios.editar(api.token(ANA), luis, ApiUsuarios.datos("Luis Gómez", LUIS, "USUARIO_FINAL", NUEVA));
    }

    private static Map<String, Object> cambioDeLaPropia(String actual, String nueva) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("contrasenaActual", actual);
        datos.put("contrasenaNueva", nueva);
        return datos;
    }

    private void sesionCortada(String token) {
        MvcTestResult cortada = api.sesion(token);
        assertThat(cortada).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(json(cortada)).containsEntry("detail", CONTRASENA_CAMBIADA);
    }
}
