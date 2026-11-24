/*
 * Nombre: ServidorDeIaDePrueba.java
 * Descripción: Un servicio de IA de mentira para las pruebas: un servidor HTTP del JDK que responde /revisar,
 *              /analizar y /modelos como dice el contrato, con respuestas, errores y demoras que programa cada prueba
 *              (RF-06, RF-07, RF-08, RF-09, D-46).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * El servicio de IA de las pruebas: un {@link HttpServer} del JDK en {@code 127.0.0.1} y un puerto libre, sin
 * dependencias nuevas. Cada ruta responde lo que la prueba programe ({@link #responder}), con una demora opcional
 * ({@link #demorar}); sin programar, responde como un servicio sano:
 *
 * <ul>
 *   <li>{@code GET /modelos}: los 5 modelos de {@link #MODELOS}, con EfficientNet-B0 en uso.</li>
 *   <li>{@code POST /revisar}: la foto aprobada con 50 granos.</li>
 *   <li>{@code POST /analizar}: cada foto que recibe ({@code foto1} a {@code foto3}) aprobada con 50 granos sanos de
 *       confianza 90,00.</li>
 * </ul>
 *
 * <p>Guarda cada petición que recibe, con su tipo y su cuerpo, para revisar lo que mandó el backend.
 */
public final class ServidorDeIaDePrueba implements AutoCloseable {

    /** Los 5 modelos de la comparación que responde {@code GET /modelos}; las cifras son de prueba. */
    public static final String MODELOS = """
            {"enUso": "EfficientNet-B0", "modelos": [
              {"nombre": "EfficientNet-B0", "version": "1.0", "tipo": "CNN", "f1Macro": 73.50, "precisionMacro": 75.20,
               "recallMacro": 72.31, "recallSano": 91.20, "recallNoSano": 53.40, "precisionSano": 87.50,
               "precisionNoSano": 62.90, "sanosComoSanos": 238, "sanosComoNoSanos": 23, "noSanosComoSanos": 34,
               "noSanosComoNoSanos": 39, "tiempoPorGranoMs": 19.00},
              {"nombre": "ResNet-50", "version": "1.0", "tipo": "CNN", "f1Macro": 69.90, "precisionMacro": 70.11,
               "recallMacro": 69.71, "recallSano": 87.40, "recallNoSano": 52.10, "precisionSano": 86.69,
               "precisionNoSano": 53.52, "sanosComoSanos": 228, "sanosComoNoSanos": 33, "noSanosComoSanos": 35,
               "noSanosComoNoSanos": 38, "tiempoPorGranoMs": 50.80},
              {"nombre": "MobileNetV3-L", "version": "1.0", "tipo": "CNN", "f1Macro": 63.80, "precisionMacro": 77.77,
               "recallMacro": 61.67, "recallSano": 97.30, "recallNoSano": 26.00, "precisionSano": 82.47,
               "precisionNoSano": 73.08, "sanosComoSanos": 254, "sanosComoNoSanos": 7, "noSanosComoSanos": 54,
               "noSanosComoNoSanos": 19, "tiempoPorGranoMs": 52.10},
              {"nombre": "Transformador-A", "version": "1.0", "tipo": "TRANSFORMER", "f1Macro": 66.00,
               "precisionMacro": 67.00, "recallMacro": 65.50, "recallSano": 85.00, "recallNoSano": 46.00,
               "precisionSano": 85.00, "precisionNoSano": 46.00, "sanosComoSanos": 222, "sanosComoNoSanos": 39,
               "noSanosComoSanos": 39, "noSanosComoNoSanos": 34, "tiempoPorGranoMs": 80.00},
              {"nombre": "Transformador-B", "version": "1.0", "tipo": "TRANSFORMER", "f1Macro": 64.00,
               "precisionMacro": 65.00, "recallMacro": 63.50, "recallSano": 84.00, "recallNoSano": 43.00,
               "precisionSano": 84.00, "precisionNoSano": 43.00, "sanosComoSanos": 219, "sanosComoNoSanos": 42,
               "noSanosComoSanos": 42, "noSanosComoNoSanos": 31, "tiempoPorGranoMs": 95.00}
            ]}""";

    /**
     * Una petición que recibió el servidor.
     *
     * @param ruta la ruta, como {@code /revisar}
     * @param tipo su {@code Content-Type}
     * @param cuerpo su cuerpo
     */
    public record Peticion(String ruta, String tipo, byte[] cuerpo) {

        /**
         * Si el cuerpo multipart trae la parte con ese nombre.
         *
         * @param nombre el nombre de la parte, como {@code foto1}
         * @return {@code true} si la trae
         */
        public boolean tieneParte(String nombre) {
            return new String(cuerpo, StandardCharsets.ISO_8859_1).contains("name=\"" + nombre + "\"");
        }

        /**
         * Cuántas fotos trae: las partes {@code foto1} a {@code foto3}.
         *
         * @return los números de las fotos, en orden
         */
        public List<Integer> fotos() {
            List<Integer> numeros = new ArrayList<>();
            for (int numero = 1; numero <= 3; numero++) {
                if (tieneParte("foto" + numero)) {
                    numeros.add(numero);
                }
            }
            return numeros;
        }
    }

    /**
     * Lo que responde una ruta.
     *
     * @param estado el código HTTP
     * @param json el cuerpo
     */
    public record Respuesta(int estado, String json) {
    }

    private final HttpServer servidor;
    private final Map<String, Function<Peticion, Respuesta>> respuestas = new ConcurrentHashMap<>();
    private final Map<String, Duration> demoras = new ConcurrentHashMap<>();
    private final List<Peticion> peticiones = new CopyOnWriteArrayList<>();

    private ServidorDeIaDePrueba(HttpServer servidor) {
        this.servidor = servidor;
    }

    /**
     * Arranca el servidor en un puerto libre.
     *
     * @return el servidor, que responde como un servicio sano
     */
    public static ServidorDeIaDePrueba iniciar() {
        try {
            HttpServer http = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            ServidorDeIaDePrueba servidor = new ServidorDeIaDePrueba(http);
            http.createContext("/", servidor::atender);
            http.setExecutor(Executors.newCachedThreadPool(Thread.ofPlatform().daemon(true).factory()));
            http.start();
            servidor.restablecer();
            return servidor;
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }

    /**
     * La dirección del servidor.
     *
     * @return como {@code http://127.0.0.1:54321}
     */
    public URI url() {
        return URI.create("http://127.0.0.1:" + servidor.getAddress().getPort());
    }

    /**
     * Programa lo que responde una ruta.
     *
     * @param ruta la ruta, como {@code /analizar}
     * @param estado el código HTTP
     * @param json el cuerpo
     */
    public void responder(String ruta, int estado, String json) {
        respuestas.put(ruta, peticion -> new Respuesta(estado, json));
    }

    /**
     * Programa lo que responde una ruta según la petición.
     *
     * @param ruta la ruta
     * @param respuesta la respuesta para cada petición
     */
    public void responder(String ruta, Function<Peticion, Respuesta> respuesta) {
        respuestas.put(ruta, respuesta);
    }

    /**
     * Hace esperar una ruta antes de responder, como un servicio lento.
     *
     * @param ruta la ruta
     * @param demora cuánto espera
     */
    public void demorar(String ruta, Duration demora) {
        demoras.put(ruta, demora);
    }

    /**
     * Las peticiones que recibió una ruta, en orden.
     *
     * @param ruta la ruta
     * @return las peticiones
     */
    public List<Peticion> peticiones(String ruta) {
        return peticiones.stream().filter(peticion -> peticion.ruta().equals(ruta)).toList();
    }

    /** Vuelve a responder como un servicio sano, sin demoras, y olvida las peticiones. */
    public void restablecer() {
        respuestas.clear();
        demoras.clear();
        peticiones.clear();
        responder("/modelos", 200, MODELOS);
        responder("/revisar", 200, revision("APROBADA", null, 50));
        responder("/analizar", peticion -> new Respuesta(200,
                analisis(peticion.fotos().stream().map(numero -> FotoAnalizada.aprobada(numero, 50, 0, 0)).toList())));
    }

    @Override
    public void close() {
        servidor.stop(0);
    }

    /**
     * El JSON de una revisión.
     *
     * @param estado {@code APROBADA} o {@code POR_REPETIR}
     * @param motivo el motivo, o {@code null}
     * @param granos los granos encontrados
     * @param regiones las regiones de granos pegados, cada una {x, y, ancho, alto}
     * @return el JSON
     */
    public static String revision(String estado, String motivo, int granos, int[]... regiones) {
        StringBuilder json = new StringBuilder("{\"estado\": \"").append(estado).append("\", \"motivo\": ")
                .append(motivo == null ? "null" : "\"" + motivo + "\"").append(", \"granosEncontrados\": ")
                .append(granos).append(", \"regionesPegadas\": [");
        for (int i = 0; i < regiones.length; i++) {
            json.append(i == 0 ? "" : ", ").append(String.format(Locale.ROOT,
                    "{\"x\": %d, \"y\": %d, \"ancho\": %d, \"alto\": %d}", regiones[i][0], regiones[i][1],
                    regiones[i][2], regiones[i][3]));
        }
        return json.append("], \"anchoImagen\": 4000, \"altoImagen\": 3000}").toString();
    }

    /**
     * Una foto de la respuesta de {@code /analizar}.
     *
     * @param numero su número
     * @param revision el JSON de su revisión
     * @param granos cuántos granos tiene; ninguno si está por repetir
     * @param noSanos cuántos de los primeros son no sanos
     * @param dudosos cuántos de los primeros tienen confianza 55,00, menos que el umbral de 60
     */
    public record FotoAnalizada(int numero, String revision, int granos, int noSanos, int dudosos) {

        /**
         * Una foto aprobada con sus granos.
         *
         * @param numero su número
         * @param granos cuántos granos tiene
         * @param noSanos cuántos son no sanos
         * @param dudosos cuántos son dudosos
         * @return la foto
         */
        public static FotoAnalizada aprobada(int numero, int granos, int noSanos, int dudosos) {
            return new FotoAnalizada(numero, ServidorDeIaDePrueba.revision("APROBADA", null, granos), granos, noSanos,
                    dudosos);
        }

        /**
         * Una foto que no pasó la revisión de la cola, sin granos.
         *
         * @param numero su número
         * @param motivo su motivo
         * @param granos los granos encontrados
         * @return la foto
         */
        public static FotoAnalizada porRepetir(int numero, String motivo, int granos) {
            return new FotoAnalizada(numero, ServidorDeIaDePrueba.revision("POR_REPETIR", motivo, granos), 0, 0, 0);
        }
    }

    /**
     * El JSON de {@code /analizar}: el modelo EfficientNet-B0 1.0 y cada foto con sus granos en una fila, de 10 × 10 px.
     *
     * @param fotos las fotos
     * @return el JSON
     */
    public static String analisis(List<FotoAnalizada> fotos) {
        StringBuilder json = new StringBuilder("{\"modelo\": {\"nombre\": \"EfficientNet-B0\", \"version\": \"1.0\"}, "
                + "\"fotos\": [");
        for (int f = 0; f < fotos.size(); f++) {
            FotoAnalizada foto = fotos.get(f);
            json.append(f == 0 ? "" : ", ").append("{\"numero\": ").append(foto.numero()).append(", \"revision\": ")
                    .append(foto.revision()).append(", \"granos\": [");
            for (int g = 0; g < foto.granos(); g++) {
                json.append(g == 0 ? "" : ", ").append(String.format(Locale.ROOT,
                        "{\"x\": %d, \"y\": %d, \"ancho\": 10, \"alto\": 10, \"clase\": \"%s\", \"confianza\": %s}",
                        20 * g, 7, g < foto.noSanos() ? "NO_SANO" : "SANO", g < foto.dudosos() ? "55.00" : "90.00"));
            }
            json.append("]}");
        }
        return json.append("]}").toString();
    }

    private void atender(HttpExchange intercambio) throws IOException {
        try (intercambio) {
            String ruta = intercambio.getRequestURI().getPath();
            Peticion peticion = new Peticion(ruta, intercambio.getRequestHeaders().getFirst("Content-Type"),
                    intercambio.getRequestBody().readAllBytes());
            peticiones.add(peticion);
            Duration demora = demoras.get(ruta);
            if (demora != null) {
                try {
                    Thread.sleep(demora);
                } catch (InterruptedException excepcion) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            Function<Peticion, Respuesta> programada = respuestas.get(ruta);
            Respuesta respuesta = programada == null ? new Respuesta(404, "{\"detail\": \"Not Found\"}")
                    : programada.apply(peticion);
            byte[] cuerpo = respuesta.json().getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().set("Content-Type", "application/json");
            intercambio.sendResponseHeaders(respuesta.estado(), cuerpo.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(cuerpo);
            }
        } catch (IOException excepcion) {
            // El cliente cortó la conexión porque se le acabó el tiempo: es lo que prueban las demoras.
        }
    }
}
