/*
 * Nombre: EnlacesFirmadosIT.java
 * Descripción: Pruebas de los enlaces firmados de las fotos en la carpeta local (D-41, RP-12): no piden sesión, la
 *              firma ata el método, el tipo y el tamaño, sin firma válida responden 403, una foto subida no se pisa y
 *              la foto se sirve como imagen.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis;

import static co.edu.usco.inspectia.analisis.ApiFotos.ENLACE_NO_VALIDO;
import static co.edu.usco.inspectia.analisis.ApiFotos.JPG;
import static co.edu.usco.inspectia.analisis.ApiFotos.PNG;
import static co.edu.usco.inspectia.analisis.ApiFotos.YA_SUBIDA;
import static co.edu.usco.inspectia.analisis.ApiFotos.jpg;
import static co.edu.usco.inspectia.analisis.ApiFotos.vista;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.ConfiguracionContenedores;
import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Los enlaces firmados de la carpeta local (D-41): hacen las veces de los de S3. No piden sesión, porque un
 * {@code <img>} no manda el token, y los autoriza su firma. Sin firma, con una firma que no coincide o vencida,
 * responden 403 y no 401: la página no debe cerrar la sesión por un enlace vencido. El vencimiento se prueba en
 * {@code FirmaDeEnlacesTest}, con el reloj fijo.
 */
@PruebaIntegracion
class EnlacesFirmadosIT {

    private static final String LUIS = "luis@cacaos.test";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiFotos fotos;
    private String token;
    private UUID lote;

    @BeforeEach
    void preparar() {
        ApiSesion api = new ApiSesion(mvc, jdbc);
        fotos = new ApiFotos(api, mvc);
        ApiSesion.borrarDatos(jdbc);
        UUID empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");
        token = api.token(LUIS);
        lote = new ApiLotes(api).registrarOk(token, "L-001");
    }

    @AfterEach
    void limpiar() {
        ApiSesion.borrarDatos(jdbc);
        ConfiguracionContenedores.vaciarCarpetaDeFotos();
    }

    @Test
    @DisplayName("D-41 · el enlace de subida es relativo a la página y la foto se sube sin el token, con el tipo que se pidió")
    void subeSinToken() {
        byte[] contenido = jpg();
        Map<String, Object> subida = fotos.subida(token, lote, 1, JPG, contenido.length);

        MvcTestResult resultado = fotos.subir((String) subida.get("url"), JPG, contenido);

        assertThat((String) subida.get("url")).startsWith(ApiFotos.ARCHIVOS + "/");
        assertThat(subida).containsEntry("metodo", "PUT").containsEntry("encabezados", Map.of("Content-Type", JPG));
        assertThat(resultado).hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("D-41 · el enlace de vista no sirve para subir: un PUT con la firma de un GET responde 403 y no escribe nada")
    void laVistaNoSirveParaSubir() {
        Map<String, Object> foto = fotos.subirFoto(token, lote, 1, JPG, jpg());
        byte[] original = fotos.ver(vista(foto)).getResponse().getContentAsByteArray();

        MvcTestResult resultado = fotos.subir(vista(foto), JPG, ApiFotos.png());

        assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(json(resultado)).containsEntry("detail", ENLACE_NO_VALIDO);
        assertThat(fotos.ver(vista(foto)).getResponse().getContentAsByteArray()).isEqualTo(original);
    }

    @Test
    @DisplayName("D-41 · un PUT con otro tamaño o con otro tipo del que se firmó responde 403 y no guarda nada")
    void laFirmaAtaElTipoYElTamano() {
        byte[] contenido = jpg();
        Map<String, Object> subida = fotos.subida(token, lote, 1, JPG, contenido.length);
        String url = (String) subida.get("url");

        MvcTestResult otroTamano = fotos.subir(url, JPG, Arrays.copyOf(contenido, contenido.length + 1));
        MvcTestResult otroTipo = fotos.subir(url, PNG, contenido);

        assertThat(otroTamano).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(otroTipo).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(fotos.confirmar(token, lote, 1, (String) subida.get("llave"))).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("D-41 · una foto ya subida no se puede pisar con el mismo enlace: responde 409 y queda la primera")
    void noSePisa() {
        byte[] contenido = jpg();
        Map<String, Object> subida = fotos.subida(token, lote, 1, JPG, contenido.length);
        String url = (String) subida.get("url");
        fotos.subir(url, JPG, contenido);
        byte[] otro = jpg();
        otro[otro.length - 3] ^= 0x01;

        MvcTestResult resultado = fotos.subir(url, JPG, otro);

        assertThat(resultado).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(resultado)).containsEntry("detail", YA_SUBIDA);
        String llave = (String) subida.get("llave");
        assertThat(ConfiguracionContenedores.CARPETA_DE_FOTOS.resolve(llave)).hasBinaryContent(contenido);
    }

    @Test
    @DisplayName("D-41 · sin firma, con la firma cambiada o con el vencimiento cambiado, el archivo responde 403 y no 401")
    void sinFirmaValidaResponde403() {
        Map<String, Object> foto = fotos.subirFoto(token, lote, 1, JPG, jpg());
        String url = vista(foto);
        String sinFirma = url.substring(0, url.indexOf('?'));
        String firmaCambiada = url.substring(0, url.length() - 2) + (url.endsWith("AA") ? "BB" : "AA");
        String vence = url.replaceFirst("vence=(\\d+)", "vence=$19");

        Stream.of(sinFirma, firmaCambiada, vence).forEach(enlace -> {
            MvcTestResult resultado = fotos.ver(enlace);
            assertThat(resultado).as(enlace).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(json(resultado)).containsEntry("detail", ENLACE_NO_VALIDO);
        });
    }

    @Test
    @DisplayName("D-41 · la foto se sirve como imagen, con el tipo de su formato, nosniff y caché privada, sin pedir sesión")
    void seSirveComoImagen() {
        byte[] contenido = jpg();
        Map<String, Object> foto = fotos.subirFoto(token, lote, 1, JPG, contenido);

        MvcTestResult resultado = fotos.ver(vista(foto));

        assertThat(resultado).hasStatus(HttpStatus.OK).hasContentType(JPG)
                .hasHeader("X-Content-Type-Options", "nosniff");
        assertThat(resultado.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).startsWith("private, max-age=");
        assertThat(resultado.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).isEqualTo("inline");
        assertThat(resultado.getResponse().getContentAsByteArray()).isEqualTo(contenido);
    }

    @Test
    @DisplayName("D-41 · un token vencido o que no sirve en el encabezado no impide subir ni ver con el enlace firmado")
    void unTokenViejoNoEstorba() {
        byte[] contenido = jpg();
        Map<String, Object> subida = fotos.subida(token, lote, 1, JPG, contenido.length);

        MvcTestResult subir = mvc.put().uri((String) subida.get("url")).contentType(JPG).content(contenido)
                .header(HttpHeaders.AUTHORIZATION, "Bearer token-que-no-sirve").exchange();
        Map<String, Object> foto = json(fotos.confirmar(token, lote, 1, (String) subida.get("llave")));
        MvcTestResult ver = mvc.get().uri(vista(foto)).header(HttpHeaders.AUTHORIZATION, "Bearer token-que-no-sirve")
                .exchange();

        assertThat(subir).hasStatus(HttpStatus.OK);
        assertThat(ver).hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("D-41 · un enlace de una foto que se borró responde 404")
    void fotoBorradaNoExiste() throws Exception {
        Map<String, Object> foto = fotos.subirFoto(token, lote, 1, JPG, jpg());
        String llave = jdbc.queryForObject("SELECT ruta FROM foto", String.class);
        Files.delete(ConfiguracionContenedores.CARPETA_DE_FOTOS.resolve(llave));

        MvcTestResult resultado = fotos.ver(vista(foto));

        assertThat(resultado).hasStatus(HttpStatus.NOT_FOUND);
    }
}
