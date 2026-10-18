/*
 * Nombre: CrearAdminPlataformaIT.java
 * Descripción: Pruebas del script infra/bd/crear-admin-plataforma.sql, que crea el primer administrador
 *              de la plataforma, corrido con psql dentro del contenedor de PostgreSQL 18.
 * Fecha de creación: 13/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.ExecConfig;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Pruebas del script que crea el primer administrador de la plataforma (precondición de RF-01, RP-15).
 * El script corre como en {@code crear-admin-plataforma.sh} y {@code .ps1}: psql dentro del contenedor
 * lo lee por la entrada estándar y toma los datos de variables de entorno.
 *
 * <p>El script confirma su propia transacción, así que estas pruebas no se deshacen solas: usan correos
 * de {@code @inspectia.test} y los borran al final de cada una.
 */
@PruebaIntegracion
class CrearAdminPlataformaIT {

    /** El script, visto desde {@code backend/}, la carpeta donde corre Maven. */
    private static final Path SCRIPT = Path.of("..", "infra", "bd", "crear-admin-plataforma.sql");

    private static final String SCRIPT_EN_CONTENEDOR = "/tmp/crear-admin-plataforma.sql";

    /** El mismo psql que corren los envoltorios. */
    private static final String PSQL = "exec psql -X -q -U \"$POSTGRES_USER\" -d \"$POSTGRES_DB\"";

    /** Código de salida de psql cuando el script se detiene por un error (ON_ERROR_STOP). */
    private static final int ERROR_DEL_SCRIPT = 3;

    /** Con eñe, tilde y {@code $}, para probar que pgcrypto y Spring Security leen los mismos bytes. */
    private static final String CONTRASENA = "Clave-Ñandú-2026$x";

    private static final String NIT_EMPRESA = "900123456-8";

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    @Autowired
    private PostgreSQLContainer postgres;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void copiarScript() {
        postgres.copyFileToContainer(MountableFile.forHostPath(SCRIPT), SCRIPT_EN_CONTENEDOR);
    }

    @AfterEach
    void borrarDatosDePrueba() {
        jdbc.update("DELETE FROM usuario WHERE correo LIKE '%@inspectia.test'");
        jdbc.update("DELETE FROM empresa WHERE nit = ?", NIT_EMPRESA);
    }

    @Test
    @DisplayName("RF-01 · el script crea el administrador de la plataforma sin empresa y con un hash que Spring Security acepta")
    void creaElAdministrador() throws Exception {
        Resultado resultado = correr("  Ana Pérez  ", "  Ana.Perez@InspectIA.test ", CONTRASENA);

        assertThat(resultado.codigo()).as(resultado.salida()).isZero();
        assertThat(resultado.salida()).contains("Administrador de la plataforma creado: ana.perez@inspectia.test.");
        Map<String, Object> admin = jdbc.queryForMap("""
                SELECT nombre, rol, empresa_id, activo, intentos_fallidos, bloqueado_hasta, contrasena_hash
                FROM usuario WHERE correo = 'ana.perez@inspectia.test'""");
        assertThat(admin)
                .containsEntry("nombre", "Ana Pérez")
                .containsEntry("rol", "ADMIN_PLATAFORMA")
                .containsEntry("empresa_id", null)
                .containsEntry("activo", true)
                .containsEntry("intentos_fallidos", 0)
                .containsEntry("bloqueado_hasta", null);
        String hash = (String) admin.get("contrasena_hash");
        assertThat(hash).startsWith("$2a$10$");
        assertThat(BCRYPT.matches(CONTRASENA, hash)).isTrue();
        assertThat(BCRYPT.matches("Clave-Nandu-2026$x", hash)).isFalse();
    }

    @Test
    @DisplayName("RF-01 · el script no cambia nada si el administrador ya existe, así que se puede correr varias veces")
    void noCambiaUnAdministradorQueYaExiste() throws Exception {
        correr("Ana Pérez", "ana.perez@inspectia.test", CONTRASENA);
        String hashOriginal = hashDe("ana.perez@inspectia.test");

        Resultado otraVez = correr("Otro Nombre", " ANA.PEREZ@inspectia.test", "Otra-Clave-2027");

        assertThat(otraVez.codigo()).as(otraVez.salida()).isZero();
        assertThat(otraVez.salida()).contains("ana.perez@inspectia.test ya existe: no se cambió nada.");
        assertThat(jdbc.queryForList("SELECT nombre FROM usuario WHERE correo = 'ana.perez@inspectia.test'",
                String.class)).containsExactly("Ana Pérez");
        assertThat(hashDe("ana.perez@inspectia.test")).isEqualTo(hashOriginal);
    }

    @ParameterizedTest(name = "{0} caracteres")
    @DisplayName("RF-01 · la contraseña del administrador acepta de 8 a 64 caracteres (prueba de límite)")
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
    @DisplayName("RF-01 · la contraseña del administrador ocupa como máximo 72 bytes, con el mensaje de RF-02 (prueba de límite)")
    @CsvSource(delimiter = '|', textBlock = """
            8 |
            9 | La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes
            """)
    void limiteDeBytes(int enes, String error) throws Exception {
        comprobarLimite("ñ".repeat(enes) + "a".repeat(64 - enes), error);
    }

    @ParameterizedTest(name = "«{0}»")
    @DisplayName("RF-01 · una contraseña de solo espacios cuenta como si faltara, como en la API; con una letra se acepta (prueba de límite)")
    @CsvSource(delimiter = '|', textBlock = """
            '        ' | Falta INSPECTIA_ADMIN_CONTRASENA
            '       x' |
            """)
    void contrasenaDeSoloEspacios(String contrasena, String error) throws Exception {
        comprobarLimite(contrasena, error);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("RF-01 · el script no crea nada si falta un dato o si el nombre o el correo no son válidos")
    @MethodSource("datosInvalidos")
    void rechazaDatosInvalidos(String caso, String nombre, String correo, String contrasena, String error)
            throws Exception {
        int usuariosAntes = totalUsuarios();

        Resultado resultado = correr(nombre, correo, contrasena);

        assertThat(resultado.codigo()).as(resultado.salida()).isEqualTo(ERROR_DEL_SCRIPT);
        assertThat(resultado.salida()).contains(error);
        assertThat(totalUsuarios()).isEqualTo(usuariosAntes);
    }

    static Stream<Arguments> datosInvalidos() {
        String correo = "nuevo@inspectia.test";
        return Stream.of(
                Arguments.of("sin nombre", null, correo, CONTRASENA, "Falta INSPECTIA_ADMIN_NOMBRE"),
                Arguments.of("nombre en blanco", "   ", correo, CONTRASENA, "Falta INSPECTIA_ADMIN_NOMBRE"),
                Arguments.of("nombre de 121 caracteres", "N".repeat(121), correo, CONTRASENA,
                        "El nombre puede tener como máximo 120 caracteres."),
                Arguments.of("sin correo", "Ana", null, CONTRASENA, "Falta INSPECTIA_ADMIN_CORREO"),
                Arguments.of("correo sin dominio", "Ana", "ana@inspectia", CONTRASENA,
                        "INSPECTIA_ADMIN_CORREO no es un correo válido."),
                Arguments.of("sin contraseña", "Ana", correo, null, "Falta INSPECTIA_ADMIN_CONTRASENA"),
                Arguments.of("caracteres que no venían en UTF-8", "Ana", correo, "Clave�mala-1",
                        "guarda infra/.env con codificación UTF-8"));
    }

    @Test
    @DisplayName("RF-01 · el script no toca un usuario de una empresa que tenga el mismo correo")
    void noTocaUnUsuarioDeEmpresa() throws Exception {
        UUID empresa = jdbc.queryForObject(
                "INSERT INTO empresa (nombre, nit, ciudad) VALUES ('Cacaos del Huila', ?, 'Neiva') RETURNING id",
                UUID.class, NIT_EMPRESA);
        jdbc.update("""
                INSERT INTO usuario (empresa_id, nombre, correo, contrasena_hash, rol)
                VALUES (?, 'Gerente', 'gerente@inspectia.test', 'hash-original', 'ADMIN_EMPRESA')""", empresa);

        Resultado resultado = correr("Ana Pérez", "  GERENTE@inspectia.test", CONTRASENA);

        assertThat(resultado.codigo()).as(resultado.salida()).isEqualTo(ERROR_DEL_SCRIPT);
        assertThat(resultado.salida())
                .contains("El correo gerente@inspectia.test ya es de un usuario de una empresa: usa otro correo.");
        assertThat(jdbc.queryForMap("SELECT rol, contrasena_hash FROM usuario WHERE correo = 'gerente@inspectia.test'"))
                .containsEntry("rol", "ADMIN_EMPRESA")
                .containsEntry("contrasena_hash", "hash-original");
    }

    @Test
    @DisplayName("RF-01 · la contraseña no sale en pantalla ni en el registro de PostgreSQL, aunque el servidor registre todas las sentencias")
    void laContrasenaNoQuedaEnElRegistro() throws Exception {
        String valida = "Registro-" + UUID.randomUUID();
        String demasiadoLarga = "Registro-" + "x".repeat(60);
        Map<String, String> registrarTodo = Map.of("PGOPTIONS", "-c log_statement=all");

        Resultado creado = correr("Ana Pérez", "registro@inspectia.test", valida, registrarTodo);
        Resultado rechazado = correr("Ana Pérez", "registro-2@inspectia.test", demasiadoLarga, registrarTodo);

        assertThat(creado.codigo()).as(creado.salida()).isZero();
        assertThat(rechazado.codigo()).as(rechazado.salida()).isEqualTo(ERROR_DEL_SCRIPT);
        // La primera línea comprueba que el servidor sí estaba registrando las sentencias.
        assertThat(registroCompleto())
                .contains("statement: SET LOCAL log_statement = 'none'")
                .doesNotContain(valida)
                .doesNotContain(demasiadoLarga);
    }

    /**
     * Prueba de límite de la contraseña: sin error esperado, el administrador queda creado y Spring
     * Security acepta su hash; con error, el script se detiene con ese mensaje y no crea nada.
     */
    private void comprobarLimite(String contrasena, String error) throws Exception {
        String correo = "limite@inspectia.test";
        int usuariosAntes = totalUsuarios();

        Resultado resultado = correr("Ana Pérez", correo, contrasena);

        if (error == null) {
            assertThat(resultado.codigo()).as(resultado.salida()).isZero();
            assertThat(BCRYPT.matches(contrasena, hashDe(correo))).isTrue();
        } else {
            assertThat(resultado.codigo()).as(resultado.salida()).isEqualTo(ERROR_DEL_SCRIPT);
            assertThat(resultado.salida()).contains(error);
            assertThat(totalUsuarios()).isEqualTo(usuariosAntes);
        }
    }

    private Resultado correr(String nombre, String correo, String contrasena) throws Exception {
        return correr(nombre, correo, contrasena, Map.of());
    }

    /**
     * Corre el script como los envoltorios. Un dato {@code null} es una variable de entorno que no existe.
     * En cualquier caso, la salida no puede mostrar la contraseña.
     */
    private Resultado correr(String nombre, String correo, String contrasena, Map<String, String> otras)
            throws Exception {
        Map<String, String> entorno = new HashMap<>(otras);
        entorno.put("PGCLIENTENCODING", "UTF8");
        if (nombre != null) {
            entorno.put("INSPECTIA_ADMIN_NOMBRE", nombre);
        }
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

    private String hashDe(String correo) {
        return jdbc.queryForObject("SELECT contrasena_hash FROM usuario WHERE correo = ?", String.class, correo);
    }

    private int totalUsuarios() {
        return jdbc.queryForObject("SELECT count(*) FROM usuario", Integer.class);
    }

    /** Código de salida de psql y lo que escribió en pantalla (salida estándar y de errores). */
    private record Resultado(int codigo, String salida) {
    }
}
