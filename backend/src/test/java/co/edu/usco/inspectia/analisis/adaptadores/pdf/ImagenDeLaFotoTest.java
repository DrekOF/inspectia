/*
 * Nombre: ImagenDeLaFotoTest.java
 * Descripción: Pruebas unitarias de las fotos del reporte PDF: la orientación EXIF de un JPG, la foto girada como la ve
 *              la página, con las medidas de la original ya girada, y la reducción de una foto grande (RF-15, D-45).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Las fotos del reporte (D-45). */
class ImagenDeLaFotoTest {

    /** Un JPG de 40 × 20, rojo a la izquierda y azul a la derecha. */
    private static byte[] jpg() throws IOException {
        BufferedImage imagen = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D lapiz = imagen.createGraphics();
        lapiz.setColor(Color.RED);
        lapiz.fillRect(0, 0, 20, 20);
        lapiz.setColor(Color.BLUE);
        lapiz.fillRect(20, 0, 20, 20);
        lapiz.dispose();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "jpg", salida);
        return salida.toByteArray();
    }

    /** El JPG con un segmento APP1 «Exif» justo después del inicio, con la orientación dada. */
    private static byte[] conOrientacion(byte[] jpg, int orientacion, boolean intel) {
        byte[] tiff = intel
                ? new byte[] {'I', 'I', 42, 0, 8, 0, 0, 0, 1, 0, 0x12, 0x01, 3, 0, 1, 0, 0, 0, (byte) orientacion, 0,
                    0, 0, 0, 0, 0, 0}
                : new byte[] {'M', 'M', 0, 42, 0, 0, 0, 8, 0, 1, 0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, (byte) orientacion,
                    0, 0, 0, 0, 0, 0};
        int largo = 2 + 6 + tiff.length;
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        salida.write(jpg, 0, 2);
        salida.write(0xFF);
        salida.write(0xE1);
        salida.write(largo >> 8);
        salida.write(largo & 0xFF);
        salida.writeBytes(new byte[] {'E', 'x', 'i', 'f', 0, 0});
        salida.writeBytes(tiff);
        salida.write(jpg, 2, jpg.length - 2);
        return salida.toByteArray();
    }

    private static boolean rojo(int rgb) {
        Color color = new Color(rgb);
        return color.getRed() > 180 && color.getBlue() < 90;
    }

    @Test
    @DisplayName("RF-15 · la orientación EXIF de un JPG se lee en los dos órdenes de bytes; sin EXIF, o en un PNG, es 1 (D-45)")
    void orientacion() throws IOException {
        byte[] jpg = jpg();

        assertThat(OrientacionExif.de(conOrientacion(jpg, 6, true))).isEqualTo(6);
        assertThat(OrientacionExif.de(conOrientacion(jpg, 8, false))).isEqualTo(8);
        assertThat(OrientacionExif.de(jpg)).isEqualTo(1);
        assertThat(OrientacionExif.de(new byte[] {(byte) 0x89, 'P', 'N', 'G'})).isEqualTo(1);
        assertThat(OrientacionExif.de(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE1, 0x7F})).isEqualTo(1);
        assertThat(OrientacionExif.de(null)).isEqualTo(1);
    }

    @Test
    @DisplayName("RF-15 · una foto con orientación 6 se dibuja girada un cuarto de vuelta a la derecha, y sus medidas son las de la foto girada, en las que vienen los granos (D-45)")
    void giraSegunElExif() throws IOException {
        ImagenDeLaFoto foto = ImagenDeLaFoto.leer(conOrientacion(jpg(), 6, true)).orElseThrow();

        assertThat(foto.anchoOriginal()).isEqualTo(20);
        assertThat(foto.altoOriginal()).isEqualTo(40);
        assertThat(foto.imagen().getWidth()).isEqualTo(20);
        assertThat(rojo(foto.imagen().getRGB(10, 5))).as("la izquierda roja queda arriba").isTrue();
        assertThat(rojo(foto.imagen().getRGB(10, 35))).as("la derecha azul queda abajo").isFalse();
    }

    @Test
    @DisplayName("RF-15 · las 8 orientaciones EXIF dejan la foto con las medidas que le tocan")
    void ochoOrientaciones() {
        BufferedImage imagen = new BufferedImage(4, 2, BufferedImage.TYPE_INT_RGB);
        imagen.setRGB(0, 0, Color.RED.getRGB());
        int[][] esquinaRoja = {{0, 0}, {3, 0}, {3, 1}, {0, 1}, {0, 0}, {1, 0}, {1, 3}, {0, 3}};
        for (int orientacion = 1; orientacion <= 8; orientacion++) {
            BufferedImage girada = ImagenDeLaFoto.girada(imagen, orientacion);
            assertThat(girada.getWidth()).as("ancho con %d", orientacion).isEqualTo(orientacion >= 5 ? 2 : 4);
            int[] esquina = esquinaRoja[orientacion - 1];
            assertThat(girada.getRGB(esquina[0], esquina[1])).as("esquina con %d", orientacion)
                    .isEqualTo(Color.RED.getRGB());
        }
    }

    @Test
    @DisplayName("RF-15 · una foto grande se lee reducida, sin pasar de dos veces el lado máximo, y conserva sus medidas originales para ubicar los granos")
    void reduceLaFotoGrande() throws IOException {
        BufferedImage grande = new BufferedImage(4000, 3000, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(grande, "png", salida);

        ImagenDeLaFoto foto = ImagenDeLaFoto.leer(salida.toByteArray()).orElseThrow();

        assertThat(foto.anchoOriginal()).isEqualTo(4000);
        assertThat(foto.altoOriginal()).isEqualTo(3000);
        assertThat(foto.imagen().getWidth()).isEqualTo(1000);
        assertThat(ImagenDeLaFoto.leer(new byte[] {1, 2, 3})).isEmpty();
        assertThat(ImagenDeLaFoto.leer(null)).isEmpty();
    }
}
