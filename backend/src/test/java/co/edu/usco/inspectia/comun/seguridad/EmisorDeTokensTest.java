/*
 * Nombre: EmisorDeTokensTest.java
 * Descripción: Pruebas unitarias de la emisión de los tokens: firma HS256, contenido y vigencia de 8
 *              horas (RNF-04, D-28), y el token con el que sigue una sesión al cambiar la contraseña
 *              (RF-18).
 * Fecha de creación: 14/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia.comun.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Pruebas de {@link EmisorDeTokens} con un reloj fijo y una clave de prueba. */
class EmisorDeTokensTest {

    private static final SecretKey CLAVE =
            new SecretKeySpec("x".repeat(32).getBytes(StandardCharsets.US_ASCII), "HmacSHA256");

    @Test
    @DisplayName("RNF-04 · C4 · el token vence 8 horas después de emitido, en segundos enteros, y solo lleva el emisor, el usuario, las fechas y la versión de sesión")
    void emiteUnTokenDeOchoHoras() {
        Instant ahora = Instant.parse("2026-10-14T15:20:30.750Z");
        EmisorDeTokens emisor = new EmisorDeTokens(
                NimbusJwtEncoder.withSecretKey(CLAVE).algorithm(MacAlgorithm.HS256).build(),
                Clock.fixed(ahora, ZoneOffset.UTC));
        UUID usuario = UUID.fromString("0199e2a0-0000-7000-8000-000000000001");

        EmisorDeTokens.TokenEmitido emitido = emisor.emitir(usuario, 3);

        Jwt token = leer(emitido.token());
        assertThat(token.getHeaders()).containsEntry("alg", "HS256");
        assertThat(token.getClaims()).containsOnlyKeys("iss", "sub", "iat", "exp", "version_sesion");
        assertThat(token.getClaimAsString("iss")).isEqualTo(EmisorDeTokens.EMISOR);
        assertThat(token.getSubject()).isEqualTo(usuario.toString());
        assertThat(token.getIssuedAt()).isEqualTo(Instant.parse("2026-10-14T15:20:30Z"));
        assertThat(token.getExpiresAt()).isEqualTo(Instant.parse("2026-10-14T23:20:30Z"));
        assertThat(token.<Number>getClaim("version_sesion").intValue()).isEqualTo(3);
        assertThat(emitido.vence()).isEqualTo(token.getExpiresAt());
    }

    @Test
    @DisplayName("RF-18 · C5 · el token con el que sigue una sesión lleva la hora de emisión y el vencimiento dados, en segundos enteros, sin alargar la sesión, y su versión de sesión")
    void emiteElTokenDeUnaSesionQueSigue() {
        EmisorDeTokens emisor = new EmisorDeTokens(
                NimbusJwtEncoder.withSecretKey(CLAVE).algorithm(MacAlgorithm.HS256).build(),
                Clock.fixed(Instant.parse("2026-10-14T18:00:00Z"), ZoneOffset.UTC));
        UUID usuario = UUID.fromString("0199e2a0-0000-7000-8000-000000000001");

        EmisorDeTokens.TokenEmitido emitido = emisor.emitir(usuario, 4, Instant.parse("2026-10-14T17:30:05.400Z"),
                Instant.parse("2026-10-14T23:20:30Z"));

        Jwt token = leer(emitido.token());
        assertThat(token.getClaims()).containsOnlyKeys("iss", "sub", "iat", "exp", "version_sesion");
        assertThat(token.getSubject()).isEqualTo(usuario.toString());
        assertThat(token.getIssuedAt()).isEqualTo(Instant.parse("2026-10-14T17:30:05Z"));
        assertThat(token.getExpiresAt()).isEqualTo(Instant.parse("2026-10-14T23:20:30Z"));
        assertThat(token.<Number>getClaim("version_sesion").intValue()).isEqualTo(4);
        assertThat(emitido.vence()).isEqualTo(token.getExpiresAt());
    }

    /** Lee el token revisando solo la firma: la fecha de la prueba es fija y para el reloj real ya pasó. */
    private static Jwt leer(String token) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(CLAVE).macAlgorithm(MacAlgorithm.HS256).build();
        decodificador.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());
        return decodificador.decode(token);
    }
}
