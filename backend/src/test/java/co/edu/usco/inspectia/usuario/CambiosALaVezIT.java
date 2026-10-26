/*
 * Nombre: CambiosALaVezIT.java
 * Descripción: Pruebas de dos cambios a la misma fila de usuario a la vez, con su orden fijo: la primera
 *              transacción lee la fila y se detiene antes de guardar, la segunda termina, y entonces la primera
 *              sigue. Cada UPDATE debe escribir solo lo que cambió: desactivar a un usuario mientras cambia su
 *              contraseña no lo reactiva, y editar su nombre mientras se restablece su contraseña no la
 *              deshace. Y el contador de sesión sube una vez por cada cambio (RF-03, RF-02 C12, RF-18,
 *              RNF-04 C6).
 * Fecha de creación: 26/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario;

import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.CONTRASENA;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.CONTRASENA_CAMBIADA;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.USUARIO_DESACTIVADO;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.RelojDePrueba;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Pruebas de dos cambios a la vez sobre el mismo usuario. Una pausa en las reglas reales fija el orden: la
 * primera petición lee al usuario y se detiene antes de guardar; la prueba hace la segunda, que termina, y
 * después suelta la primera. No hay esperas por tiempo: el límite de 30 segundos solo evita que una falla
 * deje la prueba colgada. El reloj de prueba está quieto, así que todo pasa en el mismo segundo.
 */
@PruebaIntegracion
class CambiosALaVezIT {

    private static final RelojDePrueba RELOJ = new RelojDePrueba(Instant.parse("2026-10-26T16:14:05.100Z"));

    private static final String ANA = "ana@cacaos.test";
    private static final String BEA = "bea@cacaos.test";
    private static final String LUIS = "luis@cacaos.test";
    private static final String PLATAFORMA = "plataforma@inspectia.test";
    private static final String NUEVA = "Nueva-Clave-2026";
    private static final String OTRA = "Otra-Clave-2026";

    /** El reloj del backend en estas pruebas: reemplaza al bean {@code reloj} de la aplicación. */
    @TestBean(name = "reloj", methodName = "relojDePrueba")
    private Clock reloj;

    /** Las reglas reales, con la pausa que pone cada prueba. */
    @MockitoSpyBean
    private ReglasUsuario reglas;

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiSesion api;
    private ApiUsuarios usuarios;
    private UUID empresa;
    private UUID ana;
    private UUID luis;
    private ExecutorService hilo;

    static Clock relojDePrueba() {
        return RELOJ;
    }

    @BeforeEach
    void preparar() {
        api = new ApiSesion(mvc, jdbc);
        usuarios = new ApiUsuarios(api);
        ApiSesion.borrarDatos(jdbc);
        empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        ana = api.crearUsuario(empresa, "Ana Pérez", ANA, "ADMIN_EMPRESA");
        api.crearUsuario(empresa, "Bea Ruiz", BEA, "ADMIN_EMPRESA");
        luis = api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");
        api.crearUsuario(null, "Equipo InspectIA", PLATAFORMA, "ADMIN_PLATAFORMA");
        hilo = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void terminar() {
        hilo.shutdownNow();
    }

    @Test
    @DisplayName("RF-03 · desactivar a un usuario mientras cambia su contraseña (RF-18) no lo reactiva: la contraseña cambia y el usuario sigue desactivado")
    void desactivarMientrasCambiaSuContrasena() throws Exception {
        Pausa pausa = new Pausa();
        doAnswer(pausa.respuesta()).when(reglas).cortarSesionesAlCambiarContrasena(any(), any());
        String deLuis = api.token(LUIS);
        String deAna = api.token(ANA);

        Future<MvcTestResult> cambio = hilo.submit(() -> api.enviar(HttpMethod.PUT, "/api/v1/sesion/contrasena",
                cambioDeLaPropia(CONTRASENA, NUEVA), deLuis));
        pausa.esperarQueLlegue();
        assertThat(usuarios.desactivar(deAna, luis)).hasStatus(HttpStatus.OK);
        pausa.soltar();

        assertThat(cambio.get(30, TimeUnit.SECONDS)).hasStatus(HttpStatus.OK);
        assertThat(jdbc.queryForObject("SELECT activo FROM usuario WHERE id = ?", Boolean.class, luis))
                .as("sigue desactivado").isFalse();
        assertThat(json(api.iniciar(LUIS, NUEVA))).as("con la contraseña nueva responde que está desactivado")
                .containsEntry("detail", USUARIO_DESACTIVADO);
    }

    @Test
    @DisplayName("RF-03 · editar el nombre de un administrador mientras se restablece su contraseña (RF-02 C12) no la deshace: quedan la contraseña nueva y el nombre nuevo, y su sesión de antes sigue cortada")
    void editarElNombreMientrasSeRestablece() throws Exception {
        Pausa pausa = new Pausa();
        doAnswer(pausa.respuesta()).when(reglas).cumpleNoCambiarSuPropioRol(any(), any());
        String deBea = api.token(BEA);
        String dePlataforma = api.token(PLATAFORMA);
        String deAna = api.token(ANA);

        Future<MvcTestResult> edicion = hilo.submit(() -> usuarios.editar(deBea, ana,
                ApiUsuarios.datos("Ana María Pérez", ANA, "ADMIN_EMPRESA", null)));
        pausa.esperarQueLlegue();
        assertThat(restablecer(dePlataforma, NUEVA)).hasStatus(HttpStatus.NO_CONTENT);
        pausa.soltar();

        assertThat(edicion.get(30, TimeUnit.SECONDS)).hasStatus(HttpStatus.OK);
        assertThat(jdbc.queryForObject("SELECT nombre FROM usuario WHERE id = ?", String.class, ana))
                .isEqualTo("Ana María Pérez");
        assertThat(api.iniciar(ANA, NUEVA)).as("la contraseña restablecida sigue").hasStatus(HttpStatus.OK);
        assertThat(api.iniciar(ANA, CONTRASENA)).as("la anterior no vuelve").hasStatus(HttpStatus.UNAUTHORIZED);
        sesionCortada(deAna);
    }

    @Test
    @DisplayName("RNF-04 · C6 · dos restablecimientos a la vez suben el contador de sesión dos veces: una sesión abierta entre los dos también deja de servir")
    void dosRestablecimientosALaVez() throws Exception {
        Pausa pausa = new Pausa();
        doAnswer(pausa.respuesta()).when(reglas).cortarSesionesAlCambiarContrasena(any(), any());
        String dePlataforma = api.token(PLATAFORMA);
        String deBea = api.token(BEA);

        Future<MvcTestResult> primero = hilo.submit(() -> restablecer(dePlataforma, NUEVA));
        pausa.esperarQueLlegue();
        assertThat(usuarios.editar(deBea, ana, ApiUsuarios.datos("Ana Pérez", ANA, "ADMIN_EMPRESA", OTRA)))
                .hasStatus(HttpStatus.OK);
        String entreLosDos = api.token(ANA, OTRA);
        pausa.soltar();

        assertThat(primero.get(30, TimeUnit.SECONDS)).hasStatus(HttpStatus.NO_CONTENT);
        sesionCortada(entreLosDos);
        assertThat(jdbc.queryForObject("SELECT version_sesion FROM usuario WHERE id = ?", Integer.class, ana))
                .isEqualTo(2);
        assertThat(api.iniciar(ANA, NUEVA)).as("queda la contraseña del último que guardó").hasStatus(HttpStatus.OK);
    }

    /** El administrador de la plataforma restablece la contraseña de Ana (RF-02 C12). */
    private MvcTestResult restablecer(String token, String contrasena) {
        return api.enviar(HttpMethod.PUT, "/api/v1/empresas/" + empresa + "/administradores/" + ana + "/contrasena",
                Map.of("contrasena", contrasena), token);
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

    /**
     * Detiene la primera llamada a una regla hasta que la prueba la suelta. Las demás llamadas siguen sin
     * detenerse, como la de la segunda transacción.
     */
    private static final class Pausa {

        private final CountDownLatch llego = new CountDownLatch(1);
        private final CountDownLatch seguir = new CountDownLatch(1);
        private final AtomicBoolean primera = new AtomicBoolean(true);

        /** Lo que hace la regla: en la primera llamada avisa que llegó y espera; después, la regla real. */
        Answer<Object> respuesta() {
            return llamada -> {
                if (primera.getAndSet(false)) {
                    llego.countDown();
                    assertThat(seguir.await(30, TimeUnit.SECONDS)).as("la prueba no soltó la pausa").isTrue();
                }
                return llamada.callRealMethod();
            };
        }

        void esperarQueLlegue() throws InterruptedException {
            assertThat(llego.await(30, TimeUnit.SECONDS)).as("la primera transacción no llegó a la pausa").isTrue();
        }

        void soltar() {
            seguir.countDown();
        }
    }
}
