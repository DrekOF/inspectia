/*
 * Nombre: GeneradorDeReporteEnPdfTest.java
 * Descripción: Pruebas unitarias del reporte PDF con Apache PDFBox: los datos de la salida de RF-15, las cifras con el
 *              formato de la página en español y en inglés, el rango probable recortado, la sugerencia con su límite,
 *              el aviso de confianza baja, las dos muestras, las fotos marcadas, las fuentes embebidas y una foto que
 *              no está (RF-15 C1, C2, C3 y C5, D-43, D-47). El texto se lee del PDF con la misma librería.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.analisis.aplicacion.ContenidoDelReporte;
import co.edu.usco.inspectia.analisis.aplicacion.ContenidoDelReporte.FotoDelReporte;
import co.edu.usco.inspectia.analisis.aplicacion.IdiomaDelReporte;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * El reporte PDF (RF-15) con los textos del backend. El lote L-0143 de Cacaos del Huila tiene 87 de 100 sanos, 90,6 %
 * con margen 7,5 (rango de 83,1 a 98,1 %), Estándar, y sugiere otra muestra por el límite de Estándar (90 %).
 *
 * <p>Con {@code -Dreporte.muestras=<carpeta>}, la prueba de los datos guarda en esa carpeta los reportes de ejemplo en
 * español y en inglés, para revisar el diseño.
 */
class GeneradorDeReporteEnPdfTest {

    private final GeneradorDeReporteEnPdf generador = new GeneradorDeReporteEnPdf(mensajes());

    /** Los textos del backend, como los carga Spring Boot. */
    static ResourceBundleMessageSource mensajes() {
        ResourceBundleMessageSource mensajes = new ResourceBundleMessageSource();
        mensajes.setBasenames("i18n/mensajes", "i18n/generado");
        mensajes.setDefaultEncoding("UTF-8");
        mensajes.setFallbackToSystemLocale(false);
        return mensajes;
    }

    /**
     * El texto del PDF en una sola línea: los saltos de línea y los espacios que no se parten van como espacios
     * normales, así se encuentra un párrafo que el PDF partió en varias líneas.
     */
    static String texto(byte[] pdf) {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(documento).replace('\u00a0', ' ').replaceAll("\\s*\\R\\s*", " ");
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }

    /** Cuántas imágenes tiene el PDF, en todas sus hojas. */
    private static int imagenes(byte[] pdf) throws IOException {
        int cuantas = 0;
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            for (PDPage hoja : documento.getPages()) {
                PDResources recursos = hoja.getResources();
                for (COSName nombre : recursos.getXObjectNames()) {
                    if (recursos.getXObject(nombre) instanceof PDImageXObject) {
                        cuantas++;
                    }
                }
            }
        }
        return cuantas;
    }

    private void guardarMuestra(byte[] pdf, String nombre) throws IOException {
        String carpeta = System.getProperty("reporte.muestras");
        if (carpeta != null && !carpeta.isBlank()) {
            Files.write(Path.of(carpeta).resolve(nombre), pdf);
        }
    }

    @Test
    @DisplayName("RF-15 · C1 · el PDF trae todos los datos de la salida: empresa, código y fechas del lote, granos, porcentaje corregido con su margen, categoría probable, recomendación, fecha del resultado y, por la muestra, quién la analizó, sus fotos marcadas, sus granos, lo que contó el modelo y el modelo")
    void todosLosDatos() throws IOException {
        byte[] pdf = generador.generar(ContenidosDePrueba.unaMuestra(), IdiomaDelReporte.ES);
        guardarMuestra(pdf, "muestra-reporte-es.pdf");
        guardarMuestra(generador.generar(ContenidosDePrueba.unaMuestra(), IdiomaDelReporte.EN),
                "muestra-reporte-en.pdf");

        String texto = texto(pdf);

        assertThat(texto).startsWith("InspectIA").contains("Reporte del lote L-0143", "Resultado del lote L-0143",
                "Empresa", "Cacaos del Huila", "NIT", "900123456-8", "Código del lote", "L-0143",
                "Finca o proveedor", "La Esperanza", "Fecha de recepción", "20/10/2026", "Fecha del resultado",
                "23/11/2026 a las 10:00 a. m.", "90,6 %", "de granos sanos", "Rango probable: entre 83,1 y 98,1 %",
                "Granos", "100", "Sanos", "87", "No sanos", "13", "Categoría probable", "Estándar", "Recomendación",
                ContenidosDePrueba.ESTANDAR, "Muestra 1", "Analizada por Luis Gómez",
                "Modelo de IA: EfficientNet-B0 1.0", "Foto 1 · 50 granos", "Foto 2 · 50 granos",
                "87 de 100 marcados como sanos", "Marcas de las fotos:", "Sano", "No sano", "Dudoso",
                "Reporte generado el 24/11/2026 a las 3:05 p. m.", "Página 1 de");
        assertThat(imagenes(pdf)).as("las 2 fotos marcadas").isEqualTo(2);
    }

    @Test
    @DisplayName("RF-15 · C1 · el PDF aclara que la categoría es probable y que la oficial se confirma con la prueba de corte (D-10), y explica que el porcentaje corrige los errores del modelo (D-39)")
    void notaDeLaPruebaDeCorte() {
        String texto = texto(generador.generar(ContenidosDePrueba.unaMuestra(), IdiomaDelReporte.ES));

        assertThat(texto).contains("Es una categoría probable: la oficial se confirma con la prueba de corte.",
                "El porcentaje corrige los errores conocidos del modelo.",
                "Porcentaje contado por el modelo: 87,0 %");
    }

    @Test
    @DisplayName("RF-15 · C2 · las cifras del PDF son las de la página, con su formato: 90,6 %, el rango probable de 83,1 a 98,1 % y 87 de 100 marcados como sanos (D-43)")
    void cifrasComoLaPagina() {
        String texto = texto(generador.generar(ContenidosDePrueba.unaMuestra(), IdiomaDelReporte.ES));

        assertThat(texto).contains("90,6 %", "Rango probable: entre 83,1 y 98,1 %", "87 de 100 marcados como sanos",
                "El resultado quedó a 0,6 puntos del límite de Estándar (90 %).");
    }

    @Test
    @DisplayName("RF-15 · C2 · en inglés, las cifras llevan el formato de la página en inglés: 90.6 %, between 83.1 and 98.1 % y la fecha 11/23/2026 10:00 AM")
    void cifrasEnIngles() {
        String texto = texto(generador.generar(ContenidosDePrueba.unaMuestra(), IdiomaDelReporte.EN));

        assertThat(texto).contains("Report of lot L-0143", "Company", "Cacaos del Huila", "90.6 %", "healthy beans",
                "Probable range: between 83.1 and 98.1 %", "87 of 100 marked as healthy", "Standard",
                "It is a probable category: the official one is confirmed with the cut test.",
                "The result was 0.6 points from the Standard limit (90 %).", "Sample 1", "Analyzed by Luis Gómez",
                "AI model: EfficientNet-B0 1.0", "10/20/2026", "11/23/2026 at 10:00 AM", "Page 1 of");
    }

    @Test
    @DisplayName("RF-15 · C2 · el rango probable se recorta a 0–100 %: 100,0 % con margen 5,7 queda entre 94,3 y 100 %, y 3,0 % con margen 7,5, entre 0 y 10,5 % (D-43)")
    void rangoRecortado() {
        ContenidoDelReporte arriba = conLote(ContenidosDePrueba.lote("100.0", "5.7", "PREMIUM", "Exportación", false,
                null, false, ContenidosDePrueba.FIN_1));
        ContenidoDelReporte abajo = conLote(ContenidosDePrueba.lote("3.0", "7.5", "FUERA_DE_NORMA", "Reproceso",
                false, null, false, ContenidosDePrueba.FIN_1));

        assertThat(texto(generador.generar(arriba, IdiomaDelReporte.ES)))
                .contains("100,0 %", "Rango probable: entre 94,3 y 100 %", "Premium o Especial")
                .doesNotContain("Sugerencia");
        assertThat(texto(generador.generar(abajo, IdiomaDelReporte.ES)))
                .contains("Rango probable: entre 0 y 10,5 %", "Fuera de norma");
    }

    @Test
    @DisplayName("RF-15 · C2 · justo en el límite, la sugerencia dice «quedó justo en el límite», y un límite con decimal va con él (D-47)")
    void sugerenciaEnElLimite() {
        ContenidoDelReporte enElLimite = conLote(ContenidosDePrueba.lote("90.5", "7.5", "ESTANDAR", "Venta", true,
                "90.5", false, ContenidosDePrueba.FIN_1));

        assertThat(texto(generador.generar(enElLimite, IdiomaDelReporte.ES)))
                .contains("El resultado quedó justo en el límite de Estándar (90,5 %).");
    }

    @Test
    @DisplayName("RF-15 · C1 · con el aviso de confianza baja, el PDF lo trae: «Confianza baja: confirma con la prueba de corte.» (RF-09 C4)")
    void avisoDeConfianzaBaja() {
        ContenidoDelReporte conAviso = conLote(ContenidosDePrueba.lote("90.6", "7.5", "ESTANDAR", "Venta", false,
                null, true, ContenidosDePrueba.FIN_1));

        assertThat(texto(generador.generar(conAviso, IdiomaDelReporte.ES)))
                .contains("Confianza baja: confirma con la prueba de corte.");
        assertThat(texto(generador.generar(ContenidosDePrueba.unaMuestra(), IdiomaDelReporte.ES)))
                .doesNotContain("Confianza baja");
    }

    @Test
    @DisplayName("RF-15 · C5 · después de la segunda muestra, el PDF trae el resultado de la suma y las dos muestras, cada una con quien la analizó, su modelo, sus granos y sus fotos (D-25, D-18)")
    void dosMuestras() throws IOException {
        byte[] pdf = generador.generar(ContenidosDePrueba.dosMuestras(), IdiomaDelReporte.ES);
        String texto = texto(pdf);

        assertThat(texto).contains("Rango probable: entre 85,3 y 95,9 %", "174 de 200 marcados como sanos",
                "Muestra 1", "Analizada por Luis Gómez", "Modelo de IA: EfficientNet-B0 1.0", "Muestra 2",
                "Analizada por Pedro Díaz", "Modelo de IA: ResNet-50 1.0", "Foto 3 · 33 granos")
                .doesNotContain("Sugerencia");
        assertThat(texto.indexOf("Muestra 1")).isLessThan(texto.indexOf("Muestra 2"));
        assertThat(imagenes(pdf)).as("las 2 fotos de la 1 y las 3 de la 2").isEqualTo(5);
    }

    @Test
    @DisplayName("RF-15 · C3 · el PDF se abre igual en el celular y en el PC: tamaño carta, con la letra embebida y el idioma del documento")
    void fuentesEmbebidas() throws IOException {
        byte[] pdf = generador.generar(ContenidosDePrueba.dosMuestras(), IdiomaDelReporte.ES);

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            assertThat(documento.getDocumentCatalog().getLanguage()).isEqualTo("es-CO");
            assertThat(documento.getDocumentInformation().getTitle()).isEqualTo("Reporte del lote L-0143");
            for (PDPage hoja : documento.getPages()) {
                assertThat(hoja.getMediaBox().getWidth()).isEqualTo(PDRectangle.LETTER.getWidth());
                PDResources recursos = hoja.getResources();
                for (COSName nombre : recursos.getFontNames()) {
                    PDFont fuente = recursos.getFont(nombre);
                    assertThat(fuente.isEmbedded()).as(fuente.getName()).isTrue();
                    assertThat(fuente.getName()).contains("Roboto");
                }
            }
        }
    }

    @Test
    @DisplayName("RF-15 · una foto que no está en el almacén no impide el reporte: en su lugar dice «La foto no está disponible.»")
    void fotoQueNoEsta() throws IOException {
        ContenidoDelReporte original = ContenidosDePrueba.unaMuestra();
        FotoDelReporte sinArchivo = new FotoDelReporte(2, null, original.muestras().getFirst().fotos().get(1).granos());
        FotoDelReporte danada = new FotoDelReporte(3, new byte[] {1, 2, 3}, List.of());
        ContenidoDelReporte contenido = new ContenidoDelReporte(original.empresa(), original.nit(), original.lote(),
                original.totalGranos(), original.granosSanos(), original.porcentajeContado(),
                List.of(new ContenidoDelReporte.MuestraDelReporte(original.muestras().getFirst().muestra(),
                        new BigDecimal("87.0"), List.of(original.muestras().getFirst().fotos().getFirst(), sinArchivo,
                                danada))), original.generado());

        byte[] pdf = generador.generar(contenido, IdiomaDelReporte.ES);

        assertThat(texto(pdf)).contains("La foto no está disponible.", "Foto 3 · 0 granos");
        assertThat(imagenes(pdf)).isEqualTo(1);
    }

    @Test
    @DisplayName("RF-15 · un carácter que Roboto no tiene, como un emoji en el nombre de la empresa, se escribe como «?» y el reporte sale igual")
    void caracterSinLetra() {
        ContenidoDelReporte original = ContenidosDePrueba.unaMuestra();
        ContenidoDelReporte contenido = new ContenidoDelReporte("Cacaos 🍫 del Huila", original.nit(),
                original.lote(), original.totalGranos(), original.granosSanos(), original.porcentajeContado(),
                original.muestras(), original.generado());

        assertThat(texto(generador.generar(contenido, IdiomaDelReporte.ES))).contains("Cacaos ? del Huila");
    }

    private static ContenidoDelReporte conLote(co.edu.usco.inspectia.analisis.aplicacion.ResultadoGuardado lote) {
        ContenidoDelReporte base = ContenidosDePrueba.unaMuestra();
        return new ContenidoDelReporte(base.empresa(), base.nit(), lote, base.totalGranos(), base.granosSanos(),
                base.porcentajeContado(), base.muestras(), base.generado());
    }
}
