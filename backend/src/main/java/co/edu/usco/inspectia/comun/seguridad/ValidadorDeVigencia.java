/*
 * Nombre: ValidadorDeVigencia.java
 * Descripción: Revisa que un token traiga sus fechas y una versión de sesión válida, y que no haya vencido,
 *              sin tolerancia: la sesión dura 8 horas exactas (RF-01, RNF-04, D-28).
 * Fecha de creación: 14/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia.comun.seguridad;

import java.time.Clock;
import java.time.Instant;
import java.util.OptionalInt;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Revisa que un token no haya vencido: sirve hasta el segundo anterior a su {@code exp}. A diferencia
 * del validador de Spring, no da un minuto de tolerancia, porque la sesión dura 8 horas exactas
 * (RNF-04). El servidor es el único que emite y revisa los tokens, así que no hay relojes que ajustar.
 *
 * <p>Un token vencido se marca con el código {@value #SESION_VENCIDA}, para responder «Tu sesión
 * venció» y no «La sesión no es válida».
 *
 * <p>También exige la fecha de emisión ({@code iat}), que los tokens del servidor siempre traen (D-28), y la
 * versión de sesión ({@value EmisorDeTokens#VERSION_SESION}), un entero de 0 a 2147483647: con ella se sabe
 * si el token es de una sesión abierta antes de cambiar la contraseña (RNF-04 C6).
 */
public class ValidadorDeVigencia implements OAuth2TokenValidator<Jwt> {

    /** Código del error de un token vencido. */
    public static final String SESION_VENCIDA = "sesion_vencida";

    private final Clock reloj;

    /**
     * Crea el validador.
     *
     * @param reloj la hora del servidor
     */
    public ValidadorDeVigencia(Clock reloj) {
        this.reloj = reloj;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Instant vence = token.getExpiresAt();
        if (vence == null) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "El token no trae su fecha de vencimiento", null));
        }
        if (token.getIssuedAt() == null) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "El token no trae su fecha de emisión", null));
        }
        if (versionDeSesion(token).isEmpty()) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN,
                    "El token no trae su versión de sesión, o no es un entero de 0 a 2147483647", null));
        }
        if (!reloj.instant().isBefore(vence)) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(SESION_VENCIDA, "La sesión venció en " + vence, null));
        }
        return OAuth2TokenValidatorResult.success();
    }

    /**
     * La versión de sesión de un token, si es válida: un entero de 0 a 2147483647, el rango de la columna
     * {@code usuario.version_sesion}. Al leer el token, Nimbus deja los enteros en {@code Long}. Un decimal o un
     * texto no es una versión, y una mayor que el rango nunca coincidiría con la de la base y, al pasarla a
     * {@code int}, se desbordaría. Es la única definición: la usa también {@link SeguridadConfiguracion}.
     *
     * @param token el token
     * @return la versión, o vacío si el token no la trae o no es válida
     */
    static OptionalInt versionDeSesion(Jwt token) {
        if (token.getClaims().get(EmisorDeTokens.VERSION_SESION) instanceof Number version
                && (version instanceof Integer || version instanceof Long)
                && version.longValue() >= 0 && version.longValue() <= Integer.MAX_VALUE) {
            return OptionalInt.of(version.intValue());
        }
        return OptionalInt.empty();
    }
}
