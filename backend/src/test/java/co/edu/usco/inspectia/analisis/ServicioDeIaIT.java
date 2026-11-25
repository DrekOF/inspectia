/*
 * Nombre: ServicioDeIaIT.java
 * Descripción: Pruebas de integración del backend con el servicio de IA (inspectia.ia.clasificador=servicio) contra un
 *              servidor HTTP de prueba: el registro de los modelos, la revisión rápida al confirmar cada foto, la
 *              revisión de la cola, el mínimo de granos, la falta de respuesta y la repetición por granos dudosos
 *              (RF-06, RF-07, RF-08, RF-09, D-44, D-45, D-46).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis;

import static co.edu.usco.inspectia.analisis.ApiAnalisis.fotos;
import static co.edu.usco.inspectia.analisis.ApiAnalisis.granos;
import static co.edu.usco.inspectia.analisis.ApiAnalisis.lote;
import static co.edu.usco.inspectia.analisis.ApiAnalisis.muestras;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.error;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.errores;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.ConfiguracionContenedores;
import co.edu.usco.inspectia.analisis.ServidorDeIaDePrueba.FotoAnalizada;
import co.edu.usco.inspectia.analisis.aplicacion.AtenderCola;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * El backend con el servicio de IA de verdad, por HTTP, y un servicio de mentira ({@link ServidorDeIaDePrueba}) que
 * cada prueba programa. Es un contexto de Spring propio, con su contenedor de la base, porque cambia
 * {@code inspectia.ia.clasificador}; los tiempos límite son cortos (2 s) para probar la falta de respuesta. Luis Gómez,
 * de Cacaos del Huila, analiza el lote L-001; la cola va a mano.
 */
@SpringBootTest(properties = {"inspectia.ia.clasificador=servicio", "inspectia.ia.tiempos.revisar=2s",
    "inspectia.ia.tiempos.analizar=2s", "inspectia.ia.tiempos.reintento-de-modelos=1s"})
@AutoConfigureMockMvc
@Import(ConfiguracionContenedores.class)
class ServicioDeIaIT {

    private static final ServidorDeIaDePrueba IA = ServidorDeIaDePrueba.iniciar();
    private static final String POR_REPETIR = "Repite las fotos marcadas «Por repetir» antes de analizar la muestra.";
    private static final Duration MAS_QUE_EL_LIMITE = Duration.ofSeconds(4);

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AtenderCola atender;

    private ApiFotos fotos;
    private ApiAnalisis analisis;
    private String token;
    private UUID lote;

    @DynamicPropertySource
    static void servicioDeIa(DynamicPropertyRegistry registro) {
        registro.add("inspectia.ia.url", () -> IA.url().toString());
    }

    @AfterAll
    static void apagarElServicio() {
        IA.close();
    }

    @BeforeEach
    void preparar() {
        IA.restablecer();
        ApiSesion api = new ApiSesion(mvc, jdbc);
        fotos = new ApiFotos(api, mvc);
        analisis = new ApiAnalisis(api, fotos);
        ApiSesion.borrarDatos(jdbc);
        api.crearUsuario(api.crearEmpresa("Cacaos del Huila", "900123456-8"), "Luis Gómez", "luis@cacaos.test",
                "USUARIO_FINAL");
        token = api.token("luis@cacaos.test");
        lote = new ApiLotes(api).registrarOk(token, "L-001");
    }

    @AfterEach
    void limpiar() {
        IA.restablecer();
        ApiSesion.borrarDatos(jdbc);
        ConfiguracionContenedores.vaciarCarpetaDeFotos();
    }

    private Map<String, Object> subir(int numero) {
        return fotos.subirFoto(token, lote, numero, ApiFotos.PNG, ApiFotos.png(120, 100));
    }

    private List<Map<String, Object>> fotosDeLaMuestra() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lista = (List<Map<String, Object>>) json(fotos.listar(token, lote)).get("fotos");
        return lista;
    }

    private Map<String, Object> fotosYMuestra() {
        return json(fotos.listar(token, lote));
    }

    /** Programa la respuesta de /analizar con esas fotos. */
    private static void analizarCon(FotoAnalizada... fotosAnalizadas) {
        IA.responder("/analizar", 200, ServidorDeIaDePrueba.analisis(List.of(fotosAnalizadas)));
    }

    @Test
    @DisplayName("RF-08 · C4 · al arrancar registra los 5 modelos de la comparación con sus métricas y deja en uso solo el que carga el servicio, con su F1 de 65 % o más")
    void registraLosModelos() {
        assertThat(jdbc.queryForList("SELECT nombre FROM modelo WHERE tipo IS NOT NULL ORDER BY nombre", String.class))
                .containsExactly("EfficientNet-B0", "MobileNetV3-L", "ResNet-50", "Transformador-A",
                        "Transformador-B");
        Map<String, Object> enUso = jdbc.queryForMap("SELECT nombre, version, f1_macro FROM modelo WHERE en_uso");
        assertThat(enUso).containsEntry("nombre", "EfficientNet-B0").containsEntry("version", "1.0");
        assertThat((java.math.BigDecimal) enUso.get("f1_macro")).isGreaterThanOrEqualTo(new java.math.BigDecimal("65"));
    }

    @Test
    @DisplayName("RF-06 · C3 · una foto que cumple la guía pasa sin avisos: al confirmarla queda «Aprobada» con sus granos encontrados")
    void fotoAprobada() {
        IA.responder("/revisar", 200, ServidorDeIaDePrueba.revision("APROBADA", null, 52));

        Map<String, Object> foto = subir(1);

        assertThat(foto).containsEntry("estado", "APROBADA").containsEntry("motivo", null)
                .containsEntry("granosEncontrados", 52).containsEntry("regionesPegadas", List.of())
                .containsEntry("granosDudosos", List.of());
        assertThat(IA.peticiones("/revisar")).hasSize(1).allSatisfy(peticion ->
                assertThat(peticion.tieneParte("foto")).isTrue());
        assertThat(fotosDeLaMuestra().get(0)).containsEntry("estado", "APROBADA").containsEntry("granosEncontrados", 52);
    }

    @Test
    @DisplayName("RF-06 · C1 · una foto borrosa se rechaza al confirmarla: queda «Por repetir» con el motivo BORROSA y la muestra no se puede analizar hasta repetirla")
    void fotoBorrosa() {
        subir(1);
        IA.responder("/revisar", 200, ServidorDeIaDePrueba.revision("POR_REPETIR", "BORROSA", 0));

        Map<String, Object> foto = subir(2);

        assertThat(foto).containsEntry("estado", "POR_REPETIR").containsEntry("motivo", "BORROSA")
                .containsEntry("granosEncontrados", 0);
        MvcTestResult envio = analisis.enviar(token, lote);
        assertThat(envio).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(envio)).containsExactly(error("fotos", POR_REPETIR));
        assertThat(jdbc.queryForObject("SELECT estado FROM muestra WHERE lote_id = ?", String.class, lote))
                .isEqualTo("PENDIENTE");

        IA.responder("/revisar", 200, ServidorDeIaDePrueba.revision("APROBADA", null, 50));
        assertThat(subir(2)).containsEntry("estado", "APROBADA").containsEntry("motivo", null);
        analisis.enviarOk(token, lote);
    }

    @Test
    @DisplayName("RF-06 · C2 · una foto oscura se rechaza al confirmarla: queda «Por repetir» con el motivo POCA_LUZ")
    void fotoOscura() {
        IA.responder("/revisar", 200, ServidorDeIaDePrueba.revision("POR_REPETIR", "POCA_LUZ", 12));

        assertThat(subir(1)).containsEntry("estado", "POR_REPETIR").containsEntry("motivo", "POCA_LUZ")
                .containsEntry("granosEncontrados", 12);
    }

    @Test
    @DisplayName("RF-07 · C2 · una foto con granos pegados se rechaza y muestra dónde están; al reemplazarla, sus regiones se borran con ella (D-45)")
    void granosPegados() {
        IA.responder("/revisar", 200, ServidorDeIaDePrueba.revision("POR_REPETIR", "GRANOS_PEGADOS", 47,
                new int[] {1210, 860, 240, 130}, new int[] {30, 40, 90, 60}));

        Map<String, Object> foto = subir(1);

        List<Map<String, Object>> regiones = List.of(Map.of("x", 1210, "y", 860, "ancho", 240, "alto", 130),
                Map.of("x", 30, "y", 40, "ancho", 90, "alto", 60));
        assertThat(foto).containsEntry("estado", "POR_REPETIR").containsEntry("motivo", "GRANOS_PEGADOS")
                .containsEntry("granosEncontrados", 47).containsEntry("regionesPegadas", regiones);
        assertThat(fotosDeLaMuestra().get(0)).containsEntry("regionesPegadas", regiones);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM region_pegada", Integer.class)).isEqualTo(2);

        IA.restablecer();
        assertThat(subir(1)).containsEntry("estado", "APROBADA").containsEntry("regionesPegadas", List.of());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM region_pegada", Integer.class)).isZero();
    }

    @Test
    @DisplayName("D-46 · si el servicio no responde al confirmar, la foto queda «Cargada»; la cola la revisa y, si pasa, la muestra termina con la foto aprobada y sus granos")
    void sinRespuestaAlConfirmarLaRevisaLaCola() {
        IA.demorar("/revisar", MAS_QUE_EL_LIMITE);
        assertThat(subir(1)).containsEntry("estado", "CARGADA").containsEntry("granosEncontrados", null);
        assertThat(subir(2)).containsEntry("estado", "CARGADA");
        IA.restablecer();
        analizarCon(FotoAnalizada.aprobada(1, 50, 6, 0), FotoAnalizada.aprobada(2, 50, 7, 0));

        analisis.enviarOk(token, lote);
        assertThat(atender.atenderUna()).isTrue();

        assertThat(analisis.avanceOk(token, lote)).containsEntry("estado", "TERMINADA").containsEntry("fallo", false);
        assertThat(fotosDeLaMuestra()).allSatisfy(foto -> assertThat(foto).containsEntry("estado", "APROBADA")
                .containsEntry("granosEncontrados", 50));
        assertThat(IA.peticiones("/analizar").get(0).fotos()).containsExactly(1, 2);
    }

    @Test
    @DisplayName("RF-06 · C1 · una foto «Cargada» que no pasa la revisión de la cola devuelve la muestra a pendiente, sin resultado, con la foto por repetir y su motivo (D-44)")
    void fotoQueNoPasaLaRevisionDeLaCola() {
        IA.demorar("/revisar", MAS_QUE_EL_LIMITE);
        subir(1);
        subir(2);
        IA.restablecer();
        analizarCon(FotoAnalizada.aprobada(1, 50, 0, 0), FotoAnalizada.porRepetir(2, "BORROSA", 4));
        analisis.enviarOk(token, lote);

        atender.atenderUna();

        Map<String, Object> avance = analisis.avanceOk(token, lote);
        assertThat(avance).containsEntry("estado", "PENDIENTE").containsEntry("fallo", true)
                .containsEntry("motivoDevolucion", null).containsEntry("repeticiones", 0)
                .containsEntry("fotosPorRepetir", List.of(Map.of("numero", 2, "motivo", "BORROSA")));
        assertThat(fotosDeLaMuestra().get(1)).containsEntry("estado", "POR_REPETIR").containsEntry("motivo", "BORROSA");
        assertThat(fotosDeLaMuestra().get(0)).containsEntry("estado", "APROBADA").containsEntry("granosEncontrados", 50);
        assertThat(lote(analisis.resultadoOk(token, lote))).containsEntry("estado", "PENDIENTE");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM grano", Integer.class)).isZero();
    }

    @Test
    @DisplayName("RF-07 · C3 · con menos de 100 granos válidos no calcula el resultado: la muestra vuelve con «Menos de 100 granos» y el motivo se borra al enviarla de nuevo")
    void menosDeCienGranos() {
        subir(1);
        subir(2);
        analizarCon(FotoAnalizada.aprobada(1, 50, 0, 0), FotoAnalizada.aprobada(2, 49, 0, 0));
        analisis.enviarOk(token, lote);

        atender.atenderUna();

        assertThat(analisis.avanceOk(token, lote)).containsEntry("fallo", true)
                .containsEntry("motivoDevolucion", "POCOS_GRANOS").containsEntry("fotosPorRepetir", List.of());
        Map<String, Object> muestra = fotosYMuestra();
        assertThat(muestra).containsEntry("motivoDevolucion", "POCOS_GRANOS");
        assertThat(fotosDeLaMuestra()).extracting(foto -> foto.get("granosEncontrados")).containsExactly(50, 49);
        assertThat(muestras(analisis.resultadoOk(token, lote)).get(0)).containsEntry("motivoDevolucion",
                "POCOS_GRANOS");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM grano", Integer.class)).isZero();

        assertThat(analisis.enviarOk(token, lote)).containsEntry("estado", "EN_ESPERA")
                .containsEntry("motivoDevolucion", null);
        assertThat(jdbc.queryForObject("SELECT motivo_devolucion FROM muestra WHERE lote_id = ?", String.class, lote))
                .isNull();
    }

    @Test
    @DisplayName("RF-08 · C6 · si el servicio de IA no responde en la cola, el sistema avisa al usuario con «Sin respuesta del servicio de IA» y no guarda un resultado incompleto")
    void sinRespuestaEnLaCola() {
        subir(1);
        subir(2);
        IA.responder("/analizar", 503, "{\"listo\": false}");
        analisis.enviarOk(token, lote);

        atender.atenderUna();

        assertThat(analisis.avanceOk(token, lote)).containsEntry("estado", "PENDIENTE").containsEntry("fallo", true)
                .containsEntry("motivoDevolucion", "SIN_RESPUESTA_DE_LA_IA");
        assertThat(lote(analisis.resultadoOk(token, lote))).containsEntry("estado", "PENDIENTE")
                .containsEntry("porcentajeSanos", null);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM grano", Integer.class)).isZero();

        IA.restablecer();
        IA.demorar("/analizar", MAS_QUE_EL_LIMITE);
        analisis.enviarOk(token, lote);
        atender.atenderUna();
        assertThat(analisis.avanceOk(token, lote)).containsEntry("motivoDevolucion", "SIN_RESPUESTA_DE_LA_IA");
    }

    @Test
    @DisplayName("RF-08 · C3 · el análisis guarda qué modelo y qué versión lo hicieron, y cada grano con una sola clase y su confianza (C1, C2)")
    void guardaElModeloYLosGranos() {
        subir(1);
        subir(2);
        analizarCon(FotoAnalizada.aprobada(1, 50, 6, 0), FotoAnalizada.aprobada(2, 50, 7, 0));
        analisis.enviarOk(token, lote);

        atender.atenderUna();

        Map<String, Object> resultado = analisis.resultadoOk(token, lote);
        assertThat(lote(resultado)).containsEntry("estado", "ANALIZADO").containsEntry("granosSanos", 87)
                .containsEntry("avisoConfianzaBaja", false);
        Map<String, Object> muestra = muestras(resultado).get(0);
        assertThat(muestra.get("modelo")).isEqualTo(Map.of("nombre", "EfficientNet-B0", "version", "1.0"));
        assertThat(muestra).containsEntry("avisoConfianzaBaja", false).containsEntry("fotosPorRepetir", List.of());
        assertThat(fotos(muestra)).allSatisfy(foto -> assertThat(granos(foto)).hasSize(50));
        assertThat(jdbc.queryForList("SELECT DISTINCT clase FROM grano ORDER BY clase", String.class))
                .containsExactly("NO_SANO", "SANO");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM grano WHERE confianza IS NULL", Integer.class)).isZero();
    }

    @Test
    @DisplayName("RF-09 · C1 · con más del 10 % de dudosos pide repetir la foto con más dudosos y muestra cuáles son, sin la clase; la muestra vuelve con 1 repetición")
    void pideRepetirPorDudosos() {
        subir(1);
        subir(2);
        analizarCon(FotoAnalizada.aprobada(1, 50, 0, 4), FotoAnalizada.aprobada(2, 50, 0, 8));
        analisis.enviarOk(token, lote);

        atender.atenderUna();

        assertThat(analisis.avanceOk(token, lote)).containsEntry("fallo", true).containsEntry("motivoDevolucion", null)
                .containsEntry("repeticiones", 1)
                .containsEntry("fotosPorRepetir", List.of(Map.of("numero", 2, "motivo", "GRANOS_DUDOSOS")));
        Map<String, Object> muestra = fotosYMuestra();
        assertThat(muestra).containsEntry("repeticiones", 1).containsEntry("porcentajeMaxDudosos", 10.0);
        Map<String, Object> foto = fotosDeLaMuestra().get(1);
        assertThat(foto).containsEntry("estado", "POR_REPETIR").containsEntry("motivo", "GRANOS_DUDOSOS")
                .containsEntry("granosEncontrados", 50);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dudosos = (List<Map<String, Object>>) foto.get("granosDudosos");
        assertThat(dudosos).hasSize(8).allSatisfy(dudoso -> assertThat(dudoso.keySet())
                .containsExactlyInAnyOrder("x", "y", "ancho", "alto"));
        assertThat(fotosDeLaMuestra().get(0)).containsEntry("granosDudosos", List.of());
        assertThat(fotos(muestras(analisis.resultadoOk(token, lote)).get(0)))
                .allSatisfy(fotoDelResultado -> assertThat(granos(fotoDelResultado)).isEmpty());
        assertThat(lote(analisis.resultadoOk(token, lote))).containsEntry("estado", "PENDIENTE");
    }

    @Test
    @DisplayName("RF-09 · C3 · la foto repetida reemplaza a la anterior, que se borra con sus granos, y pasa por la misma revisión que las primeras; la repetición ya contada no cambia")
    void laFotoRepetidaReemplazaALaAnterior() {
        subir(1);
        subir(2);
        analizarCon(FotoAnalizada.aprobada(1, 50, 0, 0), FotoAnalizada.aprobada(2, 50, 0, 12));
        analisis.enviarOk(token, lote);
        atender.atenderUna();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM grano", Integer.class)).isEqualTo(50);
        int revisiones = IA.peticiones("/revisar").size();

        assertThat(subir(2)).containsEntry("estado", "APROBADA").containsEntry("granosDudosos", List.of());

        assertThat(IA.peticiones("/revisar")).hasSize(revisiones + 1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM grano", Integer.class)).isZero();
        assertThat(fotosYMuestra()).containsEntry("repeticiones", 1);
    }

    @Test
    @DisplayName("RF-09 · C2 · con 10 % o menos de dudosos el análisis sigue sin pedir nada: termina sin aviso (prueba de límite)")
    void conDiezPorCientoNoPideNada() {
        subir(1);
        subir(2);
        analizarCon(FotoAnalizada.aprobada(1, 50, 0, 5), FotoAnalizada.aprobada(2, 50, 0, 5));
        analisis.enviarOk(token, lote);

        atender.atenderUna();

        assertThat(analisis.avanceOk(token, lote)).containsEntry("estado", "TERMINADA").containsEntry("repeticiones", 0);
        assertThat(lote(analisis.resultadoOk(token, lote))).containsEntry("avisoConfianzaBaja", false);
    }

    @Test
    @DisplayName("RF-09 · C4 · si tras 2 repeticiones siguen pasando del 10 %, el resultado sale con el aviso de confianza baja, en la muestra y en el lote (C6)")
    void avisoTrasDosRepeticiones() {
        subir(1);
        subir(2);
        analizarCon(FotoAnalizada.aprobada(1, 50, 0, 0), FotoAnalizada.aprobada(2, 50, 0, 12));
        for (int repeticion = 1; repeticion <= 2; repeticion++) {
            analisis.enviarOk(token, lote);
            atender.atenderUna();
            assertThat(analisis.avanceOk(token, lote)).containsEntry("repeticiones", repeticion)
                    .containsEntry("fotosPorRepetir", List.of(Map.of("numero", 2, "motivo", "GRANOS_DUDOSOS")));
            subir(2);
        }
        analisis.enviarOk(token, lote);

        atender.atenderUna();

        Map<String, Object> resultado = analisis.resultadoOk(token, lote);
        assertThat(lote(resultado)).containsEntry("estado", "ANALIZADO").containsEntry("avisoConfianzaBaja", true);
        assertThat(muestras(resultado).get(0)).containsEntry("avisoConfianzaBaja", true)
                .containsEntry("repeticiones", 2).containsEntry("porcentajeMaxDudosos", 10.0);
    }
}
