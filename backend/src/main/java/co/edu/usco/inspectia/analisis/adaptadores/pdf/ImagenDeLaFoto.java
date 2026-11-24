/*
 * Nombre: ImagenDeLaFoto.java
 * Descripción: Prepara una foto de la muestra para el reporte PDF: la lee reducida, la gira según su EXIF (D-45) y la
 *              deja en RGB sobre blanco, con las medidas de la foto original ya girada para ubicar los granos (RF-15).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.pdf;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Una foto lista para el reporte (RF-15). Se lee reducida, saltando píxeles, para no cargar en memoria una foto de
 * 4000 × 3000 entera: el reporte la muestra a unos 250 puntos de ancho. Después se gira según su orientación EXIF,
 * como la ve la página, porque las posiciones de los granos son de la foto ya girada (D-45).
 *
 * @param imagen la foto reducida y girada, en RGB
 * @param anchoOriginal el ancho de la foto original ya girada, en píxeles: la escala de las posiciones de los granos
 * @param altoOriginal su alto
 */
record ImagenDeLaFoto(BufferedImage imagen, int anchoOriginal, int altoOriginal) {

    /** El lado más largo, en píxeles, desde el que se reduce la foto: a 250 puntos de ancho da unos 290 ppp. */
    static final int LADO_MAXIMO = 1000;

    /**
     * Lee una foto JPG o PNG.
     *
     * @param foto los bytes del archivo
     * @return la foto, o vacío si no es una imagen que Java sepa leer
     */
    static Optional<ImagenDeLaFoto> leer(byte[] foto) {
        if (foto == null || foto.length == 0) {
            return Optional.empty();
        }
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(foto))) {
            Iterator<ImageReader> lectores = entrada == null ? null : ImageIO.getImageReaders(entrada);
            if (lectores == null || !lectores.hasNext()) {
                return Optional.empty();
            }
            ImageReader lector = lectores.next();
            try {
                lector.setInput(entrada, true, true);
                int ancho = lector.getWidth(0);
                int alto = lector.getHeight(0);
                int paso = Math.max(1, Math.max(ancho, alto) / LADO_MAXIMO);
                ImageReadParam parametros = lector.getDefaultReadParam();
                parametros.setSourceSubsampling(paso, paso, 0, 0);
                BufferedImage leida = lector.read(0, parametros);
                int orientacion = OrientacionExif.de(foto);
                boolean acostada = orientacion >= 5;
                return Optional.of(new ImagenDeLaFoto(girada(enRgb(leida), orientacion), acostada ? alto : ancho,
                        acostada ? ancho : alto));
            } finally {
                lector.dispose();
            }
        } catch (IOException | RuntimeException excepcion) {
            return Optional.empty();
        }
    }

    /** La imagen en RGB, con las partes transparentes de un PNG sobre blanco. */
    private static BufferedImage enRgb(BufferedImage imagen) {
        BufferedImage rgb = new BufferedImage(imagen.getWidth(), imagen.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D lapiz = rgb.createGraphics();
        try {
            lapiz.setColor(Color.WHITE);
            lapiz.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            lapiz.drawImage(imagen, 0, 0, null);
        } finally {
            lapiz.dispose();
        }
        return rgb;
    }

    /**
     * La imagen girada según la orientación EXIF, de 1 a 8: 2 y 4 son espejos, 3 media vuelta, 6 un cuarto de vuelta a
     * la derecha, 8 a la izquierda, y 5 y 7 un cuarto de vuelta con espejo.
     *
     * @param imagen la imagen sin girar
     * @param orientacion la orientación
     * @return la imagen girada; con 1, la misma
     */
    static BufferedImage girada(BufferedImage imagen, int orientacion) {
        if (orientacion <= OrientacionExif.NORMAL || orientacion > 8) {
            return imagen;
        }
        int ancho = imagen.getWidth();
        int alto = imagen.getHeight();
        boolean acostada = orientacion >= 5;
        BufferedImage girada = new BufferedImage(acostada ? alto : ancho, acostada ? ancho : alto,
                BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                int destinoX;
                int destinoY;
                switch (orientacion) {
                    case 2 -> {
                        destinoX = ancho - 1 - x;
                        destinoY = y;
                    }
                    case 3 -> {
                        destinoX = ancho - 1 - x;
                        destinoY = alto - 1 - y;
                    }
                    case 4 -> {
                        destinoX = x;
                        destinoY = alto - 1 - y;
                    }
                    case 5 -> {
                        destinoX = y;
                        destinoY = x;
                    }
                    case 6 -> {
                        destinoX = alto - 1 - y;
                        destinoY = x;
                    }
                    case 7 -> {
                        destinoX = alto - 1 - y;
                        destinoY = ancho - 1 - x;
                    }
                    default -> {
                        destinoX = y;
                        destinoY = ancho - 1 - x;
                    }
                }
                girada.setRGB(destinoX, destinoY, imagen.getRGB(x, y));
            }
        }
        return girada;
    }
}
