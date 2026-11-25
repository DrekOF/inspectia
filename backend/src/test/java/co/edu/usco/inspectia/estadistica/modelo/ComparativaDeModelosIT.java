/*
 * Nombre: ComparativaDeModelosIT.java
 * Descripción: Pruebas de integración de RNF-07 con el catálogo de la release modelos-v1.0 (ia-servicio/modelos.json):
 *              el servidor registra los 5 modelos que responde el servicio de IA, quedan en la tabla modelo y en la
 *              comparativa de GET /api/v1/modelo con los valores del catálogo, el modelo en uso tiene un F1 macro de 65 %
 *              o más y la base no deja en uso uno con menos (RNF-07 C1, C2 y C4, D-18, D-48, P-02).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.estadistica.modelo;

import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.analisis.ServidorDeIaDePrueba;
import co.edu.usco.inspectia.analisis.adaptadores.ia.ClienteDelServicioDeIa;
import co.edu.usco.inspectia.analisis.adaptadores.ia.PropiedadesDelServicioDeIa;
import co.edu.usco.inspectia.analisis.adaptadores.ia.RegistroDeModelosDelServicio;
import co.edu.usco.inspectia.analisis.aplicacion.RepositorioDeModelos;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

/**
 * La comparativa de los 5 modelos con los datos de la release {@code modelos-v1.0} (RNF-07). El servicio de IA de
 * mentira ({@link ServidorDeIaDePrueba}) responde en {@code GET /modelos} lo mismo que el servicio real: los modelos de
 * {@code ia-servicio/modelos.json}, con sus métricas, y EfficientNet-B0 en uso. El registro de los modelos del servidor
 * ({@link RegistroDeModelosDelServicio}) los guarda en la tabla {@code modelo} con el repositorio real, y la API del
 * modelo los entrega. Así se prueba el camino completo del catálogo a la pantalla de RF-17, sin el modelo ni la red.
 */
@PruebaIntegracion
class ComparativaDeModelosIT {

    /** El catálogo de la release, el mismo que lee el servicio de IA (D-48); las pruebas corren desde backend/. */
    private static final Path CATALOGO = Path.of("..", "ia-servicio", "modelos.json");
    private static final String RUTA = "/api/v1/modelo";
    private static final String ANA = "admin@cacaos.test";
    private static final String EN_USO = "EfficientNet-B0";
    private static final List<String> METRICAS = List.of("f1Macro", "precisionMacro", "recallMacro", "recallSano",
            "recallNoSano", "precisionSano", "precisionNoSano", "tiempoPorGranoMs");
    private static final List<String> MATRIZ = List.of("sanosComoSanos", "sanosComoNoSanos", "noSanosComoSanos",
            "noSanosComoNoSanos");
    private static final PropiedadesDelServicioDeIa.Tiempos TIEMPOS = new PropiedadesDelServicioDeIa.Tiempos(
            Duration.ofSeconds(2), Duration.ofSeconds(2), Duration.ofSeconds(2), Duration.ofSeconds(15));

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RepositorioDeModelos repositorio;

    private ApiSesion api;
    private ServidorDeIaDePrueba servicio;
    private RegistroDeModelosDelServicio registro;

    @BeforeEach
    void preparar() {
        api = new ApiSesion(mvc, jdbc);
        ApiSesion.borrarDatos(jdbc);
        jdbc.update("DELETE FROM modelo WHERE nombre <> 'clasificador-falso'");
        UUID cacaos = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        api.crearUsuario(cacaos, "Ana Pérez", ANA, "ADMIN_EMPRESA");
        servicio = ServidorDeIaDePrueba.iniciar();
        servicio.responder("/modelos", 200, respuestaDelServicio());
        registro = new RegistroDeModelosDelServicio(new ClienteDelServicioDeIa(servicio.url(), TIEMPOS), repositorio,
                Duration.ofSeconds(30));
    }

    @AfterEach
    void limpiar() {
        registro.destroy();
        servicio.close();
        ApiSesion.borrarDatos(jdbc);
        jdbc.update("DELETE FROM modelo WHERE nombre <> 'clasificador-falso'");
    }

    @Test
    @DisplayName("RNF-07 · C4 · los 5 modelos del catálogo de modelos-v1.0, que el servidor registra desde GET /modelos del servicio de IA, quedan en la tabla modelo y en la comparativa de GET /api/v1/modelo con todos los valores del catálogo")
    void losCincoModelosQuedanEnLaComparativa() {
        assertThat(registro.asegurar()).isTrue();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM modelo WHERE tipo IS NOT NULL", Integer.class))
                .as("los 5 modelos de la comparación, con sus métricas").isEqualTo(5);
        Map<String, Map<String, Object>> enLaApi = porNombre(modelos(consultarOk()));
        List<Map<String, Object>> delCatalogo = catalogo();
        assertThat(enLaApi).containsOnlyKeys("MobileNetV3-L", "EfficientNet-B0", "ResNet-50", "Swin-Tiny",
                "DeiT-Small", "clasificador-falso");
        for (Map<String, Object> esperado : delCatalogo) {
            String nombre = (String) esperado.get("nombre");
            Map<String, Object> modelo = enLaApi.get(nombre);
            assertThat(modelo).as(nombre).containsEntry("version", esperado.get("version"))
                    .containsEntry("tipo", esperado.get("tipo")).containsEntry("enUso", EN_USO.equals(nombre));
            for (String metrica : METRICAS) {
                assertThat(numero(modelo.get(metrica))).as(nombre + " " + metrica)
                        .isEqualByComparingTo(numero(metricas(esperado).get(metrica)));
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> matriz = (Map<String, Object>) modelo.get("matriz");
            for (String celda : MATRIZ) {
                assertThat(matriz.get(celda)).as(nombre + " " + celda).isEqualTo(metricas(esperado).get(celda));
            }
        }
    }

    @Test
    @DisplayName("RNF-07 · C1 · el modelo en uso que deja el registro es EfficientNet-B0, con un F1 macro de 73,50 % en la tabla y en la API, 65 o más; poner en uso MobileNetV3-L (63,80 %) lo rechaza la base y EfficientNet-B0 sigue en uso (P-02)")
    void elModeloEnUsoTieneElF1Minimo() {
        assertThat(registro.asegurar()).isTrue();

        Map<String, Object> enUso = jdbc.queryForMap("SELECT nombre, f1_macro FROM modelo WHERE en_uso");
        assertThat(enUso).containsEntry("nombre", EN_USO);
        assertThat((BigDecimal) enUso.get("f1_macro")).isEqualByComparingTo("73.50")
                .isGreaterThanOrEqualTo(new BigDecimal("65"));
        assertThat(modelos(consultarOk())).filteredOn(modelo -> Boolean.TRUE.equals(modelo.get("enUso")))
                .singleElement().satisfies(modelo -> {
                    assertThat(modelo).containsEntry("nombre", EN_USO);
                    assertThat(numero(modelo.get("f1Macro"))).isGreaterThanOrEqualTo(new BigDecimal("65"));
                });

        assertThatThrownBy(() -> repositorio.ponerEnUso("MobileNetV3-L", "1.0"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("modelo_en_uso_con_f1_minimo");
        assertThat(jdbc.queryForList("SELECT nombre FROM modelo WHERE en_uso", String.class)).containsExactly(EN_USO);
    }

    @Test
    @DisplayName("RNF-07 · C2 · la API reporta de cada uno de los 5 modelos la precisión y el recall de cada clase y su matriz de confusión, que suma los 261 sanos y los 73 no sanos del mismo conjunto de prueba")
    void laApiReportaLasMetricasDeCadaClase() {
        assertThat(registro.asegurar()).isTrue();

        List<Map<String, Object>> comparados = modelos(consultarOk()).stream()
                .filter(modelo -> modelo.get("tipo") != null).toList();
        assertThat(comparados).hasSize(5);
        for (Map<String, Object> modelo : comparados) {
            String nombre = (String) modelo.get("nombre");
            assertThat(modelo).as(nombre).extractingByKeys("precisionSano", "precisionNoSano", "recallSano",
                    "recallNoSano").doesNotContainNull();
            @SuppressWarnings("unchecked")
            Map<String, Integer> matriz = (Map<String, Integer>) modelo.get("matriz");
            assertThat(matriz).as(nombre).containsOnlyKeys(MATRIZ);
            assertThat(matriz.get("sanosComoSanos") + matriz.get("sanosComoNoSanos")).as(nombre + " sanos")
                    .isEqualTo(261);
            assertThat(matriz.get("noSanosComoSanos") + matriz.get("noSanosComoNoSanos")).as(nombre + " no sanos")
                    .isEqualTo(73);
        }
    }

    /** Consulta la API del modelo como la administradora de la empresa y espera el 200. */
    private Map<String, Object> consultarOk() {
        MvcTestResult resultado = api.enviar(HttpMethod.GET, RUTA, null, api.token(ANA));
        assertThat(resultado).hasStatus(HttpStatus.OK);
        return json(resultado);
    }

    /** Los modelos de la respuesta de la API. */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> modelos(Map<String, Object> respuesta) {
        return (List<Map<String, Object>>) respuesta.get("modelos");
    }

    /** Los modelos de la API por su nombre. */
    private static Map<String, Map<String, Object>> porNombre(List<Map<String, Object>> modelos) {
        Map<String, Map<String, Object>> porNombre = new LinkedHashMap<>();
        modelos.forEach(modelo -> porNombre.put((String) modelo.get("nombre"), modelo));
        return porNombre;
    }

    /** Los modelos de {@code ia-servicio/modelos.json}, como los guarda el archivo. */
    private static List<Map<String, Object>> catalogo() {
        try {
            return JsonPath.read(Files.readString(CATALOGO, StandardCharsets.UTF_8), "$.modelos");
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }

    /** Las métricas de un modelo del catálogo. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> metricas(Map<String, Object> modelo) {
        return (Map<String, Object>) modelo.get("metricas");
    }

    /**
     * Lo que responde el servicio de IA en {@code GET /modelos} con el catálogo: cada modelo con su nombre, su versión,
     * su tipo y sus métricas, y EfficientNet-B0 en uso, como {@code EntradaModelo.a_json} del servicio.
     */
    private static String respuestaDelServicio() {
        List<Map<String, Object>> modelos = catalogo().stream().map(modelo -> {
            Map<String, Object> entrada = new LinkedHashMap<>();
            entrada.put("nombre", modelo.get("nombre"));
            entrada.put("version", modelo.get("version"));
            entrada.put("tipo", modelo.get("tipo"));
            entrada.putAll(metricas(modelo));
            return entrada;
        }).toList();
        return JsonMapper.builder().build().writeValueAsString(Map.of("enUso", EN_USO, "modelos", modelos));
    }

    /** Un número de JSON como decimal, para compararlo sin importar si llegó como entero o con decimales. */
    private static BigDecimal numero(Object valor) {
        return new BigDecimal(String.valueOf(valor));
    }
}
