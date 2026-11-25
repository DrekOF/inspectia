/*
 * Nombre: FotosDeLaMuestraIT.java
 * Descripción: Pruebas de integración de las fotos de la muestra de un lote (RF-05, RF-14 C5): la primera foto crea
 *              la muestra, hasta 3 fotos, JPG o PNG de hasta 10 MB, el reemplazo que borra la anterior, la
 *              confirmación de una foto que no llegó, retomar el lote con sus fotos, un lote analizado que no recibe
 *              fotos (RF-12), y la muestra que espera la página, de 1 a 2 (D-25).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia.analisis;

import static co.edu.usco.inspectia.analisis.ApiFotos.DEMASIADO_GRANDE;
import static co.edu.usco.inspectia.analisis.ApiFotos.FORMATO_NO_PERMITIDO;
import static co.edu.usco.inspectia.analisis.ApiFotos.JPG;
import static co.edu.usco.inspectia.analisis.ApiFotos.MAXIMO_DE_BYTES;
import static co.edu.usco.inspectia.analisis.ApiFotos.MUESTRA_CAMBIO;
import static co.edu.usco.inspectia.analisis.ApiFotos.NO_LLEGO;
import static co.edu.usco.inspectia.analisis.ApiFotos.PNG;
import static co.edu.usco.inspectia.analisis.ApiFotos.SIN_OTRA_MUESTRA;
import static co.edu.usco.inspectia.analisis.ApiFotos.conTamano;
import static co.edu.usco.inspectia.analisis.ApiFotos.jpg;
import static co.edu.usco.inspectia.analisis.ApiFotos.pdf;
import static co.edu.usco.inspectia.analisis.ApiFotos.png;
import static co.edu.usco.inspectia.analisis.ApiFotos.vista;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.error;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.errores;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.ConfiguracionContenedores;
import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
 * Las fotos de la muestra de un lote (RF-05, RF-14 C5). En Cacaos del Huila, Luis Gómez registra el lote y sube sus
 * fotos, y Pedro Díaz, su compañero, puede seguir subiéndolas. Las fotos se hacen en la prueba: nunca salen del
 * dataset (RNF-08).
 */
@PruebaIntegracion
class FotosDeLaMuestraIT {

    private static final String LUIS = "luis@cacaos.test";
    private static final String PEDRO = "pedro@cacaos.test";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiFotos fotos;
    private UUID luis;
    private String token;
    private UUID lote;

    @BeforeEach
    void preparar() {
        ApiSesion api = new ApiSesion(mvc, jdbc);
        fotos = new ApiFotos(api, mvc);
        ApiSesion.borrarDatos(jdbc);
        UUID empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        luis = api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");
        api.crearUsuario(empresa, "Pedro Díaz", PEDRO, "USUARIO_FINAL");
        token = api.token(LUIS);
        lote = new ApiLotes(api).registrarOk(token, "L-001");
    }

    @AfterEach
    void limpiar() {
        ApiSesion.borrarDatos(jdbc);
        ConfiguracionContenedores.vaciarCarpetaDeFotos();
    }

    @Test
    @DisplayName("RF-05 · la primera foto crea la muestra 1 del lote, pendiente y con quien la subió, y la foto queda «Cargada» en la carpeta")
    void laPrimeraFotoCreaLaMuestra() {
        byte[] contenido = jpg();

        Map<String, Object> foto = fotos.subirFoto(token, lote, 1, JPG, contenido);

        assertThat(foto).containsEntry("numero", 1).containsEntry("formato", "JPG")
                .containsEntry("tamanoBytes", contenido.length).containsEntry("estado", "CARGADA");
        Map<String, Object> muestra = jdbc.queryForMap("SELECT numero, estado, usuario_id FROM muestra WHERE lote_id = ?",
                lote);
        assertThat(muestra).containsEntry("numero", 1).containsEntry("estado", "PENDIENTE")
                .containsEntry("usuario_id", luis);
        assertThat(Files.exists(archivoDe(1))).isTrue();
    }

    @Test
    @DisplayName("RF-05 · una muestra sin fotos se lista vacía, sin número de muestra")
    void sinFotosLaListaVaVacia() {
        MvcTestResult resultado = fotos.listar(token, lote);

        assertThat(resultado).hasStatus(HttpStatus.OK);
        assertThat(json(resultado)).containsEntry("muestra", null).containsEntry("fotos", List.of());
    }

    @Test
    @DisplayName("RF-05 · C2 · la muestra acepta hasta 3 fotos: con las 3 subidas, la foto 4 y la foto 0 responden 400 (prueba de límite)")
    void hastaTresFotos() {
        for (int numero = 1; numero <= 3; numero++) {
            fotos.subirFoto(token, lote, numero, JPG, jpg());
        }

        MvcTestResult cuarta = fotos.pedirSubida(token, lote, 4, JPG, 1_000);
        MvcTestResult cero = fotos.pedirSubida(token, lote, 0, JPG, 1_000);

        assertThat(cuarta).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(cuarta)).containsExactly(error("numero", "Debe ser menor o igual a 3."));
        assertThat(cero).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(cero)).containsExactly(error("numero", "Debe ser mayor o igual a 1."));
        assertThat(contarFotos()).isEqualTo(3);
    }

    @Test
    @DisplayName("RF-05 · C3 · un archivo que no es JPG ni PNG se rechaza al pedir el enlace, con el mensaje de qué hacer, y no se guarda nada")
    void rechazaOtrosFormatos() {
        MvcTestResult resultado = fotos.pedirSubida(token, lote, 1, "application/pdf", 1_000);

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(error("tipo", FORMATO_NO_PERMITIDO));
        assertThat(contarFotos()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM muestra", Integer.class)).isZero();
    }

    @Test
    @DisplayName("RF-05 · C3 · una foto de 10.485.760 bytes se sube y una de 10.485.761 se rechaza al pedir el enlace (prueba de límite)")
    void hastaDiezMegas() {
        MvcTestResult grande = fotos.pedirSubida(token, lote, 1, JPG, MAXIMO_DE_BYTES + 1L);

        Map<String, Object> justa = fotos.subirFoto(token, lote, 2, PNG, conTamano(png(), MAXIMO_DE_BYTES));

        assertThat(grande).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(grande)).containsExactly(error("tamanoBytes", DEMASIADO_GRANDE));
        assertThat(justa).containsEntry("numero", 2).containsEntry("tamanoBytes", MAXIMO_DE_BYTES);
        assertThat(contarFotos()).isOne();
    }

    @Test
    @DisplayName("RF-05 · C3 · un archivo que dice ser JPG pero no lo es por dentro se rechaza al subirlo, con el mensaje de qué hacer, y no queda en la base")
    void revisaElContenido() {
        byte[] falso = pdf();
        Map<String, Object> subida = fotos.subida(token, lote, 1, JPG, falso.length);

        MvcTestResult resultado = fotos.subir((String) subida.get("url"), JPG, falso);
        MvcTestResult confirmada = fotos.confirmar(token, lote, 1, (String) subida.get("llave"));

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(json(resultado)).containsEntry("detail", FORMATO_NO_PERMITIDO);
        assertThat(confirmada).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(confirmada)).containsExactly(error("llave", NO_LLEGO));
        assertThat(contarFotos()).isZero();
    }

    @Test
    @DisplayName("RF-05 · C5 · al reemplazar la foto 2, la anterior se borra de la carpeta y de la base, y la nueva toma su número")
    void reemplazarBorraLaAnterior() {
        fotos.subirFoto(token, lote, 2, JPG, jpg());
        Path anterior = archivoDe(2);

        Map<String, Object> nueva = fotos.subirFoto(token, lote, 2, PNG, png());

        assertThat(nueva).containsEntry("numero", 2).containsEntry("formato", "PNG");
        assertThat(contarFotos()).isOne();
        assertThat(Files.exists(anterior)).as("la foto anterior se borra de la carpeta").isFalse();
        assertThat(Files.exists(archivoDe(2))).isTrue();
    }

    @Test
    @DisplayName("RF-05 · confirmar dos veces la misma foto no la duplica: un reintento es seguro")
    void confirmarDosVeces() {
        byte[] contenido = jpg();
        Map<String, Object> subida = fotos.subida(token, lote, 1, JPG, contenido.length);
        fotos.subir((String) subida.get("url"), JPG, contenido);

        MvcTestResult primera = fotos.confirmar(token, lote, 1, (String) subida.get("llave"));
        MvcTestResult segunda = fotos.confirmar(token, lote, 1, (String) subida.get("llave"));

        assertThat(primera).hasStatus(HttpStatus.OK);
        assertThat(segunda).hasStatus(HttpStatus.OK);
        assertThat(contarFotos()).isOne();
        assertThat(Files.exists(archivoDe(1))).isTrue();
    }

    @Test
    @DisplayName("RF-05 · C2 · confirmar la foto 4 o la 0 responde 400 en el número y no guarda nada (prueba de límite)")
    void confirmarFueraDelRango() {
        Map<String, Object> subida = fotos.subida(token, lote, 1, JPG, jpg().length);
        fotos.subir((String) subida.get("url"), JPG, jpg());

        MvcTestResult cuarta = fotos.confirmar(token, lote, 4, (String) subida.get("llave"));
        MvcTestResult cero = fotos.confirmar(token, lote, 0, (String) subida.get("llave"));

        assertThat(cuarta).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(cuarta)).containsExactly(error("numero", "Debe ser menor o igual a 3."));
        assertThat(cero).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(cero)).containsExactly(error("numero", "Debe ser mayor o igual a 1."));
        assertThat(contarFotos()).isZero();
    }

    @Test
    @DisplayName("RF-05 · una llave de este lote sin la forma de un UUID, como 36 guiones, responde 400 en la llave y no 500")
    void llaveMalFormada() {
        String llave = jdbc.queryForObject("SELECT empresa_id FROM lote WHERE id = ?", UUID.class, lote) + "/" + lote
                + "/m1-" + "-".repeat(36) + ".jpg";

        MvcTestResult resultado = fotos.confirmar(token, lote, 1, llave);

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(error("llave", "El valor no es válido."));
        assertThat(contarFotos()).isZero();
    }

    @Test
    @DisplayName("RF-05 · confirmar una foto que no se subió responde que no llegó y no guarda nada")
    void confirmarSinSubir() {
        Map<String, Object> subida = fotos.subida(token, lote, 1, JPG, 1_000);

        MvcTestResult resultado = fotos.confirmar(token, lote, 1, (String) subida.get("llave"));

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(error("llave", NO_LLEGO));
        assertThat(contarFotos()).isZero();
    }

    @Test
    @DisplayName("RF-12 · pedir el enlace de una foto de un lote que ya está analizado, sin ninguna muestra, responde 409 porque no admite otra muestra")
    void loteAnalizadoSinMuestraNoRecibeFotos() {
        jdbc.update("UPDATE lote SET estado = 'ANALIZADO', porcentaje_sanos = 93.0, margen = 5.7, "
                + "categoria = 'ESTANDAR', recomendacion = 'Apto para la venta', sugerir_otra_muestra = false, "
                + "aviso_confianza_baja = false, "
                + "fecha_resultado = now() WHERE id = ?", lote);

        MvcTestResult resultado = fotos.pedirSubida(token, lote, 1, JPG, 1_000);

        assertThat(resultado).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(resultado)).containsEntry("detail", SIN_OTRA_MUESTRA);
    }

    @Test
    @DisplayName("D-25 · la muestra que espera la página es obligatoria y va de 1 a 2: sin ella, con 0 y con 3 pedir el enlace responde 400 en «muestra»; con 1 da el enlace y con 2, en un lote pendiente, responde 409 porque el lote cambió (prueba de límite)")
    void laMuestraEsperadaDeUnoADos() {
        MvcTestResult sinMuestra = fotos.deLaMuestra(null).pedirSubida(token, lote, 1, JPG, 1_000);
        MvcTestResult cero = fotos.deLaMuestra(0).pedirSubida(token, lote, 1, JPG, 1_000);
        MvcTestResult tres = fotos.deLaMuestra(3).pedirSubida(token, lote, 1, JPG, 1_000);
        MvcTestResult uno = fotos.deLaMuestra(1).pedirSubida(token, lote, 1, JPG, 1_000);
        MvcTestResult dos = fotos.deLaMuestra(2).pedirSubida(token, lote, 1, JPG, 1_000);

        assertThat(sinMuestra).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(sinMuestra)).containsExactly(error("muestra", ApiLotes.OBLIGATORIO));
        assertThat(cero).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(cero)).containsExactly(error("muestra", "Debe ser mayor o igual a 1."));
        assertThat(tres).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(tres)).containsExactly(error("muestra", "Debe ser menor o igual a 2."));
        assertThat(uno).hasStatus(HttpStatus.CREATED);
        assertThat((String) json(uno).get("llave")).contains("/" + lote + "/m1-");
        assertThat(dos).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(dos)).containsEntry("detail", MUESTRA_CAMBIO);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM muestra", Integer.class)).isZero();
    }

    @Test
    @DisplayName("RF-14 · C5 · un lote pendiente se retoma con sus fotos: la lista trae las que ya se subieron, cada una con un enlace nuevo para verla")
    void retomarConLasFotos() {
        byte[] primera = jpg();
        fotos.subirFoto(token, lote, 1, JPG, primera);
        fotos.subirFoto(token, lote, 2, PNG, png());

        MvcTestResult resultado = fotos.listar(token, lote);

        assertThat(resultado).hasStatus(HttpStatus.OK);
        Map<String, Object> muestra = json(resultado);
        assertThat(muestra).containsEntry("muestra", 1).containsEntry("estado", "PENDIENTE");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lista = (List<Map<String, Object>>) muestra.get("fotos");
        assertThat(lista).extracting(foto -> foto.get("numero")).containsExactly(1, 2);
        MvcTestResult imagen = fotos.ver(vista(lista.get(0)));
        assertThat(imagen).hasStatus(HttpStatus.OK).hasContentType("image/jpeg");
        assertThat(imagen.getResponse().getContentAsByteArray()).isEqualTo(primera);
    }

    @Test
    @DisplayName("RF-14 · C5 · un compañero de la empresa también puede seguir subiendo las fotos del lote")
    void unCompaneroSigue() {
        fotos.subirFoto(token, lote, 1, JPG, jpg());
        ApiSesion api = new ApiSesion(mvc, jdbc);

        Map<String, Object> segunda = fotos.subirFoto(api.token(PEDRO), lote, 2, JPG, jpg());

        assertThat(segunda).containsEntry("numero", 2);
        assertThat(contarFotos()).isEqualTo(2);
    }

    /** El archivo de la foto en la carpeta temporal de las pruebas, según la ruta que guardó la base. */
    private Path archivoDe(int numero) {
        String ruta = jdbc.queryForObject("SELECT f.ruta FROM foto f JOIN muestra m ON m.id = f.muestra_id "
                + "WHERE m.lote_id = ? AND f.numero = ?", String.class, lote, numero);
        return ConfiguracionContenedores.CARPETA_DE_FOTOS.resolve(ruta);
    }

    private int contarFotos() {
        return jdbc.queryForObject("SELECT count(*) FROM foto", Integer.class);
    }
}
