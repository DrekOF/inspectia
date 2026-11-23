/*
 * Nombre: FotosPorEmpresaIT.java
 * Descripción: Pruebas de la separación entre empresas en las fotos (RNF-03, RP-12): las fotos de un lote de otra
 *              empresa no existen, un enlace solo abre la foto para la que se firmó y una llave de otra empresa o de
 *              otro lote no se puede confirmar.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis;

import static co.edu.usco.inspectia.analisis.ApiFotos.ENLACE_NO_VALIDO;
import static co.edu.usco.inspectia.analisis.ApiFotos.JPG;
import static co.edu.usco.inspectia.analisis.ApiFotos.jpg;
import static co.edu.usco.inspectia.analisis.ApiFotos.vista;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.error;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.errores;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.ConfiguracionContenedores;
import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Las fotos son datos de la empresa (RNF-03). Luis Gómez sube las de su lote en Cacaos del Huila, y Marta Ruiz, de
 * Cacao del Sur, intenta llegar a ellas.
 */
@PruebaIntegracion
class FotosPorEmpresaIT {

    private static final String LUIS = "luis@cacaos.test";
    private static final String MARTA = "marta@cacaosur.test";
    private static final String NO_EXISTE = "No existe un registro con ese identificador.";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiFotos fotos;
    private String tokenDeLuis;
    private String tokenDeMarta;
    private UUID loteDeLuis;
    private UUID loteDeMarta;

    @BeforeEach
    void preparar() {
        ApiSesion api = new ApiSesion(mvc, jdbc);
        fotos = new ApiFotos(api, mvc);
        ApiSesion.borrarDatos(jdbc);
        UUID empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");
        UUID otraEmpresa = api.crearEmpresa("Cacao del Sur", "800765432-1");
        api.crearUsuario(otraEmpresa, "Marta Ruiz", MARTA, "USUARIO_FINAL");
        tokenDeLuis = api.token(LUIS);
        tokenDeMarta = api.token(MARTA);
        ApiLotes lotes = new ApiLotes(api);
        loteDeLuis = lotes.registrarOk(tokenDeLuis, "L-001");
        loteDeMarta = lotes.registrarOk(tokenDeMarta, "S-001");
    }

    @AfterEach
    void limpiar() {
        ApiSesion.borrarDatos(jdbc);
        ConfiguracionContenedores.vaciarCarpetaDeFotos();
    }

    @Test
    @DisplayName("RNF-03 · C1 · las fotos de un lote de otra empresa no existen: listar, pedir un enlace y confirmar responden 404")
    void otraEmpresaNoExiste() {
        Map<String, Object> subida = fotos.subida(tokenDeLuis, loteDeLuis, 1, JPG, 1_000);

        MvcTestResult lista = fotos.listar(tokenDeMarta, loteDeLuis);
        MvcTestResult enlace = fotos.pedirSubida(tokenDeMarta, loteDeLuis, 1, JPG, 1_000);
        MvcTestResult confirmacion = fotos.confirmar(tokenDeMarta, loteDeLuis, 1, (String) subida.get("llave"));

        for (MvcTestResult resultado : new MvcTestResult[] {lista, enlace, confirmacion}) {
            assertThat(resultado).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(json(resultado)).containsEntry("detail", NO_EXISTE);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM muestra", Integer.class)).isZero();
    }

    @Test
    @DisplayName("RNF-03 · C3 · un enlace solo abre la foto para la que se firmó: con la firma de una foto y la llave de otra, responde 403")
    void unEnlaceSoloAbreSuFoto() {
        Map<String, Object> deLuis = fotos.subirFoto(tokenDeLuis, loteDeLuis, 1, JPG, jpg());
        Map<String, Object> deMarta = fotos.subirFoto(tokenDeMarta, loteDeMarta, 1, JPG, jpg());
        String enlaceDeMarta = vista(deMarta);
        String llaveDeLuis = jdbc.queryForObject("SELECT ruta FROM foto f JOIN muestra m ON m.id = f.muestra_id "
                + "WHERE m.lote_id = ?", String.class, loteDeLuis);
        String firmaDeMarta = enlaceDeMarta.substring(enlaceDeMarta.indexOf('?'));

        MvcTestResult resultado = fotos.ver(ApiFotos.ARCHIVOS + "/" + llaveDeLuis + firmaDeMarta);

        assertThat(vista(deLuis)).isNotEqualTo(enlaceDeMarta);
        assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(json(resultado)).containsEntry("detail", ENLACE_NO_VALIDO);
    }

    @Test
    @DisplayName("RNF-03 · C5 · una llave de otra empresa o de otro lote no se puede confirmar en este lote (prueba de seguridad)")
    void noSeConfirmanLlavesAjenas() {
        byte[] contenido = jpg();
        Map<String, Object> subidaDeMarta = fotos.subida(tokenDeMarta, loteDeMarta, 1, JPG, contenido.length);
        fotos.subir((String) subidaDeMarta.get("url"), JPG, contenido);
        ApiSesion api = new ApiSesion(mvc, jdbc);
        UUID otroLoteDeLuis = new ApiLotes(api).registrarOk(tokenDeLuis, "L-002");
        Map<String, Object> subidaDeOtroLote = fotos.subida(tokenDeLuis, otroLoteDeLuis, 1, JPG, contenido.length);
        fotos.subir((String) subidaDeOtroLote.get("url"), JPG, contenido);

        MvcTestResult deOtraEmpresa = fotos.confirmar(tokenDeLuis, loteDeLuis, 1, (String) subidaDeMarta.get("llave"));
        MvcTestResult deOtroLote = fotos.confirmar(tokenDeLuis, loteDeLuis, 1, (String) subidaDeOtroLote.get("llave"));

        assertThat(deOtraEmpresa).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(deOtraEmpresa)).containsExactly(error("llave", "El valor no es válido."));
        assertThat(deOtroLote).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(deOtroLote)).containsExactly(error("llave", "El valor no es válido."));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM foto", Integer.class)).isZero();
    }
}
