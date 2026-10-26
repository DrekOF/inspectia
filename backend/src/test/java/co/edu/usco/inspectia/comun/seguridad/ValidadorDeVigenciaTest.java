/*
 * Nombre: ValidadorDeVigenciaTest.java
 * Descripción: Pruebas unitarias del vencimiento de los tokens: sirven hasta el segundo en que vencen,
 *              sin tolerancia, y deben traer sus fechas y su versión de sesión (RF-01, RNF-04, D-28).
 * Fecha de creación: 14/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia.comun.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/** Pruebas de {@link ValidadorDeVigencia} con un reloj fijo. */
class ValidadorDeVigenciaTest {

    private static final Instant VENCE = Instant.parse("2026-10-14T23:00:00Z");

    @ParameterizedTest(name = "{0} segundos antes de vencer: ¿sirve? {1}")
    @CsvSource({"1, true", "0, false", "-60, false"})
    @DisplayName("RF-01 · C6 · el token sirve hasta un segundo antes de vencer; desde el segundo en que vence, ya no (prueba de límite)")
    void sirveHastaQueVence(long segundosAntes, boolean sirve) {
        ValidadorDeVigencia validador = new ValidadorDeVigencia(reloj(VENCE.minusSeconds(segundosAntes)));

        OAuth2TokenValidatorResult resultado = validador.validate(token(VENCE));

        assertThat(resultado.hasErrors()).isEqualTo(!sirve);
        if (!sirve) {
            assertThat(resultado.getErrors()).extracting(OAuth2Error::getErrorCode)
                    .containsExactly(ValidadorDeVigencia.SESION_VENCIDA);
        }
    }

    @Test
    @DisplayName("RNF-04 · un token sin fecha de vencimiento no sirve, y no se informa como vencido")
    void unTokenSinVencimientoNoSirve() {
        ValidadorDeVigencia validador = new ValidadorDeVigencia(reloj(VENCE));
        Jwt token = Jwt.withTokenValue("token").header("alg", "HS256").subject("usuario")
                .issuedAt(VENCE.minus(Duration.ofHours(8))).build();

        OAuth2TokenValidatorResult resultado = validador.validate(token);

        assertThat(resultado.hasErrors()).isTrue();
        assertThat(resultado.getErrors()).extracting(OAuth2Error::getErrorCode)
                .doesNotContain(ValidadorDeVigencia.SESION_VENCIDA);
    }

    @Test
    @DisplayName("RNF-04 · un token sin fecha de emisión no sirve: todos los del servidor la traen (D-28)")
    void unTokenSinEmisionNoSirve() {
        ValidadorDeVigencia validador = new ValidadorDeVigencia(reloj(VENCE.minusSeconds(60)));
        Jwt token = Jwt.withTokenValue("token").header("alg", "HS256").subject("usuario").expiresAt(VENCE)
                .claim("version_sesion", 0).build();

        OAuth2TokenValidatorResult resultado = validador.validate(token);

        assertThat(resultado.hasErrors()).isTrue();
        assertThat(resultado.getErrors()).extracting(OAuth2Error::getErrorCode)
                .doesNotContain(ValidadorDeVigencia.SESION_VENCIDA);
    }

    @Test
    @DisplayName("RNF-04 · C6 · un token sin versión de sesión no sirve: sin ella no se sabe si es de antes de cambiar la contraseña")
    void unTokenSinVersionDeSesionNoSirve() {
        ValidadorDeVigencia validador = new ValidadorDeVigencia(reloj(VENCE.minusSeconds(60)));
        Jwt token = Jwt.withTokenValue("token").header("alg", "HS256").subject("usuario")
                .issuedAt(VENCE.minus(Duration.ofHours(8))).expiresAt(VENCE).build();

        OAuth2TokenValidatorResult resultado = validador.validate(token);

        assertThat(resultado.hasErrors()).isTrue();
        assertThat(resultado.getErrors()).extracting(OAuth2Error::getErrorCode)
                .doesNotContain(ValidadorDeVigencia.SESION_VENCIDA);
    }

    private static Jwt token(Instant vence) {
        return Jwt.withTokenValue("token").header("alg", "HS256").subject("usuario")
                .issuedAt(vence.minus(Duration.ofHours(8))).expiresAt(vence).claim("version_sesion", 0).build();
    }

    private static Clock reloj(Instant ahora) {
        return Clock.fixed(ahora, ZoneOffset.UTC);
    }
}
