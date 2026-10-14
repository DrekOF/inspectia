/*
 * Nombre: InicioSesionIT.java
 * Descripción: Pruebas del inicio de sesión por la API, con PostgreSQL 18: credenciales, usuarios y
 *              empresas desactivados, y bloqueo por intentos fallidos (RF-01, RNF-04, RF-02).
 * Fecha de creación: 14/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario.sesion;

import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.AYUDA_BLOQUEO;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.CONTRASENA;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.CREDENCIALES_INCORRECTAS;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.EMPRESA_DESACTIVADA;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.USUARIO_DESACTIVADO;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.usuario;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.PruebaIntegracion;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Pruebas del inicio de sesión ({@code POST /api/v1/sesion}). Los usuarios se crean directamente en la
 * base, con su contraseña en BCrypt, salvo en las de RF-02, que registran la empresa por su API con
 * el token de un administrador de la plataforma.
 */
@PruebaIntegracion
class InicioSesionIT {

    private static final String CORREO = "ana@cacaos.test";
    private static final String PLATAFORMA = "plataforma@inspectia.test";
    private static final String ERRADA = "Otra-Clave-2026";
    private static final Duration VIGENCIA = Duration.ofHours(8);

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiSesion api;

    @BeforeEach
    void preparar() {
        api = new ApiSesion(mvc, jdbc);
        ApiSesion.borrarDatos(jdbc);
    }

    @AfterEach
    void limpiar() {
        ApiSesion.borrarDatos(jdbc);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("RF-01 · C1 · con datos correctos, cada rol inicia sesión y recibe su rol, su nombre y su empresa para armar su menú")
    @ValueSource(strings = {"ADMIN_PLATAFORMA", "ADMIN_EMPRESA", "USUARIO_FINAL"})
    void cadaRolIniciaSesion(String rol) {
        UUID empresa = "ADMIN_PLATAFORMA".equals(rol) ? null : api.crearEmpresa("Cacaos del Huila", "900123456-8");
        UUID id = api.crearUsuario(empresa, "Ana Pérez", CORREO, rol);
        Instant antes = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        MvcTestResult resultado = api.iniciar(CORREO, CONTRASENA);

        assertThat(resultado).hasStatus(HttpStatus.OK);
        Map<String, Object> sesion = json(resultado);
        Map<String, Object> esperado = usuario(id, "Ana Pérez", CORREO, rol, empresa, "Cacaos del Huila");
        assertThat(sesion).containsEntry("tipo", "Bearer").containsEntry("usuario", esperado);
        assertThat((String) sesion.get("token")).isNotBlank();
        assertThat(Instant.parse((String) sesion.get("vence")))
                .isBetween(antes.plus(VIGENCIA), Instant.now().plus(VIGENCIA));

        MvcTestResult actual = api.sesion((String) sesion.get("token"));

        assertThat(actual).hasStatus(HttpStatus.OK);
        assertThat(json(actual)).containsEntry("usuario", esperado).containsEntry("vence", sesion.get("vence"));
    }

    @Test
    @DisplayName("RF-01 · C2 · con datos incorrectos responde «Correo o contraseña incorrectos», igual si falló el correo o la contraseña")
    void laMismaRespuestaSinDecirCualFallo() {
        api.crearUsuario(null, "Ana Pérez", CORREO, "ADMIN_PLATAFORMA");

        MvcTestResult contrasenaErrada = api.iniciar(CORREO, ERRADA);
        MvcTestResult correoInexistente = api.iniciar("nadie@cacaos.test", ERRADA);

        assertThat(contrasenaErrada).hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(correoInexistente).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(json(contrasenaErrada))
                .containsEntry("detail", CREDENCIALES_INCORRECTAS)
                .containsEntry("ayuda", AYUDA_BLOQUEO)
                .isEqualTo(json(correoInexistente));
    }

    @Test
    @DisplayName("RF-01 · el correo se compara sin distinguir mayúsculas ni espacios al inicio o al final")
    void elCorreoSeNormaliza() {
        api.crearUsuario(null, "Ana Pérez", CORREO, "ADMIN_PLATAFORMA");

        assertThat(api.iniciar("  ANA@Cacaos.TEST ", CONTRASENA)).hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("RF-01 · si falta el correo o la contraseña, responde 400 con cada campo que falta")
    void faltanDatos() {
        MvcTestResult resultado = api.iniciar(" ", null);

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> errores = (List<Map<String, Object>>) json(resultado).get("errores");
        assertThat(errores).extracting(error -> error.get("campo")).containsExactlyInAnyOrder("correo", "contrasena");
    }

    @Test
    @DisplayName("RF-01 · C3 · un usuario desactivado no puede iniciar sesión")
    void unUsuarioDesactivadoNoEntra() {
        UUID empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        UUID id = api.crearUsuario(empresa, "Ana Pérez", CORREO, "USUARIO_FINAL");
        jdbc.update("UPDATE usuario SET activo = false WHERE id = ?", id);

        MvcTestResult conLaCorrecta = api.iniciar(CORREO, CONTRASENA);
        MvcTestResult conUnaErrada = api.iniciar(CORREO, ERRADA);

        assertThat(conLaCorrecta).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(json(conLaCorrecta)).containsEntry("detail", USUARIO_DESACTIVADO);
        assertThat(json(conUnaErrada)).as("con una contraseña errada no se revela que está desactivado")
                .containsEntry("detail", CREDENCIALES_INCORRECTAS);
    }

    @Test
    @DisplayName("RF-01 · C4 · un usuario de una empresa desactivada no puede iniciar sesión")
    void elUsuarioDeUnaEmpresaDesactivadaNoEntra() {
        UUID empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        api.crearUsuario(empresa, "Ana Pérez", CORREO, "ADMIN_EMPRESA");
        jdbc.update("UPDATE empresa SET activa = false WHERE id = ?", empresa);

        MvcTestResult resultado = api.iniciar(CORREO, CONTRASENA);

        assertThat(resultado).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(json(resultado)).containsEntry("detail", EMPRESA_DESACTIVADA);
    }

    @Test
    @DisplayName("RF-01 · C5 · después de 5 intentos fallidos seguidos la cuenta se bloquea: ni la contraseña correcta entra hasta que pasen 15 minutos")
    void cincoFallosBloqueanLaCuenta() {
        UUID id = api.crearUsuario(null, "Ana Pérez", CORREO, "ADMIN_PLATAFORMA");
        fallar(5);

        MvcTestResult bloqueada = api.iniciar(CORREO, CONTRASENA);

        assertThat(bloqueada).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(json(bloqueada)).as("el bloqueo responde igual que un correo que no existe")
                .isEqualTo(json(api.iniciar("nadie@cacaos.test", ERRADA)));
        jdbc.update("UPDATE usuario SET bloqueado_hasta = now() - interval '1 second' WHERE id = ?", id);
        assertThat(api.iniciar(CORREO, CONTRASENA)).as("pasados los 15 minutos").hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("RF-01 · C5 · los intentos fallidos deben ser seguidos: iniciar sesión vuelve a contar desde cero")
    void unInicioCorrectoReiniciaLaCuenta() {
        api.crearUsuario(null, "Ana Pérez", CORREO, "ADMIN_PLATAFORMA");
        fallar(4);
        assertThat(api.iniciar(CORREO, CONTRASENA)).hasStatus(HttpStatus.OK);
        fallar(4);

        assertThat(api.iniciar(CORREO, CONTRASENA)).hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("RNF-04 · C5 · el bloqueo dura 15 minutos desde el quinto fallo y los intentos durante el bloqueo no lo alargan")
    void elBloqueoDuraQuinceMinutos() {
        UUID id = api.crearUsuario(null, "Ana Pérez", CORREO, "ADMIN_PLATAFORMA");
        fallar(4);
        Instant antes = Instant.now();
        fallar(1);
        Instant despues = Instant.now();

        OffsetDateTime hasta = bloqueadoHasta(id);

        assertThat(hasta.toInstant()).isBetween(antes.plus(Duration.ofMinutes(15)).minusSeconds(1),
                despues.plus(Duration.ofMinutes(15)).plusSeconds(1));
        fallar(1);
        api.iniciar(CORREO, CONTRASENA);
        assertThat(bloqueadoHasta(id)).isEqualTo(hasta);
    }

    @Test
    @DisplayName("RF-02 · C8 · el administrador creado con la empresa inicia sesión con su contraseña inicial")
    void elAdministradorDeLaEmpresaEntra() {
        String plataforma = tokenDePlataforma();
        UUID empresa = registrarEmpresa(plataforma);

        MvcTestResult resultado = api.iniciar(CORREO, CONTRASENA);

        assertThat(resultado).hasStatus(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> usuario = (Map<String, Object>) json(resultado).get("usuario");
        assertThat(usuario)
                .containsEntry("rol", "ADMIN_EMPRESA")
                .containsEntry("empresa", Map.of("id", empresa.toString(), "nombre", "Cacaos del Huila"));
    }

    @Test
    @DisplayName("RF-02 · C9 · al desactivar una empresa, sus usuarios no pueden iniciar sesión")
    void alDesactivarLaEmpresaSusUsuariosNoEntran() {
        String plataforma = tokenDePlataforma();
        UUID empresa = registrarEmpresa(plataforma);

        assertThat(api.enviar(HttpMethod.POST, "/api/v1/empresas/" + empresa + "/desactivar", null, plataforma))
                .hasStatus(HttpStatus.OK);

        MvcTestResult resultado = api.iniciar(CORREO, CONTRASENA);
        assertThat(resultado).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(json(resultado)).containsEntry("detail", EMPRESA_DESACTIVADA);
    }

    @Test
    @DisplayName("RF-02 · C10 · al reactivar una empresa, sus usuarios activos vuelven a iniciar sesión y los desactivados no")
    void alReactivarLaEmpresaVuelvenSusUsuariosActivos() {
        String plataforma = tokenDePlataforma();
        UUID empresa = registrarEmpresa(plataforma);
        UUID inactivo = api.crearUsuario(empresa, "Luis Gómez", "luis@cacaos.test", "USUARIO_FINAL");
        jdbc.update("UPDATE usuario SET activo = false WHERE id = ?", inactivo);
        api.enviar(HttpMethod.POST, "/api/v1/empresas/" + empresa + "/desactivar", null, plataforma);

        assertThat(api.enviar(HttpMethod.POST, "/api/v1/empresas/" + empresa + "/reactivar", null, plataforma))
                .hasStatus(HttpStatus.OK);

        assertThat(api.iniciar(CORREO, CONTRASENA)).hasStatus(HttpStatus.OK);
        MvcTestResult desactivado = api.iniciar("luis@cacaos.test", CONTRASENA);
        assertThat(desactivado).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(json(desactivado)).containsEntry("detail", USUARIO_DESACTIVADO);
    }

    private void fallar(int veces) {
        for (int i = 0; i < veces; i++) {
            assertThat(api.iniciar(CORREO, ERRADA)).hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    private OffsetDateTime bloqueadoHasta(UUID id) {
        return jdbc.queryForObject("SELECT bloqueado_hasta FROM usuario WHERE id = ?", OffsetDateTime.class, id);
    }

    /** Crea un administrador de la plataforma e inicia sesión con él. */
    private String tokenDePlataforma() {
        api.crearUsuario(null, "Equipo InspectIA", PLATAFORMA, "ADMIN_PLATAFORMA");
        return api.token(PLATAFORMA);
    }

    /** Registra por la API la empresa con su primer administrador, de correo {@link #CORREO}. */
    private UUID registrarEmpresa(String token) {
        Map<String, Object> administrador = new LinkedHashMap<>();
        administrador.put("nombre", "Ana Pérez");
        administrador.put("correo", CORREO);
        administrador.put("contrasena", CONTRASENA);
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", "Cacaos del Huila");
        datos.put("nit", "900123456-8");
        datos.put("ciudad", "Neiva");
        datos.put("administrador", administrador);
        MvcTestResult resultado = api.enviar(HttpMethod.POST, "/api/v1/empresas", datos, token);
        assertThat(resultado).hasStatus(HttpStatus.CREATED);
        return UUID.fromString((String) json(resultado).get("id"));
    }
}
