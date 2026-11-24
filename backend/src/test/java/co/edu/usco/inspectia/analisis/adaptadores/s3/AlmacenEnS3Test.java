/*
 * Nombre: AlmacenEnS3Test.java
 * Descripción: Pruebas unitarias del almacén de los archivos en S3 (D-16), sin red: los enlaces prefirmados de subida
 *              y de vista con el firmador del SDK, y las llamadas a S3 con un cliente HTTP falso. También revisa que la
 *              CSP de CloudFront deje subir y ver las fotos en el host de esos enlaces (D-31).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.usco.inspectia.analisis.aplicacion.ArchivoGuardado;
import co.edu.usco.inspectia.analisis.aplicacion.EnlaceTemporal;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** El almacén en S3, con el firmador real del SDK y un cliente HTTP falso (D-16). */
class AlmacenEnS3Test {

    private static final String CUENTA = "123456789012";
    private static final String BUCKET = "inspectia-archivos-" + CUENTA;
    private static final String EMPRESA = "0199f0a0-0000-7000-8000-000000000001";
    private static final String LOTE = "0199f0a0-0000-7000-8000-000000000002";
    private static final String LLAVE = EMPRESA + "/" + LOTE + "/m1-0199f0a0-0000-7000-8000-000000000003.jpg";
    private static final String LLAVE_PNG = EMPRESA + "/" + LOTE + "/m2-0199f0a0-0000-7000-8000-000000000005.png";
    private static final String REPORTE = "reportes/" + EMPRESA + "/" + LOTE
            + "/0199f0a0-0000-7000-8000-000000000004-en.pdf";
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3, 4};
    private static final byte[] PDF = "%PDF-1.7 report".getBytes(StandardCharsets.US_ASCII);

    /** Unas credenciales de prueba: el firmador firma con ellas sin conectarse a AWS. */
    private static final StaticCredentialsProvider CREDENCIALES = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("AKIAPRUEBAPRUEBA0000", "clave-de-prueba-que-no-sirve-en-aws"));

    /** La CSP de CloudFront que usa la guía de la consola (D-31). */
    private static final Path CSP = Path.of("..", "infra", "aws", "cloudfront", "csp.txt");

    private ClienteHttpFalso http;
    private S3Client cliente;
    private S3Presigner firmador;
    private AlmacenEnS3 almacen;

    @BeforeEach
    void preparar() {
        http = new ClienteHttpFalso();
        cliente = S3Client.builder().region(Region.US_EAST_1).credentialsProvider(CREDENCIALES).httpClient(http)
                .build();
        firmador = S3Presigner.builder().region(Region.US_EAST_1).credentialsProvider(CREDENCIALES).build();
        almacen = new AlmacenEnS3(cliente, firmador, BUCKET);
    }

    @AfterEach
    void cerrar() {
        cliente.close();
        firmador.close();
    }

    @Test
    @DisplayName("D-16 · el enlace de subida es un PUT prefirmado de S3 a fotos/<llave>, de 10 minutos, que firma el tipo, el tamaño y el If-None-Match que no deja pisar la foto")
    void enlaceDeSubida() {
        Instant antes = Instant.now();
        EnlaceTemporal subida = almacen.enlaceDeSubida(LLAVE, "image/jpeg", 2_345_678, Duration.ofMinutes(10));

        URI url = URI.create(subida.url());
        Map<String, String> consulta = consulta(url);
        assertThat(url.getScheme()).isEqualTo("https");
        assertThat(url.getHost()).isEqualTo(hostDeLosEnlaces());
        assertThat(url.getPath()).isEqualTo("/fotos/" + LLAVE);
        assertThat(subida.metodo()).isEqualTo("PUT");
        assertThat(subida.encabezados()).isEqualTo(Map.of("Content-Type", "image/jpeg", "If-None-Match", "*"));
        assertThat(consulta).containsEntry("X-Amz-Algorithm", "AWS4-HMAC-SHA256").containsEntry("X-Amz-Expires", "600");
        assertThat(consulta.get("X-Amz-SignedHeaders").split(";"))
                .contains("content-length", "content-type", "host", "if-none-match");
        assertThat(consulta.get("X-Amz-Credential")).endsWith("/us-east-1/s3/aws4_request");
        assertThat(consulta.keySet()).as("sin parámetros de checksum, que el navegador no sabría mandar")
                .noneMatch(nombre -> nombre.toLowerCase().contains("checksum"));
        assertThat(subida.vence()).isBetween(antes.plusSeconds(599), Instant.now().plusSeconds(601));
        assertThat(http.peticiones()).as("firmar no llama a S3").isEmpty();
    }

    @Test
    @DisplayName("RNF-03 · C3 · una foto en S3 solo se ve con un enlace GET prefirmado para su llave, de 15 minutos, con el tipo de su extensión y la caché privada")
    void enlaceDeVista() {
        Instant antes = Instant.now();
        EnlaceTemporal vista = almacen.enlaceDeVista(LLAVE_PNG, Duration.ofMinutes(15));

        URI url = URI.create(vista.url());
        Map<String, String> consulta = consulta(url);
        assertThat(url.getHost()).isEqualTo(hostDeLosEnlaces());
        assertThat(url.getPath()).isEqualTo("/fotos/" + LLAVE_PNG);
        assertThat(vista.metodo()).isEqualTo("GET");
        assertThat(vista.encabezados()).isEmpty();
        assertThat(consulta).containsEntry("X-Amz-Expires", "900")
                .containsEntry("response-content-type", "image/png")
                .containsEntry("response-content-disposition", "inline")
                .containsEntry("response-cache-control", "private, max-age=900")
                .containsEntry("X-Amz-SignedHeaders", "host")
                .containsKey("X-Amz-Signature");
        assertThat(vista.vence()).isBetween(antes.plusSeconds(899), Instant.now().plusSeconds(901));
        assertThat(URI.create(almacen.enlaceDeVista(LLAVE, Duration.ofMinutes(15)).url()).getRawQuery())
                .contains("response-content-type=image%2Fjpeg");
    }

    @Test
    @DisplayName("D-16 · los enlaces son solo de fotos: un reporte, que se descarga por el backend con la sesión, u otra llave no tienen enlace")
    void soloLasFotosTienenEnlace() {
        for (String llave : new String[] {REPORTE, "modelos/inspectia_EfficientNet-B0_v1.0_savedmodel.zip",
            "fotos/" + LLAVE, "../" + LLAVE, null}) {
            assertThatThrownBy(() -> almacen.enlaceDeVista(llave, Duration.ofMinutes(15))).as(String.valueOf(llave))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> almacen.enlaceDeSubida(llave, "image/jpeg", 10, Duration.ofMinutes(10)))
                    .as(String.valueOf(llave)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("RF-05 · C3 · con S3, describir una foto subida da su tamaño, con HeadObject, y solo sus primeros 8 bytes, para revisar que sea JPG o PNG")
    void describir() {
        http.responder(200, Map.of("Content-Length", "2345678", "Content-Type", "image/jpeg"), new byte[0])
                .responder(206, Map.of("Content-Range", "bytes 0-7/2345678"), JPG);

        ArchivoGuardado archivo = almacen.describir(LLAVE).orElseThrow();

        assertThat(archivo.bytes()).isEqualTo(2_345_678);
        assertThat(archivo.primeros()).isEqualTo(JPG);
        List<SdkHttpRequest> peticiones = http.peticiones();
        assertThat(peticiones).hasSize(2);
        assertThat(peticiones.get(0).method()).isEqualTo(SdkHttpMethod.HEAD);
        assertThat(peticiones.get(0).host()).isEqualTo(hostDeLosEnlaces());
        assertThat(peticiones.get(0).encodedPath()).isEqualTo("/fotos/" + LLAVE);
        assertThat(peticiones.get(1).method()).isEqualTo(SdkHttpMethod.GET);
        assertThat(peticiones.get(1).encodedPath()).isEqualTo("/fotos/" + LLAVE);
        assertThat(peticiones.get(1).firstMatchingHeader("Range")).contains("bytes=0-7");
    }

    @Test
    @DisplayName("RF-05 · C3 · con S3, una foto que no llegó (404) se describe como vacía, y una de 0 bytes no se baja")
    void describirLaQueNoEsta() {
        http.responder(404, Map.of(), new byte[0]).responder(200, Map.of("Content-Length", "0"), new byte[0]);

        assertThat(almacen.describir(LLAVE)).isEmpty();
        ArchivoGuardado vacio = almacen.describir(LLAVE).orElseThrow();

        assertThat(vacio.bytes()).isZero();
        assertThat(vacio.primeros()).isEmpty();
        assertThat(http.peticiones()).extracting(SdkHttpRequest::method)
                .containsExactly(SdkHttpMethod.HEAD, SdkHttpMethod.HEAD);
    }

    @Test
    @DisplayName("D-16 · con S3, leer devuelve todo el archivo, la foto de fotos/<llave> o el reporte en su llave; uno que no está (NoSuchKey) no se puede leer")
    void leer() {
        http.responder(200, Map.of(), JPG).responder(200, Map.of(), PDF).responderError(404, "NoSuchKey");

        assertThat(almacen.leer(LLAVE)).isEqualTo(JPG);
        assertThat(almacen.leer(REPORTE)).isEqualTo(PDF);
        assertThatThrownBy(() -> almacen.leer(LLAVE)).isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("s3://" + BUCKET + "/fotos/" + LLAVE);
        assertThat(http.peticiones()).extracting(SdkHttpRequest::encodedPath)
                .containsExactly("/fotos/" + LLAVE, "/" + REPORTE, "/fotos/" + LLAVE);
    }

    @Test
    @DisplayName("D-16 · con S3, guardar sube el reporte PDF a su llave con su tipo y el If-None-Match que no deja pisarlo; si ya estaba (412), es un error del servidor")
    void guardar() {
        http.responder(200, Map.of(), new byte[0]).responderError(412, "PreconditionFailed");

        almacen.guardar(REPORTE, PDF, "application/pdf");

        SdkHttpRequest subida = http.peticiones().get(0);
        assertThat(subida.method()).isEqualTo(SdkHttpMethod.PUT);
        assertThat(subida.encodedPath()).isEqualTo("/" + REPORTE);
        assertThat(subida.firstMatchingHeader("Content-Type")).contains("application/pdf");
        assertThat(subida.firstMatchingHeader("If-None-Match")).contains("*");
        assertThat(new String(http.cuerpos().get(0), StandardCharsets.ISO_8859_1))
                .contains(new String(PDF, StandardCharsets.ISO_8859_1));
        assertThatThrownBy(() -> almacen.guardar(REPORTE, PDF, "application/pdf"))
                .isInstanceOf(IllegalStateException.class).hasCauseInstanceOf(S3Exception.class);
    }

    @Test
    @DisplayName("RF-05 · C5 · con S3, borrar una foto reemplazada borra su objeto de fotos/; si ya no estaba, S3 responde igual y no falla")
    void borrar() {
        http.responder(204, Map.of(), new byte[0]).responder(204, Map.of(), new byte[0]);

        almacen.borrar(LLAVE);
        almacen.borrar(LLAVE);

        assertThat(http.peticiones()).extracting(SdkHttpRequest::method)
                .containsExactly(SdkHttpMethod.DELETE, SdkHttpMethod.DELETE);
        assertThat(http.peticiones().get(0).encodedPath()).isEqualTo("/fotos/" + LLAVE);
    }

    @Test
    @DisplayName("D-16 · una llave que no es de una foto ni de un reporte se rechaza antes de llamar a S3: el servidor no toca modelos/ ni respaldos/")
    void otrasLlavesNoLleganAS3() {
        for (String llave : new String[] {"modelos/manifest.json", "respaldos/inspectia.sql.gz", "fotos/" + LLAVE,
            "../" + LLAVE, EMPRESA + "/" + LOTE + "/archivo.jpg", "reportes/" + EMPRESA + "/x.pdf", "", null}) {
            assertThatThrownBy(() -> almacen.describir(llave)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> almacen.leer(llave)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> almacen.guardar(llave, PDF, "application/pdf"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> almacen.borrar(llave)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(http.peticiones()).isEmpty();
        assertThat(AlmacenEnS3.objeto(LLAVE)).isEqualTo("fotos/" + LLAVE);
        assertThat(AlmacenEnS3.objeto(REPORTE)).isEqualTo(REPORTE);
    }

    @Test
    @DisplayName("D-31 · la CSP de CloudFront deja subir (connect-src) y ver (img-src) las fotos en el host de los enlaces prefirmados de us-east-1, y nada más fuera de la página")
    void laCspPermiteElHostDeLosEnlaces() throws Exception {
        List<String> lineas = Files.readAllLines(CSP, StandardCharsets.UTF_8).stream()
                .filter(linea -> !linea.startsWith("#") && !linea.isBlank()).toList();
        assertThat(lineas).as("una sola línea sin «#», la que se copia en CloudFront").hasSize(1);
        String csp = lineas.get(0).strip().replace("<cuenta>", CUENTA);
        Map<String, List<String>> directivas = Arrays.stream(csp.split(";"))
                .map(String::strip).filter(directiva -> !directiva.isEmpty())
                .collect(Collectors.toMap(directiva -> directiva.split("\\s+")[0],
                    directiva -> List.of(directiva.split("\\s+")).subList(1, directiva.split("\\s+").length),
                    (a, b) -> a, LinkedHashMap::new));
        String origen = "https://" + URI.create(almacen.enlaceDeVista(LLAVE, Duration.ofMinutes(15)).url()).getHost();

        assertThat(csp.length()).as("CloudFront acepta hasta 1783 caracteres").isLessThanOrEqualTo(1783);
        assertThat(directivas.get("connect-src")).containsExactly("'self'", origen);
        assertThat(directivas.get("img-src")).containsExactly("'self'", "data:", origen);
        assertThat(directivas.get("script-src")).containsExactly("'self'");
        assertThat(directivas.get("style-src")).containsExactly("'self'", "'unsafe-inline'");
        assertThat(directivas.get("default-src")).containsExactly("'self'");
        assertThat(directivas.get("frame-ancestors")).containsExactly("'none'");
        assertThat(directivas.get("object-src")).containsExactly("'none'");
    }

    /** El host de los enlaces prefirmados de S3 para el bucket en us-east-1, el que va en la CSP. */
    private static String hostDeLosEnlaces() {
        return BUCKET + ".s3.amazonaws.com";
    }

    private static Map<String, String> consulta(URI url) {
        Map<String, String> parametros = new LinkedHashMap<>();
        for (String par : url.getRawQuery().split("&")) {
            String[] partes = par.split("=", 2);
            parametros.put(URLDecoder.decode(partes[0], StandardCharsets.UTF_8),
                    partes.length > 1 ? URLDecoder.decode(partes[1], StandardCharsets.UTF_8) : "");
        }
        return parametros;
    }
}
