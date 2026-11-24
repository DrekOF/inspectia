/*
 * Nombre: ClienteHttpFalso.java
 * Descripción: Un cliente HTTP falso para el AWS SDK: guarda cada petición que arma el cliente de S3 y responde lo que
 *              diga la prueba, sin red (D-16).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.s3;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * Un cliente HTTP del SDK que no sale a la red: el cliente de S3 arma y firma cada petición como siempre, y este la
 * guarda y responde lo que la prueba puso en su cola (por defecto, 200 sin cuerpo). Así la prueba revisa las
 * peticiones reales a S3 (método, objeto, encabezados) y cómo el SDK lee las respuestas, como un 404.
 */
final class ClienteHttpFalso implements SdkHttpClient {

    /** Una respuesta que dará el cliente. */
    private record Respuesta(int estado, Map<String, String> encabezados, byte[] cuerpo) {
    }

    private final List<SdkHttpRequest> peticiones = new ArrayList<>();
    private final List<byte[]> cuerpos = new ArrayList<>();
    private final Deque<Respuesta> respuestas = new ArrayDeque<>();

    /**
     * Pone una respuesta en la cola.
     *
     * @param estado el estado HTTP
     * @param encabezados los encabezados
     * @param cuerpo el cuerpo
     * @return este cliente
     */
    ClienteHttpFalso responder(int estado, Map<String, String> encabezados, byte[] cuerpo) {
        respuestas.add(new Respuesta(estado, encabezados, cuerpo));
        return this;
    }

    /**
     * Pone en la cola un error de S3, con su cuerpo XML.
     *
     * @param estado el estado HTTP
     * @param codigo el código del error, como {@code NoSuchKey}
     * @return este cliente
     */
    ClienteHttpFalso responderError(int estado, String codigo) {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Error><Code>" + codigo + "</Code><Message>" + codigo
                + "</Message><RequestId>prueba</RequestId></Error>";
        return responder(estado, Map.of("Content-Type", "application/xml"), xml.getBytes(StandardCharsets.UTF_8));
    }

    /** @return las peticiones que recibió, en orden */
    List<SdkHttpRequest> peticiones() {
        return peticiones;
    }

    /** @return los cuerpos de las peticiones, en orden */
    List<byte[]> cuerpos() {
        return cuerpos;
    }

    @Override
    public ExecutableHttpRequest prepareRequest(HttpExecuteRequest pedido) {
        return new ExecutableHttpRequest() {
            @Override
            public HttpExecuteResponse call() {
                peticiones.add(pedido.httpRequest());
                cuerpos.add(pedido.contentStreamProvider().map(proveedor -> leer(proveedor.newStream()))
                        .orElse(new byte[0]));
                Respuesta respuesta = respuestas.isEmpty() ? new Respuesta(200, Map.of(), new byte[0])
                        : respuestas.poll();
                SdkHttpResponse.Builder http = SdkHttpResponse.builder().statusCode(respuesta.estado());
                if (!respuesta.encabezados().containsKey("Content-Length")) {
                    http.putHeader("Content-Length", String.valueOf(respuesta.cuerpo().length));
                }
                respuesta.encabezados().forEach(http::putHeader);
                return HttpExecuteResponse.builder()
                        .response(http.build())
                        .responseBody(AbortableInputStream.create(new ByteArrayInputStream(respuesta.cuerpo())))
                        .build();
            }

            @Override
            public void abort() {
                // No hay conexión que cortar.
            }
        };
    }

    @Override
    public void close() {
        // No hay conexiones que cerrar.
    }

    private static byte[] leer(InputStream contenido) {
        try (contenido) {
            return contenido.readAllBytes();
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }
}
