/*
 * Nombre: ApiFotos.java
 * Descripción: Llamadas a la API de las fotos de un lote y a sus enlaces firmados, sus mensajes, y fotos JPG y PNG
 *              hechas en la misma prueba, para las pruebas (RF-05, RF-14 C5, RNF-03, RNF-04, D-41).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis;

import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Llamadas a la API de las fotos de un lote ({@code /api/v1/lotes/{id}/fotos}) y a los enlaces firmados de los
 * archivos ({@code /api/v1/archivos}), y los mensajes que responden.
 *
 * <p>Contrato que fijan estas pruebas (RF-05, RF-14 C5, D-41). La página sigue el mismo flujo que con S3:
 * <ol>
 *   <li>Pide el enlace de subida de la foto N (de 1 a 3) con {@code POST .../fotos/{N}/subida}, con {@code tipo}
 *       ({@code image/jpeg} o {@code image/png}) y {@code tamanoBytes} (de 1 a 10.485.760). Responde 201 con la
 *       {@code llave} del archivo y el enlace: {@code url}, relativa a la página; {@code metodo}, {@code PUT};
 *       {@code encabezados}, con el {@code Content-Type} que hay que enviar, y {@code vence}.</li>
 *   <li>Sube los bytes con {@code PUT} a ese enlace, sin el token y con el tipo y el tamaño que pidió. Responde 200.
 *       Sin firma, con una firma que no coincide o vencida, 403; con una llave que ya se subió, 409.</li>
 *   <li>Confirma la foto con {@code PUT .../fotos/{N}} y la {@code llave}. Responde 200 con la foto:
 *       {@code numero}, {@code formato} ({@code JPG} o {@code PNG}), {@code tamanoBytes}, {@code estado}
 *       ({@code CARGADA} hasta que la revise el servicio de IA) y {@code vista}, el enlace para verla
 *       ({@code url} y {@code vence}).</li>
 * </ol>
 * La lista de la muestra en curso ({@code GET .../fotos}) responde {@code muestra} (su número, o vacío si todavía
 * no tiene fotos), {@code estado} y {@code fotos}, cada una con un enlace de vista nuevo. Solo el usuario final usa
 * estas rutas, siempre con lotes de su empresa: los de otra responden 404, como si no existieran (RNF-03).
 */
final class ApiFotos {

    static final String ARCHIVOS = "/api/v1/archivos";
    static final String JPG = "image/jpeg";
    static final String PNG = "image/png";
    static final int MAXIMO_DE_BYTES = 10_485_760;

    static final String FORMATO_NO_PERMITIDO = "La foto debe ser JPG o PNG: elige una foto en uno de esos formatos.";
    static final String DEMASIADO_GRANDE =
            "La foto pesa más de 10 MB: tómala de nuevo con la resolución normal de la cámara o elige otra.";
    static final String NO_LLEGO = "La foto no llegó completa: súbela de nuevo.";
    static final String LOTE_NO_PENDIENTE =
            "Este lote ya no está pendiente de análisis: sus fotos no se pueden cambiar.";
    static final String ENLACE_NO_VALIDO = "El enlace de la foto venció o no es válido: vuelve a intentarlo.";
    static final String YA_SUBIDA = "Ese enlace ya se usó: pide uno nuevo para subir otra foto.";

    private final ApiSesion api;
    private final MockMvcTester mvc;

    ApiFotos(ApiSesion api, MockMvcTester mvc) {
        this.api = api;
        this.mvc = mvc;
    }

    /** La ruta de las fotos de un lote. */
    static String ruta(UUID lote) {
        return ApiLotes.RUTA + "/" + lote + "/fotos";
    }

    MvcTestResult listar(String token, UUID lote) {
        return api.enviar(HttpMethod.GET, ruta(lote), null, token);
    }

    MvcTestResult pedirSubida(String token, UUID lote, int numero, String tipo, long bytes) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("tipo", tipo);
        datos.put("tamanoBytes", bytes);
        return api.enviar(HttpMethod.POST, ruta(lote) + "/" + numero + "/subida", datos, token);
    }

    /** Sube los bytes al enlace, sin el token, como la página. */
    MvcTestResult subir(String url, String tipo, byte[] contenido) {
        return mvc.put().uri(url).contentType(tipo).content(contenido).exchange();
    }

    MvcTestResult confirmar(String token, UUID lote, int numero, String llave) {
        Map<String, Object> datos = llave == null ? Map.of() : Map.of("llave", llave);
        return api.enviar(HttpMethod.PUT, ruta(lote) + "/" + numero, datos, token);
    }

    /** Abre un enlace de vista, sin el token, como un {@code <img>}. */
    MvcTestResult ver(String url) {
        return mvc.get().uri(url).exchange();
    }

    /** Pide el enlace de subida y espera el 201. */
    Map<String, Object> subida(String token, UUID lote, int numero, String tipo, long bytes) {
        MvcTestResult resultado = pedirSubida(token, lote, numero, tipo, bytes);
        assertThat(resultado).hasStatus(HttpStatus.CREATED);
        return json(resultado);
    }

    /** Sube una foto completa: pide el enlace, sube los bytes y la confirma. Devuelve la foto confirmada. */
    Map<String, Object> subirFoto(String token, UUID lote, int numero, String tipo, byte[] contenido) {
        Map<String, Object> subida = subida(token, lote, numero, tipo, contenido.length);
        assertThat(subir((String) subida.get("url"), tipo, contenido)).hasStatus(HttpStatus.OK);
        MvcTestResult confirmada = confirmar(token, lote, numero, (String) subida.get("llave"));
        assertThat(confirmada).hasStatus(HttpStatus.OK);
        return json(confirmada);
    }

    /** El enlace de vista de una foto confirmada. */
    @SuppressWarnings("unchecked")
    static String vista(Map<String, Object> foto) {
        return (String) ((Map<String, Object>) foto.get("vista")).get("url");
    }

    /** Una foto JPG de 2 × 2 píxeles, hecha en la prueba. */
    static byte[] jpg() {
        return imagen("jpg");
    }

    /** Una foto PNG de 2 × 2 píxeles, hecha en la prueba. */
    static byte[] png() {
        return imagen("png");
    }

    /** La foto con ceros al final hasta medir exactamente {@code bytes}: sigue empezando como JPG o PNG. */
    static byte[] conTamano(byte[] foto, int bytes) {
        return Arrays.copyOf(foto, bytes);
    }

    /** Un archivo que no es una foto: el comienzo de un PDF. */
    static byte[] pdf() {
        return "%PDF-1.7\n%prueba de InspectIA\n".getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] imagen(String formato) {
        BufferedImage imagen = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        imagen.setRGB(0, 0, 0x6B3F22);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            assertThat(ImageIO.write(imagen, formato, salida)).as("ImageIO escribe " + formato).isTrue();
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
        return salida.toByteArray();
    }
}
