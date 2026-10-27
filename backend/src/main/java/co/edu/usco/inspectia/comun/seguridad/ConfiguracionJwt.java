/*
 * Nombre: ConfiguracionJwt.java
 * Descripción: Clave, firma y validación de los tokens JWT del inicio de sesión (D-28, RNF-04).
 * Fecha de creación: 14/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.comun.seguridad;

import java.time.Clock;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Los tokens del inicio de sesión (D-28): se firman con HS256 y una clave que sale de
 * {@code INSPECTIA_JWT_CLAVE}. Sin esa clave, o si no sirve, el servidor no arranca.
 *
 * <p>Al recibir un token se revisa:
 * <ul>
 *   <li>La firma, solo con HS256: un token sin firma ({@code alg: none}) o de otro algoritmo no sirve.</li>
 *   <li>El emisor: {@value EmisorDeTokens#EMISOR}.</li>
 *   <li>Que traiga sus fechas y su versión de sesión, y que no haya vencido, sin tolerancia
 *       ({@link ValidadorDeVigencia}).</li>
 * </ul>
 * El usuario, su rol y su empresa se leen de la base en cada petición ({@link CargadorDeSesion}).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PropiedadesJwt.class)
public class ConfiguracionJwt {

    /** Bytes mínimos de la clave: HS256 pide al menos 256 bits. */
    static final int BYTES_MINIMOS = 32;

    private static final String COMO_CREARLA = " Créala como dice infra/.env.example.";

    /**
     * Firma los tokens que entrega el inicio de sesión.
     *
     * @param propiedades la clave
     * @return el codificador
     */
    @Bean
    JwtEncoder codificadorDeTokens(PropiedadesJwt propiedades) {
        return NimbusJwtEncoder.withSecretKey(clave(propiedades.clave())).algorithm(MacAlgorithm.HS256).build();
    }

    /**
     * Revisa los tokens que llegan en el encabezado {@code Authorization: Bearer}.
     *
     * @param propiedades la clave
     * @param reloj la hora del servidor
     * @return el decodificador
     */
    @Bean
    JwtDecoder decodificadorDeTokens(PropiedadesJwt propiedades, Clock reloj) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(clave(propiedades.clave()))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decodificador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtIssuerValidator(EmisorDeTokens.EMISOR), new ValidadorDeVigencia(reloj)));
        return decodificador;
    }

    /**
     * Arma la clave HS256 desde su valor en Base64.
     *
     * @param base64 el valor de {@code INSPECTIA_JWT_CLAVE}
     * @return la clave
     * @throws IllegalStateException si falta, si no está en Base64 o si tiene menos de 32 bytes
     */
    static SecretKey clave(String base64) {
        if (base64 == null || base64.isBlank()) {
            throw new IllegalStateException("Falta INSPECTIA_JWT_CLAVE, la clave de los tokens." + COMO_CREARLA);
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64.strip());
        } catch (IllegalArgumentException error) {
            throw new IllegalStateException("INSPECTIA_JWT_CLAVE no está en Base64." + COMO_CREARLA, error);
        }
        if (bytes.length < BYTES_MINIMOS) {
            throw new IllegalStateException("INSPECTIA_JWT_CLAVE tiene " + bytes.length + " bytes y necesita al menos "
                    + BYTES_MINIMOS + "." + COMO_CREARLA);
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
