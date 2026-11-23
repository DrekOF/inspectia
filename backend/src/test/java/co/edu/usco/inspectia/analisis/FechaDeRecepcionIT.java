/*
 * Nombre: FechaDeRecepcionIT.java
 * Descripción: Pruebas de límite de la fecha de recepción de un lote: no puede ser posterior a hoy (RF-04 C4) ni
 *              de hace más de un año (RF-04, restricción 4), y «hoy» es el día en Colombia, no en UTC.
 * Fecha de creación: 10/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis;

import static co.edu.usco.inspectia.analisis.ApiLotes.DATOS_INVALIDOS;
import static co.edu.usco.inspectia.analisis.ApiLotes.FECHA_ANTIGUA;
import static co.edu.usco.inspectia.analisis.ApiLotes.FECHA_FUTURA;
import static co.edu.usco.inspectia.analisis.ApiLotes.datos;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.error;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.errores;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.RelojDePrueba;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * La fecha de recepción no puede ser posterior a hoy (RF-04 C4), con «hoy» en la hora de Colombia
 * (America/Bogota, UTC−5; ver «Fechas» en specs/modelo-datos.md). El reloj del backend queda fijo el 10 de
 * noviembre de 2026 a las 3:00 en UTC, cuando en Colombia todavía son las 22:00 del 9: con un «hoy» en
 * UTC, el 10 pasaría.
 */
@PruebaIntegracion
class FechaDeRecepcionIT {

    private static final String LUIS = "luis@cacaos.test";

    /** El 10/11/2026 a las 3:00 en UTC: en Colombia son las 22:00 del 9/11. */
    private static final Instant NOCHE_EN_COLOMBIA = Instant.parse("2026-11-10T03:00:00Z");
    private static final RelojDePrueba RELOJ = new RelojDePrueba(NOCHE_EN_COLOMBIA);

    /** Hoy en Colombia, con el reloj fijo. */
    private static final LocalDate HOY = LocalDate.of(2026, 11, 9);

    /** Hace un año en Colombia, con el reloj fijo. Con un «hoy» en UTC sería el 10/11/2025. */
    private static final LocalDate HACE_UN_ANIO = LocalDate.of(2025, 11, 9);

    /** El reloj del backend en estas pruebas: reemplaza al bean {@code reloj} de la aplicación. */
    @TestBean(name = "reloj", methodName = "relojDePrueba")
    private Clock reloj;

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiLotes lotes;
    private String token;

    static Clock relojDePrueba() {
        return RELOJ;
    }

    @BeforeEach
    void preparar() {
        RELOJ.fijar(NOCHE_EN_COLOMBIA);
        ApiSesion api = new ApiSesion(mvc, jdbc);
        lotes = new ApiLotes(api);
        ApiSesion.borrarDatos(jdbc);
        UUID empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");
        token = api.token(LUIS);
    }

    @AfterEach
    void limpiar() {
        ApiSesion.borrarDatos(jdbc);
    }

    @Test
    @DisplayName("RF-04 · C4 · la fecha de recepción no puede ser posterior a hoy: con hoy en Colombia, el día siguiente se rechaza y hoy pasa (prueba de límite)")
    void noPosteriorAHoyEnColombia() {
        MvcTestResult manana = lotes.registrar(token, datos("L-001", HOY.plusDays(1), "Finca La Esperanza", null));
        MvcTestResult hoy = lotes.registrar(token, datos("L-002", HOY, "Finca La Esperanza", null));

        assertThat(manana).as("el 10/11 ya es hoy en UTC, pero en Colombia es mañana").hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(json(manana)).containsEntry("detail", DATOS_INVALIDOS);
        assertThat(errores(manana)).containsExactly(error("fechaRecepcion", FECHA_FUTURA));
        assertThat(hoy).hasStatus(HttpStatus.CREATED);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lote", Integer.class)).isOne();
    }

    @Test
    @DisplayName("RF-04 · C4 · una fecha de recepción de varias semanas después también se rechaza")
    void rechazaFechasLejanas() {
        MvcTestResult resultado = lotes.registrar(token, datos("L-001", HOY.plusWeeks(3), "Finca La Esperanza", null));

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(error("fechaRecepcion", FECHA_FUTURA));
    }

    @Test
    @DisplayName("RF-04 · la fecha de recepción puede ser de hace exactamente un año en Colombia, pero no del día anterior (prueba de límite)")
    void noAnteriorAUnAnioEnColombia() {
        MvcTestResult anterior = lotes.registrar(token,
                datos("L-001", HACE_UN_ANIO.minusDays(1), "Finca La Esperanza", null));
        MvcTestResult justo = lotes.registrar(token, datos("L-002", HACE_UN_ANIO, "Finca La Esperanza", null));

        assertThat(anterior).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(json(anterior)).containsEntry("detail", DATOS_INVALIDOS);
        assertThat(errores(anterior)).containsExactly(error("fechaRecepcion", FECHA_ANTIGUA));
        assertThat(justo).as("con un «hoy» en UTC, hace un año sería el 10/11/2025 y el 9 se rechazaría")
                .hasStatus(HttpStatus.CREATED);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lote", Integer.class)).isOne();
    }

    @Test
    @DisplayName("RF-04 · una fecha de recepción de hace miles de años responde 400 con su mensaje y no llega a la base, que no la admite (antes respondía 500)")
    void rechazaFechasMuyAntiguas() {
        MvcTestResult resultado = lotes.registrar(token,
                datos("L-001", LocalDate.of(-10000, 1, 1), "Finca La Esperanza", null));

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(error("fechaRecepcion", FECHA_ANTIGUA));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lote", Integer.class)).isZero();
    }

    @Test
    @DisplayName("RF-04 · el ejemplo de la fecha de recepción en la documentación OpenAPI es de hace una semana en Colombia, así el ejemplo de Swagger UI siempre se puede registrar")
    void ejemploDeLaDocumentacion() {
        MvcTestResult resultado = mvc.get().uri("/v3/api-docs").exchange();

        assertThat(resultado).hasStatus(HttpStatus.OK);
        String documento = new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        String ejemplo = JsonPath.read(documento,
                "$.components.schemas.LoteSolicitud.properties.fechaRecepcion.example");
        assertThat(ejemplo).isEqualTo(HOY.minusWeeks(1).toString());
    }
}
