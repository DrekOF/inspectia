/*
 * Nombre: EstadisticasIT.java
 * Descripción: Pruebas de integración y de seguridad de las estadísticas de la empresa (RF-16, RNF-03, RNF-04): las
 *              cifras coinciden con el historial en el mismo periodo, sin lotes la respuesta lo dice, solo entra el
 *              administrador de empresa, la misma finca escrita de otra forma cuenta una vez, nunca se mezclan
 *              empresas y el periodo se valida como los filtros del historial.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.estadistica.lotes;

import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.error;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.errores;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Las estadísticas por la API ({@code GET /api/v1/estadisticas?desde=&hasta=}). En Cacaos del Huila, Luis Gómez
 * registra lotes y Ana Pérez es la administradora; en Cacaos del Caquetá, Marta Ruiz registra los suyos. Los lotes se
 * registran por la API y su resultado se fija por SQL, con la fecha del resultado en la hora de Colombia (UTC−5), como
 * en las pruebas de los filtros del historial.
 */
@PruebaIntegracion
class EstadisticasIT {

    private static final String RUTA = "/api/v1/estadisticas";
    private static final String HISTORIAL = "/api/v1/lotes";
    private static final String LUIS = "luis@cacaos.test";
    private static final String ANA = "admin@cacaos.test";
    private static final String MARTA = "marta@caqueta.test";
    private static final String ADMIN_CAQUETA = "admin@caqueta.test";
    private static final String PLATAFORMA = "plataforma@inspectia.test";
    private static final String VALOR_INVALIDO = "El valor no es válido.";
    private static final String HASTA_ANTES_DE_DESDE = "La fecha «Hasta» no puede ser anterior a «Desde».";

    /** Una fecha de recepción de hace una semana en Colombia: la API no acepta una de hace más de un año (RF-04). */
    private static final LocalDate RECEPCION = LocalDate.now(ZoneId.of("America/Bogota")).minusWeeks(1);

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiSesion api;
    private String tokenDeLuis;
    private String tokenDeAna;
    private String tokenDeMarta;

    @BeforeEach
    void preparar() {
        api = new ApiSesion(mvc, jdbc);
        ApiSesion.borrarDatos(jdbc);
        UUID huila = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        api.crearUsuario(huila, "Luis Gómez", LUIS, "USUARIO_FINAL");
        api.crearUsuario(huila, "Ana Pérez", ANA, "ADMIN_EMPRESA");
        UUID caqueta = api.crearEmpresa("Cacaos del Caquetá", "900654321-0");
        api.crearUsuario(caqueta, "Marta Ruiz", MARTA, "USUARIO_FINAL");
        api.crearUsuario(caqueta, "Jorge Ríos", ADMIN_CAQUETA, "ADMIN_EMPRESA");
        api.crearUsuario(null, "Equipo InspectIA", PLATAFORMA, "ADMIN_PLATAFORMA");
        tokenDeLuis = api.token(LUIS);
        tokenDeAna = api.token(ANA);
        tokenDeMarta = api.token(MARTA);
    }

    @AfterEach
    void limpiar() {
        ApiSesion.borrarDatos(jdbc);
    }

    /** Registra un lote por la API y devuelve su id; falla si no responde 201. */
    private UUID registrar(String token, String codigo, String origen) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("codigo", codigo);
        datos.put("fechaRecepcion", RECEPCION.toString());
        datos.put("origen", origen);
        MvcTestResult resultado = api.enviar(HttpMethod.POST, HISTORIAL, datos, token);
        assertThat(resultado).as("registrar %s", codigo).hasStatus(HttpStatus.CREATED);
        return UUID.fromString((String) json(resultado).get("id"));
    }

    /**
     * Registra un lote y lo deja analizado, sin sugerencia, con su resultado.
     *
     * @param fecha la fecha del resultado, con su zona, como {@code 2026-11-20T10:00-05:00}
     */
    private UUID analizado(String token, String codigo, String origen, String porcentaje, String categoria,
            String fecha) {
        UUID lote = registrar(token, codigo, origen);
        jdbc.update("""
                UPDATE lote SET estado = 'ANALIZADO', porcentaje_sanos = ?, margen = 5.7, categoria = ?,
                    recomendacion = 'Recomendación de prueba', sugerir_otra_muestra = false,
                    aviso_confianza_baja = false, fecha_resultado = ?
                WHERE id = ?""", new BigDecimal(porcentaje), categoria, OffsetDateTime.parse(fecha), lote);
        return lote;
    }

    private MvcTestResult estadisticas(String token, String consulta) {
        return api.enviar(HttpMethod.GET, RUTA + (consulta.isEmpty() ? "" : "?" + consulta), null, token);
    }

    /** Las estadísticas de Ana con esa consulta; falla si no responde 200. */
    private String estadisticasOk(String consulta) {
        MvcTestResult resultado = estadisticas(tokenDeAna, consulta);
        assertThat(resultado).as("estadísticas %s", consulta).hasStatus(HttpStatus.OK);
        return cuerpo(resultado);
    }

    /** El historial de la empresa de Ana con esa consulta, en una sola página de 100. */
    private String historial(String consulta) {
        MvcTestResult resultado = api.enviar(HttpMethod.GET, HISTORIAL + "?tamano=100&" + consulta, null, tokenDeAna);
        assertThat(resultado).as("historial %s", consulta).hasStatus(HttpStatus.OK);
        return cuerpo(resultado);
    }

    private static String cuerpo(MvcTestResult resultado) {
        return new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    /** Un número del JSON como decimal, o {@code null}. */
    private static BigDecimal decimal(String documento, String ruta) {
        Object valor = JsonPath.read(documento, ruta);
        return valor == null ? null : new BigDecimal(valor.toString());
    }

    private static long entero(String documento, String ruta) {
        return ((Number) JsonPath.read(documento, ruta)).longValue();
    }

    @Test
    @DisplayName("RF-16 · C1 · las cifras coinciden con los lotes del historial en el mismo periodo: lotes analizados, lotes por categoría y promedio, sin los pendientes ni los de fuera del periodo")
    void coincidenConElHistorial() {
        analizado(tokenDeLuis, "L-001", "Finca La Esperanza", "97.0", "PREMIUM", "2026-11-01T00:00-05:00");
        analizado(tokenDeLuis, "L-002", "Finca La Esperanza", "93.4", "ESTANDAR", "2026-11-05T10:00-05:00");
        analizado(tokenDeLuis, "L-003", "Finca El Paraíso", "85.1", "CORRIENTE", "2026-11-10T10:00-05:00");
        analizado(tokenDeLuis, "L-004", "Finca El Paraíso", "70.2", "FUERA_DE_NORMA", "2026-11-20T23:59:59.999999-05:00");
        analizado(tokenDeLuis, "L-005", "Finca El Paraíso", "96.0", "PREMIUM", "2026-10-31T23:59:59.999999-05:00");
        analizado(tokenDeLuis, "L-006", "Finca La Esperanza", "91.0", "ESTANDAR", "2026-11-21T00:00-05:00");
        registrar(tokenDeLuis, "L-007", "Finca La Esperanza");
        String periodo = "desde=2026-11-01&hasta=2026-11-20";

        String cifras = estadisticasOk(periodo);
        String delHistorial = historial(periodo + "&estado=ANALIZADO");

        assertThat(entero(cifras, "$.lotesAnalizados")).isEqualTo(entero(delHistorial, "$.totalElementos"))
                .isEqualTo(4);
        List<Number> porcentajes = JsonPath.read(delHistorial, "$.contenido[*].porcentajeSanos");
        BigDecimal suma = porcentajes.stream().map(numero -> new BigDecimal(numero.toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(decimal(cifras, "$.promedioSanos"))
                .isEqualByComparingTo(suma.divide(BigDecimal.valueOf(porcentajes.size()), 1, RoundingMode.HALF_UP))
                .isEqualByComparingTo("86.4");
        for (String categoria : List.of("PREMIUM", "ESTANDAR", "CORRIENTE", "FUERA_DE_NORMA")) {
            long enElHistorial = entero(historial(periodo + "&categoria=" + categoria), "$.totalElementos");
            List<Number> enLasCifras = JsonPath.read(cifras, "$.porCategoria[?(@.categoria == '" + categoria + "')].lotes");
            assertThat(enLasCifras).as(categoria).singleElement()
                    .satisfies(lotes -> assertThat(lotes.longValue()).isEqualTo(enElHistorial));
        }
        assertThat(JsonPath.<List<String>>read(cifras, "$.porCategoria[*].categoria"))
                .containsExactly("PREMIUM", "ESTANDAR", "CORRIENTE", "FUERA_DE_NORMA");
        assertThat(JsonPath.<List<String>>read(cifras, "$.porMes[*].mes")).containsExactly("2026-11");
        assertThat(entero(cifras, "$.porMes[0].lotes")).isEqualTo(4);
        assertThat(JsonPath.<String>read(cifras, "$.desde")).isEqualTo("2026-11-01");
        assertThat(JsonPath.<String>read(cifras, "$.hasta")).isEqualTo("2026-11-20");
        assertThat(JsonPath.<Boolean>read(cifras, "$.sinLotes")).isFalse();
    }

    @Test
    @DisplayName("RF-16 · C1 · sin fechas, las cifras cuentan todos los lotes analizados de la empresa, como el historial filtrado por «Analizado»")
    void sinPeriodoCuentaTodos() {
        analizado(tokenDeLuis, "L-001", "Finca A", "90.0", "ESTANDAR", "2025-12-01T10:00-05:00");
        analizado(tokenDeLuis, "L-002", "Finca A", "92.0", "ESTANDAR", "2026-11-01T10:00-05:00");
        registrar(tokenDeLuis, "L-003", "Finca A");

        String cifras = estadisticasOk("");

        assertThat(entero(cifras, "$.lotesAnalizados"))
                .isEqualTo(entero(historial("estado=ANALIZADO"), "$.totalElementos")).isEqualTo(2);
        assertThat(decimal(cifras, "$.promedioSanos")).isEqualByComparingTo("91.0");
        assertThat(JsonPath.<List<String>>read(cifras, "$.porMes[*].mes")).hasSize(12).startsWith("2025-12")
                .endsWith("2026-11");
        assertThat(JsonPath.<Object>read(cifras, "$.desde")).isNull();
    }

    @Test
    @DisplayName("RF-16 · C2 · sin lotes en el periodo, la respuesta lo dice: sinLotes, 0 lotes, sin promedio, sin meses ni fincas")
    void sinLotesEnElPeriodo() {
        analizado(tokenDeLuis, "L-001", "Finca A", "90.0", "ESTANDAR", "2026-10-15T10:00-05:00");
        registrar(tokenDeLuis, "L-002", "Finca A");

        String cifras = estadisticasOk("desde=2026-11-01&hasta=2026-11-30");

        assertThat(JsonPath.<Boolean>read(cifras, "$.sinLotes")).isTrue();
        assertThat(entero(cifras, "$.lotesAnalizados")).isZero();
        assertThat(decimal(cifras, "$.promedioSanos")).isNull();
        assertThat(JsonPath.<List<Object>>read(cifras, "$.porMes")).isEmpty();
        assertThat(JsonPath.<List<Object>>read(cifras, "$.porOrigen")).isEmpty();
        assertThat(JsonPath.<List<Number>>read(cifras, "$.porCategoria[*].lotes")).allSatisfy(
                lotes -> assertThat(lotes.longValue()).isZero());
    }

    @Test
    @DisplayName("RF-16 · C3 · un usuario final no puede entrar a las estadísticas: recibe 403 en JSON")
    void elUsuarioFinalNoEntra() {
        MvcTestResult resultado = estadisticas(tokenDeLuis, "desde=2026-11-01&hasta=2026-11-30");

        assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(json(resultado)).containsEntry("status", 403);
    }

    @Test
    @DisplayName("RNF-04 · C2 · el administrador de la plataforma recibe 403 en las estadísticas, aunque el periodo no sea válido: el permiso se revisa primero")
    void laPlataformaNoEntra() {
        MvcTestResult resultado = estadisticas(api.token(PLATAFORMA), "desde=2026-11-20&hasta=2026-11-01");

        assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(json(resultado)).containsEntry("status", 403);
    }

    @Test
    @DisplayName("RNF-04 · C1 · sin sesión, las estadísticas responden 401 en JSON")
    void sinSesion() {
        MvcTestResult resultado = estadisticas(null, "");

        assertThat(resultado).hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(json(resultado)).containsEntry("status", 401);
    }

    @Test
    @DisplayName("RF-16 · C4 · «La Esperanza» y «la esperanza », y «Paraíso» y «Paraiso», cuentan como la misma finca en el promedio por finca o proveedor")
    void mismaFinca() {
        UUID conEspacio = analizado(tokenDeLuis, "L-001", "La Esperanza", "90.0", "ESTANDAR",
                "2026-11-02T10:00-05:00");
        analizado(tokenDeLuis, "L-002", "La Esperanza", "92.0", "ESTANDAR", "2026-11-03T10:00-05:00");
        // La API guarda el origen sin los espacios del final; un lote guardado con ellos también cuenta como la misma
        // finca. Las dos formas empatan con un lote cada una: se muestra la del resultado más reciente.
        jdbc.update("UPDATE lote SET origen = 'la esperanza ' WHERE id = ?", conEspacio);
        analizado(tokenDeLuis, "L-003", "Paraíso", "80.0", "CORRIENTE", "2026-11-04T10:00-05:00");
        analizado(tokenDeLuis, "L-004", "Paraiso", "82.0", "CORRIENTE", "2026-11-05T10:00-05:00");
        analizado(tokenDeLuis, "L-005", "paraíso", "84.0", "CORRIENTE", "2026-11-06T10:00-05:00");

        String cifras = estadisticasOk("desde=2026-11-01&hasta=2026-11-30");

        assertThat(JsonPath.<List<String>>read(cifras, "$.porOrigen[*].origen"))
                .containsExactly("La Esperanza", "paraíso");
        assertThat(JsonPath.<List<Number>>read(cifras, "$.porOrigen[*].lotes")).extracting(Number::longValue)
                .containsExactly(2L, 3L);
        assertThat(decimal(cifras, "$.porOrigen[0].promedioSanos")).isEqualByComparingTo("91.0");
        assertThat(decimal(cifras, "$.porOrigen[1].promedioSanos")).isEqualByComparingTo("82.0");
    }

    @Test
    @DisplayName("RNF-03 · C2 · las estadísticas nunca mezclan datos de empresas distintas: cada administrador ve solo los lotes de la suya")
    void noMezclaEmpresas() {
        analizado(tokenDeLuis, "L-001", "Finca A", "90.0", "ESTANDAR", "2026-11-02T10:00-05:00");
        analizado(tokenDeMarta, "L-001", "Finca A", "70.0", "FUERA_DE_NORMA", "2026-11-02T10:00-05:00");
        analizado(tokenDeMarta, "L-002", "Finca B", "72.0", "FUERA_DE_NORMA", "2026-11-03T10:00-05:00");

        String deAna = estadisticasOk("desde=2026-11-01&hasta=2026-11-30");
        MvcTestResult delCaqueta = estadisticas(api.token(ADMIN_CAQUETA), "desde=2026-11-01&hasta=2026-11-30");

        assertThat(entero(deAna, "$.lotesAnalizados")).isEqualTo(1);
        assertThat(decimal(deAna, "$.promedioSanos")).isEqualByComparingTo("90.0");
        assertThat(JsonPath.<List<String>>read(deAna, "$.porOrigen[*].origen")).containsExactly("Finca A");
        assertThat(delCaqueta).hasStatus(HttpStatus.OK);
        assertThat(entero(cuerpo(delCaqueta), "$.lotesAnalizados")).isEqualTo(2);
        assertThat(decimal(cuerpo(delCaqueta), "$.promedioSanos")).isEqualByComparingTo("71.0");
    }

    @Test
    @DisplayName("RF-16 · el periodo usa días de Colombia con las dos fechas incluidas: del 20 al 20 cuenta los resultados de las 00:00 a las 23:59:59,999999 de Bogotá (prueba de límite)")
    void periodoEnDiasDeColombia() {
        analizado(tokenDeLuis, "L-001", "Finca A", "90.0", "ESTANDAR", "2026-11-19T23:59:59.999999-05:00");
        analizado(tokenDeLuis, "L-002", "Finca A", "91.0", "ESTANDAR", "2026-11-20T00:00:00-05:00");
        analizado(tokenDeLuis, "L-003", "Finca A", "93.0", "ESTANDAR", "2026-11-20T23:59:59.999999-05:00");
        analizado(tokenDeLuis, "L-004", "Finca A", "94.0", "ESTANDAR", "2026-11-21T00:00:00-05:00");

        String cifras = estadisticasOk("desde=2026-11-20&hasta=2026-11-20");

        assertThat(entero(cifras, "$.lotesAnalizados")).isEqualTo(2);
        assertThat(decimal(cifras, "$.promedioSanos")).isEqualByComparingTo("92.0");
    }

    @Test
    @DisplayName("RF-16 · si «Hasta» es anterior a «Desde», responde 400 en «hasta» con el motivo; con las dos iguales, 200 (prueba de límite)")
    void hastaAntesDeDesde() {
        MvcTestResult alReves = estadisticas(tokenDeAna, "desde=2026-11-20&hasta=2026-11-19");

        assertThat(alReves).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(alReves)).containsExactly(error("hasta", HASTA_ANTES_DE_DESDE));
        assertThat(estadisticas(tokenDeAna, "desde=2026-11-20&hasta=2026-11-20")).hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("RF-16 · una fecha que no es un día, o fuera de los años 0001 a 9999, responde 400 en su campo y no un error del servidor (prueba de límite)")
    void fechasQueNoSirven() {
        Map<String, String> casos = Map.of("desde=2026-13-01", "desde", "hasta=20/11/2026", "hasta",
                "desde=0000-12-31", "desde", "hasta=+10000-01-01", "hasta");

        casos.forEach((consulta, campo) -> {
            MvcTestResult resultado = estadisticas(tokenDeAna, consulta);
            assertThat(resultado).as(consulta).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(errores(resultado)).as(consulta).containsExactly(error(campo, VALOR_INVALIDO));
        });
        assertThat(estadisticas(tokenDeAna, "desde=0001-01-01&hasta=9999-12-31")).hasStatus(HttpStatus.OK);
    }
}
