/*
 * Nombre: EmisorDeTokens.java
 * Descripción: Emite el token JWT de una sesión: emisor, usuario y fechas, firmado con HS256 y con
 *              vigencia de 8 horas (RNF-04, D-28). Al cambiar la propia contraseña, emite el token con
 *              el que sigue la misma sesión (RF-18).
 * Fecha de creación: 14/10/2026
 * Actualización: 19/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.comun.seguridad;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Emite el token de una sesión (D-28). Solo lleva el emisor ({@code iss}), el identificador del
 * usuario ({@code sub}) y sus fechas ({@code iat} y {@code exp}): el rol y la empresa se leen de la
 * base en cada petición, para que un cambio surta efecto sin otro token.
 */
@Component
public class EmisorDeTokens {

    /** Emisor de los tokens: el servidor solo acepta los suyos. */
    public static final String EMISOR = "inspectia";

    /** Vigencia de una sesión (RNF-04). No se renueva: después hay que iniciar sesión de nuevo. */
    public static final Duration VIGENCIA = Duration.ofHours(8);

    private final JwtEncoder codificador;
    private final Clock reloj;

    /**
     * Crea el emisor.
     *
     * @param codificador firma los tokens con la clave del servidor
     * @param reloj la hora del servidor
     */
    public EmisorDeTokens(JwtEncoder codificador, Clock reloj) {
        this.codificador = codificador;
        this.reloj = reloj;
    }

    /**
     * Emite el token de una sesión que empieza ahora. Las fechas van en segundos enteros, como las
     * guarda el token, así {@code vence} coincide con su {@code exp}.
     *
     * @param usuario el identificador del usuario
     * @return el token y cuándo vence
     */
    public TokenEmitido emitir(UUID usuario) {
        Instant emitido = reloj.instant().truncatedTo(ChronoUnit.SECONDS);
        return emitir(usuario, emitido, emitido.plus(VIGENCIA));
    }

    /**
     * Emite el token con el que sigue una sesión ya abierta, con su hora de emisión y su vencimiento.
     * Al cambiar la propia contraseña, la sesión sigue con un token emitido en el segundo del cambio, que
     * vence cuando vencía el anterior: cambiar la contraseña no alarga la sesión (RF-18 C5, RNF-04 C4).
     * Las fechas van en segundos enteros, como las guarda el token.
     *
     * @param usuario el identificador del usuario
     * @param emitido la hora de emisión
     * @param vence cuándo vence
     * @return el token y cuándo vence
     */
    public TokenEmitido emitir(UUID usuario, Instant emitido, Instant vence) {
        Instant vencimiento = vence.truncatedTo(ChronoUnit.SECONDS);
        JwtClaimsSet contenido = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(usuario.toString())
                .issuedAt(emitido.truncatedTo(ChronoUnit.SECONDS))
                .expiresAt(vencimiento)
                .build();
        JwsHeader encabezado = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String token = codificador.encode(JwtEncoderParameters.from(encabezado, contenido)).getTokenValue();
        return new TokenEmitido(token, vencimiento);
    }

    /**
     * Un token recién emitido.
     *
     * @param token el token firmado
     * @param vence cuándo vence
     */
    public record TokenEmitido(String token, Instant vence) {

        /** No muestra el token si termina en un registro del servidor: con él se entra a la cuenta. */
        @Override
        public String toString() {
            return "TokenEmitido[token=****, vence=" + vence + "]";
        }
    }
}
