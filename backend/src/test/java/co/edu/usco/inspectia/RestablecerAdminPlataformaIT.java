/*
 * Nombre: RestablecerAdminPlataformaIT.java
 * Descripción: Pruebas del script infra/bd/restablecer-admin-plataforma.sql, con el que el equipo
 *              restablece la contraseña del administrador de la plataforma, corrido con psql dentro del
 *              contenedor de PostgreSQL 18 (D-30, RF-18).
 * Fecha de creación: 19/10/2026
 * Actualización: 19/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia;

import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.CONTRASENA;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.CONTRASENA_CAMBIADA;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import co.edu.usco.inspectia.usuario.sesion.TokensDePrueba;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.ExecConfig;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Pruebas del script que restablece la contraseña del administrador de la plataforma (D-30). El script
 * corre como en {@code restablecer-admin-plataforma.sh} y {@code .ps1}: psql dentro del contenedor lo lee
 * por la entrada estándar y toma el correo y la contraseña nueva de variables de entorno. Los envoltorios
 * piden la contraseña por teclado y la pasan en esa variable.
 *
 * <p>La sesión «de antes» se arma con el reloj de la base: el script toma de él la hora del cambio, y en
 * Docker Desktop puede diferir unos segundos del reloj del equipo donde corren las pruebas.
 */
@PruebaIntegracion
class RestablecerAdminPlataformaIT {

    /** El script, visto desde {@code backend/}, la carpeta donde corre Maven. */
    private static final Path SCRIPT = Path.of("..", "infra", "bd", "restablecer-admin-plataforma.sql");

    private static final String SCRIPT_EN_CONTENEDOR = "/tmp/restablecer-admin-plataforma.sql";

    /** El mismo psql que corren los envoltorios. */
    private static final String PSQL = "exec psql -X -q -U \"$POSTGRES_USER\" -d \"$POSTGRES_DB\"";

    /** Código de salida de psql cuando el script se detiene por un error (ON_ERROR_STOP). */
    private static final int ERROR_DEL_SCRIPT = 3;

    private static final String ADMIN = "plataforma@inspectia.test";

    /** Con eñe, tilde y {@code $}, para probar que pgcrypto y Spring Security leen los mismos bytes. */
    private static final String NUEVA = "Nueva-Ñandú-2026$x";

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    @Autowired
    private PostgreSQLContainer postgres;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MockMvcTester mvc;

    private ApiSesion api;
    private UUID admin;

    @BeforeEach
    void preparar() {
        postgres.copyFileToContainer(MountableFile.forHostPath(SCRIPT), SCRIPT_EN_CONTENEDOR);
        api = new ApiSesion(mvc, jdbc);
        ApiSesion.borrarDatos(jdbc);
        admin = api.crearUsuario(null, "Equipo InspectIA", ADMIN, "ADMIN_PLATAFORMA");
    }

    @AfterEach
    void limpiar() {
        ApiSesion.borrarDatos(jdbc);
    }

    @Test
    @DisplayName("D-30 · el script restablece la contraseña del administrador de la plataforma: entra la nueva y no la anterior, con un hash que Spring Security acepta")
    void restableceLaContrasena() throws Exception {
        Resultado resultado = correr("  Plataforma@InspectIA.test ", NUEVA);

        assertThat(resultado.codigo()).as(resultado.salida()).isZero();
        assertThat(resultado.salida()).contains("Contraseña restablecida para plataforma@inspectia.test");
        assertThat(hash()).startsWith("$2a$10$");
        assertThat(BCRYPT.matches(NUEVA, hash())).isTrue();
        assertThat(api.iniciar(ADMIN, NUEVA)).hasStatus(HttpStatus.OK);
        assertThat(api.iniciar(ADMIN, CONTRASENA)).as("la anterior ya no entra").hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("D-30 · el script corta las sesiones abiertas del administrador y quita su bloqueo por intentos fallidos: entra de una vez con la nueva")
    void cortaLasSesionesYDesbloquea() throws Exception {
        String abierta = TokensDePrueba.token(admin, ahoraEnLaBase().minusSeconds(5));
        assertThat(api.sesion(abierta)).hasStatus(HttpStatus.OK);
        jdbc.update("UPDATE usuario SET intentos_fallidos = 3, bloqueado_hasta = now() + interval '10 minutes' WHERE id = ?",
                admin);

        Resultado resultado = correr(ADMIN, NUEVA);

        assertThat(resultado.codigo()).as(resultado.salida()).isZero();
        MvcTestResult cortada = api.sesion(abierta);
        assertThat(cortada).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(json(cortada)).containsEntry("detail", CONTRASENA_CAMBIADA);
        assertThat(jdbc.queryForMap("SELECT intentos_fallidos, bloqueado_hasta FROM usuario WHERE id = ?", admin))
                .containsEntry("intentos_fallidos", 0)
                .containsEntry("bloqueado_hasta", null);
        assertThat(api.iniciar(ADMIN, NUEVA)).hasStatus(HttpStatus.OK);
    }

    @ParameterizedTest(name = "{0} caracteres")
    @DisplayName("D-30 · la contraseña nueva acepta de 8 a 64 caracteres, con los mensajes de la API (prueba de límite)")
    @CsvSource(delimiter = '|', textBlock = """
            7  | La contraseña debe tener al menos 8 caracteres.
            8  |
            64 |
            65 | La contraseña puede tener como máximo 64 caracteres.
            """)
    void limiteDeCaracteres(int caracteres, String error) throws Exception {
        comprobarLimite("Ab1-".repeat(17).substring(0, caracteres), error);
    }

    @ParameterizedTest(name = "{0} eñes en 64 caracteres")
    @DisplayName("D-30 · la contraseña nueva ocupa como máximo 72 bytes, con el mensaje de la API (prueba de límite)")
    @CsvSource(delimiter = '|', textBlock = """
            8 |
            9 | La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes
            """)
    void limiteDeBytes(int enes, String error) throws Exception {
        comprobarLimite("ñ".repeat(enes) + "a".repeat(64 - enes), error);
    }

    @ParameterizedTest(name = "«{0}»")
    @DisplayName("D-30 · una contraseña nueva de solo espacios cuenta como si faltara; con una letra se acepta (prueba de límite)")
    @CsvSource(delimiter = '|', textBlock = """
            '        ' | Falta la contraseña nueva.
            '       x' |
            """)
    void contrasenaDeSoloEspacios(String contrasena, String error) throws Exception {
        comprobarLimite(contrasena, error);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("D-30 · el script no cambia nada si falta un dato, si el correo no es de ningún usuario o si sus caracteres no venían en UTF-8")
    @MethodSource("datosInvalidos")
    void rechazaDatosInvalidos(String caso, String correo, String contrasena, String error) throws Exception {
        String antes = hash();

        Resultado resultado = correr(correo, contrasena);

        assertThat(resultado.codigo()).as(resultado.salida()).isEqualTo(ERROR_DEL_SCRIPT);
        assertThat(resultado.salida()).contains(error);
        assertThat(hash()).isEqualTo(antes);
    }

    static Stream<Arguments> datosInvalidos() {
        return Stream.of(
                Arguments.of("sin correo", null, NUEVA, "Falta INSPECTIA_ADMIN_CORREO: escribe el correo del administrador."),
                Arguments.of("sin contraseña", ADMIN, null, "Falta la contraseña nueva."),
                Arguments.of("correo que no existe", "nadie@inspectia.test", NUEVA,
                        "No hay ningún administrador de la plataforma con el correo nadie@inspectia.test."),
                Arguments.of("caracteres que no venían en UTF-8", ADMIN, "Clave�mala-1",
                        "Hay caracteres que no se pudieron leer"));
    }

    @Test
    @DisplayName("D-30 · el script no toca a un usuario de una empresa: su contraseña se restablece desde la app")
    void noTocaUnUsuarioDeEmpresa() throws Exception {
        UUID empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        UUID gerente = api.crearUsuario(empresa, "Gerente", "gerente@cacaos.test", "ADMIN_EMPRESA");
        String antes = jdbc.queryForObject("SELECT contrasena_hash FROM usuario WHERE id = ?", String.class, gerente);

        Resultado resultado = correr("gerente@cacaos.test", NUEVA);

        assertThat(resultado.codigo()).as(resultado.salida()).isEqualTo(ERROR_DEL_SCRIPT);
        assertThat(resultado.salida()).contains(
                "El correo gerente@cacaos.test es de un usuario de una empresa: su contraseña se restablece desde la app (D-30).");
        assertThat(jdbc.queryForObject("SELECT contrasena_hash FROM usuario WHERE id = ?", String.class, gerente))
                .isEqualTo(antes);
    }

    @Test
    @DisplayName("D-30 · el script no reactiva a un administrador desactivado")
    void noReactiva() throws Exception {
        jdbc.update("UPDATE usuario SET activo = false WHERE id = ?", admin);

        assertThat(correr(ADMIN, NUEVA).codigo()).isZero();

        assertThat(jdbc.queryForObject("SELECT activo FROM usuario WHERE id = ?", Boolean.class, admin)).isFalse();
    }

    @Test
    @DisplayName("D-30 · la contraseña nueva no sale en pantalla ni en el registro de PostgreSQL, aunque el servidor registre todas las sentencias")
    void laContrasenaNoQuedaEnElRegistro() throws Exception {
        String valida = "Registro-" + UUID.randomUUID();
        String demasiadoLarga = "Registro-" + "x".repeat(60);
        Map<String, String> registrarTodo = Map.of("PGOPTIONS", "-c log_statement=all");

        Resultado restablecida = correr(ADMIN, valida, registrarTodo);
        Resultado rechazada = correr(ADMIN, demasiadoLarga, registrarTodo);

        assertThat(restablecida.codigo()).as(restablecida.salida()).isZero();
        assertThat(rechazada.codigo()).as(rechazada.salida()).isEqualTo(ERROR_DEL_SCRIPT);
        // La primera línea comprueba que el servidor sí estaba registrando las sentencias.
        assertThat(registroCompleto())
                .contains("statement: SET LOCAL log_statement = 'none'")
                .doesNotContain(valida)
                .doesNotContain(demasiadoLarga);
    }

    /**
     * Prueba de límite de la contraseña nueva: sin error esperado, queda restablecida y Spring Security
     * acepta su hash; con error, el script se detiene con ese mensaje y no cambia nada.
     */
    private void comprobarLimite(String contrasena, String error) throws Exception {
        String antes = hash();

        Resultado resultado = correr(ADMIN, contrasena);

        if (error == null) {
            assertThat(resultado.codigo()).as(resultado.salida()).isZero();
            assertThat(BCRYPT.matches(contrasena, hash())).isTrue();
        } else {
            assertThat(resultado.codigo()).as(resultado.salida()).isEqualTo(ERROR_DEL_SCRIPT);
            assertThat(resultado.salida()).contains(error);
            assertThat(hash()).isEqualTo(antes);
        }
    }

    private Resultado correr(String correo, String contrasena) throws Exception {
        return correr(correo, contrasena, Map.of());
    }

    /**
     * Corre el script como los envoltorios. Un dato {@code null} es una variable de entorno que no existe.
     * En cualquier caso, la salida no puede mostrar la contraseña.
     */
    private Resultado correr(String correo, String contrasena, Map<String, String> otras) throws Exception {
        Map<String, String> entorno = new HashMap<>(otras);
        entorno.put("PGCLIENTENCODING", "UTF8");
        if (correo != null) {
            entorno.put("INSPECTIA_ADMIN_CORREO", correo);
        }
        if (contrasena != null) {
            entorno.put("INSPECTIA_ADMIN_CONTRASENA", contrasena);
        }
        ExecResult ejecucion = postgres.execInContainer(StandardCharsets.UTF_8, ExecConfig.builder()
                .command(new String[] {"sh", "-c", PSQL + " < " + SCRIPT_EN_CONTENEDOR})
                .envVars(entorno)
                .build());
        Resultado resultado = new Resultado(ejecucion.getExitCode(), ejecucion.getStdout() + ejecucion.getStderr());
        if (contrasena != null && !contrasena.isBlank()) {
            assertThat(resultado.salida()).as("la salida no muestra la contraseña").doesNotContain(contrasena);
        }
        return resultado;
    }

    /**
     * Devuelve el registro de PostgreSQL completo hasta este momento. Docker lo recoge con un pequeño
     * retraso; por eso se registra una marca nueva y se espera a que aparezca.
     */
    private String registroCompleto() throws Exception {
        String marca = "fin-de-la-prueba-" + UUID.randomUUID();
        postgres.execInContainer(StandardCharsets.UTF_8, ExecConfig.builder()
                .command(new String[] {"sh", "-c", PSQL + " -c \"SELECT '" + marca + "'\""})
                .envVars(Map.of("PGOPTIONS", "-c log_statement=all"))
                .build());
        for (int intento = 0; intento < 50; intento++) {
            String registro = postgres.getLogs();
            if (registro.contains(marca)) {
                return registro;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("El registro de PostgreSQL no llegó a la marca " + marca);
    }

    /** La hora de la base: de ella toma el script la hora del cambio. */
    private Instant ahoraEnLaBase() {
        return jdbc.queryForObject("SELECT now()", OffsetDateTime.class).toInstant();
    }

    private String hash() {
        return jdbc.queryForObject("SELECT contrasena_hash FROM usuario WHERE id = ?", String.class, admin);
    }

    /** Código de salida de psql y lo que escribió en pantalla (salida estándar y de errores). */
    private record Resultado(int codigo, String salida) {
    }
}
