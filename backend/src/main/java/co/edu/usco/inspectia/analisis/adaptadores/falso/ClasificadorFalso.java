/*
 * Nombre: ClasificadorFalso.java
 * Descripción: El clasificador de granos falso de desarrollo y de las pruebas: devuelve 50 granos por foto, en una
 *              cuadrícula dentro de la imagen, con tantos no sanos y dudosos como digan el ancho y el alto de la foto
 *              (RF-07, RF-08, RF-11).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.falso;

import co.edu.usco.inspectia.analisis.aplicacion.Clasificacion;
import co.edu.usco.inspectia.analisis.aplicacion.ClasificadorDeGranos;
import co.edu.usco.inspectia.analisis.aplicacion.FotoParaClasificar;
import co.edu.usco.inspectia.analisis.aplicacion.GranoClasificado;
import co.edu.usco.inspectia.analisis.aplicacion.ModeloUsado;
import co.edu.usco.inspectia.analisis.dominio.ClaseDeGrano;
import co.edu.usco.inspectia.analisis.dominio.RecallsDelModelo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

/**
 * El clasificador falso ({@code inspectia.ia.clasificador=falso}), mientras el servicio de IA no use el modelo de Julián
 * (PR 4). Es determinista según cada foto, para que las pruebas armen cada caso con la imagen que suben:
 *
 * <ul>
 *   <li>Lee el ancho y el alto de la cabecera de la imagen, sin decodificarla ({@link ImageReader}).</li>
 *   <li>Devuelve {@value #GRANOS_POR_FOTO} granos por foto, en una cuadrícula de {@value #COLUMNAS} × {@value #FILAS}
 *       dentro de la imagen: cada grano en el centro de su celda, del 60 % de la celda, de al menos 1 × 1 px y siempre
 *       dentro de la imagen.</li>
 *   <li>Son no sanos los primeros {@code ancho mod 50} granos, en orden de lectura (por filas, de izquierda a derecha),
 *       y dudosos (confianza {@value #CONFIANZA_DUDOSA}) los primeros {@code alto mod 50}; los demás tienen confianza
 *       {@value #CONFIANZA_ALTA}.</li>
 * </ul>
 *
 * <p>Por ejemplo, dos fotos de 106 × 100 y 107 × 100 dan 6 + 7 = 13 no sanos de 100: 87 de 100, 90,6 % Estándar con
 * sugerencia. Responde como el modelo {@code clasificador-falso} {@code 1.0}, que se registra al arrancar con los
 * recall de EfficientNet-B0, así las cifras coinciden con los ejemplos de las fichas.
 */
public class ClasificadorFalso implements ClasificadorDeGranos {

    /** El modelo con que responde. */
    public static final ModeloUsado MODELO = new ModeloUsado("clasificador-falso", "1.0");

    /** Sus recall: los de EfficientNet-B0 (D-39). */
    public static final RecallsDelModelo RECALLS = new RecallsDelModelo(new BigDecimal("91.20"),
            new BigDecimal("53.40"));

    /** Columnas de la cuadrícula. */
    static final int COLUMNAS = 10;

    /** Filas de la cuadrícula. */
    static final int FILAS = 5;

    /** Granos por foto. */
    static final int GRANOS_POR_FOTO = COLUMNAS * FILAS;

    /** Confianza de un grano dudoso: menos que el umbral provisional de 60 (P-15). */
    static final String CONFIANZA_DUDOSA = "55.00";

    /** Confianza de los demás granos. */
    static final String CONFIANZA_ALTA = "90.00";

    private static final BigDecimal DUDOSA = new BigDecimal(CONFIANZA_DUDOSA);
    private static final BigDecimal ALTA = new BigDecimal(CONFIANZA_ALTA);

    private final Duration demora;

    /**
     * Crea el clasificador.
     *
     * @param demora cuánto espera antes de responder, para ver el indicador de avance en desarrollo (RNF-01 C4)
     */
    public ClasificadorFalso(Duration demora) {
        this.demora = Objects.requireNonNull(demora, "demora");
    }

    @Override
    public Clasificacion clasificar(List<FotoParaClasificar> fotos) {
        esperar();
        List<GranoClasificado> granos = new ArrayList<>();
        for (FotoParaClasificar foto : fotos) {
            int[] medidas = medidas(foto.contenido());
            granos.addAll(granos(foto.numero(), medidas[0], medidas[1]));
        }
        return new Clasificacion(MODELO, granos);
    }

    /**
     * Los 50 granos de una foto de ese ancho y ese alto, en orden de lectura.
     *
     * @param numeroDeFoto el número de la foto
     * @param ancho su ancho, en píxeles, al menos 1
     * @param alto su alto, en píxeles, al menos 1
     * @return los granos
     */
    static List<GranoClasificado> granos(int numeroDeFoto, int ancho, int alto) {
        int noSanos = ancho % GRANOS_POR_FOTO;
        int dudosos = alto % GRANOS_POR_FOTO;
        List<GranoClasificado> granos = new ArrayList<>(GRANOS_POR_FOTO);
        for (int fila = 0; fila < FILAS; fila++) {
            for (int columna = 0; columna < COLUMNAS; columna++) {
                int orden = fila * COLUMNAS + columna;
                int[] x = enLaCelda(columna, COLUMNAS, ancho);
                int[] y = enLaCelda(fila, FILAS, alto);
                granos.add(new GranoClasificado(numeroDeFoto, x[0], y[0], x[1], y[1],
                        orden < noSanos ? ClaseDeGrano.NO_SANO : ClaseDeGrano.SANO,
                        orden < dudosos ? DUDOSA : ALTA));
            }
        }
        return granos;
    }

    /**
     * La posición y el tamaño de un grano en una dirección: el 60 % de su celda, centrado, de al menos 1 px y sin
     * salir de la imagen.
     *
     * @return la posición y el tamaño
     */
    private static int[] enLaCelda(int celda, int celdas, int medida) {
        int desde = celda * medida / celdas;
        int hasta = (celda + 1) * medida / celdas;
        int tamano = Math.min(medida, Math.max(1, (hasta - desde) * 3 / 5));
        int posicion = Math.min(desde + (hasta - desde - tamano) / 2, medida - tamano);
        return new int[] {Math.max(0, posicion), tamano};
    }

    /**
     * El ancho y el alto de una imagen, leídos de su cabecera sin decodificarla: una foto de 10 MB decodificada ocupa
     * mucha memoria.
     *
     * @param contenido los bytes de la imagen
     * @return el ancho y el alto
     * @throws IllegalArgumentException si no es una imagen que ImageIO sepa leer
     */
    static int[] medidas(byte[] contenido) {
        try (ImageInputStream entrada = new MemoryCacheImageInputStream(new ByteArrayInputStream(contenido))) {
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) {
                throw new IllegalArgumentException("La foto no es una imagen que se pueda leer");
            }
            ImageReader lector = lectores.next();
            try {
                lector.setInput(entrada, true, true);
                return new int[] {lector.getWidth(0), lector.getHeight(0)};
            } finally {
                lector.dispose();
            }
        } catch (IOException excepcion) {
            throw new UncheckedIOException("No se pudieron leer las medidas de la foto", excepcion);
        }
    }

    private void esperar() {
        if (demora.isZero() || demora.isNegative()) {
            return;
        }
        try {
            Thread.sleep(demora);
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpió la espera del clasificador falso", excepcion);
        }
    }
}
