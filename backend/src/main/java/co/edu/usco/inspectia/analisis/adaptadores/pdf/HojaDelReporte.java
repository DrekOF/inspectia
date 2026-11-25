/*
 * Nombre: HojaDelReporte.java
 * Descripción: Las hojas del reporte PDF con Apache PDFBox: tamaño carta, la barra de InspectIA arriba, el pie con la
 *              fecha y el número de página, Roboto embebida, y el texto partido en líneas, los recuadros y las fotos
 *              con los colores del prototipo (RF-15).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.pdf;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Las hojas del reporte (RF-15). Escribe de arriba abajo: {@link #y()} es la altura por la que va, y
 * {@link #asegurar(float)} pasa a una hoja nueva si lo que sigue no cabe. Cada hoja lleva arriba la barra café de la
 * página con «InspectIA» y el título, y abajo, al terminar, el pie con la fecha del reporte y «Página n de m».
 *
 * <p>La letra es Roboto, la del prototipo, embebida solo con los caracteres que se usan (RF-15 C3). Un carácter que
 * Roboto no tiene, como un emoji en el nombre de la empresa, se escribe como «?»: PDFBox no puede escribirlo.
 */
final class HojaDelReporte implements AutoCloseable {

    /** Tamaño carta, el de las impresoras de Colombia; en el celular se ve con zoom, como cualquier PDF. */
    static final PDRectangle TAMANO = PDRectangle.LETTER;

    /** Margen de los lados. */
    static final float MARGEN = 48;

    /** Alto de la barra de arriba. */
    static final float ALTO_DE_LA_BARRA = 44;

    /** Lo que se reserva abajo para el pie. */
    static final float ALTO_DEL_PIE = 54;

    /** Colores del prototipo, en el tema claro (su README). */
    static final Color BARRA = new Color(0x6B3F22);
    static final Color TEXTO = new Color(0x2B1D14);
    static final Color SECUNDARIO = new Color(0x65503F);
    static final Color SELECCION = new Color(0xF1E4D8);
    static final Color FONDO = new Color(0xFAF6F1);
    static final Color BORDE = new Color(0xE8DED3);
    static final Color EXITO = new Color(0x2E6B34);
    static final Color EXITO_FONDO = new Color(0xE7F1E7);
    static final Color ERROR = new Color(0xB3261E);
    static final Color ERROR_TEXTO = new Color(0x8C1D18);
    static final Color ERROR_FONDO = new Color(0xFBEAE8);
    static final Color ADVERTENCIA = new Color(0x8A5300);
    static final Color ADVERTENCIA_TEXTO = new Color(0x6B4100);
    static final Color ADVERTENCIA_FONDO = new Color(0xFBF0DC);
    static final Color INFORMACION = new Color(0x265C8A);
    static final Color INFORMACION_TEXTO = new Color(0x1B4468);
    static final Color INFORMACION_FONDO = new Color(0xE6EEF6);

    /** Curva de Bézier que aproxima un cuarto de círculo. */
    private static final float CURVA = 0.5523f;

    private static final byte[] ROBOTO_NORMAL = fuente("Roboto-Regular.ttf");
    private static final byte[] ROBOTO_MEDIA = fuente("Roboto-Medium.ttf");
    private static final byte[] ROBOTO_NEGRITA = fuente("Roboto-Bold.ttf");

    private final PDDocument documento = new PDDocument();
    private final PDFont normal;
    private final PDFont media;
    private final PDFont negrita;
    private final String titulo;
    private final Map<PDFont, Map<Integer, Boolean>> escribibles = new HashMap<>();
    private PDPageContentStream trazo;
    private float y;

    /**
     * Crea el documento con su primera hoja.
     *
     * @param titulo el título de la barra de cada hoja, como «Reporte del lote L-0143»
     * @param asunto el asunto del PDF, en sus propiedades
     */
    HojaDelReporte(String titulo, String asunto) {
        this.titulo = titulo;
        try {
            normal = PDType0Font.load(documento, new ByteArrayInputStream(ROBOTO_NORMAL));
            media = PDType0Font.load(documento, new ByteArrayInputStream(ROBOTO_MEDIA));
            negrita = PDType0Font.load(documento, new ByteArrayInputStream(ROBOTO_NEGRITA));
            PDDocumentInformation informacion = documento.getDocumentInformation();
            informacion.setTitle(titulo);
            informacion.setSubject(asunto);
            informacion.setAuthor("InspectIA");
            informacion.setCreator("InspectIA");
            nuevaHoja();
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }

    /**
     * Pone el idioma del documento, para los lectores de pantalla.
     *
     * @param idioma la etiqueta del idioma, como {@code es-CO}
     */
    void idioma(String idioma) {
        documento.getDocumentCatalog().setLanguage(idioma);
    }

    PDFont normal() {
        return normal;
    }

    PDFont media() {
        return media;
    }

    PDFont negrita() {
        return negrita;
    }

    /** El ancho para escribir, entre los márgenes. */
    float ancho() {
        return TAMANO.getWidth() - 2 * MARGEN;
    }

    /** La altura por la que va la escritura, desde abajo de la hoja. */
    float y() {
        return y;
    }

    /**
     * Baja la escritura.
     *
     * @param puntos cuánto
     */
    void bajar(float puntos) {
        y -= puntos;
    }

    /**
     * Pasa a una hoja nueva si lo que sigue no cabe en esta.
     *
     * @param alto el alto de lo que sigue
     */
    void asegurar(float alto) {
        if (y - alto < ALTO_DEL_PIE) {
            nuevaHoja();
        }
    }

    /** Termina la hoja actual y empieza otra, con su barra. */
    void nuevaHoja() {
        try {
            if (trazo != null) {
                trazo.close();
            }
            PDPage hoja = new PDPage(TAMANO);
            documento.addPage(hoja);
            trazo = new PDPageContentStream(documento, hoja);
            float arriba = TAMANO.getHeight();
            rectangulo(0, arriba - ALTO_DE_LA_BARRA, TAMANO.getWidth(), ALTO_DE_LA_BARRA, BARRA, null, 0);
            texto("InspectIA", negrita, 16, Color.WHITE, MARGEN, arriba - 28);
            float anchoDelTitulo = anchoDe(titulo, normal, 10);
            texto(titulo, normal, 10, SELECCION, TAMANO.getWidth() - MARGEN - anchoDelTitulo, arriba - 27);
            y = arriba - ALTO_DE_LA_BARRA - 30;
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }

    /**
     * Escribe un texto en una línea.
     *
     * @param texto el texto
     * @param fuente la fuente
     * @param tamano su tamaño, en puntos
     * @param color su color
     * @param x dónde empieza
     * @param linea la altura de su línea de base
     */
    void texto(String texto, PDFont fuente, float tamano, Color color, float x, float linea) {
        try {
            trazo.beginText();
            trazo.setFont(fuente, tamano);
            trazo.setNonStrokingColor(color);
            trazo.newLineAtOffset(x, linea);
            trazo.showText(escribible(texto, fuente));
            trazo.endText();
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }

    /**
     * El ancho de un texto.
     *
     * @param texto el texto
     * @param fuente la fuente
     * @param tamano su tamaño
     * @return el ancho, en puntos
     */
    float anchoDe(String texto, PDFont fuente, float tamano) {
        try {
            return fuente.getStringWidth(escribible(texto, fuente)) / 1000 * tamano;
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }

    /**
     * Parte un texto en las líneas que caben en un ancho, por los espacios; una palabra más larga que el ancho se
     * parte donde llegue. El espacio que no se parte (U+00A0) mantiene juntos un número y su signo.
     *
     * @param texto el texto
     * @param fuente la fuente
     * @param tamano su tamaño
     * @param ancho el ancho disponible
     * @return las líneas
     */
    List<String> lineas(String texto, PDFont fuente, float tamano, float ancho) {
        List<String> lineas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabra : limpio(texto).split(" ")) {
            if (palabra.isEmpty()) {
                continue;
            }
            String conPalabra = actual.isEmpty() ? palabra : actual + " " + palabra;
            if (anchoDe(conPalabra, fuente, tamano) <= ancho) {
                actual.setLength(0);
                actual.append(conPalabra);
                continue;
            }
            if (!actual.isEmpty()) {
                lineas.add(actual.toString());
                actual.setLength(0);
            }
            String resto = palabra;
            while (anchoDe(resto, fuente, tamano) > ancho && resto.length() > 1) {
                int corte = resto.length() - 1;
                while (corte > 1 && anchoDe(resto.substring(0, corte), fuente, tamano) > ancho) {
                    corte--;
                }
                lineas.add(resto.substring(0, corte));
                resto = resto.substring(corte);
            }
            actual.append(resto);
        }
        if (!actual.isEmpty()) {
            lineas.add(actual.toString());
        }
        return lineas;
    }

    /**
     * El alto de un párrafo partido en líneas.
     *
     * @param texto el texto
     * @param fuente la fuente
     * @param tamano su tamaño
     * @param ancho el ancho disponible
     * @return su alto, con el interlineado de 1,4
     */
    float altoDe(String texto, PDFont fuente, float tamano, float ancho) {
        return lineas(texto, fuente, tamano, ancho).size() * tamano * 1.4f;
    }

    /**
     * Escribe un párrafo partido en líneas, desde una altura hacia abajo.
     *
     * @param texto el texto
     * @param fuente la fuente
     * @param tamano su tamaño
     * @param color su color
     * @param x dónde empieza cada línea
     * @param arriba la altura de arriba del párrafo
     * @param ancho el ancho disponible
     * @return el alto que ocupó
     */
    float parrafo(String texto, PDFont fuente, float tamano, Color color, float x, float arriba, float ancho) {
        float interlineado = tamano * 1.4f;
        float linea = arriba - tamano;
        List<String> lineas = lineas(texto, fuente, tamano, ancho);
        for (String cada : lineas) {
            texto(cada, fuente, tamano, color, x, linea);
            linea -= interlineado;
        }
        return lineas.size() * interlineado;
    }

    /**
     * Escribe un párrafo a todo el ancho donde va la escritura, y baja.
     *
     * @param texto el texto
     * @param fuente la fuente
     * @param tamano su tamaño
     * @param color su color
     */
    void parrafo(String texto, PDFont fuente, float tamano, Color color) {
        float alto = altoDe(texto, fuente, tamano, ancho());
        asegurar(alto);
        bajar(parrafo(texto, fuente, tamano, color, MARGEN, y, ancho()));
    }

    /**
     * Dibuja un rectángulo, relleno, con borde o las dos cosas, con las esquinas redondeadas si se pide.
     *
     * @param x su izquierda
     * @param abajo su borde de abajo
     * @param ancho su ancho
     * @param alto su alto
     * @param relleno el color del relleno, o {@code null}
     * @param borde el color del borde, o {@code null}
     * @param radio el radio de las esquinas; 0 para esquinas rectas
     */
    void rectangulo(float x, float abajo, float ancho, float alto, Color relleno, Color borde, float radio) {
        try {
            if (relleno != null) {
                trazo.setNonStrokingColor(relleno);
                camino(x, abajo, ancho, alto, radio);
                trazo.fill();
            }
            if (borde != null) {
                trazo.setStrokingColor(borde);
                trazo.setLineWidth(1);
                camino(x, abajo, ancho, alto, radio);
                trazo.stroke();
            }
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }

    /**
     * Dibuja el borde de un rectángulo con un grosor.
     *
     * @param x su izquierda
     * @param abajo su borde de abajo
     * @param ancho su ancho
     * @param alto su alto
     * @param color el color
     * @param grosor el grosor de la línea
     */
    void marco(float x, float abajo, float ancho, float alto, Color color, float grosor) {
        try {
            trazo.setStrokingColor(color);
            trazo.setLineWidth(grosor);
            trazo.addRect(x, abajo, ancho, alto);
            trazo.stroke();
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }

    /**
     * Dibuja una línea con varios tramos.
     *
     * @param color el color
     * @param grosor el grosor
     * @param puntos las coordenadas, de a pares: x1, y1, x2, y2…
     */
    void linea(Color color, float grosor, float... puntos) {
        try {
            trazo.setStrokingColor(color);
            trazo.setLineWidth(grosor);
            trazo.moveTo(puntos[0], puntos[1]);
            for (int i = 2; i < puntos.length; i += 2) {
                trazo.lineTo(puntos[i], puntos[i + 1]);
            }
            trazo.stroke();
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }

    /**
     * Dibuja un círculo relleno.
     *
     * @param x su centro
     * @param centro la altura de su centro
     * @param radio su radio
     * @param color su color
     */
    void circulo(float x, float centro, float radio, Color color) {
        rectangulo(x - radio, centro - radio, 2 * radio, 2 * radio, color, null, radio);
    }

    /**
     * Pone una imagen.
     *
     * @param imagen la imagen, en RGB
     * @param x su izquierda
     * @param abajo su borde de abajo
     * @param ancho su ancho
     * @param alto su alto
     */
    void imagen(BufferedImage imagen, float x, float abajo, float ancho, float alto) {
        try {
            PDImageXObject objeto = JPEGFactory.createFromImage(documento, imagen, 0.85f);
            trazo.drawImage(objeto, x, abajo, ancho, alto);
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }

    /**
     * Termina el documento: escribe en cada hoja su pie, con el texto dado y «Página n de m».
     *
     * @param pie el texto del pie, como «InspectIA · Reporte generado el …»
     * @param pagina el texto del número de página, con {@code {0}} y {@code {1}} para la página y el total
     * @return los bytes del PDF
     */
    byte[] terminar(String pie, String pagina) {
        try {
            trazo.close();
            trazo = null;
            int total = documento.getNumberOfPages();
            for (int i = 0; i < total; i++) {
                try (PDPageContentStream alPie = new PDPageContentStream(documento, documento.getPage(i),
                        PDPageContentStream.AppendMode.APPEND, true)) {
                    trazo = alPie;
                    linea(BORDE, 0.75f, MARGEN, 40, TAMANO.getWidth() - MARGEN, 40);
                    texto(pie, normal, 8, SECUNDARIO, MARGEN, 28);
                    String numero = pagina.replace("{0}", String.valueOf(i + 1)).replace("{1}", String.valueOf(total));
                    texto(numero, normal, 8, SECUNDARIO, TAMANO.getWidth() - MARGEN - anchoDe(numero, normal, 8), 28);
                }
            }
            trazo = null;
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }

    @Override
    public void close() {
        try {
            if (trazo != null) {
                trazo.close();
            }
            documento.close();
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }

    /** El texto en una sola línea: los saltos, los tabuladores y los demás caracteres de control van como espacios. */
    private static String limpio(String texto) {
        return texto == null ? "" : texto.replaceAll("\\p{Cntrl}", " ");
    }

    /** El texto con «?» en lugar de los caracteres que la fuente no tiene. */
    private String escribible(String texto, PDFont fuente) {
        Map<Integer, Boolean> conocidos = escribibles.computeIfAbsent(fuente, cual -> new HashMap<>());
        StringBuilder resultado = new StringBuilder();
        limpio(texto).codePoints().forEach(caracter -> {
            boolean puede = conocidos.computeIfAbsent(caracter, cual -> {
                try {
                    fuente.encode(new String(Character.toChars(cual)));
                    return true;
                } catch (IOException | IllegalArgumentException excepcion) {
                    return false;
                }
            });
            if (puede) {
                resultado.appendCodePoint(caracter);
            } else {
                resultado.append('?');
            }
        });
        return resultado.toString();
    }

    /** El camino de un rectángulo, con las esquinas redondeadas si el radio es mayor que 0. */
    private void camino(float x, float abajo, float ancho, float alto, float radio) throws IOException {
        float r = Math.min(radio, Math.min(ancho, alto) / 2);
        if (r <= 0) {
            trazo.addRect(x, abajo, ancho, alto);
            return;
        }
        float k = r * CURVA;
        float derecha = x + ancho;
        float arriba = abajo + alto;
        trazo.moveTo(x + r, abajo);
        trazo.lineTo(derecha - r, abajo);
        trazo.curveTo(derecha - r + k, abajo, derecha, abajo + r - k, derecha, abajo + r);
        trazo.lineTo(derecha, arriba - r);
        trazo.curveTo(derecha, arriba - r + k, derecha - r + k, arriba, derecha - r, arriba);
        trazo.lineTo(x + r, arriba);
        trazo.curveTo(x + r - k, arriba, x, arriba - r + k, x, arriba - r);
        trazo.lineTo(x, abajo + r);
        trazo.curveTo(x, abajo + r - k, x + r - k, abajo, x + r, abajo);
        trazo.closePath();
    }

    private static byte[] fuente(String archivo) {
        try (InputStream entrada = HojaDelReporte.class.getResourceAsStream("/reporte/fuentes/" + archivo)) {
            if (entrada == null) {
                throw new IllegalStateException("Falta la fuente " + archivo + " en reporte/fuentes");
            }
            return entrada.readAllBytes();
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }
}
