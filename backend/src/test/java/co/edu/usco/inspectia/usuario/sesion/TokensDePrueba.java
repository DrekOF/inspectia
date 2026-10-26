/*
 * Nombre: TokensDePrueba.java
 * Descripción: Arma tokens JWT para las pruebas de sesión sin usar el código del backend: válidos,
 *              vencidos, alterados, sin firma o firmados con otra clave, con la versión de sesión que se
 *              pida (RNF-04, D-28).
 * Fecha de creación: 14/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
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
 * backend los emite. Siguen D-28: firma HS256 y, en el contenido, {@code iss}, {@code sub}, {@code iat},
 * {@code exp} y la versión de sesión del usuario ({@code version_sesion}), que sube cada vez que cambian su
 * contraseña: un token con la versión de antes es de una sesión que ya no sirve (RNF-04 C6).
 */
public final class TokensDePrueba {

    /** Emisor que el backend pone en sus tokens y exige en los que recibe. */
    public static final String EMISOR = "inspectia";

    /** Vigencia de una sesión (RNF-04). */
    public static final Duration VIGENCIA = Duration.ofHours(8);

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

    private TokensDePrueba() {
    }

    /**
     * Un token como los del backend, emitido en {@code emitido} con la versión de sesión 0, la de un usuario que
     * nunca cambió su contraseña, firmado con la clave de las pruebas.
     */
    public static String token(UUID usuario, Instant emitido) {
        return token(usuario, emitido, 0);
    }

    /** Un token como los del backend, emitido en {@code emitido} con la versión de sesión dada. */
    public static String token(UUID usuario, Instant emitido, int versionSesion) {
        Map<String, Object> contenido = contenido(EMISOR, usuario, emitido);
        contenido.put("version_sesion", versionSesion);
        return firmar(contenido, clave());
    }

    /** El contenido de un token: emisor, usuario, fechas y la versión de sesión 0. */
    public static Map<String, Object> contenido(String emisor, UUID usuario, Instant emitido) {
        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("iss", emisor);
        contenido.put("sub", usuario.toString());
        contenido.put("iat", emitido.getEpochSecond());
        contenido.put("exp", emitido.plus(VIGENCIA).getEpochSecond());
        contenido.put("version_sesion", 0);
        return contenido;
    }

    /** Firma el contenido con HS256 y la clave dada. */
    public static String firmar(Map<String, Object> contenido, byte[] clave) {
        String datos = parte(Map.of("alg", "HS256", "typ", "JWT")) + "." + parte(contenido);
        return datos + "." + hmac(datos, clave);
    }

    /** Un token con {@code alg: none}: sin firma. */
    public static String sinFirma(Map<String, Object> contenido) {
        return parte(Map.of("alg", "none", "typ", "JWT")) + "." + parte(contenido) + ".";
    }

    /** El mismo token con otro contenido y la firma original: un token alterado. */
    public static String alterado(String token, Map<String, Object> contenido) {
        String[] partes = token.split("\\.");
        return partes[0] + "." + parte(contenido) + "." + partes[2];
    }

    /** El contenido de un token, sin revisar la firma. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> leer(String token) {
        return JSON.readValue(Base64.getUrlDecoder().decode(token.split("\\.")[1]), Map.class);
    }

    /** La clave de esta corrida, la misma que usa el backend. */
    public static byte[] clave() {
        return Base64.getDecoder().decode(ConfiguracionContenedores.CLAVE_JWT);
    }

    /** Una clave distinta de la del backend. */
    public static byte[] otraClave() {
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
