/*
 * Nombre: FirmaDeEnlaces.java
 * Descripción: Firma y revisa los enlaces temporales del almacén en carpeta, con HMAC-SHA256 y una clave al azar que
 *              solo vive en la memoria del servidor (D-41).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.carpeta;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * La firma de los enlaces temporales del almacén en carpeta (D-41), como la de los enlaces prefirmados de S3. Firma con
 * HMAC-SHA256 un texto con el método, la llave, el vencimiento y, al subir, el tipo y el tamaño: así un enlace de vista
 * no sirve para subir, y uno de subida solo sirve para ese archivo. La clave es al azar y solo vive en la memoria del
 * servidor: si el servidor se reinicia, los enlaces que ya entregó dejan de servir y la página pide otros.
 *
 * <p>La firma se compara en tiempo constante, y el enlace vence en el segundo de {@code vence}, sin tolerancia.
 */
public final class FirmaDeEnlaces {

    /** Versión del texto que se firma: si cambia su forma, las firmas viejas dejan de servir. */
    static final String VERSION = "inspectia-enlace-v1";

    private static final String ALGORITMO = "HmacSHA256";

    private final byte[] clave;
    private final Clock reloj;

    /**
     * Crea la firma.
     *
     * @param clave la clave, de al menos 32 bytes
     * @param reloj el reloj de la aplicación, para los vencimientos
     */
    public FirmaDeEnlaces(byte[] clave, Clock reloj) {
        if (clave.length < 32) {
            throw new IllegalArgumentException("La clave de los enlaces debe tener al menos 32 bytes");
        }
        this.clave = clave.clone();
        this.reloj = reloj;
    }

    /**
     * Una clave al azar de 32 bytes, que no se guarda en ninguna parte (D-41).
     *
     * @return la clave
     */
    public static byte[] claveAlAzar() {
        byte[] clave = new byte[32];
        new SecureRandom().nextBytes(clave);
        return clave;
    }

    /**
     * La firma de un enlace de subida.
     *
     * @param llave la llave del archivo
     * @param vence cuándo vence, en segundos desde 1970 (UTC)
     * @param tipo el tipo del contenido que hay que enviar
     * @param bytes cuánto pesa el archivo
     * @return la firma, en Base64 para URL y sin relleno
     */
    public String firmarSubida(String llave, long vence, String tipo, long bytes) {
        return enBase64(hmac("PUT", llave, vence, tipo, String.valueOf(bytes)));
    }

    /**
     * La firma de un enlace de vista.
     *
     * @param llave la llave del archivo
     * @param vence cuándo vence, en segundos desde 1970 (UTC)
     * @return la firma, en Base64 para URL y sin relleno
     */
    public String firmarVista(String llave, long vence) {
        return enBase64(hmac("GET", llave, vence, "", ""));
    }

    /**
     * Si una subida trae una firma válida y vigente para esa llave, ese tipo y ese tamaño.
     *
     * @param llave la llave del enlace
     * @param vence el vencimiento del enlace
     * @param tipo el tipo del contenido que llegó
     * @param bytes cuánto dice que pesa lo que llegó
     * @param firma la firma del enlace; puede ser {@code null}
     * @return {@code true} si la firma coincide y el enlace no venció
     */
    public boolean esSubidaValida(String llave, long vence, String tipo, long bytes, String firma) {
        return vigente(vence) && coincide(hmac("PUT", llave, vence, tipo == null ? "" : tipo, String.valueOf(bytes)),
                firma);
    }

    /**
     * Si una vista trae una firma válida y vigente para esa llave.
     *
     * @param llave la llave del enlace
     * @param vence el vencimiento del enlace
     * @param firma la firma del enlace; puede ser {@code null}
     * @return {@code true} si la firma coincide y el enlace no venció
     */
    public boolean esVistaValida(String llave, long vence, String firma) {
        return vigente(vence) && coincide(hmac("GET", llave, vence, "", ""), firma);
    }

    /** El enlace sirve hasta el segundo anterior a {@code vence}. */
    private boolean vigente(long vence) {
        return reloj.instant().getEpochSecond() < vence;
    }

    private static String enBase64(byte[] firma) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(firma);
    }

    private static boolean coincide(byte[] esperada, String firma) {
        if (firma == null) {
            return false;
        }
        try {
            return MessageDigest.isEqual(esperada, Base64.getUrlDecoder().decode(firma));
        } catch (IllegalArgumentException noEsBase64) {
            return false;
        }
    }

    /**
     * El HMAC del texto canónico del enlace. Sus partes van en líneas, que no pueden aparecer en una llave, un tipo o
     * un número: así no se pueden mover datos de una parte a otra.
     */
    private byte[] hmac(String metodo, String llave, long vence, String tipo, String bytes) {
        String texto = String.join("\n", VERSION, metodo, llave, String.valueOf(vence), tipo, bytes);
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(clave, ALGORITMO));
            return mac.doFinal(texto.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException excepcion) {
            throw new IllegalStateException("La JVM no tiene " + ALGORITMO, excepcion);
        }
    }
}
