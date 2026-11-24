/*
 * Nombre: ReporteDelLoteIT.java
 * Descripción: Pruebas de integración del reporte PDF del lote: la descarga para los dos roles en español y en inglés
 *              con las cifras del resultado, el nombre del archivo, el lote pendiente o de otra empresa, el idioma que
 *              no existe, el reporte guardado que no cambia con los rangos y el que se rehace con la segunda muestra
 *              (RF-15 C1, C2, C4 y C5, RNF-03, RNF-04, D-25, D-50).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis;

import static co.edu.usco.inspectia.analisis.ApiAnalisis.lote;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.error;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.errores;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.ConfiguracionContenedores;
import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.analisis.aplicacion.AtenderCola;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * El reporte PDF del lote (RF-15). En Cacaos del Huila, Luis Gómez analiza el lote L-0143 de La Esperanza con fotos
 * de 106 × 100 y 107 × 100: 87 de 100, 90,6 % con margen 7,5, Estándar, con sugerencia por el límite de 90. Ana
 * Pérez, la administradora, también lo descarga; Marta Ruiz, de Cacao del Sur, no lo ve. La muestra 2 igual da 174
 * de 200, 90,6 % con margen 5,3, sin sugerencia (D-25).
 */
@PruebaIntegracion
class ReporteDelLoteIT {

    private static final String LUIS = "luis@cacaos.test";
    private static final String ANA = "admin@cacaos.test";
    private static final String MARTA = "marta@cacaodelsur.test";
    private static final int[] SEIS_NO_SANOS = {106, 100};
    private static final int[] SIETE_NO_SANOS = {107, 100};

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
    private ApiAnalisis analisisDeLaDos;
    private UUID empresa;
    private String tokenDeLuis;
    private String tokenDeAna;
    private String tokenDeMarta;
    private UUID lote;

    @BeforeEach
    void preparar() {
        api = new ApiSesion(mvc, jdbc);
        ApiFotos fotos = new ApiFotos(api, mvc);
        analisis = new ApiAnalisis(api, fotos);
        analisisDeLaDos = new ApiAnalisis(api, fotos.deLaMuestra(2));
        ApiSesion.borrarDatos(jdbc);
        empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");
        api.crearUsuario(empresa, "Ana Pérez", ANA, "ADMIN_EMPRESA");
        UUID otraEmpresa = api.crearEmpresa("Cacao del Sur", "800765432-1");
        api.crearUsuario(otraEmpresa, "Marta Ruiz", MARTA, "USUARIO_FINAL");
        tokenDeLuis = api.token(LUIS);
        tokenDeAna = api.token(ANA);
        tokenDeMarta = api.token(MARTA);
        lote = new ApiLotes(api).registrarOk(tokenDeLuis, "L-0143", "La Esperanza");
    }

    @AfterEach
    void limpiar() {
        clasificador.restablecer();
        ApiSesion.borrarDatos(jdbc);
        ConfiguracionContenedores.vaciarCarpetaDeFotos();
    }

    /** Sube las fotos de la muestra, la envía y atiende la cola. */
    private void analizar(ApiAnalisis deSuMuestra) {
        deSuMuestra.subirYEnviar(tokenDeLuis, lote, SEIS_NO_SANOS, SIETE_NO_SANOS);
        assertThat(atender.atenderUna()).as("la cola atiende la muestra").isTrue();
    }

    private MvcTestResult reporte(String token, UUID cual, String idioma) {
        MockMvcTester.MockMvcRequestBuilder peticion = mvc.get().uri(ApiLotes.RUTA + "/" + cual + "/reporte"
                + (idioma == null ? "" : "?idioma=" + idioma)).header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return peticion.exchange();
    }

    /** El PDF de una descarga con 200. */
    private byte[] pdf(MvcTestResult resultado) {
        assertThat(resultado).hasStatus(HttpStatus.OK).hasContentType(MediaType.APPLICATION_PDF);
        return resultado.getResponse().getContentAsByteArray();
    }

    private static String nombreDelArchivo(MvcTestResult resultado) {
        return ContentDisposition.parse(resultado.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .getFilename();
    }

    private static String texto(byte[] pdf) {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(documento).replace(' ', ' ').replaceAll("\\s*\\R\\s*", " ");
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }

    private String rutaDelReporte() {
        return jdbc.queryForObject("SELECT reporte_ruta FROM lote WHERE id = ?", String.class, lote);
    }

    @Test
    @DisplayName("RF-15 · C1 · el usuario final descarga el PDF del lote analizado como archivo InspectIA-L-0143-es.pdf, con los datos de la empresa, del lote, del resultado y de la muestra")
    void elUsuarioFinalLoDescarga() {
        analizar(analisis);

        MvcTestResult resultado = reporte(tokenDeLuis, lote, "es");

        String texto = texto(pdf(resultado));
        assertThat(resultado.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).startsWith("attachment");
        assertThat(nombreDelArchivo(resultado)).isEqualTo("InspectIA-L-0143-es.pdf");
        assertThat(resultado.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).contains("no-store");
        assertThat(texto).contains("Cacaos del Huila", "900123456-8", "L-0143", "La Esperanza", "Estándar",
                "Es una categoría probable: la oficial se confirma con la prueba de corte.", "Muestra 1",
                "Analizada por Luis Gómez", "Modelo de IA: clasificador-falso 1.0", "Foto 1", "Foto 2");
    }

    @Test
    @DisplayName("RF-15 · C2 · las cifras del PDF coinciden con las del resultado que muestra la página: el porcentaje, su rango probable, los granos y lo que contó el modelo")
    void cifrasComoElResultado() {
        analizar(analisis);
        Map<String, Object> delLote = lote(analisis.resultadoOk(tokenDeLuis, lote));

        String texto = texto(pdf(reporte(tokenDeLuis, lote, "es")));

        BigDecimal porcentaje = new BigDecimal(delLote.get("porcentajeSanos").toString());
        BigDecimal margen = new BigDecimal(delLote.get("margen").toString());
        String coma = porcentaje.toPlainString().replace('.', ',');
        assertThat(texto).contains(coma + " %", "Rango probable: entre "
                + porcentaje.subtract(margen).toPlainString().replace('.', ',') + " y "
                + porcentaje.add(margen).toPlainString().replace('.', ',') + " %",
                delLote.get("granosSanos") + " de " + delLote.get("totalGranos") + " marcados como sanos",
                "Porcentaje contado por el modelo: " + delLote.get("porcentajeContado").toString().replace('.', ',')
                        + " %");
        assertThat(texto).contains("90,6 %", "Rango probable: entre 83,1 y 98,1 %", "87 de 100 marcados como sanos",
                "El resultado quedó a 0,6 puntos del límite de Estándar (90 %).");
    }

    @Test
    @DisplayName("RF-15 · la administradora de empresa también lo descarga, en inglés con idioma=en o con Accept-Language en inglés")
    void laAdministradoraEnIngles() {
        analizar(analisis);

        MvcTestResult conParametro = reporte(tokenDeAna, lote, "en");
        MvcTestResult conEncabezado = mvc.get().uri(ApiLotes.RUTA + "/" + lote + "/reporte")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDeAna)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en").exchange();

        assertThat(texto(pdf(conParametro))).contains("Report of lot L-0143", "90.6 %",
                "Probable range: between 83.1 and 98.1 %", "87 of 100 marked as healthy");
        assertThat(nombreDelArchivo(conParametro)).isEqualTo("InspectIA-L-0143-en.pdf");
        assertThat(nombreDelArchivo(conEncabezado)).isEqualTo("InspectIA-L-0143-en.pdf");
    }

    @Test
    @DisplayName("RF-15 · un lote pendiente todavía no tiene reporte: 409 con el motivo")
    void pendiente() {
        MvcTestResult resultado = reporte(tokenDeLuis, lote, "es");

        assertThat(resultado).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(resultado)).containsEntry("detail",
                "Este lote todavía no tiene resultado: su reporte se descarga cuando termine el análisis.");
    }

    @Test
    @DisplayName("RNF-03 · el reporte de un lote de otra empresa responde 404, como si no existiera")
    void deOtraEmpresa() {
        analizar(analisis);

        assertThat(reporte(tokenDeMarta, lote, "es")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(reporte(tokenDeMarta, UUID.randomUUID(), "es")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(rutaDelReporte()).as("no se armó").isNull();
    }

    @Test
    @DisplayName("RF-15 · un idioma que no es es ni en responde 400 en «idioma»; sin sesión, 401")
    void idiomaQueNoExiste() {
        analizar(analisis);

        MvcTestResult resultado = reporte(tokenDeLuis, lote, "fr");

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(error("idioma", "El idioma del reporte debe ser es o en."));
        assertThat(mvc.get().uri(ApiLotes.RUTA + "/" + lote + "/reporte").exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("RF-15 · C4 · al descargarlo de nuevo se obtiene el mismo reporte, aunque la empresa haya cambiado sus rangos y su nombre: queda guardado en los dos idiomas (D-50)")
    void elMismoReporte() {
        analizar(analisis);
        byte[] primero = pdf(reporte(tokenDeLuis, lote, "es"));
        String base = rutaDelReporte();
        String insertar = "INSERT INTO rango_recomendacion (empresa_id, categoria, limite_inferior, recomendacion) "
                + "VALUES (?, ?, ?, ?)";
        jdbc.update(insertar, empresa, "PREMIUM", new BigDecimal("96.0"), "Exportación");
        jdbc.update(insertar, empresa, "ESTANDAR", new BigDecimal("91.0"), "Venta estándar de la casa");
        jdbc.update(insertar, empresa, "CORRIENTE", new BigDecimal("82.0"), "Selección a mano antes de vender");
        jdbc.update(insertar, empresa, "FUERA_DE_NORMA", new BigDecimal("0.0"), "Reproceso completo");
        jdbc.update("UPDATE empresa SET nombre = 'Cacaos del Huila S. A. S.' WHERE id = ?", empresa);

        byte[] segundo = pdf(reporte(tokenDeAna, lote, "es"));

        assertThat(segundo).isEqualTo(primero);
        assertThat(rutaDelReporte()).isEqualTo(base);
        assertThat(base).matches("reportes/" + empresa + "/" + lote + "/[0-9a-f-]{36}");
        assertThat(ConfiguracionContenedores.CARPETA_DE_FOTOS.resolve(base + "-es.pdf")).exists();
        assertThat(ConfiguracionContenedores.CARPETA_DE_FOTOS.resolve(base + "-en.pdf")).exists();
        assertThat(texto(segundo)).contains("Cacaos del Huila").doesNotContain("S. A. S.", "Venta estándar de la casa");
    }

    @Test
    @DisplayName("RF-15 · C5 · después de la segunda muestra, el reporte anterior se borra y el nuevo trae el resultado de la suma y las dos muestras (D-25)")
    void segundaMuestra() throws IOException {
        analizar(analisis);
        pdf(reporte(tokenDeLuis, lote, "es"));
        String anterior = rutaDelReporte();

        analizar(analisisDeLaDos);

        assertThat(rutaDelReporte()).as("el lote olvida el reporte anterior").isNull();
        assertThat(ConfiguracionContenedores.CARPETA_DE_FOTOS.resolve(anterior + "-es.pdf")).doesNotExist();
        assertThat(ConfiguracionContenedores.CARPETA_DE_FOTOS.resolve(anterior + "-en.pdf")).doesNotExist();
        String texto = texto(pdf(reporte(tokenDeLuis, lote, "es")));
        assertThat(texto).contains("90,6 %", "Rango probable: entre 85,3 y 95,9 %", "174 de 200 marcados como sanos",
                "Muestra 1", "Muestra 2").doesNotContain("Sugerencia");
        assertThat(rutaDelReporte()).isNotNull().isNotEqualTo(anterior);
        assertThat(Files.exists(ConfiguracionContenedores.CARPETA_DE_FOTOS.resolve(rutaDelReporte() + "-en.pdf")))
                .isTrue();
    }
}
