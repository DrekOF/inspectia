/*
 * Nombre: ResultadoDelAnalisisIT.java
 * Descripción: Pruebas de integración del resultado del análisis: el corregido y su margen guardados con el lote, la
 *              muestra con sus conteos y su modelo, los recall del modelo de la muestra, los rangos de la empresa, el
 *              detalle sin recalcular para los dos roles y los granos sin la confianza (RF-10 C6 y C13, RF-11 C4,
 *              RF-12 C9, RF-13 C2, RF-14 C3). Los modelos en uso llevan sus 11 métricas (P-02, D-48).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis;

import static co.edu.usco.inspectia.analisis.ApiAnalisis.fotos;
import static co.edu.usco.inspectia.analisis.ApiAnalisis.granos;
import static co.edu.usco.inspectia.analisis.ApiAnalisis.lote;
import static co.edu.usco.inspectia.analisis.ApiAnalisis.muestras;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.ConfiguracionContenedores;
import co.edu.usco.inspectia.ModeloDeLaComparacion;
import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.analisis.aplicacion.AtenderCola;
import co.edu.usco.inspectia.analisis.aplicacion.ModeloUsado;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
 * El resultado del análisis (RF-10 a RF-12). En Cacaos del Huila, Luis Gómez analiza el lote L-0143 de La Esperanza y
 * Ana Pérez, la administradora, lo consulta. Las fotos de 106 × 100 y 107 × 100 dan, con el clasificador falso, 87 de
 * 100 sanos: 90,6 % corregido con EfficientNet-B0, Estándar, con sugerencia de otra muestra.
 */
@PruebaIntegracion
class ResultadoDelAnalisisIT {

    private static final String LUIS = "luis@cacaos.test";
    private static final String ANA = "admin@cacaos.test";
    private static final int[] SEIS_NO_SANOS = {106, 100};
    private static final int[] SIETE_NO_SANOS = {107, 100};
    private static final String TEXTO_ESTANDAR =
            "Apto para venta estándar. Retirar a mano los granos defectuosos podría subirlo de categoría.";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AtenderCola atender;

    @Autowired
    private ClasificadorProgramable clasificador;

    private ApiSesion api;
    private ApiAnalisis analisis;
    private UUID empresa;
    private UUID luis;
    private String tokenDeLuis;
    private String tokenDeAna;
    private UUID lote;

    @BeforeEach
    void preparar() {
        api = new ApiSesion(mvc, jdbc);
        analisis = new ApiAnalisis(api, new ApiFotos(api, mvc));
        ApiSesion.borrarDatos(jdbc);
        empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        luis = api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");
        api.crearUsuario(empresa, "Ana Pérez", ANA, "ADMIN_EMPRESA");
        tokenDeLuis = api.token(LUIS);
        tokenDeAna = api.token(ANA);
        lote = new ApiLotes(api).registrarOk(tokenDeLuis, "L-0143", "La Esperanza");
    }

    @AfterEach
    void limpiar() {
        clasificador.restablecer();
        ApiSesion.borrarDatos(jdbc);
        jdbc.update("DELETE FROM modelo WHERE nombre <> 'clasificador-falso'");
        ConfiguracionContenedores.vaciarCarpetaDeFotos();
    }

    /** Analiza un lote con dos fotos: las sube, lo envía y atiende la cola. */
    private void analizar(UUID cual, int[]... medidas) {
        analisis.subirYEnviar(tokenDeLuis, cual, medidas);
        assertThat(atender.atenderUna()).isTrue();
    }

    /** Los rangos de la empresa como los dejó RF-13, con Estándar desde 91 %. */
    private void cambiarLosRangos() {
        String insertar = "INSERT INTO rango_recomendacion (empresa_id, categoria, limite_inferior, recomendacion) "
                + "VALUES (?, ?, ?, ?)";
        jdbc.update(insertar, empresa, "PREMIUM", new BigDecimal("96.0"), "Exportación");
        jdbc.update(insertar, empresa, "ESTANDAR", new BigDecimal("91.0"), "Venta estándar de la casa");
        jdbc.update(insertar, empresa, "CORRIENTE", new BigDecimal("82.0"), "Selección a mano antes de vender");
        jdbc.update(insertar, empresa, "FUERA_DE_NORMA", new BigDecimal("0.0"), "Reproceso completo");
    }

    @Test
    @DisplayName("RF-10 · C6 · el porcentaje corregido y su margen se guardan con el lote, y la muestra guarda sus conteos, el modelo que la analizó y la copia de los parámetros")
    void elResultadoQuedaGuardado() {
        analizar(lote, SEIS_NO_SANOS, SIETE_NO_SANOS);

        Map<String, Object> delLote = jdbc.queryForMap("SELECT estado, porcentaje_sanos, margen, categoria, "
                + "recomendacion, sugerir_otra_muestra, aviso_confianza_baja FROM lote WHERE id = ?", lote);
        assertThat(delLote).containsEntry("estado", "ANALIZADO")
                .containsEntry("porcentaje_sanos", new BigDecimal("90.6")).containsEntry("margen", new BigDecimal("7.5"))
                .containsEntry("categoria", "ESTANDAR").containsEntry("recomendacion", TEXTO_ESTANDAR)
                .containsEntry("sugerir_otra_muestra", true).containsEntry("aviso_confianza_baja", false);
        UUID modeloFalso = jdbc.queryForObject("SELECT id FROM modelo WHERE nombre = 'clasificador-falso' "
                + "AND version = '1.0'", UUID.class);
        Map<String, Object> muestra = jdbc.queryForMap("SELECT estado, total_granos, granos_sanos, modelo_id, "
                + "aviso_confianza_baja, repeticiones, umbral_confianza_baja, porcentaje_max_dudosos, usuario_id "
                + "FROM muestra WHERE lote_id = ?", lote);
        assertThat(muestra).containsEntry("estado", "TERMINADA").containsEntry("total_granos", 100)
                .containsEntry("granos_sanos", 87).containsEntry("modelo_id", modeloFalso)
                .containsEntry("aviso_confianza_baja", false).containsEntry("repeticiones", 0)
                .containsEntry("umbral_confianza_baja", new BigDecimal("60.00"))
                .containsEntry("porcentaje_max_dudosos", new BigDecimal("10.0")).containsEntry("usuario_id", luis);
        assertThat(jdbc.queryForObject("SELECT fecha_resultado = (SELECT fecha_fin FROM muestra WHERE lote_id = ?) "
                + "FROM lote WHERE id = ?", Boolean.class, lote, lote)).as("la fecha del resultado es el fin").isTrue();
        assertThat(jdbc.queryForList("SELECT DISTINCT f.estado FROM foto f JOIN muestra m ON m.id = f.muestra_id "
                + "WHERE m.lote_id = ?", String.class, lote)).containsExactly("APROBADA");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM grano", Integer.class)).isEqualTo(100);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM grano WHERE clase = 'NO_SANO'", Integer.class))
                .isEqualTo(13);
    }

    @Test
    @DisplayName("RF-14 · C3 · el resultado responde las cifras guardadas del lote y de su muestra, con las fotos marcadas, al usuario final y a la administradora de empresa")
    void elDetalleParaLosDosRoles() {
        analizar(lote, SEIS_NO_SANOS, SIETE_NO_SANOS);

        Map<String, Object> deLuis = analisis.resultadoOk(tokenDeLuis, lote);
        Map<String, Object> deAna = analisis.resultadoOk(tokenDeAna, lote);

        Map<String, Object> delLote = lote(deLuis);
        assertThat(delLote).containsEntry("id", lote.toString()).containsEntry("codigo", "L-0143")
                .containsEntry("origen", "La Esperanza").containsEntry("estado", "ANALIZADO")
                .containsEntry("porcentajeSanos", 90.6).containsEntry("margen", 7.5)
                .containsEntry("categoria", "ESTANDAR").containsEntry("recomendacion", TEXTO_ESTANDAR)
                .containsEntry("sugerirOtraMuestra", true).containsEntry("avisoConfianzaBaja", false)
                .containsEntry("totalGranos", 100).containsEntry("granosSanos", 87)
                .containsEntry("porcentajeContado", 87.0);
        assertThat(delLote.get("fechaResultado")).isNotNull();
        assertThat(lote(deAna)).isEqualTo(delLote);
        Map<String, Object> muestra = muestras(deLuis).get(0);
        assertThat(muestras(deLuis)).hasSize(1);
        assertThat(muestra).containsEntry("numero", 1).containsEntry("estado", "TERMINADA")
                .containsEntry("analizadaPor", Map.of("id", luis.toString(), "nombre", "Luis Gómez"))
                .containsEntry("modelo", Map.of("nombre", "clasificador-falso", "version", "1.0"))
                .containsEntry("totalGranos", 100).containsEntry("granosSanos", 87)
                .containsEntry("porcentajeContado", 87.0);
        assertThat(muestra.get("fechaEnvio")).isNotNull();
        assertThat(muestra.get("fechaFin")).isEqualTo(delLote.get("fechaResultado"));
        assertThat(fotos(muestra)).extracting(foto -> foto.get("numero")).containsExactly(1, 2);
        assertThat(fotos(muestra)).allSatisfy(foto -> assertThat(granos(foto)).hasSize(50));
        assertThat(fotos(muestras(deAna).get(0))).allSatisfy(foto -> assertThat(granos(foto)).hasSize(50));
        @SuppressWarnings("unchecked")
        String vista = (String) ((Map<String, Object>) fotos(muestra).get(0).get("vista")).get("url");
        assertThat(mvc.get().uri(vista).exchange()).hasStatus(HttpStatus.OK).hasContentType("image/png");
    }

    @Test
    @DisplayName("RF-11 · C4 · los granos del resultado traen su posición, su clase y si son dudosos, pero no la confianza")
    void granosSinLaConfianza() {
        analizar(lote, new int[] {106, 72}, new int[] {107, 72});

        MvcTestResult resultado = analisis.resultado(tokenDeLuis, lote);

        String cuerpo = new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(cuerpo).doesNotContain("confianza");
        List<Map<String, Object>> todos = fotos(muestras(ApiSesion.json(resultado)).get(0)).stream()
                .flatMap(foto -> granos(foto).stream()).toList();
        assertThat(todos).hasSize(100).allSatisfy(grano -> assertThat(grano.keySet())
                .containsExactlyInAnyOrder("x", "y", "ancho", "alto", "clase", "dudoso"));
        assertThat(todos.get(0)).isEqualTo(Map.of("x", 2, "y", 3, "ancho", 6, "alto", 8, "clase", "NO_SANO",
                "dudoso", true));
        assertThat(todos.stream().filter(grano -> Boolean.TRUE.equals(grano.get("dudoso")))).hasSize(44);
        assertThat(todos.stream().filter(grano -> "NO_SANO".equals(grano.get("clase")))).hasSize(13);
        assertThat(lote(ApiSesion.json(resultado))).as("sin repeticiones todavía no hay aviso (RF-09, PR 4)")
                .containsEntry("avisoConfianzaBaja", false).containsEntry("granosSanos", 87);
    }

    @Test
    @DisplayName("RF-10 · C13 · la corrección usa los recall del modelo que analizó la muestra: con ResNet-50, 87 de 100 dan 99,0 %, y si cambia el modelo en uso el lote conserva su porcentaje")
    void recallDelModeloDeLaMuestra() {
        UUID resnet = ModeloDeLaComparacion.RESNET_50.insertar(jdbc, true);
        clasificador.conModelo(new ModeloUsado(ModeloDeLaComparacion.RESNET_50.nombre(), "1.0"));
        analizar(lote, SEIS_NO_SANOS, SIETE_NO_SANOS);

        jdbc.update("UPDATE modelo SET en_uso = false WHERE id = ?", resnet);
        ModeloDeLaComparacion.EFFICIENTNET_B0.insertar(jdbc, true);
        Map<String, Object> resultado = analisis.resultadoOk(tokenDeLuis, lote);

        assertThat(jdbc.queryForObject("SELECT modelo_id FROM muestra WHERE lote_id = ?", UUID.class, lote))
                .isEqualTo(resnet);
        assertThat(lote(resultado)).containsEntry("porcentajeSanos", 99.0).containsEntry("margen", 8.5)
                .containsEntry("categoria", "PREMIUM").containsEntry("sugerirOtraMuestra", false)
                .containsEntry("porcentajeContado", 87.0);
        assertThat(muestras(resultado).get(0)).containsEntry("modelo", Map.of("nombre", "ResNet-50",
                "version", "1.0"));
    }

    @Test
    @DisplayName("RF-12 · C9 · si la empresa cambió sus rangos, el análisis usa los de la empresa: con Estándar desde 91 %, 90,6 % es Corriente, con su texto y con sugerencia")
    void rangosDeLaEmpresa() {
        cambiarLosRangos();

        analizar(lote, SEIS_NO_SANOS, SIETE_NO_SANOS);

        assertThat(lote(analisis.resultadoOk(tokenDeLuis, lote))).containsEntry("porcentajeSanos", 90.6)
                .containsEntry("categoria", "CORRIENTE")
                .containsEntry("recomendacion", "Selección a mano antes de vender")
                .containsEntry("sugerirOtraMuestra", true);
    }

    @Test
    @DisplayName("RF-13 · C2 · un análisis hecho después de cambiar los rangos usa los nuevos, y el resultado del lote anterior no cambia")
    void despuesDeCambiarLosRangos() {
        UUID anterior = lote;
        UUID nuevo = new ApiLotes(api).registrarOk(tokenDeLuis, "L-0144", "La Esperanza");
        analizar(anterior, SEIS_NO_SANOS, SIETE_NO_SANOS);

        cambiarLosRangos();
        analizar(nuevo, SEIS_NO_SANOS, SIETE_NO_SANOS);

        assertThat(lote(analisis.resultadoOk(tokenDeLuis, nuevo))).containsEntry("categoria", "CORRIENTE")
                .containsEntry("recomendacion", "Selección a mano antes de vender");
        assertThat(lote(analisis.resultadoOk(tokenDeLuis, anterior))).containsEntry("categoria", "ESTANDAR")
                .containsEntry("recomendacion", TEXTO_ESTANDAR).containsEntry("porcentajeSanos", 90.6);
    }

    @Test
    @DisplayName("RF-14 · el resultado de un lote pendiente trae las cifras del resultado vacías y su muestra en espera, con sus fotos sin granos")
    void lotePendiente() {
        analisis.subirYEnviar(tokenDeLuis, lote, SEIS_NO_SANOS, SIETE_NO_SANOS);

        Map<String, Object> resultado = analisis.resultadoOk(tokenDeAna, lote);

        assertThat(lote(resultado)).containsEntry("estado", "PENDIENTE").containsEntry("porcentajeSanos", null)
                .containsEntry("margen", null).containsEntry("categoria", null).containsEntry("recomendacion", null)
                .containsEntry("sugerirOtraMuestra", null).containsEntry("avisoConfianzaBaja", null)
                .containsEntry("fechaResultado", null).containsEntry("totalGranos", null)
                .containsEntry("granosSanos", null).containsEntry("porcentajeContado", null);
        Map<String, Object> muestra = muestras(resultado).get(0);
        assertThat(muestra).containsEntry("estado", "EN_ESPERA").containsEntry("modelo", null)
                .containsEntry("totalGranos", null).containsEntry("granosSanos", null)
                .containsEntry("porcentajeContado", null).containsEntry("fechaFin", null);
        assertThat(fotos(muestra)).hasSize(2).allSatisfy(foto -> assertThat(granos(foto)).isEmpty());
    }
}
