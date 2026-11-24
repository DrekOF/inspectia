/*
 * Nombre: ContenidosDePrueba.java
 * Descripción: Contenidos del reporte PDF para las pruebas: el lote L-0143 de Cacaos del Huila con una muestra (87 de
 *              100, 90,6 %, Estándar, con sugerencia por el límite de 90) o con dos (174 de 200), y fotos sintéticas
 *              con sus granos (RF-15).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.pdf;

import co.edu.usco.inspectia.analisis.aplicacion.ContenidoDelReporte;
import co.edu.usco.inspectia.analisis.aplicacion.ContenidoDelReporte.FotoDelReporte;
import co.edu.usco.inspectia.analisis.aplicacion.ContenidoDelReporte.MuestraDelReporte;
import co.edu.usco.inspectia.analisis.aplicacion.GranoMarcado;
import co.edu.usco.inspectia.analisis.aplicacion.MuestraGuardada;
import co.edu.usco.inspectia.analisis.aplicacion.ResultadoGuardado;
import co.edu.usco.inspectia.analisis.dominio.ClaseDeGrano;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeLote;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeMuestra;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;

/** Los contenidos de ejemplo del reporte. */
final class ContenidosDePrueba {

    static final Instant FIN_1 = Instant.parse("2026-11-23T15:00:03Z");
    static final Instant FIN_2 = Instant.parse("2026-11-23T16:30:00Z");
    static final Instant GENERADO = Instant.parse("2026-11-24T20:05:00Z");
    static final String ESTANDAR =
            "Apto para venta estándar. Retirar a mano los granos defectuosos podría subirlo de categoría.";

    private ContenidosDePrueba() {
    }

    /** El lote con su resultado guardado. */
    static ResultadoGuardado lote(String porcentaje, String margen, String categoria, String recomendacion,
            boolean sugerir, String limite, boolean aviso, Instant fin) {
        return new ResultadoGuardado(UUID.randomUUID(), "L-0143", LocalDate.of(2026, 10, 20), "La Esperanza",
                EstadoDeLote.ANALIZADO, OffsetDateTime.ofInstant(fin, ZoneOffset.UTC), new BigDecimal(porcentaje),
                new BigDecimal(margen), categoria, recomendacion, sugerir, limite == null ? null : new BigDecimal(limite),
                limite == null ? null : "ESTANDAR", aviso);
    }

    /** Una muestra terminada con sus fotos. */
    static MuestraDelReporte muestra(int numero, String analista, String modelo, int total, int sanos,
            String contado, Instant fin, List<FotoDelReporte> fotos) {
        MuestraGuardada guardada = new MuestraGuardada(UUID.randomUUID(), numero, EstadoDeMuestra.TERMINADA,
                UUID.randomUUID(), analista, modelo, "1.0", total, sanos, new BigDecimal("60.00"),
                new BigDecimal("10.0"), fin.minusSeconds(10), fin);
        return new MuestraDelReporte(guardada, new BigDecimal(contado), fotos);
    }

    /** El lote L-0143 con una muestra: 87 de 100, 90,6 %, Estándar, sugerencia por el límite de 90. */
    static ContenidoDelReporte unaMuestra() {
        return new ContenidoDelReporte("Cacaos del Huila", "900123456-8",
                lote("90.6", "7.5", "ESTANDAR", ESTANDAR, true, "90.0", false, FIN_1), 100, 87, new BigDecimal("87.0"),
                List.of(muestra(1, "Luis Gómez", "EfficientNet-B0", 100, 87, "87.0", FIN_1,
                        List.of(foto(1, 50, 6, 2), foto(2, 50, 7, 1)))), GENERADO);
    }

    /** El lote con dos muestras: 87 de 100 y 87 de 100, 174 de 200, sin sugerencia (D-25). */
    static ContenidoDelReporte dosMuestras() {
        return new ContenidoDelReporte("Cacaos del Huila", "900123456-8",
                lote("90.6", "5.3", "ESTANDAR", ESTANDAR, false, null, false, FIN_2), 200, 174,
                new BigDecimal("87.0"),
                List.of(muestra(1, "Luis Gómez", "EfficientNet-B0", 100, 87, "87.0", FIN_1,
                        List.of(foto(1, 50, 6, 0), foto(2, 50, 7, 0))),
                        muestra(2, "Pedro Díaz", "ResNet-50", 100, 87, "87.0", FIN_2,
                                List.of(foto(1, 34, 4, 1), foto(2, 33, 5, 0), foto(3, 33, 4, 0)))),
                GENERADO);
    }

    /**
     * Una foto sintética de 1060 × 800 con sus granos en una cuadrícula: los primeros no sanos y, entre los sanos, los
     * dudosos pedidos.
     */
    static FotoDelReporte foto(int numero, int granos, int noSanos, int dudosos) {
        int ancho = 1060;
        int alto = 800;
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D lapiz = imagen.createGraphics();
        lapiz.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        lapiz.setColor(new Color(0xF4F1EA));
        lapiz.fillRect(0, 0, ancho, alto);
        List<GranoMarcado> marcados = new ArrayList<>();
        int columnas = 10;
        for (int i = 0; i < granos; i++) {
            int x = 30 + (i % columnas) * 102;
            int y = 30 + (i / columnas) * 150;
            boolean sano = i >= noSanos;
            lapiz.setColor(sano ? new Color(0x7A4A2A) : new Color(0x3B2A20));
            lapiz.fillOval(x + 6, y + 10, 70, 110);
            marcados.add(new GranoMarcado(x, y, 82, 130, sano ? ClaseDeGrano.SANO : ClaseDeGrano.NO_SANO,
                    sano && i < noSanos + dudosos));
        }
        lapiz.dispose();
        return new FotoDelReporte(numero, png(imagen), marcados);
    }

    /** Una imagen en PNG. */
    static byte[] png(BufferedImage imagen) {
        try {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            ImageIO.write(imagen, "png", salida);
            return salida.toByteArray();
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }
}
