/*
 * Nombre: ConfiguracionJwtTest.java
 * Descripción: Pruebas unitarias de la clave de los tokens: sin una clave válida, el servidor no
 *              arranca (D-28, RNF-04).
 * Fecha de creación: 14/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas de la clave de los tokens ({@code INSPECTIA_JWT_CLAVE}, D-28). Si falta o no sirve, el
 * servidor no arranca y el mensaje dice dónde ver cómo crearla.
 */
class ConfiguracionJwtTest {

    /** 32 bytes en Base64: el mínimo para HS256. */
    private static final String CLAVE_DE_32_BYTES = "eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHg=";

    /** 31 bytes en Base64: le falta uno. */
    private static final String CLAVE_DE_31_BYTES = "eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eA==";

    @ParameterizedTest(name = "«{0}»")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "esto no es Base64", CLAVE_DE_31_BYTES})
    @DisplayName("RNF-04 · sin INSPECTIA_JWT_CLAVE, o si no está en Base64 o tiene menos de 32 bytes, el servidor no arranca")
    void unaClaveQueNoSirveDetieneElArranque(String clave) {
        assertThatThrownBy(() -> ConfiguracionJwt.clave(clave))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INSPECTIA_JWT_CLAVE")
                .hasMessageContaining("infra/.env.example");
    }

    @Test
    @DisplayName("RNF-04 · una clave de 32 bytes en Base64 sirve para firmar con HS256 (prueba de límite)")
    void unaClaveDe32BytesSirve() {
        assertThat(ConfiguracionJwt.clave(CLAVE_DE_32_BYTES).getEncoded()).hasSize(32);
    }
}
