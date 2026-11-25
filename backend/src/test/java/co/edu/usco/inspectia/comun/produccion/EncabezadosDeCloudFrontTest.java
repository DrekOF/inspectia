/*
 * Nombre: EncabezadosDeCloudFrontTest.java
 * Descripción: Pruebas unitarias del filtro de los encabezados de CloudFront (D-16): la IP del cliente sale de
 *              CloudFront-Viewer-Address y el protocolo de CloudFront-Forwarded-Proto; X-Forwarded-For no se cree y un
 *              valor que no se entiende se ignora.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.produccion;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** El filtro de los encabezados de CloudFront (D-16). */
class EncabezadosDeCloudFrontTest {

    /** Una petición como la que llega de CloudFront a la instancia: por HTTP y desde una IP de CloudFront. */
    private static MockHttpServletRequest deCloudFront() {
        MockHttpServletRequest peticion = new MockHttpServletRequest("POST", "/api/v1/empresas");
        peticion.setScheme("http");
        peticion.setServerName("d111111abcdef8.cloudfront.net");
        peticion.setServerPort(80);
        peticion.setRemoteAddr("130.176.0.10");
        return peticion;
    }

    private static HttpServletRequest filtrar(MockHttpServletRequest peticion) throws Exception {
        MockFilterChain cadena = new MockFilterChain();
        new EncabezadosDeCloudFront().doFilter(peticion, new MockHttpServletResponse(), cadena);
        return (HttpServletRequest) cadena.getRequest();
    }

    @Test
    @DisplayName("D-16 · la IP del cliente sale de CloudFront-Viewer-Address y el protocolo de CloudFront-Forwarded-Proto: las direcciones que arma el servidor son https y sin el puerto de la instancia")
    void ipYProtocoloDelCliente() throws Exception {
        MockHttpServletRequest peticion = deCloudFront();
        peticion.addHeader("CloudFront-Viewer-Address", "198.51.100.10:46532");
        peticion.addHeader("CloudFront-Forwarded-Proto", "https");
        peticion.addHeader("X-Forwarded-For", "203.0.113.99, 198.51.100.10");

        HttpServletRequest filtrada = filtrar(peticion);

        assertThat(filtrada.getRemoteAddr()).isEqualTo("198.51.100.10");
        assertThat(filtrada.getRemoteHost()).isEqualTo("198.51.100.10");
        assertThat(filtrada.getScheme()).isEqualTo("https");
        assertThat(filtrada.isSecure()).isTrue();
        assertThat(filtrada.getServerPort()).isEqualTo(443);
        assertThat(filtrada.getRequestURL()).hasToString("https://d111111abcdef8.cloudfront.net/api/v1/empresas");
        assertThat(ServletUriComponentsBuilder.fromContextPath(filtrada).path("/api/v1/empresas/{id}")
                .buildAndExpand("e1").toUriString()).isEqualTo("https://d111111abcdef8.cloudfront.net/api/v1/empresas/e1");
    }

    @Test
    @DisplayName("D-16 · una IPv6 de CloudFront-Viewer-Address se lee sin su puerto")
    void ipv6() {
        assertThat(EncabezadosDeCloudFront.ipDe("2001:db8::8a2e:370:7334:46532")).isEqualTo("2001:db8:0:0:0:8a2e:370:7334");
        assertThat(EncabezadosDeCloudFront.ipDe(" 198.51.100.10:443 ")).isEqualTo("198.51.100.10");
    }

    @Test
    @DisplayName("D-16 · sin los encabezados de CloudFront, o con valores que no se entienden, la petición queda como llegó; X-Forwarded-For sola no cambia la IP")
    void sinEncabezadosNoCambia() throws Exception {
        MockHttpServletRequest peticion = deCloudFront();
        peticion.addHeader("X-Forwarded-For", "203.0.113.99");
        peticion.addHeader("X-Forwarded-Proto", "https");

        HttpServletRequest filtrada = filtrar(peticion);

        assertThat(filtrada).isSameAs(peticion);
        assertThat(filtrada.getRemoteAddr()).isEqualTo("130.176.0.10");
        for (String raro : new String[] {"evil.example.com:80", "198.51.100.10", "198.51.100.10:", "<script>:80",
            "999.1.1.1:80", "", "198.51.100.10:443, 1.2.3.4:5"}) {
            assertThat(EncabezadosDeCloudFront.ipDe(raro)).as(raro).isNull();
        }
        assertThat(EncabezadosDeCloudFront.protocoloDe("ftp")).isNull();
        assertThat(EncabezadosDeCloudFront.protocoloDe("HTTPS")).isEqualTo("https");
        assertThat(EncabezadosDeCloudFront.protocoloDe(null)).isNull();
    }

    @Test
    @DisplayName("D-16 · con solo el protocolo http, la petición queda en http y el puerto 80; la IP no cambia")
    void soloElProtocolo() throws Exception {
        MockHttpServletRequest peticion = deCloudFront();
        peticion.setServerPort(8080);
        peticion.addHeader("CloudFront-Forwarded-Proto", "http");

        HttpServletRequest filtrada = filtrar(peticion);

        assertThat(filtrada.getRemoteAddr()).isEqualTo("130.176.0.10");
        assertThat(filtrada.isSecure()).isFalse();
        assertThat(filtrada.getServerPort()).isEqualTo(80);
        assertThat(filtrada.getRequestURL()).hasToString("http://d111111abcdef8.cloudfront.net/api/v1/empresas");
    }
}
