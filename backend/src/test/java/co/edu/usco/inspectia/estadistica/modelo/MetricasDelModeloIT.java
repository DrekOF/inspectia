/*
 * Nombre: MetricasDelModeloIT.java
 * Descripción: Pruebas de integración de la API del modelo de IA (RF-17): las métricas de cada modelo, que coinciden
 *              con la tabla comparativa de D-18 y D-48, cuál está en uso, la confianza promedio de los granos de los
 *              análisis de la empresa, sin los de otra (RNF-03), y los permisos: solo el administrador de empresa
 *              (RNF-04). El periodo opcional de la confianza, por la fecha en que terminó cada muestra, en días de
 *              Colombia como el historial, y sus fechas que no sirven.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.estadistica.modelo;

import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.error;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.errores;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.ModeloDeLaComparacion;
import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
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
 * Pruebas de la API del modelo de IA ({@code GET /api/v1/modelo}, RF-17). Insertan sus propios modelos de la
 * comparación ({@link ModeloDeLaComparacion}) y sus propios análisis, con la base directamente: una muestra terminada
 * por lote, con una foto y los granos con la confianza que la prueba necesita. El modelo del clasificador falso, que
 * se registra al arrancar, sigue en la tabla, sin métricas.
 */
@PruebaIntegracion
class MetricasDelModeloIT {

    private static final String RUTA = "/api/v1/modelo";
    private static final String ANA = "admin@cacaos.test";
    private static final String LUIS = "luis@cacaos.test";
    private static final String SOFIA = "admin@andino.test";
    private static final String PEDRO = "pedro@andino.test";
    private static final String PLATAFORMA = "plataforma@inspectia.test";
    private static final String VALOR_INVALIDO = "El valor no es válido.";
    private static final String HASTA_ANTES_DE_DESDE = "La fecha «Hasta» no puede ser anterior a «Desde».";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiSesion api;
    private UUID cacaos;
    private UUID luis;
    private UUID andino;
    private UUID pedro;
    private UUID efficientNet;

    @BeforeEach
    void preparar() {
        api = new ApiSesion(mvc, jdbc);
        ApiSesion.borrarDatos(jdbc);
        jdbc.update("DELETE FROM modelo WHERE nombre <> 'clasificador-falso'");
        cacaos = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        luis = api.crearUsuario(cacaos, "Luis Gómez", LUIS, "USUARIO_FINAL");
        api.crearUsuario(cacaos, "Ana Pérez", ANA, "ADMIN_EMPRESA");
        andino = api.crearEmpresa("Cacao Andino", "800765432-1");
        pedro = api.crearUsuario(andino, "Pedro Salas", PEDRO, "USUARIO_FINAL");
        api.crearUsuario(andino, "Sofía Mora", SOFIA, "ADMIN_EMPRESA");
        api.crearUsuario(null, "Equipo InspectIA", PLATAFORMA, "ADMIN_PLATAFORMA");
        efficientNet = ModeloDeLaComparacion.EFFICIENTNET_B0.insertar(jdbc, true);
        ModeloDeLaComparacion.RESNET_50.insertar(jdbc, false);
        ModeloDeLaComparacion.MOBILENET_V3_L.insertar(jdbc, false);
    }

    @AfterEach
    void limpiar() {
        ApiSesion.borrarDatos(jdbc);
        jdbc.update("DELETE FROM modelo WHERE nombre <> 'clasificador-falso'");
    }

    @Test
    @DisplayName("RF-17 · C1 · las métricas de cada modelo coinciden con la tabla comparativa de D-18 y D-48: F1, precisión y recall macro y de cada clase, la matriz de confusión y el tiempo por grano, con EfficientNet-B0 en uso")
    void lasMetricasCoincidenConLaComparacion() {
        Map<String, Object> respuesta = consultarOk(api.token(ANA));

        List<Map<String, Object>> modelos = modelos(respuesta);
        assertThat(modelos).extracting(modelo -> modelo.get("nombre"))
                .as("del mayor F1 macro al menor, y el modelo sin métricas al final")
                .containsExactly("EfficientNet-B0", "ResNet-50", "MobileNetV3-L", "clasificador-falso");
        assertThat(modelos.get(0)).containsAllEntriesOf(Map.ofEntries(
                Map.entry("id", efficientNet.toString()), Map.entry("version", "1.0"), Map.entry("tipo", "CNN"),
                Map.entry("enUso", true), Map.entry("f1Macro", 73.5), Map.entry("precisionMacro", 75.2),
                Map.entry("recallMacro", 72.31), Map.entry("recallSano", 91.2), Map.entry("recallNoSano", 53.4),
                Map.entry("precisionSano", 87.5), Map.entry("precisionNoSano", 62.9),
                Map.entry("tiempoPorGranoMs", 19.0)));
        assertThat(modelos.get(0)).containsEntry("matriz", matriz(238, 23, 34, 39));
        assertThat(modelos.get(1)).containsEntry("enUso", false).containsEntry("f1Macro", 69.9)
                .containsEntry("precisionSano", 86.69).containsEntry("matriz", matriz(228, 33, 35, 38))
                .containsEntry("tiempoPorGranoMs", 50.8);
        assertThat(modelos.get(2)).containsEntry("enUso", false).containsEntry("f1Macro", 63.8)
                .containsEntry("recallNoSano", 26.0).containsEntry("matriz", matriz(254, 7, 54, 19));
    }

    @Test
    @DisplayName("RF-17 · C1 · el modelo del clasificador falso sale con sus recall y sin las demás métricas, sin matriz y sin estar en uso")
    void elModeloFalsoSaleSinMetricas() {
        Map<String, Object> falso = modelos(consultarOk(api.token(ANA))).get(3);

        assertThat(falso).containsEntry("nombre", "clasificador-falso").containsEntry("version", "1.0")
                .containsEntry("enUso", false).containsEntry("recallSano", 91.2).containsEntry("recallNoSano", 53.4)
                .containsEntry("tipo", null).containsEntry("f1Macro", null).containsEntry("precisionMacro", null)
                .containsEntry("recallMacro", null).containsEntry("precisionSano", null)
                .containsEntry("precisionNoSano", null).containsEntry("matriz", null)
                .containsEntry("tiempoPorGranoMs", null);
    }

    @Test
    @DisplayName("RF-17 · C1 · si cambia el modelo en uso, la respuesta marca el nuevo y solo ese")
    void marcaElModeloEnUso() {
        jdbc.update("UPDATE modelo SET en_uso = false WHERE id = ?", efficientNet);
        jdbc.update("UPDATE modelo SET en_uso = true WHERE nombre = 'ResNet-50'");

        List<Map<String, Object>> modelos = modelos(consultarOk(api.token(ANA)));

        assertThat(modelos).filteredOn(modelo -> Boolean.TRUE.equals(modelo.get("enUso")))
                .extracting(modelo -> modelo.get("nombre")).containsExactly("ResNet-50");
    }

    @Test
    @DisplayName("RF-17 · C2 · la confianza promedio usa solo los granos de los análisis de la empresa: 90, 80 y 55,5 dan 75,17 en 2 muestras, sin los granos de otra empresa")
    void laConfianzaUsaSoloLaEmpresa() {
        insertarAnalisis(cacaos, luis, "L-001", "90.00", "80.00");
        insertarAnalisis(cacaos, luis, "L-002", "55.50");
        insertarAnalisis(andino, pedro, "A-001", "10.00", "20.00", "30.00");

        Map<String, Object> deAna = confianza(consultarOk(api.token(ANA)));
        Map<String, Object> deSofia = confianza(consultarOk(api.token(SOFIA)));

        assertThat(deAna).containsEntry("promedio", 75.17).containsEntry("granos", 3).containsEntry("muestras", 2);
        assertThat(deSofia).containsEntry("promedio", 20.0).containsEntry("granos", 3).containsEntry("muestras", 1);
    }

    @Test
    @DisplayName("RF-17 · C2 · sin análisis en la empresa, la confianza promedio va vacía, aunque otra empresa sí tenga análisis")
    void sinAnalisisLaConfianzaVaVacia() {
        insertarAnalisis(andino, pedro, "A-001", "90.00");

        Map<String, Object> deAna = confianza(consultarOk(api.token(ANA)));

        assertThat(deAna).containsEntry("promedio", null).containsEntry("granos", 0).containsEntry("muestras", 0);
    }

    @Test
    @DisplayName("RF-17 · con un periodo, la confianza promedio usa solo las muestras que terminaron en esos días de Colombia, con los dos incluidos (prueba de límite)")
    void laConfianzaDeUnPeriodo() {
        insertarAnalisis(cacaos, luis, "L-001", "2026-11-19T23:59:59.999999-05:00", "10.00");
        insertarAnalisis(cacaos, luis, "L-002", "2026-11-20T00:00:00-05:00", "80.00");
        insertarAnalisis(cacaos, luis, "L-003", "2026-11-20T23:59:59.999999-05:00", "90.00", "70.00");
        insertarAnalisis(cacaos, luis, "L-004", "2026-11-21T00:00:00-05:00", "20.00");
        insertarAnalisis(andino, pedro, "A-001", "2026-11-20T10:00:00-05:00", "5.00");

        Map<String, Object> delDia = confianza(consultarOk(api.token(ANA), "desde=2026-11-20&hasta=2026-11-20"));
        Map<String, Object> desde = confianza(consultarOk(api.token(ANA), "desde=2026-11-20"));
        Map<String, Object> hasta = confianza(consultarOk(api.token(ANA), "hasta=2026-11-20"));
        Map<String, Object> todo = confianza(consultarOk(api.token(ANA)));

        assertThat(delDia).containsEntry("promedio", 80.0).containsEntry("granos", 3).containsEntry("muestras", 2);
        assertThat(desde).containsEntry("promedio", 65.0).containsEntry("granos", 4).containsEntry("muestras", 3);
        assertThat(hasta).containsEntry("promedio", 62.5).containsEntry("granos", 4).containsEntry("muestras", 3);
        assertThat(todo).containsEntry("promedio", 54.0).containsEntry("granos", 5).containsEntry("muestras", 4);
    }

    @Test
    @DisplayName("RF-17 · un periodo sin análisis deja la confianza vacía y las métricas del modelo siguen iguales")
    void periodoSinAnalisis() {
        insertarAnalisis(cacaos, luis, "L-001", "2026-11-20T10:00:00-05:00", "90.00");

        Map<String, Object> respuesta = consultarOk(api.token(ANA), "desde=2026-10-01&hasta=2026-10-31");

        assertThat(confianza(respuesta)).containsEntry("promedio", null).containsEntry("granos", 0)
                .containsEntry("muestras", 0);
        assertThat(modelos(respuesta)).hasSize(4);
    }

    @Test
    @DisplayName("RF-17 · si «Hasta» es anterior a «Desde», responde 400 en «hasta»; una fecha que no es un día, o fuera de los años 0001 a 9999, 400 en su campo (prueba de límite)")
    void periodoQueNoSirve() {
        MvcTestResult alReves = api.enviar(HttpMethod.GET, RUTA + "?desde=2026-11-20&hasta=2026-11-19", null,
                api.token(ANA));

        assertThat(alReves).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(alReves)).containsExactly(error("hasta", HASTA_ANTES_DE_DESDE));
        Map.of("desde=2026-13-01", "desde", "hasta=20/11/2026", "hasta", "desde=0000-12-31", "desde",
                "hasta=+10000-01-01", "hasta").forEach((consulta, campo) -> {
                    MvcTestResult resultado = api.enviar(HttpMethod.GET, RUTA + "?" + consulta, null,
                            api.token(ANA));
                    assertThat(resultado).as(consulta).hasStatus(HttpStatus.BAD_REQUEST);
                    assertThat(errores(resultado)).as(consulta).containsExactly(error(campo, VALOR_INVALIDO));
                });
        consultarOk(api.token(ANA), "desde=0001-01-01&hasta=9999-12-31");
        consultarOk(api.token(ANA), "desde=2026-11-20&hasta=2026-11-20");
    }

    @Test
    @DisplayName("RNF-04 · C1 · sin sesión, la API del modelo responde 401 en JSON")
    void sinSesionResponde401() {
        MvcTestResult resultado = api.enviar(HttpMethod.GET, RUTA, null, null);

        assertThat(resultado).hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(json(resultado)).containsEntry("status", 401);
    }

    @Test
    @DisplayName("RF-17 · C3 · un usuario final no puede ver las métricas del modelo: la API responde 403 en JSON")
    void elUsuarioFinalRecibe403() {
        MvcTestResult resultado = api.enviar(HttpMethod.GET, RUTA, null, api.token(LUIS));

        assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(json(resultado)).containsEntry("status", 403);
    }

    @Test
    @DisplayName("RNF-04 · C2 · el administrador de la plataforma, que no tiene empresa, recibe 403 en la API del modelo")
    void laPlataformaRecibe403() {
        MvcTestResult resultado = api.enviar(HttpMethod.GET, RUTA, null, api.token(PLATAFORMA));

        assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(json(resultado)).containsEntry("status", 403);
    }

    @Test
    @DisplayName("RP-04 · la documentación OpenAPI describe la API del modelo, con el token Bearer de toda la API")
    void laDocumentacionDescribeLaApi() {
        MvcTestResult resultado = mvc.get().uri("/v3/api-docs").exchange();

        assertThat(resultado).hasStatus(HttpStatus.OK);
        String documento = new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        Map<String, Object> operacion = JsonPath.read(documento, "$.paths['/api/v1/modelo'].get");
        assertThat(operacion).containsKey("summary").doesNotContainKey("security");
        assertThat((String) operacion.get("description")).contains("ADMIN_EMPRESA");
    }

    /** Consulta la API del modelo con el token y espera el 200. */
    private Map<String, Object> consultarOk(String token) {
        return consultarOk(token, "");
    }

    /** Consulta la API del modelo con el token y la consulta del periodo, como {@code desde=…&hasta=…}, y espera el 200. */
    private Map<String, Object> consultarOk(String token, String consulta) {
        MvcTestResult resultado = api.enviar(HttpMethod.GET, RUTA + (consulta.isEmpty() ? "" : "?" + consulta), null,
                token);
        assertThat(resultado).hasStatus(HttpStatus.OK).hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON);
        return json(resultado);
    }

    /** Los modelos de la respuesta. */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> modelos(Map<String, Object> respuesta) {
        return (List<Map<String, Object>>) respuesta.get("modelos");
    }

    /** La confianza promedio de la respuesta. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> confianza(Map<String, Object> respuesta) {
        return (Map<String, Object>) respuesta.get("confianza");
    }

    /** Una matriz de confusión como la responde la API. */
    private static Map<String, Object> matriz(int sanosComoSanos, int sanosComoNoSanos, int noSanosComoSanos,
            int noSanosComoNoSanos) {
        Map<String, Object> matriz = new LinkedHashMap<>();
        matriz.put("sanosComoSanos", sanosComoSanos);
        matriz.put("sanosComoNoSanos", sanosComoNoSanos);
        matriz.put("noSanosComoSanos", noSanosComoSanos);
        matriz.put("noSanosComoNoSanos", noSanosComoNoSanos);
        return matriz;
    }

    /**
     * Un lote con su muestra 1 terminada por EfficientNet-B0, una foto aprobada y un grano por cada confianza. Los
     * conteos de la muestra no tienen que coincidir con los granos: la consulta solo lee los granos.
     *
     * @param empresa la empresa
     * @param usuario el usuario final que registró el lote
     * @param codigo el código del lote
     * @param confianzas la confianza de cada grano, en porcentaje
     */
    private void insertarAnalisis(UUID empresa, UUID usuario, String codigo, String... confianzas) {
        insertarAnalisis(empresa, usuario, codigo, OffsetDateTime.now().toString(), confianzas);
    }

    /**
     * Como {@link #insertarAnalisis(UUID, UUID, String, String...)}, con la muestra terminada en un instante.
     *
     * @param empresa la empresa
     * @param usuario el usuario final que registró el lote
     * @param codigo el código del lote
     * @param terminada cuándo terminó la muestra, como {@code 2026-11-20T10:00:00-05:00}
     * @param confianzas la confianza de cada grano, en porcentaje
     */
    private void insertarAnalisis(UUID empresa, UUID usuario, String codigo, String terminada,
            String... confianzas) {
        UUID lote = jdbc.queryForObject("""
                INSERT INTO lote (empresa_id, codigo, fecha_recepcion, origen, usuario_id)
                VALUES (?, ?, current_date, 'Finca La Esperanza', ?) RETURNING id""", UUID.class, empresa, codigo,
                usuario);
        UUID muestra = jdbc.queryForObject("""
                INSERT INTO muestra (empresa_id, lote_id, numero, usuario_id, estado, modelo_id, total_granos,
                    granos_sanos, aviso_confianza_baja, umbral_confianza_baja, porcentaje_max_dudosos, fecha_envio,
                    fecha_inicio, fecha_fin)
                VALUES (?, ?, 1, ?, 'TERMINADA', ?, 100, 90, false, 60, 10, ?::timestamptz, ?::timestamptz,
                    ?::timestamptz) RETURNING id""",
                UUID.class, empresa, lote, usuario, efficientNet, terminada, terminada, terminada);
        UUID foto = jdbc.queryForObject("""
                INSERT INTO foto (empresa_id, muestra_id, numero, ruta, formato, tamano_bytes, estado,
                    granos_encontrados)
                VALUES (?, ?, 1, ?, 'PNG', 1000, 'APROBADA', ?) RETURNING id""", UUID.class, empresa, muestra,
                empresa + "/" + lote + "/m1-" + UUID.randomUUID() + ".png", confianzas.length);
        for (String confianza : confianzas) {
            jdbc.update("""
                    INSERT INTO grano (empresa_id, foto_id, x, y, ancho, alto, clase, confianza)
                    VALUES (?, ?, 0, 0, 10, 10, 'SANO', ?::numeric)""", empresa, foto, confianza);
        }
    }
}
