/*
 * Nombre: AlmacenEnCarpeta.java
 * Descripción: El puerto AlmacenDeArchivos en una carpeta del servidor (D-41): enlaces firmados relativos a la página,
 *              llaves que no salen de la carpeta y que llevan la muestra para la que se pidieron (D-25), archivos que
 *              no se pisan, la lectura de una foto para clasificarla y los archivos que arma el servidor, como el
 *              reporte PDF (RF-15).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.3
 */
package co.edu.usco.inspectia.analisis.adaptadores.carpeta;

import co.edu.usco.inspectia.analisis.aplicacion.AlmacenDeArchivos;
import co.edu.usco.inspectia.analisis.aplicacion.ArchivoGuardado;
import co.edu.usco.inspectia.analisis.aplicacion.EnlaceTemporal;
import co.edu.usco.inspectia.analisis.aplicacion.LlavesDeArchivos;
import co.edu.usco.inspectia.analisis.dominio.FormatoDeFoto;
import co.edu.usco.inspectia.comun.errores.ConflictoExcepcion;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * El almacén de los archivos en una carpeta del servidor (D-41), que hace las veces de S3 mientras no haya despliegue
 * en AWS. Los enlaces son relativos a la página ({@value #RUTA}), así pasan por el mismo origen (D-31). Las llaves las
 * arma el caso de uso: {@code <empresa>/<lote>/m<muestra>-<uuid>.<jpg|png>}, con la muestra 1 o 2 para la que se
 * pidió el enlace (D-25); una llave con otra forma, como la de antes sin la muestra, o que saldría de la carpeta, no
 * se acepta. Un archivo ya subido no se pisa. Los reportes PDF (RF-15) van en la misma carpeta, con su llave
 * {@code reportes/<empresa>/<lote>/<uuid>-<es|en>.pdf} ({@link LlavesDeArchivos}); no tienen enlaces firmados, porque
 * se descargan por el backend con la sesión.
 */
public class AlmacenEnCarpeta implements AlmacenDeArchivos {

    /** Ruta de los enlaces firmados de los archivos. */
    public static final String RUTA = "/api/v1/archivos";

    private static final String UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    /**
     * La forma de una llave: dos UUID y el nombre del archivo, con la muestra (1 o 2), un UUID y su extensión, como
     * {@code m1-<uuid>.jpg}.
     */
    private static final Pattern LLAVE = Pattern.compile(
            "^" + UUID + "/" + UUID + "/m[12]-" + UUID + "\\.(jpg|png)$");

    private final Path raiz;
    private final FirmaDeEnlaces firma;
    private final Clock reloj;

    /**
     * Crea el almacén.
     *
     * @param raiz la carpeta de las fotos, como ruta completa
     * @param firma la firma de los enlaces
     * @param reloj el reloj de la aplicación, para los vencimientos
     */
    public AlmacenEnCarpeta(Path raiz, FirmaDeEnlaces firma, Clock reloj) {
        this.raiz = raiz.toAbsolutePath().normalize();
        this.firma = firma;
        this.reloj = reloj;
    }

    /**
     * La llave de un enlace, si tiene la forma que arma el backend.
     *
     * @param empresa la primera parte
     * @param lote la segunda parte
     * @param archivo el nombre del archivo
     * @return la llave, o vacío si no tiene esa forma
     */
    public static Optional<String> llave(String empresa, String lote, String archivo) {
        String llave = empresa + "/" + lote + "/" + archivo;
        return LLAVE.matcher(llave).matches() ? Optional.of(llave) : Optional.empty();
    }

    @Override
    public EnlaceTemporal enlaceDeSubida(String llave, String tipo, long bytes, Duration vigencia) {
        long vence = venceEn(vigencia);
        String url = enlace(llave, vence, firma.firmarSubida(deFoto(llave), vence, tipo, bytes));
        return new EnlaceTemporal(url, "PUT", Map.of("Content-Type", tipo), Instant.ofEpochSecond(vence));
    }

    @Override
    public EnlaceTemporal enlaceDeVista(String llave, Duration vigencia) {
        long vence = venceEn(vigencia);
        String url = enlace(llave, vence, firma.firmarVista(deFoto(llave), vence));
        return new EnlaceTemporal(url, "GET", Map.of(), Instant.ofEpochSecond(vence));
    }

    @Override
    public Optional<ArchivoGuardado> describir(String llave) {
        Path archivo = ruta(llave);
        if (!Files.isRegularFile(archivo)) {
            return Optional.empty();
        }
        try (InputStream contenido = Files.newInputStream(archivo)) {
            byte[] primeros = contenido.readNBytes(FormatoDeFoto.BYTES_PARA_RECONOCER);
            return Optional.of(new ArchivoGuardado(Files.size(archivo), primeros));
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }

    @Override
    public byte[] leer(String llave) {
        try {
            return Files.readAllBytes(ruta(llave));
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }

    /**
     * Guarda un archivo que arma el propio servidor, como el reporte PDF (RF-15). La carpeta no guarda el tipo: el de
     * cada archivo sale de su extensión. Como una foto subida, no pisa un archivo que ya está; con la llave al azar que
     * arma el servidor, eso solo pasaría por un error del servidor.
     *
     * @param llave la llave del archivo
     * @param contenido sus bytes
     * @param tipo su tipo, como {@code application/pdf}
     * @throws IllegalStateException si ya hay un archivo con esa llave
     */
    @Override
    public void guardar(String llave, byte[] contenido, String tipo) {
        try {
            guardar(llave, contenido);
        } catch (ConflictoExcepcion excepcion) {
            throw new IllegalStateException("Ya hay un archivo con la llave " + llave, excepcion);
        }
    }

    @Override
    public void borrar(String llave) {
        try {
            Files.deleteIfExists(ruta(llave));
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }

    /**
     * Si ya se subió un archivo con esa llave.
     *
     * @param llave la llave
     * @return {@code true} si existe
     */
    public boolean existe(String llave) {
        return Files.exists(ruta(llave));
    }

    /**
     * Guarda un archivo subido. Lo escribe primero en un archivo temporal de la misma carpeta y después le pone su
     * nombre, sin reemplazar: un enlace que sigue vigente no puede pisar una foto ya subida.
     *
     * @param llave la llave
     * @param contenido los bytes del archivo
     * @throws ConflictoExcepcion si ya se subió un archivo con esa llave (409)
     */
    public void guardar(String llave, byte[] contenido) {
        Path destino = ruta(llave);
        Path temporal = null;
        try {
            Files.createDirectories(destino.getParent());
            temporal = Files.createTempFile(destino.getParent(), "subida-", ".tmp");
            Files.write(temporal, contenido);
            Files.move(temporal, destino);
        } catch (FileAlreadyExistsException excepcion) {
            throw new ConflictoExcepcion("foto.ya_subida");
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        } finally {
            borrarSiQueda(temporal);
        }
    }

    /**
     * El archivo de una llave, dentro de la carpeta.
     *
     * @param llave la llave
     * @return su ruta
     * @throws IllegalArgumentException si la llave no es de una foto ni de un reporte ({@link LlavesDeArchivos}) o
     *         saldría de la carpeta
     */
    public Path ruta(String llave) {
        Path ruta = raiz.resolve(revisada(llave)).normalize();
        if (!ruta.startsWith(raiz)) {
            throw new IllegalArgumentException("La llave sale de la carpeta de las fotos");
        }
        return ruta;
    }

    /** Una llave de una foto o de un reporte; cualquier otra no se acepta. */
    private static String revisada(String llave) {
        LlavesDeArchivos.clase(llave);
        return llave;
    }

    /** Una llave de una foto: solo las fotos tienen enlaces firmados. */
    private static String deFoto(String llave) {
        if (llave == null || !LLAVE.matcher(llave).matches()) {
            throw new IllegalArgumentException("La llave no tiene la forma <empresa>/<lote>/m<muestra>-<archivo>");
        }
        return llave;
    }

    private long venceEn(Duration vigencia) {
        return reloj.instant().plus(vigencia).getEpochSecond();
    }

    private static String enlace(String llave, long vence, String firma) {
        return RUTA + "/" + llave + "?vence=" + vence + "&firma=" + firma;
    }

    private static void borrarSiQueda(Path temporal) {
        if (temporal == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporal);
        } catch (IOException excepcion) {
            // Un temporal que no se pudo borrar no se sirve nunca: su nombre no tiene la forma de una llave.
        }
    }
}
