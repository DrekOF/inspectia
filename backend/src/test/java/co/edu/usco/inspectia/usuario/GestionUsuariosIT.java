/*
 * Nombre: GestionUsuariosIT.java
 * Descripción: Pruebas de la gestión de usuarios de la empresa por la API, con PostgreSQL 18 y tokens
 *              reales: crear, editar, desactivar y reactivar con sus reglas, el límite de 25 con dos
 *              altas a la vez y el corte de sesiones al restablecer una contraseña (RF-03, RNF-04).
 * Fecha de creación: 18/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.usuario;

import static co.edu.usco.inspectia.usuario.ApiUsuarios.CONTRASENA_BYTES;
import static co.edu.usco.inspectia.usuario.ApiUsuarios.CONTRASENA_CORTA;
import static co.edu.usco.inspectia.usuario.ApiUsuarios.CONTRASENA_LARGA;
import static co.edu.usco.inspectia.usuario.ApiUsuarios.CORREO_REPETIDO;
import static co.edu.usco.inspectia.usuario.ApiUsuarios.OBLIGATORIO;
import static co.edu.usco.inspectia.usuario.ApiUsuarios.REGLA_CONTRASENA;
import static co.edu.usco.inspectia.usuario.ApiUsuarios.REGLA_DESACTIVARSE;
import static co.edu.usco.inspectia.usuario.ApiUsuarios.REGLA_MAXIMO;
import static co.edu.usco.inspectia.usuario.ApiUsuarios.REGLA_ROL;
import static co.edu.usco.inspectia.usuario.ApiUsuarios.ROL_NO_PERMITIDO;
import static co.edu.usco.inspectia.usuario.ApiUsuarios.VALOR_INVALIDO;
import static co.edu.usco.inspectia.usuario.ApiUsuarios.datos;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.CONTRASENA;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.CONTRASENA_CAMBIADA;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.USUARIO_DESACTIVADO;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.error;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.errores;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import co.edu.usco.inspectia.usuario.sesion.TokensDePrueba;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Pruebas de la gestión de usuarios ({@code /api/v1/usuarios}) con el token real de un administrador
 * de empresa. Los usuarios de partida se crean directamente en la base; las sesiones abiertas «de
 * antes» las arma {@link TokensDePrueba}, emitidas unos segundos atrás.
 */
@PruebaIntegracion
class GestionUsuariosIT {

    private static final String ADMIN = "admin@cacaos.test";
    private static final String BETO = "beto@cacaos.test";
    private static final String LUIS = "luis@cacaos.test";
    private static final String NUEVA = "Nueva-Clave-2026";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource fuente;

    private ApiSesion api;
    private ApiUsuarios usuarios;
    private UUID empresa;
    private UUID admin;
    private String token;

    @BeforeEach
    void preparar() {
        api = new ApiSesion(mvc, jdbc);
        usuarios = new ApiUsuarios(api);
        ApiSesion.borrarDatos(jdbc);
        empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        admin = api.crearUsuario(empresa, "Ana Pérez", ADMIN, "ADMIN_EMPRESA");
        token = api.token(ADMIN);
    }

    @AfterEach
    void limpiar() {
        ApiSesion.borrarDatos(jdbc);
    }

    @Test
    @DisplayName("RF-03 · C1 · un usuario creado con datos válidos puede iniciar sesión, con su rol y la empresa del administrador")
    void unUsuarioCreadoIniciaSesion() {
        MvcTestResult creado = usuarios.crear(token, datos("Luis Gómez", LUIS, "USUARIO_FINAL", CONTRASENA));

        assertThat(creado).hasStatus(HttpStatus.CREATED);
        Map<String, Object> usuario = json(creado);
        assertThat(usuario).as("nunca trae la contraseña, su hash, los intentos ni el bloqueo")
                .containsOnlyKeys("id", "nombre", "correo", "rol", "activo", "fechaCreacion", "fechaActualizacion")
                .containsEntry("nombre", "Luis Gómez").containsEntry("correo", LUIS)
                .containsEntry("rol", "USUARIO_FINAL").containsEntry("activo", true);
        assertThat(creado.getResponse().getHeader(HttpHeaders.LOCATION)).endsWith(ApiUsuarios.RUTA + "/" + usuario.get("id"));
        MvcTestResult sesion = api.iniciar(LUIS, CONTRASENA);
        assertThat(sesion).hasStatus(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> enSesion = (Map<String, Object>) json(sesion).get("usuario");
        assertThat(enSesion).containsEntry("rol", "USUARIO_FINAL")
                .containsEntry("empresa", Map.of("id", empresa.toString(), "nombre", "Cacaos del Huila"));
    }

    @Test
    @DisplayName("RF-03 · C2 · no se puede crear un usuario con un correo que ya existe en la plataforma, aunque sea de otra empresa o cambie en mayúsculas")
    void elCorreoEsUnicoEnLaPlataforma() {
        UUID otra = api.crearEmpresa("Cacao del Sur", "800765432-1");
        api.crearUsuario(otra, "Marta Ruiz", "marta@cacaosur.test", "USUARIO_FINAL");
        int antes = usuariosEnLaBase();

        MvcTestResult deLaMisma = usuarios.crear(token, datos("Otra Ana", "ADMIN@Cacaos.test", "USUARIO_FINAL", CONTRASENA));
        MvcTestResult deOtra = usuarios.crear(token, datos("Otra Marta", "marta@cacaosur.test", "USUARIO_FINAL", CONTRASENA));

        for (MvcTestResult resultado : List.of(deLaMisma, deOtra)) {
            assertThat(resultado).hasStatus(HttpStatus.CONFLICT);
            assertThat(errores(resultado)).containsExactly(error("correo", CORREO_REPETIDO));
        }
        assertThat(usuariosEnLaBase()).isEqualTo(antes);
    }

    @ParameterizedTest(name = "{0} caracteres: {1}")
    @DisplayName("RF-03 · C3 · la contraseña acepta de 8 a 64 caracteres: con 7 o con 65 se rechaza (prueba de límite)")
    @CsvSource({"7, 400", "8, 201", "64, 201", "65, 400"})
    void largoDeLaContrasena(int caracteres, int estado) {
        MvcTestResult resultado = usuarios.crear(token, datos("Luis Gómez", LUIS, "USUARIO_FINAL", "x".repeat(caracteres)));

        assertThat(resultado).hasStatus(estado);
        if (estado == 400) {
            assertThat(errores(resultado))
                    .containsExactly(error("contrasena", caracteres < 8 ? CONTRASENA_CORTA : CONTRASENA_LARGA));
        }
    }

    @ParameterizedTest(name = "{0} eñes: {1}")
    @DisplayName("RF-03 · C4 · una contraseña de 64 caracteres con 8 eñes (72 bytes) se acepta y con 9 (73 bytes) se rechaza con su mensaje (prueba de límite)")
    @CsvSource({"8, 201", "9, 400"})
    void bytesDeLaContrasena(int enes, int estado) {
        String contrasena = "ñ".repeat(enes) + "a".repeat(64 - enes);

        MvcTestResult resultado = usuarios.crear(token, datos("Luis Gómez", LUIS, "USUARIO_FINAL", contrasena));

        assertThat(resultado).hasStatus(estado);
        if (estado == 400) {
            assertThat(errores(resultado)).containsExactly(error("contrasena", CONTRASENA_BYTES));
        } else {
            assertThat(api.iniciar(LUIS, contrasena)).hasStatus(HttpStatus.OK);
        }
    }

    @ParameterizedTest(name = "al {0}, «{1}»: {2}")
    @DisplayName("RF-03 · una contraseña de solo espacios se rechaza al crear y al restablecer, con el mismo mensaje que si faltara; con una letra se acepta (prueba de límite)")
    @CsvSource({
        "crear,       '        ', 400",
        "crear,       '       x', 201",
        "restablecer, '        ', 400",
        "restablecer, '       x', 200"})
    void contrasenaDeSoloEspacios(String operacion, String contrasena, int estado) {
        MvcTestResult resultado;
        if ("crear".equals(operacion)) {
            resultado = usuarios.crear(token, datos("Luis Gómez", LUIS, "USUARIO_FINAL", contrasena));
        } else {
            UUID luis = api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");
            resultado = usuarios.editar(token, luis, datos("Luis Gómez", LUIS, "USUARIO_FINAL", contrasena));
        }

        assertThat(resultado).hasStatus(estado);
        if (estado == 400) {
            assertThat(errores(resultado)).containsExactly(error("contrasena", OBLIGATORIO));
        } else {
            assertThat(api.iniciar(LUIS, contrasena)).as("entra con ella, espacios incluidos").hasStatus(HttpStatus.OK);
        }
    }

    @Test
    @DisplayName("RF-03 · C5 · con 25 usuarios activos no deja crear ni reactivar otro, y explica por qué (prueba de límite)")
    void maximoVeinticincoActivos() {
        for (int i = 1; i <= 23; i++) {
            api.crearUsuario(empresa, "Usuario " + i, "usuario" + i + "@cacaos.test", "USUARIO_FINAL");
        }
        UUID inactivo = api.crearUsuario(empresa, "Usuario inactivo", "inactivo@cacaos.test", "USUARIO_FINAL");
        jdbc.update("UPDATE usuario SET activo = false WHERE id = ?", inactivo);
        assertThat(usuarios.crear(token, datos("Usuario 24", "usuario24@cacaos.test", "USUARIO_FINAL", CONTRASENA)))
                .as("el activo número 25 sí se crea").hasStatus(HttpStatus.CREATED);

        MvcTestResult otro = usuarios.crear(token, datos("Usuario 25", "usuario25@cacaos.test", "USUARIO_FINAL", CONTRASENA));
        MvcTestResult reactivado = usuarios.reactivar(token, inactivo);

        for (MvcTestResult resultado : List.of(otro, reactivado)) {
            assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(errores(resultado)).containsExactly(error(null, REGLA_MAXIMO));
        }
        assertThat(activosDeLaEmpresa()).isEqualTo(25);
    }

    @ParameterizedTest(name = "el otro administrador {0}")
    @DisplayName("RF-03 · C5 · con 24 activos, si dos administradores crean o reactivan un usuario al mismo tiempo, solo uno lo logra y la empresa queda con 25 (prueba de límite)")
    @ValueSource(strings = {"crea", "reactiva"})
    void dosAltasAlMismoTiempo(String otro) throws Exception {
        api.crearUsuario(empresa, "Beto Díaz", BETO, "ADMIN_EMPRESA");
        for (int i = 1; i <= 22; i++) {
            api.crearUsuario(empresa, "Usuario " + i, "usuario" + i + "@cacaos.test", "USUARIO_FINAL");
        }
        UUID inactivo = api.crearUsuario(empresa, "Usuario inactivo", "inactivo@cacaos.test", "USUARIO_FINAL");
        jdbc.update("UPDATE usuario SET activo = false WHERE id = ?", inactivo);
        String tokenBeto = api.token(BETO);
        assertThat(activosDeLaEmpresa()).isEqualTo(24);

        List<MvcTestResult> resultados = aLaVez(
                () -> usuarios.crear(token, datos("Usuario 23", "usuario23@cacaos.test", "USUARIO_FINAL", CONTRASENA)),
                "crea".equals(otro)
                        ? () -> usuarios.crear(tokenBeto, datos("Usuario 24", "usuario24@cacaos.test", "USUARIO_FINAL",
                                CONTRASENA))
                        : () -> usuarios.reactivar(tokenBeto, inactivo));

        assertThat(resultados).filteredOn(resultado -> resultado.getResponse().getStatus() == 400)
                .as("uno de los dos se rechaza con el mensaje de la regla").singleElement()
                .satisfies(rechazado -> assertThat(errores(rechazado)).containsExactly(error(null, REGLA_MAXIMO)));
        assertThat(resultados).filteredOn(resultado -> resultado.getResponse().getStatus() < 300)
                .as("y el otro se guarda").hasSize(1);
        assertThat(activosDeLaEmpresa()).isEqualTo(25);
    }

    @Test
    @DisplayName("RF-03 · C6 · un usuario desactivado no puede iniciar sesión")
    void unUsuarioDesactivadoNoEntra() {
        UUID luis = api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");

        MvcTestResult desactivado = usuarios.desactivar(token, luis);

        assertThat(desactivado).hasStatus(HttpStatus.OK);
        assertThat(json(desactivado)).containsEntry("activo", false);
        MvcTestResult inicio = api.iniciar(LUIS, CONTRASENA);
        assertThat(inicio).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(json(inicio)).containsEntry("detail", USUARIO_DESACTIVADO);
    }

    @Test
    @DisplayName("RF-03 · al reactivar un usuario, vuelve a iniciar sesión")
    void unUsuarioReactivadoEntra() {
        UUID luis = api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");
        jdbc.update("UPDATE usuario SET activo = false WHERE id = ?", luis);

        MvcTestResult reactivado = usuarios.reactivar(token, luis);

        assertThat(reactivado).hasStatus(HttpStatus.OK);
        assertThat(json(reactivado)).containsEntry("activo", true);
        assertThat(api.iniciar(LUIS, CONTRASENA)).hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("RF-03 · C7 · el administrador no puede desactivarse a sí mismo")
    void noSeDesactivaASiMismo() {
        MvcTestResult resultado = usuarios.desactivar(token, admin);

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(error(null, REGLA_DESACTIVARSE));
        assertThat(jdbc.queryForObject("SELECT activo FROM usuario WHERE id = ?", Boolean.class, admin)).isTrue();
    }

    @Test
    @DisplayName("RF-03 · C8 · el administrador no puede cambiar su propio rol: no guarda el cambio y explica por qué")
    void noCambiaSuPropioRol() {
        MvcTestResult resultado = usuarios.editar(token, admin, datos("Ana Pérez", ADMIN, "USUARIO_FINAL", null));

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(error("rol", REGLA_ROL));
        assertThat(rol(admin)).isEqualTo("ADMIN_EMPRESA");
        assertThat(usuarios.editar(token, admin, datos("Ana María Pérez", ADMIN, "ADMIN_EMPRESA", null)))
                .as("sí puede editar sus otros datos").hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("RF-03 · C10 · si el administrador envía un usuario con rol ADMIN_PLATAFORMA, la API responde 403 y no guarda nada (prueba de seguridad)")
    void noAsignaElRolDePlataforma() {
        UUID luis = api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");
        int antes = usuariosEnLaBase();

        MvcTestResult creado = usuarios.crear(token, datos("Marta Ruiz", "marta@cacaos.test", "ADMIN_PLATAFORMA", CONTRASENA));
        MvcTestResult editado = usuarios.editar(token, luis, datos("Luis Gómez", LUIS, "ADMIN_PLATAFORMA", null));

        for (MvcTestResult resultado : List.of(creado, editado)) {
            assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(errores(resultado)).containsExactly(error("rol", ROL_NO_PERMITIDO));
        }
        assertThat(usuariosEnLaBase()).isEqualTo(antes);
        assertThat(rol(luis)).isEqualTo("USUARIO_FINAL");
    }

    @Test
    @DisplayName("RF-03 · C10 · el 403 del rol va antes que los demás errores de los datos")
    void elPermisoAntesQueLosDatos() {
        MvcTestResult resultado = usuarios.crear(token, datos(" ", "sin-arroba", "ADMIN_PLATAFORMA", "corta"));

        assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(errores(resultado)).containsExactly(error("rol", ROL_NO_PERMITIDO));
    }

    @Test
    @DisplayName("RF-03 · un rol que no existe responde 400 con el campo rol")
    void unRolQueNoExiste() {
        MvcTestResult resultado = usuarios.crear(token, datos("Marta Ruiz", "marta@cacaos.test", "JEFE", CONTRASENA));

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(error("rol", VALOR_INVALIDO));
    }

    @Test
    @DisplayName("RF-03 · C11 · el administrador no puede cambiar su propia contraseña desde la gestión de usuarios: para eso está «Cambiar contraseña»")
    void noCambiaSuPropiaContrasena() {
        MvcTestResult resultado = usuarios.editar(token, admin, datos("Ana Pérez", ADMIN, "ADMIN_EMPRESA", NUEVA));

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(error("contrasena", REGLA_CONTRASENA));
        assertThat(api.iniciar(ADMIN, CONTRASENA)).as("su contraseña no cambió").hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("RNF-04 · C6 · al restablecer la contraseña de un usuario desde la gestión de usuarios, sus sesiones abiertas dejan de servir")
    void restablecerCortaLasSesiones() {
        UUID luis = api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");
        String abierta = TokensDePrueba.token(luis, Instant.now().minusSeconds(5));
        assertThat(api.sesion(abierta)).hasStatus(HttpStatus.OK);

        MvcTestResult editado = usuarios.editar(token, luis, datos("Luis Gómez", LUIS, "USUARIO_FINAL", NUEVA));

        assertThat(editado).hasStatus(HttpStatus.OK);
        MvcTestResult cortada = api.sesion(abierta);
        assertThat(cortada).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(json(cortada)).containsEntry("detail", CONTRASENA_CAMBIADA);
        assertThat(api.iniciar(LUIS, CONTRASENA)).as("la anterior ya no entra").hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(api.iniciar(LUIS, NUEVA)).as("la nueva sí").hasStatus(HttpStatus.OK);
        assertThat(api.sesion(token)).as("la sesión del administrador sigue").hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("RNF-04 · editar un usuario sin cambiar su contraseña no corta sus sesiones")
    void editarSinContrasenaNoCortaSesiones() {
        UUID luis = api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");
        String abierta = TokensDePrueba.token(luis, Instant.now().minusSeconds(5));

        MvcTestResult editado = usuarios.editar(token, luis, datos("Luis Alberto Gómez", LUIS, "USUARIO_FINAL", null));

        assertThat(editado).hasStatus(HttpStatus.OK);
        assertThat(json(editado)).containsEntry("nombre", "Luis Alberto Gómez");
        assertThat(api.sesion(abierta)).hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("RF-03 · restablecer la contraseña de un usuario desbloquea su cuenta")
    void restablecerDesbloquea() {
        UUID luis = api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");
        jdbc.update("UPDATE usuario SET intentos_fallidos = 3, bloqueado_hasta = now() + interval '10 minutes' WHERE id = ?",
                luis);

        assertThat(usuarios.editar(token, luis, datos("Luis Gómez", LUIS, "USUARIO_FINAL", NUEVA))).hasStatus(HttpStatus.OK);

        assertThat(api.iniciar(LUIS, NUEVA)).hasStatus(HttpStatus.OK);
    }

    /**
     * Manda dos peticiones al mismo tiempo, cada una en su hilo. Mientras salen, la prueba tiene tomada
     * la fila de la empresa ({@code SELECT … FOR UPDATE}), así las dos llegan a la base antes de que
     * alguna termine, sin depender de la suerte. La suelta cuando cada una terminó o quedó esperando un
     * bloqueo.
     *
     * <p>Sin el bloqueo del límite de 25, las dos cuentan 24 y la prueba suelta la empresa cuando las dos
     * esperan para guardar: quedarían 26. Con él, las dos esperan antes de contar y se atienden una tras
     * otra.
     */
    private List<MvcTestResult> aLaVez(Callable<MvcTestResult> primera, Callable<MvcTestResult> segunda)
            throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try (Connection bloqueo = fuente.getConnection()) {
            bloqueo.setAutoCommit(false);
            try (PreparedStatement sentencia = bloqueo.prepareStatement("SELECT id FROM empresa WHERE id = ? FOR UPDATE")) {
                sentencia.setObject(1, empresa);
                sentencia.executeQuery().close();
            }
            List<Future<MvcTestResult>> peticiones = List.of(hilos.submit(primera), hilos.submit(segunda));
            esperarALasDos(peticiones);
            bloqueo.commit();
            List<MvcTestResult> resultados = new ArrayList<>();
            for (Future<MvcTestResult> peticion : peticiones) {
                resultados.add(peticion.get(30, TimeUnit.SECONDS));
            }
            return resultados;
        } finally {
            hilos.shutdownNow();
        }
    }

    /** Espera, hasta 10 segundos, a que cada petición haya terminado o esté esperando un bloqueo de la base. */
    private void esperarALasDos(List<Future<MvcTestResult>> peticiones) throws InterruptedException {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (peticiones.stream().filter(Future::isDone).count() + esperandoUnBloqueo() < peticiones.size()) {
            assertThat(System.nanoTime()).as("las dos peticiones no llegaron a la base").isLessThan(limite);
            Thread.sleep(20);
        }
    }

    /** Cuántas conexiones a la base esperan un bloqueo que tiene otra transacción. */
    private int esperandoUnBloqueo() {
        return jdbc.queryForObject("""
                SELECT count(*) FROM pg_stat_activity
                WHERE datname = current_database() AND wait_event_type = 'Lock'""", Integer.class);
    }

    private int usuariosEnLaBase() {
        return jdbc.queryForObject("SELECT count(*) FROM usuario", Integer.class);
    }

    private int activosDeLaEmpresa() {
        return jdbc.queryForObject("SELECT count(*) FROM usuario WHERE empresa_id = ? AND activo", Integer.class, empresa);
    }

    private String rol(UUID id) {
        return jdbc.queryForObject("SELECT rol FROM usuario WHERE id = ?", String.class, id);
    }
}
