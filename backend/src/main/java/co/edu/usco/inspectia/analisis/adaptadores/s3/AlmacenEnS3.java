/*
 * Nombre: AlmacenEnS3.java
 * Descripción: El puerto AlmacenDeArchivos en Amazon S3 (D-16): enlaces prefirmados para subir y ver una foto, con el
 *              tipo y el tamaño firmados y sin pisar una foto ya subida, como la carpeta (D-41); y revisar, leer,
 *              guardar y borrar las fotos (prefijo fotos/) y los reportes PDF (prefijo reportes/).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.s3;

import co.edu.usco.inspectia.analisis.aplicacion.AlmacenDeArchivos;
import co.edu.usco.inspectia.analisis.aplicacion.ArchivoGuardado;
import co.edu.usco.inspectia.analisis.aplicacion.EnlaceTemporal;
import co.edu.usco.inspectia.analisis.aplicacion.LlavesDeArchivos;
import co.edu.usco.inspectia.analisis.dominio.FormatoDeFoto;
import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

/**
 * El almacén de los archivos en un bucket de S3 (D-16), el de {@code inspectia.archivos.bucket}. Hace lo mismo que la
 * carpeta de desarrollo (D-41), con los enlaces prefirmados de S3 en lugar de los que firma el servidor:
 * <ul>
 *   <li>Las llaves son las de {@link LlavesDeArchivos}. Una foto va en el objeto {@value #PREFIJO_DE_FOTOS} + su
 *       llave; un reporte, en su llave tal cual, que ya empieza por {@code reportes/}. Cualquier otra llave se rechaza
 *       antes de llamar a S3: el servidor nunca toca los modelos ni los respaldos del bucket.</li>
 *   <li>El enlace de subida es un {@code PUT} que firma el tipo, el tamaño y el encabezado
 *       {@code If-None-Match: *}: S3 rechaza otro tipo u otro tamaño (403) y no pisa una foto ya subida (412). La
 *       página manda los encabezados de {@link EnlaceTemporal#encabezados()}; el tamaño lo pone el navegador.</li>
 *   <li>El enlace de vista es un {@code GET} que fija el tipo de la respuesta por la extensión de la llave, nunca el que
 *       llegó al subirla, y la caché privada mientras dura el enlace.</li>
 *   <li>Solo las fotos tienen enlaces: un reporte se descarga por el backend, con la sesión.</li>
 * </ul>
 * Las credenciales son las del rol de la instancia ({@code inspectia-ec2}): este adaptador no lee ni guarda claves.
 */
public class AlmacenEnS3 implements AlmacenDeArchivos {

    /** El prefijo de las fotos en el bucket. */
    public static final String PREFIJO_DE_FOTOS = "fotos/";

    /** El encabezado firmado que impide pisar un objeto que ya existe. */
    static final String SIN_PISAR = "If-None-Match";

    private static final int PRECONDICION_FALLIDA = 412;

    private final S3Client cliente;
    private final S3Presigner firmador;
    private final String bucket;

    /**
     * Crea el almacén.
     *
     * @param cliente el cliente de S3, para revisar, leer, guardar y borrar
     * @param firmador el firmador de los enlaces, que firma sin conectarse
     * @param bucket el bucket de los archivos, como {@code inspectia-archivos-<cuenta>}
     */
    public AlmacenEnS3(S3Client cliente, S3Presigner firmador, String bucket) {
        this.cliente = cliente;
        this.firmador = firmador;
        this.bucket = bucket;
    }

    /**
     * El objeto de S3 de una llave.
     *
     * @param llave la llave de una foto o de un reporte
     * @return la llave del objeto en el bucket
     * @throws IllegalArgumentException si la llave no es de una foto ni de un reporte
     */
    public static String objeto(String llave) {
        return switch (LlavesDeArchivos.clase(llave)) {
            case FOTO -> PREFIJO_DE_FOTOS + llave;
            case REPORTE -> llave;
        };
    }

    @Override
    public EnlaceTemporal enlaceDeSubida(String llave, String tipo, long bytes, Duration vigencia) {
        PutObjectRequest subida = PutObjectRequest.builder()
                .bucket(bucket)
                .key(PREFIJO_DE_FOTOS + deFoto(llave))
                .contentType(tipo)
                .contentLength(bytes)
                .ifNoneMatch("*")
                .build();
        PresignedPutObjectRequest firmado = firmador.presignPutObject(peticion -> peticion
                .signatureDuration(vigencia)
                .putObjectRequest(subida));
        return new EnlaceTemporal(firmado.url().toString(), "PUT",
                Map.of("Content-Type", tipo, SIN_PISAR, "*"), firmado.expiration());
    }

    @Override
    public EnlaceTemporal enlaceDeVista(String llave, Duration vigencia) {
        String foto = deFoto(llave);
        FormatoDeFoto formato = foto.endsWith("." + FormatoDeFoto.PNG.getExtension())
                ? FormatoDeFoto.PNG : FormatoDeFoto.JPG;
        GetObjectRequest vista = GetObjectRequest.builder()
                .bucket(bucket)
                .key(PREFIJO_DE_FOTOS + foto)
                .responseContentType(formato.getTipo())
                .responseContentDisposition("inline")
                .responseCacheControl("private, max-age=" + vigencia.toSeconds())
                .build();
        PresignedGetObjectRequest firmado = firmador.presignGetObject(peticion -> peticion
                .signatureDuration(vigencia)
                .getObjectRequest(vista));
        return new EnlaceTemporal(firmado.url().toString(), "GET", Map.of(), firmado.expiration());
    }

    /**
     * Revisa un archivo con {@code HeadObject}, por su tamaño, y lee sus primeros bytes con un {@code GET} de un rango,
     * para reconocer el formato sin bajar la foto entera. Un objeto que no está responde 404 solo si el rol tiene
     * {@code s3:ListBucket} en el bucket; sin ese permiso S3 responde 403, que aquí no se toma como «no está».
     */
    @Override
    public Optional<ArchivoGuardado> describir(String llave) {
        String objeto = objeto(llave);
        HeadObjectResponse cabecera;
        try {
            cabecera = cliente.headObject(peticion -> peticion.bucket(bucket).key(objeto));
        } catch (NoSuchKeyException excepcion) {
            return Optional.empty();
        }
        long bytes = cabecera.contentLength() == null ? 0 : cabecera.contentLength();
        if (bytes == 0) {
            return Optional.of(new ArchivoGuardado(0, new byte[0]));
        }
        try {
            byte[] primeros = cliente.getObjectAsBytes(peticion -> peticion.bucket(bucket).key(objeto)
                    .range("bytes=0-" + (FormatoDeFoto.BYTES_PARA_RECONOCER - 1))).asByteArray();
            return Optional.of(new ArchivoGuardado(bytes, primeros));
        } catch (NoSuchKeyException excepcion) {
            return Optional.empty();
        }
    }

    @Override
    public byte[] leer(String llave) {
        String objeto = objeto(llave);
        try {
            return cliente.getObjectAsBytes(peticion -> peticion.bucket(bucket).key(objeto)).asByteArray();
        } catch (NoSuchKeyException excepcion) {
            UncheckedIOException noEsta = new UncheckedIOException(
                    new FileNotFoundException("No está el archivo s3://" + bucket + "/" + objeto));
            noEsta.addSuppressed(excepcion);
            throw noEsta;
        }
    }

    /**
     * Guarda un archivo que arma el propio servidor, como el reporte PDF (RF-15), con su tipo. Como la carpeta, no pisa
     * un objeto que ya está ({@code If-None-Match: *}); con la llave al azar que arma el servidor, eso solo pasaría por
     * un error del servidor.
     *
     * @param llave la llave del archivo
     * @param contenido sus bytes
     * @param tipo su tipo, como {@code application/pdf}
     * @throws IllegalStateException si ya hay un archivo con esa llave
     */
    @Override
    public void guardar(String llave, byte[] contenido, String tipo) {
        String objeto = objeto(llave);
        try {
            cliente.putObject(peticion -> peticion.bucket(bucket).key(objeto).contentType(tipo).ifNoneMatch("*"),
                    RequestBody.fromBytes(contenido));
        } catch (S3Exception excepcion) {
            if (excepcion.statusCode() == PRECONDICION_FALLIDA) {
                throw new IllegalStateException("Ya hay un archivo con la llave " + llave, excepcion);
            }
            throw excepcion;
        }
    }

    @Override
    public void borrar(String llave) {
        String objeto = objeto(llave);
        cliente.deleteObject(peticion -> peticion.bucket(bucket).key(objeto));
    }

    /** Una llave de una foto: solo las fotos tienen enlaces. */
    private static String deFoto(String llave) {
        if (!LlavesDeArchivos.esFoto(llave)) {
            throw new IllegalArgumentException("Solo las fotos tienen enlaces, con la forma "
                    + "<empresa>/<lote>/m<muestra>-<archivo>");
        }
        return llave;
    }
}
