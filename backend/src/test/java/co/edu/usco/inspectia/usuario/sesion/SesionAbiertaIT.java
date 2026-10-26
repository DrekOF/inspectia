/*
 * Nombre: SesionAbiertaIT.java
 * Descripción: Pruebas de una sesión abierta, por la API y con PostgreSQL 18: vencimiento a las 8
 *              horas, volver a entrar con la sesión vencida, revisión del usuario y su empresa en
 *              cada petición y tokens que no sirven (RF-01, RNF-04, D-28).
 * Fecha de creación: 14/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia.usuario.sesion;

import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.CONTRASENA;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.EMPRESA_DESACTIVADA;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.SESION_INVALIDA;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.SESION_VENCIDA;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.SIN_SESION;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.USUARIO_DESACTIVADO;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.PruebaIntegracion;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Pruebas de una sesión abierta ({@code GET /api/v1/sesion} y la API de empresas con un token real).
 * Los tokens vencidos, alterados, sin firma o de otra clave los arma {@link TokensDePrueba}, sin usar
 * el código del backend.
 */
@PruebaIntegracion
class SesionAbiertaIT {

    private static final String CORREO = "ana@cacaos.test";

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

    @Test
    @DisplayName("RF-01 · C3 · al desactivar un usuario, su sesión abierta deja de servir en la siguiente petición")
    void desactivarElUsuarioCortaSuSesion() {
        UUID empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        UUID id = api.crearUsuario(empresa, "Ana Pérez", CORREO, "ADMIN_EMPRESA");
        String token = api.token(CORREO);
        assertThat(api.sesion(token)).hasStatus(HttpStatus.OK);
        jdbc.update("UPDATE usuario SET activo = false WHERE id = ?", id);

        rechazada(api.sesion(token), USUARIO_DESACTIVADO);
    }

    @Test
    @DisplayName("RF-01 · C4 · al desactivar una empresa, las sesiones abiertas de sus usuarios dejan de servir")
    void desactivarLaEmpresaCortaLasSesiones() {
        UUID empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        api.crearUsuario(empresa, "Ana Pérez", CORREO, "USUARIO_FINAL");
        String token = api.token(CORREO);
        jdbc.update("UPDATE empresa SET activa = false WHERE id = ?", empresa);

        rechazada(api.sesion(token), EMPRESA_DESACTIVADA);
    }

    @ParameterizedTest(name = "emitido hace {0} minutos: {1}")
    @DisplayName("RF-01 · C6 · la sesión vence a las 8 horas y pide iniciar sesión de nuevo (prueba de límite)")
    @CsvSource({"479, 200", "481, 401"})
    void laSesionVenceALasOchoHoras(int minutos, int estado) {
        UUID id = api.crearUsuario(null, "Ana Pérez", CORREO, "ADMIN_PLATAFORMA");
        String token = TokensDePrueba.token(id, Instant.now().minus(Duration.ofMinutes(minutos)));

        MvcTestResult resultado = api.sesion(token);

        assertThat(resultado).hasStatus(estado);
        if (estado == 401) {
            assertThat(json(resultado)).containsEntry("detail", SESION_VENCIDA);
        }
    }

    @Test
    @DisplayName("RF-01 · C6 · con la sesión vencida se puede volver a iniciar sesión, aunque el frontend todavía envíe el token viejo")
    void conLaSesionVencidaSePuedeVolverAEntrar() {
        UUID id = api.crearUsuario(null, "Ana Pérez", CORREO, "ADMIN_PLATAFORMA");
        String vencido = TokensDePrueba.token(id, Instant.now().minus(Duration.ofHours(9)));
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("correo", CORREO);
        datos.put("contrasena", CONTRASENA);

        MvcTestResult conTokenVencido = api.enviar(HttpMethod.POST, ApiSesion.RUTA, datos, vencido);
        MvcTestResult conTokenMalFormado = api.enviar(HttpMethod.POST, ApiSesion.RUTA, datos, "abc.def");

        assertThat(conTokenVencido).hasStatus(HttpStatus.OK);
        assertThat(conTokenMalFormado).hasStatus(HttpStatus.OK);
        rechazada(api.sesion(vencido), SESION_VENCIDA);
    }

    @Test
    @DisplayName("RNF-04 · C4 · el token del inicio de sesión dura exactamente 8 horas y solo lleva el emisor, el usuario, las fechas y la versión de sesión, que empieza en 0")
    void elTokenDuraOchoHoras() {
        UUID id = api.crearUsuario(null, "Ana Pérez", CORREO, "ADMIN_PLATAFORMA");

        MvcTestResult inicio = api.iniciar(CORREO, CONTRASENA);

        assertThat(inicio).hasStatus(HttpStatus.OK);
        Map<String, Object> contenido = TokensDePrueba.leer((String) json(inicio).get("token"));
        assertThat(contenido).containsOnlyKeys("iss", "sub", "iat", "exp", "version_sesion")
                .containsEntry("iss", TokensDePrueba.EMISOR)
                .containsEntry("sub", id.toString())
                .containsEntry("version_sesion", 0);
        long emitido = ((Number) contenido.get("iat")).longValue();
        long vence = ((Number) contenido.get("exp")).longValue();
        assertThat(vence - emitido).isEqualTo(Duration.ofHours(8).toSeconds());
        assertThat(Instant.parse((String) json(inicio).get("vence"))).isEqualTo(Instant.ofEpochSecond(vence));
    }

    @Test
    @DisplayName("RNF-04 · un token alterado no sirve: cambiar el usuario sin volver a firmar se rechaza")
    void unTokenAlteradoNoSirve() {
        api.crearUsuario(null, "Ana Pérez", CORREO, "ADMIN_PLATAFORMA");
        UUID otro = api.crearUsuario(null, "Luis Gómez", "luis@inspectia.test", "ADMIN_PLATAFORMA");
        String token = api.token(CORREO);
        Map<String, Object> contenido = TokensDePrueba.leer(token);
        contenido.put("sub", otro.toString());

        rechazada(api.sesion(TokensDePrueba.alterado(token, contenido)), SESION_INVALIDA);
    }

    @Test
    @DisplayName("RNF-04 · un token firmado con otra clave no sirve")
    void unTokenDeOtraClaveNoSirve() {
        UUID id = api.crearUsuario(null, "Ana Pérez", CORREO, "ADMIN_PLATAFORMA");
        String token = TokensDePrueba.firmar(TokensDePrueba.contenido(TokensDePrueba.EMISOR, id, Instant.now()),
                TokensDePrueba.otraClave());

        rechazada(api.sesion(token), SESION_INVALIDA);
    }

    @Test
    @DisplayName("RNF-04 · un token sin firma (alg none) no sirve")
    void unTokenSinFirmaNoSirve() {
        UUID id = api.crearUsuario(null, "Ana Pérez", CORREO, "ADMIN_PLATAFORMA");
        String token = TokensDePrueba.sinFirma(TokensDePrueba.contenido(TokensDePrueba.EMISOR, id, Instant.now()));

        rechazada(api.sesion(token), SESION_INVALIDA);
    }

    @Test
    @DisplayName("RNF-04 · un token de otro emisor no sirve, aunque esté firmado con la clave correcta")
    void unTokenDeOtroEmisorNoSirve() {
        UUID id = api.crearUsuario(null, "Ana Pérez", CORREO, "ADMIN_PLATAFORMA");
        String token = TokensDePrueba.firmar(TokensDePrueba.contenido("otro-sistema", id, Instant.now()),
                TokensDePrueba.clave());

        rechazada(api.sesion(token), SESION_INVALIDA);
    }

    @Test
    @DisplayName("RNF-04 · el token de un usuario que no existe no sirve")
    void elTokenDeUnUsuarioQueNoExisteNoSirve() {
        rechazada(api.sesion(TokensDePrueba.token(UUID.randomUUID(), Instant.now())), SESION_INVALIDA);
    }

    @ParameterizedTest(name = "«{0}»")
    @DisplayName("RNF-04 · un encabezado Authorization mal formado no sirve")
    @ValueSource(strings = {"Bearer ", "Bearer abc.def", "Bearer abc.def.ghi"})
    void unEncabezadoMalFormadoNoSirve(String encabezado) {
        rechazada(api.conEncabezado(encabezado), SESION_INVALIDA);
    }

    @Test
    @DisplayName("RNF-04 · C1 · sin token, la sesión actual responde 401")
    void sinTokenResponde401() {
        rechazada(api.sesion(null), SIN_SESION);
    }

    @Test
    @DisplayName("RNF-04 · el rol sale de la base en cada petición: si cambia, la sesión abierta lo refleja sin otro token")
    void elRolSaleDeLaBase() {
        UUID empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        UUID id = api.crearUsuario(empresa, "Ana Pérez", CORREO, "ADMIN_EMPRESA");
        String token = api.token(CORREO);
        jdbc.update("UPDATE usuario SET rol = 'USUARIO_FINAL' WHERE id = ?", id);

        MvcTestResult resultado = api.sesion(token);

        assertThat(resultado).hasStatus(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> usuario = (Map<String, Object>) json(resultado).get("usuario");
        assertThat(usuario).containsEntry("rol", "USUARIO_FINAL");
    }

    @Test
    @DisplayName("RNF-04 · C2 · con el token de un administrador de empresa, la API de empresas responde 403; con el de la plataforma, 200")
    void losRolesSeAplicanConElTokenReal() {
        UUID empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        api.crearUsuario(empresa, "Ana Pérez", CORREO, "ADMIN_EMPRESA");
        api.crearUsuario(null, "Equipo InspectIA", "plataforma@inspectia.test", "ADMIN_PLATAFORMA");

        MvcTestResult deEmpresa = api.enviar(HttpMethod.GET, "/api/v1/empresas", null, api.token(CORREO));
        MvcTestResult dePlataforma = api.enviar(HttpMethod.GET, "/api/v1/empresas", null,
                api.token("plataforma@inspectia.test"));

        assertThat(deEmpresa).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(dePlataforma).hasStatus(HttpStatus.OK);
    }

    /** La sesión se rechaza con 401, en JSON y con el mensaje dado. */
    private static void rechazada(MvcTestResult resultado, String mensaje) {
        assertThat(resultado).hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(json(resultado)).containsEntry("detail", mensaje);
    }
}
