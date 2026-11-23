/*
 * Nombre: RegistroDeLotesIT.java
 * Descripción: Pruebas de integración del registro de un lote: datos válidos, código repetido en la empresa
 *              sin distinguir mayúsculas ni espacios, validaciones de los datos y sugerencias de finca o
 *              proveedor (RF-04).
 * Fecha de creación: 10/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis;

import static co.edu.usco.inspectia.analisis.ApiLotes.CODIGO_REPETIDO;
import static co.edu.usco.inspectia.analisis.ApiLotes.DATOS_INVALIDOS;
import static co.edu.usco.inspectia.analisis.ApiLotes.DATO_REPETIDO;
import static co.edu.usco.inspectia.analisis.ApiLotes.OBLIGATORIO;
import static co.edu.usco.inspectia.analisis.ApiLotes.RECEPCION;
import static co.edu.usco.inspectia.analisis.ApiLotes.VALOR_INVALIDO;
import static co.edu.usco.inspectia.analisis.ApiLotes.datos;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.error;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.errores;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Pruebas del registro de un lote (RF-04). Luis Gómez, usuario final de Cacaos del Huila, registra los
 * lotes; Pedro Díaz es otro usuario final de la misma empresa y Marta Ruiz, de Cacao del Sur.
 */
@PruebaIntegracion
class RegistroDeLotesIT {

    private static final String LUIS = "luis@cacaos.test";
    private static final String PEDRO = "pedro@cacaos.test";
    private static final String MARTA = "marta@cacaosur.test";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiSesion api;
    private ApiLotes lotes;
    private UUID empresa;
    private UUID luis;
    private String token;

    @BeforeEach
    void preparar() {
        api = new ApiSesion(mvc, jdbc);
        lotes = new ApiLotes(api);
        ApiSesion.borrarDatos(jdbc);
        empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        luis = api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");
        api.crearUsuario(empresa, "Pedro Díaz", PEDRO, "USUARIO_FINAL");
        UUID otraEmpresa = api.crearEmpresa("Cacao del Sur", "800765432-1");
        api.crearUsuario(otraEmpresa, "Marta Ruiz", MARTA, "USUARIO_FINAL");
        token = api.token(LUIS);
    }

    @AfterEach
    void limpiar() {
        ApiSesion.borrarDatos(jdbc);
    }

    @Test
    @DisplayName("RF-04 · C1 · con datos válidos, el lote queda registrado como pendiente de análisis, con la empresa y el usuario de la sesión")
    void registraElLote() {
        MvcTestResult resultado = lotes.registrar(token,
                datos("L-001", RECEPCION, "Finca La Esperanza", "Secado al sol durante 6 días"));

        assertThat(resultado).hasStatus(HttpStatus.CREATED);
        Map<String, Object> lote = json(resultado);
        assertThat(lote).containsEntry("codigo", "L-001")
                .containsEntry("fechaRecepcion", RECEPCION.toString())
                .containsEntry("origen", "Finca La Esperanza")
                .containsEntry("observaciones", "Secado al sol durante 6 días")
                .containsEntry("estado", "PENDIENTE")
                .containsEntry("usuario", Map.of("id", luis.toString(), "nombre", "Luis Gómez"))
                .containsKeys("id", "fechaCreacion");
        assertThat(lote.get("porcentajeSanos")).as("sin resultado mientras está pendiente").isNull();
        assertThat(lote.get("categoria")).isNull();
        assertThat(lote.get("fechaResultado")).isNull();
        UUID id = UUID.fromString((String) lote.get("id"));
        assertThat(jdbc.queryForMap("SELECT empresa_id, usuario_id, estado FROM lote WHERE id = ?", id))
                .containsEntry("empresa_id", empresa)
                .containsEntry("usuario_id", luis)
                .containsEntry("estado", "PENDIENTE");
    }

    @Test
    @DisplayName("RF-04 · C2 · si el código ya existe en la empresa, el sistema lo avisa y no registra el lote, aunque lo registre otro usuario")
    void codigoRepetidoEnLaEmpresa() {
        lotes.registrarOk(token, "L-001");

        MvcTestResult resultado = lotes.registrar(api.token(PEDRO),
                datos("L-001", RECEPCION, "Finca El Paraíso", null));

        assertThat(resultado).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(resultado)).containsEntry("detail", DATO_REPETIDO);
        assertThat(errores(resultado)).containsExactly(error("codigo", CODIGO_REPETIDO));
        assertThat(contarLotes()).isOne();
    }

    @Test
    @DisplayName("RF-04 · C3 · el código «l-001 » cuenta como repetido si ya existe «L-001»")
    void codigoRepetidoSinMayusculasNiEspacios() {
        lotes.registrarOk(token, "L-001");

        MvcTestResult resultado = lotes.registrar(token, datos("l-001 ", RECEPCION, "Finca La Esperanza", null));

        assertThat(resultado).hasStatus(HttpStatus.CONFLICT);
        assertThat(errores(resultado)).containsExactly(error("codigo", CODIGO_REPETIDO));
        assertThat(contarLotes()).isOne();
    }

    @Test
    @DisplayName("RF-04 · el mismo código se puede usar en otra empresa")
    void elMismoCodigoEnOtraEmpresa() {
        lotes.registrarOk(token, "L-001");

        MvcTestResult resultado = lotes.registrar(api.token(MARTA), datos("L-001", RECEPCION, "Finca del Sur", null));

        assertThat(resultado).hasStatus(HttpStatus.CREATED);
        assertThat(contarLotes()).isEqualTo(2);
    }

    @Test
    @DisplayName("RF-04 · el código, la finca o el proveedor y las observaciones se guardan sin los espacios del inicio y del final; unas observaciones en blanco quedan vacías")
    void recortaLosTextos() {
        MvcTestResult conEspacios = lotes.registrar(token,
                datos("  L-002  ", RECEPCION, " Finca La Esperanza ", "  Grano húmedo  "));
        MvcTestResult enBlanco = lotes.registrar(token, datos("L-003", RECEPCION, "Finca La Esperanza", "   "));

        assertThat(conEspacios).hasStatus(HttpStatus.CREATED);
        assertThat(json(conEspacios)).containsEntry("codigo", "L-002")
                .containsEntry("origen", "Finca La Esperanza")
                .containsEntry("observaciones", "Grano húmedo");
        assertThat(enBlanco).hasStatus(HttpStatus.CREATED);
        assertThat(json(enBlanco).get("observaciones")).isNull();
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("RF-04 · los datos obligatorios y sus longitudes máximas: código de 30 caracteres, finca o proveedor de 120 y observaciones de 500")
    @MethodSource("datosInvalidos")
    void rechazaDatosInvalidos(String caso, Map<String, Object> datos, String campo, String mensaje) {
        MvcTestResult resultado = lotes.registrar(token, datos);

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(json(resultado)).containsEntry("detail", DATOS_INVALIDOS);
        assertThat(errores(resultado)).containsExactly(error(campo, mensaje));
        assertThat(contarLotes()).isZero();
    }

    @Test
    @DisplayName("RF-04 · con el código de 30 caracteres, la finca o el proveedor de 120 y las observaciones de 500, el lote se registra (prueba de límite)")
    void aceptaLasLongitudesMaximas() {
        MvcTestResult resultado = lotes.registrar(token,
                datos("L".repeat(30), RECEPCION, "F".repeat(120), "O".repeat(500)));

        assertThat(resultado).hasStatus(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("RF-04 · una fecha de recepción que no tiene el formato aaaa-mm-dd se rechaza en su campo")
    void rechazaUnaFechaSinFormato() {
        Map<String, Object> datos = datos("L-001", null, "Finca La Esperanza", null);
        datos.put("fechaRecepcion", "09/11/2026");

        MvcTestResult resultado = lotes.registrar(token, datos);

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(error("fechaRecepcion", VALOR_INVALIDO));
        assertThat(contarLotes()).isZero();
    }

    @Test
    @DisplayName("RF-04 · al escribir la finca o el proveedor, el sistema sugiere los que ya usó la empresa, sin distinguir mayúsculas y sin repetirlos")
    void sugiereLosOrigenesDeLaEmpresa() {
        lotes.registrarOk(token, "L-001", "Finca La Esperanza");
        lotes.registrarOk(token, "L-002", "FINCA LA ESPERANZA");
        lotes.registrarOk(token, "L-003", "Finca El Paraíso");
        lotes.registrarOk(token, "L-004", "Cooperativa Agrosur");

        MvcTestResult fincas = lotes.origenes(token, "finca");
        MvcTestResult cooperativa = lotes.origenes(token, "AGRO");

        assertThat(fincas).hasStatus(HttpStatus.OK);
        assertThat(textos(fincas)).as("una sola vez cada finca, en orden alfabético; las dos formas de La Esperanza "
                + "se usaron una vez, así que gana la más reciente").containsExactly("Finca El Paraíso", "FINCA LA ESPERANZA");
        assertThat(textos(cooperativa)).as("el texto puede estar en cualquier parte del nombre")
                .containsExactly("Cooperativa Agrosur");
    }

    @Test
    @DisplayName("RF-04 · las sugerencias de finca o proveedor tampoco distinguen tildes: «paraiso» sugiere «Finca El Paraíso» y «unión», «Finca La Union»")
    void sugerenciasSinTildes() {
        lotes.registrarOk(token, "L-001", "Finca El Paraíso");
        lotes.registrarOk(token, "L-002", "Finca La Union");

        MvcTestResult paraiso = lotes.origenes(token, "paraiso");
        MvcTestResult union = lotes.origenes(token, "unión");

        assertThat(textos(paraiso)).containsExactly("Finca El Paraíso");
        assertThat(textos(union)).containsExactly("Finca La Union");
    }

    @Test
    @DisplayName("RF-04 · si la misma finca se escribió con tilde y sin ella el mismo número de veces, se sugiere una sola vez, con la forma más reciente")
    void unaSolaFormaConYSinTilde() {
        lotes.registrarOk(token, "L-001", "Finca El Paraíso");
        lotes.registrarOk(token, "L-002", "Finca el paraiso");

        MvcTestResult sugeridas = lotes.origenes(token, "PARAÍSO");

        assertThat(textos(sugeridas)).containsExactly("Finca el paraiso");
    }

    @Test
    @DisplayName("RF-04 · se sugiere la forma más usada de cada finca, aunque otra sea más reciente: «Finca El Paraíso», escrita bien 3 veces, gana a «finca el paraiso», escrita después una vez")
    void laFormaMasUsada() {
        lotes.registrarOk(token, "L-001", "Finca El Paraíso");
        lotes.registrarOk(token, "L-002", "Finca El Paraíso");
        lotes.registrarOk(token, "L-003", "Finca El Paraíso");
        lotes.registrarOk(token, "L-004", "finca el paraiso");

        MvcTestResult sugeridas = lotes.origenes(token, "paraiso");

        assertThat(textos(sugeridas)).containsExactly("Finca El Paraíso");
    }

    @Test
    @DisplayName("RF-04 · se sugieren como máximo 10 fincas o proveedores")
    void comoMaximoDiez() {
        for (int numero = 1; numero <= 12; numero++) {
            lotes.registrarOk(token, "L-%03d".formatted(numero), "Finca %02d".formatted(numero));
        }

        MvcTestResult sugeridas = lotes.origenes(token, "finca");

        assertThat(textos(sugeridas)).as("las 10 primeras en orden alfabético").hasSize(10)
                .startsWith("Finca 01").endsWith("Finca 10");
    }

    static Stream<Arguments> datosInvalidos() {
        String finca = "Finca La Esperanza";
        return Stream.of(
                Arguments.of("sin código", datos(null, RECEPCION, finca, null), "codigo", OBLIGATORIO),
                Arguments.of("código de solo espacios", datos("   ", RECEPCION, finca, null), "codigo", OBLIGATORIO),
                Arguments.of("código de 31 caracteres", datos("L".repeat(31), RECEPCION, finca, null), "codigo",
                        "Puede tener como máximo 30 caracteres."),
                Arguments.of("sin fecha de recepción", datos("L-001", null, finca, null), "fechaRecepcion", OBLIGATORIO),
                Arguments.of("sin finca o proveedor", datos("L-001", RECEPCION, "  ", null), "origen", OBLIGATORIO),
                Arguments.of("finca o proveedor de 121 caracteres", datos("L-001", RECEPCION, "F".repeat(121), null),
                        "origen", "Puede tener como máximo 120 caracteres."),
                Arguments.of("observaciones de 501 caracteres", datos("L-001", RECEPCION, finca, "O".repeat(501)),
                        "observaciones", "Puede tener como máximo 500 caracteres."));
    }

    private int contarLotes() {
        return jdbc.queryForObject("SELECT count(*) FROM lote", Integer.class);
    }

    private static List<String> textos(MvcTestResult resultado) {
        return JsonPath.read(new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8), "$");
    }
}
