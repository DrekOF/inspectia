/*
 * Nombre: CambioDeContrasenaIT.java
 * Descripción: Pruebas del cambio de la propia contraseña por la API, con PostgreSQL 18 y tokens reales:
 *              contraseña actual, bloqueo por intentos fallidos, validaciones de la nueva y la sesión que
 *              sigue abierta mientras las demás se cortan (RF-18, RNF-04, D-30).
 * Fecha de creación: 19/10/2026
 * Actualización: 19/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario.sesion;

import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.AYUDA_BLOQUEO;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.CONTRASENA;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.CONTRASENA_CAMBIADA;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.error;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.errores;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.usuario;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.PruebaIntegracion;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Pruebas del cambio de la propia contraseña (RF-18).
 *
 * <p>Contrato que fijan estas pruebas:
 * <ul>
 *   <li>{@code PUT /api/v1/sesion/contrasena} con {@code contrasenaActual} y {@code contrasenaNueva}, y el
 *       token de la sesión. Lo usa cualquier rol, y cada usuario solo cambia la suya: el usuario sale
 *       del token.</li>
 *   <li>Si cambia, responde 200 como el inicio de sesión ({@code token}, {@code tipo}, {@code vence} y
 *       {@code usuario}), con un token nuevo para la misma sesión: vence cuando vencía el anterior.</li>
 *   <li>Los datos se revisan primero (400 en su campo) y no cuentan como intento. Una contraseña actual
 *       errada responde 400 en {@code contrasenaActual}, con la regla del bloqueo en {@code ayuda}, y
 *       cuenta para el bloqueo de RF-01. Desde el quinto fallo seguido y mientras dure el bloqueo
 *       responde 429, con {@code bloqueadoHasta} y el encabezado {@code Retry-After}.</li>
 * </ul>
 * Las sesiones «de antes» las arma {@link TokensDePrueba}, emitidas 5 segundos atrás: un token del mismo
 * segundo del cambio sigue sirviendo, por la ventana que se aceptó en RF-03.
 */
@PruebaIntegracion
class CambioDeContrasenaIT {

    private static final String RUTA = "/api/v1/sesion/contrasena";
    private static final String ANA = "ana@cacaos.test";
    private static final String BEA = "bea@cacaos.test";
    private static final String LUIS = "luis@cacaos.test";
    private static final String PLATAFORMA = "plataforma@inspectia.test";
    private static final String NUEVA = "Nueva-Clave-2026";
    private static final String ERRADA = "Otra-Clave-2026";

    private static final String ACTUAL_INCORRECTA = "La contraseña actual no es correcta.";
    private static final String CUENTA_BLOQUEADA =
            "Tu cuenta está bloqueada por intentos fallidos: podrás cambiar la contraseña cuando termine el bloqueo.";
    private static final String OBLIGATORIO = "Este dato es obligatorio.";
    private static final String CORTA = "La contraseña debe tener al menos 8 caracteres.";
    private static final String LARGA = "La contraseña puede tener como máximo 64 caracteres.";
    private static final String BYTES = "La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiSesion api;
    private UUID empresa;
    private UUID ana;
    private UUID luis;
    private String token;

    @BeforeEach
    void preparar() {
        api = new ApiSesion(mvc, jdbc);
        ApiSesion.borrarDatos(jdbc);
        empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        ana = api.crearUsuario(empresa, "Ana Pérez", ANA, "ADMIN_EMPRESA");
        luis = api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");
        api.crearUsuario(null, "Equipo InspectIA", PLATAFORMA, "ADMIN_PLATAFORMA");
        token = api.token(ANA);
    }

    @AfterEach
    void limpiar() {
        ApiSesion.borrarDatos(jdbc);
    }

    @Test
    @DisplayName("RF-18 · C1 · con la contraseña actual correcta y una nueva válida, la contraseña cambia: la nueva inicia sesión y la anterior ya no")
    void cambiaLaContrasena() {
        MvcTestResult resultado = cambiar(token, CONTRASENA, NUEVA);

        assertThat(resultado).hasStatus(HttpStatus.OK);
        assertThat(api.iniciar(ANA, NUEVA)).hasStatus(HttpStatus.OK);
        assertThat(api.iniciar(ANA, CONTRASENA)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("RF-18 · C2 · con la contraseña actual errada no cambia nada, responde con la regla del bloqueo y el intento cuenta")
    void conLaActualErradaNoCambia() {
        MvcTestResult resultado = cambiar(token, ERRADA, NUEVA);

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(error("contrasenaActual", ACTUAL_INCORRECTA));
        assertThat(json(resultado)).containsEntry("ayuda", AYUDA_BLOQUEO);
        assertThat(intentosFallidos(ana)).isEqualTo(1);
        assertThat(api.iniciar(ANA, CONTRASENA)).as("la contraseña no cambió").hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("RF-18 · C2 · después de 5 fallos seguidos la cuenta se bloquea 15 minutos: el quinto ya responde 429 y durante el bloqueo tampoco se cambia con la contraseña correcta")
    void cincoFallosBloquean() {
        for (int intento = 1; intento <= 4; intento++) {
            assertThat(cambiar(token, ERRADA, NUEVA)).as("intento " + intento).hasStatus(HttpStatus.BAD_REQUEST);
        }

        MvcTestResult quinto = cambiar(token, ERRADA, NUEVA);
        MvcTestResult conLaCorrecta = cambiar(token, CONTRASENA, NUEVA);

        for (MvcTestResult resultado : List.of(quinto, conLaCorrecta)) {
            assertThat(resultado).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(json(resultado)).containsEntry("detail", CUENTA_BLOQUEADA).containsKey("bloqueadoHasta");
            assertThat(Long.parseLong(resultado.getResponse().getHeader(HttpHeaders.RETRY_AFTER)))
                    .isBetween(1L, Duration.ofMinutes(15).toSeconds());
        }
        assertThat(bloqueadoHasta(ana)).isBetween(Instant.now().plus(Duration.ofMinutes(14)),
                Instant.now().plus(Duration.ofMinutes(15)));
        assertThat(api.iniciar(ANA, CONTRASENA)).as("tampoco inicia sesión (RF-01)").hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(api.sesion(token)).as("la sesión abierta sigue sirviendo para lo demás").hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("RF-18 · C2 · al terminar el bloqueo se puede cambiar la contraseña otra vez")
    void alTerminarElBloqueoSePuede() {
        jdbc.update("UPDATE usuario SET intentos_fallidos = 0, bloqueado_hasta = now() - interval '1 second' WHERE id = ?",
                ana);

        assertThat(cambiar(token, CONTRASENA, NUEVA)).hasStatus(HttpStatus.OK);
        assertThat(api.iniciar(ANA, NUEVA)).hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("RF-18 · C2 · los fallos del inicio de sesión y los del cambio de contraseña cuentan juntos para el bloqueo de RF-01")
    void losFallosCuentanJuntos() {
        for (int intento = 1; intento <= 3; intento++) {
            assertThat(api.iniciar(ANA, ERRADA)).hasStatus(HttpStatus.UNAUTHORIZED);
        }

        assertThat(cambiar(token, ERRADA, NUEVA)).as("el cuarto fallo seguido").hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(cambiar(token, ERRADA, NUEVA)).as("el quinto fallo seguido").hasStatus(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("RF-18 · una contraseña actual de más de 72 bytes cuenta como errada aunque sus primeros 72 coincidan, como al iniciar sesión (RF-01 C8)")
    void unaActualDeMasDe72BytesEsErrada() {
        String deSetentaYDos = "ñ".repeat(8) + "a".repeat(56);
        UUID bea = api.crearUsuario(empresa, "Bea Gil", BEA, "USUARIO_FINAL", deSetentaYDos);
        String tokenBea = api.token(BEA, deSetentaYDos);

        MvcTestResult resultado = cambiar(tokenBea, deSetentaYDos + "b", NUEVA);

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(error("contrasenaActual", ACTUAL_INCORRECTA));
        assertThat(intentosFallidos(bea)).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0} caracteres: {1}")
    @DisplayName("RF-18 · C3 · la contraseña nueva acepta de 8 a 64 caracteres: con 7 o con 65 se rechaza (prueba de límite)")
    @CsvSource({"7, 400", "8, 200", "64, 200", "65, 400"})
    void largoDeLaNueva(int caracteres, int estado) {
        String nueva = "x".repeat(caracteres);

        MvcTestResult resultado = cambiar(token, CONTRASENA, nueva);

        assertThat(resultado).hasStatus(estado);
        if (estado == 400) {
            assertThat(errores(resultado)).containsExactly(error("contrasenaNueva", caracteres < 8 ? CORTA : LARGA));
            assertThat(intentosFallidos(ana)).as("un dato inválido no cuenta como intento").isZero();
            assertThat(api.iniciar(ANA, CONTRASENA)).as("la contraseña no cambió").hasStatus(HttpStatus.OK);
        } else {
            assertThat(api.iniciar(ANA, nueva)).hasStatus(HttpStatus.OK);
        }
    }

    @ParameterizedTest(name = "{0} eñes: {1}")
    @DisplayName("RF-18 · C4 · una contraseña nueva de 64 caracteres con 8 eñes (72 bytes) se acepta y con 9 (73 bytes) se rechaza con su mensaje (prueba de límite)")
    @CsvSource({"8, 200", "9, 400"})
    void bytesDeLaNueva(int enes, int estado) {
        String nueva = "ñ".repeat(enes) + "a".repeat(64 - enes);

        MvcTestResult resultado = cambiar(token, CONTRASENA, nueva);

        assertThat(resultado).hasStatus(estado);
        if (estado == 400) {
            assertThat(errores(resultado)).containsExactly(error("contrasenaNueva", BYTES));
        } else {
            assertThat(api.iniciar(ANA, nueva)).hasStatus(HttpStatus.OK);
        }
    }

    @Test
    @DisplayName("RF-18 · C5 · al cambiarla, la sesión desde la que se cambió sigue abierta con un token nuevo, que vence cuando vencía el anterior, y las demás dejan de servir")
    void laSesionActualSigueYLasDemasNo() {
        Instant antes = Instant.now().minusSeconds(5);
        String otra = TokensDePrueba.token(ana, antes);
        String esta = TokensDePrueba.token(ana, antes);
        assertThat(api.sesion(otra)).hasStatus(HttpStatus.OK);

        MvcTestResult resultado = cambiar(esta, CONTRASENA, NUEVA);

        assertThat(resultado).hasStatus(HttpStatus.OK);
        Map<String, Object> sesion = json(resultado);
        assertThat(sesion).containsOnlyKeys("token", "tipo", "vence", "usuario")
                .containsEntry("tipo", "Bearer")
                .containsEntry("usuario", usuario(ana, "Ana Pérez", ANA, "ADMIN_EMPRESA", empresa, "Cacaos del Huila"));
        String nuevo = (String) sesion.get("token");
        assertThat(api.sesion(nuevo)).as("la sesión desde la que se cambió sigue, con el token nuevo").hasStatus(HttpStatus.OK);
        for (String anterior : List.of(otra, esta)) {
            MvcTestResult cortada = api.sesion(anterior);
            assertThat(cortada).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(json(cortada)).containsEntry("detail", CONTRASENA_CAMBIADA);
        }
        Instant vence = Instant.ofEpochSecond(antes.getEpochSecond()).plus(TokensDePrueba.VIGENCIA);
        assertThat(Instant.parse((String) sesion.get("vence"))).as("cambiar la contraseña no alarga la sesión")
                .isEqualTo(vence);
        Map<String, Object> contenido = TokensDePrueba.leer(nuevo);
        assertThat(((Number) contenido.get("exp")).longValue()).isEqualTo(vence.getEpochSecond());
        assertThat(((Number) contenido.get("iat")).longValue()).as("el token nuevo es del segundo del cambio")
                .isEqualTo(sesionesDesde(ana).getEpochSecond());
    }

    @ParameterizedTest(name = "«{0}»: {1}")
    @DisplayName("RF-18 · una contraseña nueva de solo espacios se rechaza con el mismo mensaje que si faltara; con una letra se acepta (prueba de límite)")
    @CsvSource({"'        ', 400", "'       x', 200"})
    void nuevaDeSoloEspacios(String nueva, int estado) {
        MvcTestResult resultado = cambiar(token, CONTRASENA, nueva);

        assertThat(resultado).hasStatus(estado);
        if (estado == 400) {
            assertThat(errores(resultado)).containsExactly(error("contrasenaNueva", OBLIGATORIO));
        } else {
            assertThat(api.iniciar(ANA, nueva)).as("entra con ella, espacios incluidos").hasStatus(HttpStatus.OK);
        }
    }

    @Test
    @DisplayName("RF-18 · sin la contraseña actual ni la nueva responde 400 en los dos campos y no cuenta como intento")
    void sinDatos() {
        MvcTestResult resultado = cambiar(token, null, null);

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactlyInAnyOrder(error("contrasenaActual", OBLIGATORIO),
                error("contrasenaNueva", OBLIGATORIO));
        assertThat(intentosFallidos(ana)).isZero();
    }

    @Test
    @DisplayName("RF-18 · la contraseña nueva puede ser igual a la actual: el cambio igual corta las demás sesiones")
    void laNuevaPuedeSerIgualALaActual() {
        String otra = TokensDePrueba.token(ana, Instant.now().minusSeconds(5));

        assertThat(cambiar(token, CONTRASENA, CONTRASENA)).hasStatus(HttpStatus.OK);

        assertThat(api.iniciar(ANA, CONTRASENA)).hasStatus(HttpStatus.OK);
        assertThat(api.sesion(otra)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("RF-18 · cada usuario solo cambia la suya: un identificador en el cuerpo se ignora y la contraseña y las sesiones de los demás no cambian (prueba de seguridad)")
    void soloLaPropia() {
        String deLuis = TokensDePrueba.token(luis, Instant.now().minusSeconds(5));
        Map<String, Object> datos = datos(CONTRASENA, NUEVA);
        datos.put("usuarioId", luis.toString());

        assertThat(api.enviar(HttpMethod.PUT, RUTA, datos, token)).hasStatus(HttpStatus.OK);

        assertThat(api.iniciar(ANA, NUEVA)).hasStatus(HttpStatus.OK);
        assertThat(api.iniciar(LUIS, CONTRASENA)).as("la de Luis no cambió").hasStatus(HttpStatus.OK);
        assertThat(api.sesion(deLuis)).as("la sesión de Luis sigue").hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("D-30 · cambiar la propia contraseña deja los intentos fallidos en cero")
    void elCambioReiniciaLosIntentos() {
        cambiar(token, ERRADA, NUEVA);
        cambiar(token, ERRADA, NUEVA);
        assertThat(intentosFallidos(ana)).isEqualTo(2);

        assertThat(cambiar(token, CONTRASENA, NUEVA)).hasStatus(HttpStatus.OK);

        assertThat(intentosFallidos(ana)).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("RNF-04 · todos los roles cambian su propia contraseña: usuario final, administrador de empresa y administrador de la plataforma")
    @ValueSource(strings = {LUIS, ANA, PLATAFORMA})
    void todosLosRoles(String correo) {
        assertThat(cambiar(api.token(correo), CONTRASENA, NUEVA)).hasStatus(HttpStatus.OK);

        assertThat(api.iniciar(correo, NUEVA)).hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("RNF-04 · C1 · sin sesión, cambiar la contraseña responde 401 y no cambia nada")
    void sinSesion() {
        assertThat(cambiar(null, CONTRASENA, NUEVA)).hasStatus(HttpStatus.UNAUTHORIZED);

        assertThat(api.iniciar(ANA, CONTRASENA)).hasStatus(HttpStatus.OK);
    }

    /** Pide el cambio con el token dado; {@code null} para enviar la petición sin sesión. */
    private MvcTestResult cambiar(String token, String actual, String nueva) {
        return api.enviar(HttpMethod.PUT, RUTA, datos(actual, nueva), token);
    }

    /** El cuerpo del cambio; un dato {@code null} no se envía. */
    private static Map<String, Object> datos(String actual, String nueva) {
        Map<String, Object> datos = new LinkedHashMap<>();
        if (actual != null) {
            datos.put("contrasenaActual", actual);
        }
        if (nueva != null) {
            datos.put("contrasenaNueva", nueva);
        }
        return datos;
    }

    private int intentosFallidos(UUID usuario) {
        return jdbc.queryForObject("SELECT intentos_fallidos FROM usuario WHERE id = ?", Integer.class, usuario);
    }

    private Instant bloqueadoHasta(UUID usuario) {
        return jdbc.queryForObject("SELECT bloqueado_hasta FROM usuario WHERE id = ?", OffsetDateTime.class, usuario)
                .toInstant();
    }

    private Instant sesionesDesde(UUID usuario) {
        return jdbc.queryForObject("SELECT sesiones_desde FROM usuario WHERE id = ?", OffsetDateTime.class, usuario)
                .toInstant();
    }
}
