/*
 * Nombre: TokensDePrueba.java
 * Descripción: Arma tokens JWT para las pruebas de sesión sin usar el código del backend: válidos,
 *              vencidos, alterados, sin firma o firmados con otra clave (RNF-04, D-28).
 * Fecha de creación: 14/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario.sesion;

import co.edu.usco.inspectia.ConfiguracionContenedores;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tokens JWT armados a mano con HMAC-SHA256 de Java, para que las pruebas no dependan de cómo el
 * backend los emite. Siguen D-28: firma HS256 y, en el contenido, solo {@code iss}, {@code sub},
 * {@code iat} y {@code exp}.
 */
final class TokensDePrueba {

    /** Emisor que el backend pone en sus tokens y exige en los que recibe. */
    static final String EMISOR = "inspectia";

    /** Vigencia de una sesión (RNF-04). */
    static final Duration VIGENCIA = Duration.ofHours(8);

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

    private TokensDePrueba() {
    }

    /** Un token como los del backend, emitido en {@code emitido}, firmado con la clave de las pruebas. */
    static String token(UUID usuario, Instant emitido) {
        return firmar(contenido(EMISOR, usuario, emitido), clave());
    }

    /** El contenido de un token: emisor, usuario y fechas. */
    static Map<String, Object> contenido(String emisor, UUID usuario, Instant emitido) {
        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("iss", emisor);
        contenido.put("sub", usuario.toString());
        contenido.put("iat", emitido.getEpochSecond());
        contenido.put("exp", emitido.plus(VIGENCIA).getEpochSecond());
        return contenido;
    }

    /** Firma el contenido con HS256 y la clave dada. */
    static String firmar(Map<String, Object> contenido, byte[] clave) {
        String datos = parte(Map.of("alg", "HS256", "typ", "JWT")) + "." + parte(contenido);
        return datos + "." + hmac(datos, clave);
    }

    /** Un token con {@code alg: none}: sin firma. */
    static String sinFirma(Map<String, Object> contenido) {
        return parte(Map.of("alg", "none", "typ", "JWT")) + "." + parte(contenido) + ".";
    }

    /** El mismo token con otro contenido y la firma original: un token alterado. */
    static String alterado(String token, Map<String, Object> contenido) {
        String[] partes = token.split("\\.");
        return partes[0] + "." + parte(contenido) + "." + partes[2];
    }

    /** El contenido de un token, sin revisar la firma. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> leer(String token) {
        return JSON.readValue(Base64.getUrlDecoder().decode(token.split("\\.")[1]), Map.class);
    }

    /** La clave de esta corrida, la misma que usa el backend. */
    static byte[] clave() {
        return Base64.getDecoder().decode(ConfiguracionContenedores.CLAVE_JWT);
    }

    /** Una clave distinta de la del backend. */
    static byte[] otraClave() {
        byte[] clave = new byte[32];
        new SecureRandom().nextBytes(clave);
        return clave;
    }

    private static String parte(Object json) {
        return BASE64_URL.encodeToString(JSON.writeValueAsBytes(json));
    }

    private static String hmac(String datos, byte[] clave) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave, "HmacSHA256"));
            return BASE64_URL.encodeToString(mac.doFinal(datos.getBytes(StandardCharsets.US_ASCII)));
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("No se pudo firmar el token de prueba", error);
        }
    }
}
