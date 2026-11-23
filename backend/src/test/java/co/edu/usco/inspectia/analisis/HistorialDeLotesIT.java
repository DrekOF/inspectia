/*
 * Nombre: HistorialDeLotesIT.java
 * Descripción: Pruebas de integración del historial de lotes: solo los de la empresa, del más reciente al
 *              más antiguo, por páginas de 20, y el detalle de un lote (RF-14).
 * Fecha de creación: 10/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia.analisis;

import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.error;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.errores;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.comun.api.Pagina;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Pruebas del historial de lotes (RF-14). En Cacaos del Huila, Luis Gómez y Pedro Díaz registran lotes y
 * Ana Pérez es la administradora; en Cacao del Sur, Marta Ruiz registra los suyos.
 */
@PruebaIntegracion
class HistorialDeLotesIT {

    private static final String LUIS = "luis@cacaos.test";
    private static final String PEDRO = "pedro@cacaos.test";
    private static final String ANA = "admin@cacaos.test";
    private static final String MARTA = "marta@cacaosur.test";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiSesion api;
    private ApiLotes lotes;
    private UUID luis;
    private String token;

    @BeforeEach
    void preparar() {
        api = new ApiSesion(mvc, jdbc);
        lotes = new ApiLotes(api);
        ApiSesion.borrarDatos(jdbc);
        UUID empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        luis = api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");
        api.crearUsuario(empresa, "Pedro Díaz", PEDRO, "USUARIO_FINAL");
        api.crearUsuario(empresa, "Ana Pérez", ANA, "ADMIN_EMPRESA");
        UUID otraEmpresa = api.crearEmpresa("Cacao del Sur", "800765432-1");
        api.crearUsuario(otraEmpresa, "Marta Ruiz", MARTA, "USUARIO_FINAL");
        token = api.token(LUIS);
    }

    @AfterEach
    void limpiar() {
        ApiSesion.borrarDatos(jdbc);
    }

    @Test
    @DisplayName("RF-14 · C1 · la lista muestra solo lotes de la empresa del usuario, también los que registraron sus compañeros")
    void soloLotesDeLaEmpresa() {
        lotes.registrarOk(token, "L-001");
        lotes.registrarOk(api.token(PEDRO), "L-002");
        lotes.registrarOk(api.token(MARTA), "S-001");

        MvcTestResult historial = lotes.listar(token, 0);

        assertThat(historial).hasStatus(HttpStatus.OK);
        assertThat(codigos(historial)).as("del más reciente al más antiguo, sin el de Cacao del Sur")
                .containsExactly("L-002", "L-001");
        assertThat(json(historial)).containsEntry("totalElementos", 2);
    }

    @Test
    @DisplayName("RF-14 · C4 · con más de 20 lotes, la lista se divide en páginas de 20, del lote más reciente al más antiguo")
    void paginasDeVeinte() {
        IntStream.rangeClosed(1, 25).forEach(numero -> lotes.registrarOk(token, "L-%03d".formatted(numero)));

        MvcTestResult primera = lotes.listar(token, 0);
        MvcTestResult segunda = lotes.listar(token, 1);

        assertThat(primera).hasStatus(HttpStatus.OK);
        assertThat(json(primera)).containsEntry("pagina", 0)
                .containsEntry("tamano", 20)
                .containsEntry("totalElementos", 25)
                .containsEntry("totalPaginas", 2);
        List<String> codigosPrimera = codigos(primera);
        assertThat(codigosPrimera).hasSize(20);
        assertThat(codigosPrimera.getFirst()).isEqualTo("L-025");
        assertThat(codigosPrimera.getLast()).isEqualTo("L-006");
        assertThat(codigos(segunda)).containsExactly("L-005", "L-004", "L-003", "L-002", "L-001");
    }

    @Test
    @DisplayName("RF-14 · la administradora de la empresa también ve el historial de su empresa")
    void laAdministradoraVeElHistorial() {
        lotes.registrarOk(token, "L-001");
        lotes.registrarOk(api.token(PEDRO), "L-002");

        MvcTestResult historial = lotes.listar(api.token(ANA), 0);

        assertThat(historial).hasStatus(HttpStatus.OK);
        assertThat(codigos(historial)).containsExactly("L-002", "L-001");
    }

    @Test
    @DisplayName("RF-14 · cada lote de la lista trae su fecha de recepción, código, estado, quién lo registró, porcentaje y categoría; los de un lote pendiente van vacíos")
    void datosDeCadaLote() {
        UUID id = lotes.registrarOk(token, "L-001");

        Map<String, Object> lote = JsonPath.read(cuerpo(lotes.listar(token, 0)), "$.contenido[0]");

        assertThat(lote).containsEntry("id", id.toString())
                .containsEntry("codigo", "L-001")
                .containsEntry("fechaRecepcion", ApiLotes.RECEPCION.toString())
                .containsEntry("origen", "Finca La Esperanza")
                .containsEntry("estado", "PENDIENTE")
                .containsEntry("usuario", Map.of("id", luis.toString(), "nombre", "Luis Gómez"))
                .containsKeys("fechaCreacion", "fechaResultado");
        assertThat(lote.get("porcentajeSanos")).isNull();
        assertThat(lote.get("categoria")).isNull();
        assertThat(lote.get("fechaResultado")).isNull();
    }

    @Test
    @DisplayName("RF-14 · la lista va del último movimiento al más antiguo: la fecha del resultado si el lote se analizó, o la de registro si está pendiente, y trae la fecha del resultado")
    void ordenPorUltimoMovimiento() {
        UUID primero = lotes.registrarOk(token, "L-001");
        lotes.registrarOk(token, "L-002");
        lotes.registrarOk(token, "L-003");
        // L-001 se analiza justo después del registro de L-002 y antes del de L-003: sube por encima de L-002.
        jdbc.update("""
                UPDATE lote SET estado = 'ANALIZADO', porcentaje_sanos = 93.0, margen = 5.7, categoria = 'ESTANDAR',
                    recomendacion = 'Apto para venta estándar.', sugerir_otra_muestra = false,
                    aviso_confianza_baja = false,
                    fecha_resultado = (SELECT fecha_creacion FROM lote WHERE codigo = 'L-002') + interval '1 microsecond'
                WHERE id = ?""", primero);

        MvcTestResult historial = lotes.listar(token, 0);

        assertThat(codigos(historial)).containsExactly("L-003", "L-001", "L-002");
        Map<String, Object> analizado = JsonPath.read(cuerpo(historial), "$.contenido[1]");
        assertThat(analizado).containsEntry("codigo", "L-001")
                .containsEntry("estado", "ANALIZADO")
                .containsEntry("porcentajeSanos", 93.0)
                .containsEntry("categoria", "ESTANDAR");
        assertThat(analizado.get("fechaResultado")).as("la columna «Fecha del resultado» del prototipo").isNotNull();
    }

    @Test
    @DisplayName("RF-14 · el detalle de un lote de la empresa se abre por su identificador")
    void detalleDeUnLote() {
        UUID id = lotes.registrarOk(api.token(PEDRO), "L-001");

        MvcTestResult detalle = lotes.ver(token, id);

        assertThat(detalle).hasStatus(HttpStatus.OK);
        assertThat(json(detalle)).containsEntry("id", id.toString())
                .containsEntry("codigo", "L-001")
                .containsEntry("estado", "PENDIENTE");
    }

    @Test
    @DisplayName("RF-14 · las páginas van de 0 a la más alta que la consulta puede pedir y traen de 1 a 100 lotes; "
            + "fuera de esos límites responde 400 y no un error del servidor (prueba de límite)")
    void limitesDeLaPagina() {
        String ultima = "?pagina=" + Pagina.PAGINA_MAXIMA + "&tamano=" + Pagina.TAMANO_MAXIMO;
        MvcTestResult negativa = api.enviar(HttpMethod.GET, ApiLotes.RUTA + "?pagina=-1", null, token);
        MvcTestResult laUltima = api.enviar(HttpMethod.GET, ApiLotes.RUTA + ultima, null, token);
        MvcTestResult pasada = api.enviar(HttpMethod.GET, ApiLotes.RUTA + "?pagina=" + (Pagina.PAGINA_MAXIMA + 1),
                null, token);
        MvcTestResult sinLotes = api.enviar(HttpMethod.GET, ApiLotes.RUTA + "?tamano=0", null, token);
        MvcTestResult grande = api.enviar(HttpMethod.GET, ApiLotes.RUTA + "?tamano=101", null, token);

        assertThat(negativa).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(negativa)).containsExactly(error("pagina", "Debe ser mayor o igual a 0."));
        assertThat(laUltima).hasStatus(HttpStatus.OK);
        assertThat(codigos(laUltima)).isEmpty();
        assertThat(pasada).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(pasada)).containsExactly(error("pagina", "Debe ser menor o igual a 21474836."));
        assertThat(sinLotes).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(sinLotes)).containsExactly(error("tamano", "Debe ser mayor o igual a 1."));
        assertThat(grande).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(grande)).containsExactly(error("tamano", "Debe ser menor o igual a 100."));
    }

    private static List<String> codigos(MvcTestResult resultado) {
        return JsonPath.read(cuerpo(resultado), "$.contenido[*].codigo");
    }

    private static String cuerpo(MvcTestResult resultado) {
        return new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }
}
