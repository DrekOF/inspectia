/*
 * Nombre: ReglasDeFotosTest.java
 * Descripción: Pruebas unitarias de las reglas de las fotos en el dominio, sin Spring (RF-05 C2 y C3): el tamaño, el
 *              formato por sus primeros bytes y el número de la foto.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Las reglas de las fotos (RF-05), en el dominio, sin la base ni la API. */
class ReglasDeFotosTest {

    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F'};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    @Test
    @DisplayName("RF-05 · C3 · una foto de 10.485.760 bytes pasa y una de 10.485.761 no (prueba de límite)")
    void hastaDiezMegas() {
        assertThat(ReglasDeFotos.revisarArchivo("image/jpeg", 10_485_760L)).isEqualTo(FormatoDeFoto.JPG);
        assertThatThrownBy(() -> ReglasDeFotos.revisarArchivo("image/jpeg", 10_485_761L))
                .isInstanceOf(FotoDemasiadoGrandeExcepcion.class);
    }

    @Test
    @DisplayName("RF-05 · C3 · solo se aceptan los tipos JPG y PNG, sin distinguir mayúsculas ni sus parámetros")
    void soloJpgYPng() {
        assertThat(ReglasDeFotos.revisarArchivo("image/png", 1)).isEqualTo(FormatoDeFoto.PNG);
        assertThat(ReglasDeFotos.revisarArchivo("IMAGE/JPEG; charset=binary", 1)).isEqualTo(FormatoDeFoto.JPG);
        for (String tipo : new String[] {"image/gif", "image/webp", "image/heic", "application/pdf", "", null}) {
            assertThatThrownBy(() -> ReglasDeFotos.revisarArchivo(tipo, 1)).as(String.valueOf(tipo))
                    .isInstanceOf(FormatoDeFotoNoPermitidoExcepcion.class);
        }
    }

    @Test
    @DisplayName("RF-05 · C3 · JPG y PNG se reconocen por sus primeros bytes; GIF, WebP, PDF, un archivo vacío o con el formato cambiado, no")
    void reconoceElContenido() {
        ReglasDeFotos.revisarContenido(FormatoDeFoto.JPG, JPG);
        ReglasDeFotos.revisarContenido(FormatoDeFoto.PNG, PNG);
        byte[][] otros = {"GIF89a\0\0".getBytes(StandardCharsets.US_ASCII),
            "RIFF\0\0\0\0".getBytes(StandardCharsets.US_ASCII), "%PDF-1.7".getBytes(StandardCharsets.US_ASCII),
            new byte[0], {(byte) 0xFF, (byte) 0xD8}, PNG};
        for (byte[] primeros : otros) {
            assertThatThrownBy(() -> ReglasDeFotos.revisarContenido(FormatoDeFoto.JPG, primeros))
                    .isInstanceOf(FormatoDeFotoNoPermitidoExcepcion.class);
        }
        assertThatThrownBy(() -> ReglasDeFotos.revisarContenido(FormatoDeFoto.PNG, JPG))
                .isInstanceOf(FormatoDeFotoNoPermitidoExcepcion.class);
    }

    @Test
    @DisplayName("RF-05 · C2 · el número de foto va de 1 a 3: el 0 y el 4 no (prueba de límite)")
    void numeroDeUnoATres() {
        assertThat(ReglasDeFotos.esNumeroValido(0)).isFalse();
        assertThat(ReglasDeFotos.esNumeroValido(1)).isTrue();
        assertThat(ReglasDeFotos.esNumeroValido(3)).isTrue();
        assertThat(ReglasDeFotos.esNumeroValido(4)).isFalse();
    }

    @Test
    @DisplayName("RF-05 · el formato de una extensión y su tipo: jpg es image/jpeg y png es image/png")
    void extensionYTipo() {
        assertThat(FormatoDeFoto.deExtension("jpg")).contains(FormatoDeFoto.JPG);
        assertThat(FormatoDeFoto.deExtension("png")).contains(FormatoDeFoto.PNG);
        assertThat(FormatoDeFoto.deExtension("jpeg")).isEmpty();
        assertThat(FormatoDeFoto.JPG.getTipo()).isEqualTo("image/jpeg");
        assertThat(FormatoDeFoto.PNG.getTipo()).isEqualTo("image/png");
    }
}
