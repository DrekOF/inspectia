/*
 * Nombre: ClasificadorFalsoTest.java
 * Descripción: Pruebas unitarias del clasificador falso: los conteos según el ancho y el alto de cada foto, los granos
 *              dentro de la imagen, una foto que no es imagen y la demora (RF-07, RF-08, RF-11).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.falso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.usco.inspectia.analisis.aplicacion.Clasificacion;
import co.edu.usco.inspectia.analisis.aplicacion.FotoParaClasificar;
import co.edu.usco.inspectia.analisis.aplicacion.GranoClasificado;
import co.edu.usco.inspectia.analisis.dominio.ClaseDeGrano;
import co.edu.usco.inspectia.analisis.dominio.FormatoDeFoto;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** El clasificador falso, determinista según cada foto (RF-08). Las imágenes se hacen en la prueba (RNF-08). */
class ClasificadorFalsoTest {

    private final ClasificadorFalso clasificador = new ClasificadorFalso(Duration.ZERO);

    /** Una imagen de ese tamaño, en ese formato ({@code jpg} o {@code png}). */
    static byte[] imagen(String formato, int ancho, int alto) {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            assertThat(ImageIO.write(imagen, formato, salida)).isTrue();
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
        return salida.toByteArray();
    }

    private static FotoParaClasificar foto(int numero, int ancho, int alto) {
        return new FotoParaClasificar(numero, FormatoDeFoto.PNG, imagen("png", ancho, alto));
    }

    private static long contar(List<GranoClasificado> granos, ClaseDeGrano clase) {
        return granos.stream().filter(grano -> grano.clase() == clase).count();
    }

    @Test
    @DisplayName("RF-08 · dos fotos de 106 × 100 y 107 × 100 dan 50 granos cada una, 6 + 7 = 13 no sanos y ningún dudoso: 87 de 100")
    void conteosPorElAncho() {
        Clasificacion clasificacion = clasificador.clasificar(List.of(foto(1, 106, 100), foto(2, 107, 100)));

        assertThat(clasificacion.modelo()).isEqualTo(ClasificadorFalso.MODELO);
        assertThat(clasificacion.granos()).hasSize(100);
        assertThat(clasificacion.granos().subList(0, 50)).allMatch(grano -> grano.numeroDeFoto() == 1);
        assertThat(clasificacion.granos().subList(50, 100)).allMatch(grano -> grano.numeroDeFoto() == 2);
        assertThat(contar(clasificacion.granos().subList(0, 50), ClaseDeGrano.NO_SANO)).isEqualTo(6);
        assertThat(contar(clasificacion.granos().subList(50, 100), ClaseDeGrano.NO_SANO)).isEqualTo(7);
        assertThat(contar(clasificacion.granos(), ClaseDeGrano.SANO)).isEqualTo(87);
        assertThat(clasificacion.granos()).extracting(GranoClasificado::confianza)
                .containsOnly(new BigDecimal("90.00"));
    }

    @Test
    @DisplayName("RF-08 · los no sanos son los primeros en orden de lectura, y los dudosos, con confianza 55,00, los primeros según el alto: 96 × 72 da 46 no sanos y 22 dudosos")
    void noSanosYDudososPorElAlto() {
        List<GranoClasificado> granos = clasificador.clasificar(List.of(foto(1, 96, 72))).granos();

        assertThat(granos.subList(0, 46)).allMatch(grano -> grano.clase() == ClaseDeGrano.NO_SANO);
        assertThat(granos.subList(46, 50)).allMatch(grano -> grano.clase() == ClaseDeGrano.SANO);
        assertThat(granos.subList(0, 22)).extracting(GranoClasificado::confianza)
                .containsOnly(new BigDecimal("55.00"));
        assertThat(granos.subList(22, 50)).extracting(GranoClasificado::confianza)
                .containsOnly(new BigDecimal("90.00"));
    }

    @Test
    @DisplayName("RF-08 · con 100 × 100 todos los granos son sanos y ninguno es dudoso: 100 de 100")
    void todosSanos() {
        List<GranoClasificado> granos = clasificador.clasificar(List.of(foto(1, 100, 100), foto(2, 100, 100)))
                .granos();

        assertThat(contar(granos, ClaseDeGrano.SANO)).isEqualTo(100);
    }

    @Test
    @DisplayName("RF-11 · cada grano queda en el centro de su celda de una cuadrícula de 10 × 5, del 60 % de la celda y dentro de la foto")
    void granosEnLaCuadricula() {
        List<GranoClasificado> granos = ClasificadorFalso.granos(1, 1000, 500);

        assertThat(granos).hasSize(50);
        assertThat(granos.get(0)).isEqualTo(new GranoClasificado(1, 20, 20, 60, 60, ClaseDeGrano.SANO,
                new BigDecimal("90.00")));
        assertThat(granos.get(11)).isEqualTo(new GranoClasificado(1, 120, 120, 60, 60, ClaseDeGrano.SANO,
                new BigDecimal("90.00")));
        assertThat(granos.get(49)).isEqualTo(new GranoClasificado(1, 920, 420, 60, 60, ClaseDeGrano.SANO,
                new BigDecimal("90.00")));
    }

    @Test
    @DisplayName("RF-11 · en fotos de cualquier tamaño, hasta de 1 × 1, cada grano mide al menos 1 × 1 y queda dentro de la imagen (prueba de límite)")
    void siempreDentroDeLaFoto() {
        int[][] medidas = {{1, 1}, {2, 2}, {9, 4}, {10, 5}, {11, 6}, {106, 100}, {96, 72}, {4032, 3024}};
        for (int[] medida : medidas) {
            int ancho = medida[0];
            int alto = medida[1];
            assertThat(ClasificadorFalso.granos(1, ancho, alto)).as("%d × %d", ancho, alto).hasSize(50)
                    .allSatisfy(grano -> {
                        assertThat(grano.ancho()).isPositive();
                        assertThat(grano.alto()).isPositive();
                        assertThat(grano.x()).isNotNegative();
                        assertThat(grano.y()).isNotNegative();
                        assertThat(grano.x() + grano.ancho()).isLessThanOrEqualTo(ancho);
                        assertThat(grano.y() + grano.alto()).isLessThanOrEqualTo(alto);
                    });
        }
    }

    @Test
    @DisplayName("RF-08 · lee el ancho y el alto de la cabecera de un JPG y de un PNG")
    void leeLasMedidas() {
        assertThat(ClasificadorFalso.medidas(imagen("jpg", 107, 72))).containsExactly(107, 72);
        assertThat(ClasificadorFalso.medidas(imagen("png", 3, 200))).containsExactly(3, 200);
    }

    @Test
    @DisplayName("RF-08 · un archivo que no es una imagen no se clasifica: la muestra vuelve a pendiente")
    void noEsUnaImagen() {
        FotoParaClasificar pdf = new FotoParaClasificar(1, FormatoDeFoto.JPG,
                "%PDF-1.7".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> clasificador.clasificar(List.of(pdf))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RNF-01 · con una demora, el clasificador falso espera antes de responder, para ver el indicador de avance")
    void demora() {
        ClasificadorFalso lento = new ClasificadorFalso(Duration.ofMillis(200));

        long antes = System.nanoTime();
        lento.clasificar(List.of(foto(1, 100, 100)));

        assertThat(Duration.ofNanos(System.nanoTime() - antes)).isGreaterThanOrEqualTo(Duration.ofMillis(200));
    }
}
