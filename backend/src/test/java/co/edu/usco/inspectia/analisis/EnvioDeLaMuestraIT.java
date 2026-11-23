/*
 * Nombre: EnvioDeLaMuestraIT.java
 * Descripción: Pruebas de integración de «Analizar la muestra»: al menos 2 fotos, la muestra en espera con su fecha y
 *              con quien la envía, el reintento seguro, una muestra en espera que ya no recibe fotos y un lote analizado
 *              que no se vuelve a analizar (RF-05 C2, RNF-02 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis;

import static co.edu.usco.inspectia.analisis.ApiAnalisis.FOTOS_MINIMO;
import static co.edu.usco.inspectia.analisis.ApiAnalisis.LOTE_ANALIZADO;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.error;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.errores;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.ConfiguracionContenedores;
import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.analisis.aplicacion.AtenderCola;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * «Analizar la muestra» (RF-05 C2). En Cacaos del Huila, Pedro Díaz sube las fotos del lote y Luis Gómez, su
 * compañero, lo envía a analizar. La cola va a mano: estas pruebas la atienden con {@link AtenderCola#atenderUna()}.
 */
@PruebaIntegracion
class EnvioDeLaMuestraIT {

    private static final String LUIS = "luis@cacaos.test";
    private static final String PEDRO = "pedro@cacaos.test";
    private static final int[] SANAS = {100, 100};

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AtenderCola atender;

    private ApiFotos fotos;
    private ApiAnalisis analisis;
    private UUID luis;
    private String tokenDeLuis;
    private String tokenDePedro;
    private UUID lote;

    @BeforeEach
    void preparar() {
        ApiSesion api = new ApiSesion(mvc, jdbc);
        fotos = new ApiFotos(api, mvc);
        analisis = new ApiAnalisis(api, fotos);
        ApiSesion.borrarDatos(jdbc);
        UUID empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        luis = api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");
        api.crearUsuario(empresa, "Pedro Díaz", PEDRO, "USUARIO_FINAL");
        tokenDeLuis = api.token(LUIS);
        tokenDePedro = api.token(PEDRO);
        lote = new ApiLotes(api).registrarOk(tokenDeLuis, "L-001");
    }

    @AfterEach
    void limpiar() {
        ApiSesion.borrarDatos(jdbc);
        ConfiguracionContenedores.vaciarCarpetaDeFotos();
    }

    @Test
    @DisplayName("RF-05 · C2 · con 1 foto, «Analizar la muestra» responde 400 en «fotos» con el mensaje de qué hacer, y la muestra sigue pendiente sin enviar (prueba de límite)")
    void conUnaFotoNoSeEnvia() {
        analisis.subirFotos(tokenDePedro, lote, SANAS);

        MvcTestResult resultado = analisis.enviar(tokenDeLuis, lote);

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(error("fotos", FOTOS_MINIMO));
        Map<String, Object> muestra = jdbc.queryForMap("SELECT estado, fecha_envio FROM muestra WHERE lote_id = ?",
                lote);
        assertThat(muestra).containsEntry("estado", "PENDIENTE").containsEntry("fecha_envio", null);
        assertThat(atender.atenderUna()).as("la cola sigue vacía").isFalse();
    }

    @Test
    @DisplayName("RF-05 · C2 · con 2 fotos, «Analizar la muestra» responde 202: la muestra queda en espera, con la fecha de envío al microsegundo y con quien la envió (prueba de límite)")
    void conDosFotosQuedaEnEspera() {
        analisis.subirFotos(tokenDePedro, lote, SANAS, SANAS);

        MvcTestResult resultado = analisis.enviar(tokenDeLuis, lote);

        assertThat(resultado).hasStatus(HttpStatus.ACCEPTED);
        Map<String, Object> avance = json(resultado);
        assertThat(avance).containsEntry("muestra", 1).containsEntry("estado", "EN_ESPERA")
                .containsEntry("fechaInicio", null).containsEntry("fechaFin", null).containsEntry("fallo", false);
        Map<String, Object> muestra = jdbc.queryForMap(
                "SELECT estado, usuario_id, fecha_envio FROM muestra WHERE lote_id = ?", lote);
        assertThat(muestra).containsEntry("estado", "EN_ESPERA").containsEntry("usuario_id", luis);
        assertThat(Instant.parse((String) avance.get("fechaEnvio")))
                .as("la fecha que responde es la que quedó guardada")
                .isEqualTo(((OffsetDateTime) jdbc.queryForObject("SELECT fecha_envio FROM muestra WHERE lote_id = ?",
                        OffsetDateTime.class, lote)).toInstant());
    }

    @Test
    @DisplayName("RF-05 · C2 · con 3 fotos, el máximo, la muestra también se envía")
    void conTresFotosSeEnvia() {
        analisis.subirFotos(tokenDePedro, lote, SANAS, SANAS, SANAS);

        assertThat(analisis.enviarOk(tokenDeLuis, lote)).containsEntry("estado", "EN_ESPERA");
    }

    @Test
    @DisplayName("RF-05 · C2 · un lote sin fotos no se envía: responde 400 en «fotos» y no crea ninguna muestra")
    void sinFotosNoSeEnvia() {
        MvcTestResult resultado = analisis.enviar(tokenDeLuis, lote);

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(error("fotos", FOTOS_MINIMO));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM muestra", Integer.class)).isZero();
    }

    @Test
    @DisplayName("RNF-02 · enviar otra vez la misma muestra responde lo mismo, sin cambiar su fecha de envío ni ponerla dos veces en la cola: el reintento es seguro")
    void reintentoSeguro() {
        analisis.subirFotos(tokenDePedro, lote, SANAS, SANAS);
        Map<String, Object> primero = analisis.enviarOk(tokenDeLuis, lote);

        Map<String, Object> segundo = analisis.enviarOk(tokenDePedro, lote);

        assertThat(segundo).isEqualTo(primero);
        assertThat(jdbc.queryForObject("SELECT usuario_id FROM muestra WHERE lote_id = ?", UUID.class, lote))
                .as("quien la envió primero").isEqualTo(luis);
        assertThat(atender.atenderUna()).isTrue();
        assertThat(atender.atenderUna()).as("la muestra estaba una sola vez en la cola").isFalse();
    }

    @Test
    @DisplayName("RF-05 · una muestra enviada ya no recibe fotos: responde 409 con el motivo")
    void enEsperaNoRecibeFotos() {
        analisis.subirFotos(tokenDePedro, lote, SANAS, SANAS);
        analisis.enviarOk(tokenDeLuis, lote);

        MvcTestResult resultado = fotos.pedirSubida(tokenDePedro, lote, 3, ApiFotos.PNG, 1_000);

        assertThat(resultado).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(resultado)).containsEntry("detail", ApiFotos.LOTE_NO_PENDIENTE);
    }

    @Test
    @DisplayName("RF-12 · un lote ya analizado no se vuelve a analizar: responde 409 con el motivo y su resultado no cambia")
    void loteAnalizado() {
        analisis.subirYEnviar(tokenDeLuis, lote, SANAS, SANAS);
        atender.atenderUna();
        Map<String, Object> antes = jdbc.queryForMap("SELECT * FROM lote WHERE id = ?", lote);

        MvcTestResult resultado = analisis.enviar(tokenDeLuis, lote);

        assertThat(resultado).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(resultado)).containsEntry("detail", LOTE_ANALIZADO);
        assertThat(jdbc.queryForMap("SELECT * FROM lote WHERE id = ?", lote)).isEqualTo(antes);
    }
}
