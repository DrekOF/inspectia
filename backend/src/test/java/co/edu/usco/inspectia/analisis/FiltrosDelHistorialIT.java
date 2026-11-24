/*
 * Nombre: FiltrosDelHistorialIT.java
 * Descripción: Pruebas de integración de los filtros del historial de lotes: la fecha del resultado en días de
 *              Colombia, el estado, la categoría, el código y el usuario, solos y combinados, con sus límites; la lista
 *              de quienes registraron lotes para el filtro «Usuario»; y los lotes de un usuario desactivado (RF-14 C2,
 *              RF-14 restricciones 5 y 6, RF-03 C6).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis;

import static co.edu.usco.inspectia.analisis.ApiLotes.HASTA_ANTES_DE_DESDE;
import static co.edu.usco.inspectia.analisis.ApiLotes.LONGITUD_DEL_CODIGO;
import static co.edu.usco.inspectia.analisis.ApiLotes.VALOR_INVALIDO;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.error;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.errores;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
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
 * Los filtros del historial (RF-14 C2). En Cacaos del Huila, Luis Gómez y Pedro Díaz registran lotes y Ana Pérez es la
 * administradora. Los lotes se registran por la API y su resultado se fija por SQL, con la fecha del resultado en la
 * hora de Colombia (UTC−5): así cada prueba pone los lotes justo antes y justo después de la medianoche, sin cambiar el
 * reloj del backend. Los valores de los filtros van tal cual en la consulta: la llamada los codifica.
 */
@PruebaIntegracion
class FiltrosDelHistorialIT {

    private static final String LUIS = "luis@cacaos.test";
    private static final String PEDRO = "pedro@cacaos.test";
    private static final String ANA = "admin@cacaos.test";

    /** El porcentaje de un lote de cada categoría con los rangos por defecto (D-10). */
    private static final Map<String, BigDecimal> PORCENTAJE = Map.of("PREMIUM", new BigDecimal("97.0"),
            "ESTANDAR", new BigDecimal("93.0"), "CORRIENTE", new BigDecimal("85.0"),
            "FUERA_DE_NORMA", new BigDecimal("70.0"));

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiSesion api;
    private ApiLotes lotes;
    private UUID empresa;
    private UUID luis;
    private UUID pedro;
    private String token;
    private String tokenDePedro;

    @BeforeEach
    void preparar() {
        api = new ApiSesion(mvc, jdbc);
        lotes = new ApiLotes(api);
        ApiSesion.borrarDatos(jdbc);
        empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        luis = api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");
        pedro = api.crearUsuario(empresa, "Pedro Díaz", PEDRO, "USUARIO_FINAL");
        api.crearUsuario(empresa, "Ana Pérez", ANA, "ADMIN_EMPRESA");
        token = api.token(LUIS);
        tokenDePedro = api.token(PEDRO);
    }

    @AfterEach
    void limpiar() {
        ApiSesion.borrarDatos(jdbc);
    }

    /**
     * Deja un lote analizado, sin sugerencia, con la categoría y la fecha del resultado dadas.
     *
     * @param fecha la fecha del resultado, con su zona, como {@code 2026-11-20T00:00-05:00}
     */
    private void analizado(UUID lote, String categoria, String fecha) {
        jdbc.update("""
                UPDATE lote SET estado = 'ANALIZADO', porcentaje_sanos = ?, margen = 5.7, categoria = ?,
                    recomendacion = 'Recomendación de prueba', sugerir_otra_muestra = false,
                    aviso_confianza_baja = false, fecha_resultado = ?
                WHERE id = ?""", PORCENTAJE.get(categoria), categoria, OffsetDateTime.parse(fecha), lote);
    }

    /** Registra un lote y lo deja analizado. */
    private UUID analizado(String tokenDeQuien, String codigo, String categoria, String fecha) {
        UUID lote = lotes.registrarOk(tokenDeQuien, codigo);
        analizado(lote, categoria, fecha);
        return lote;
    }

    /** Los filtros, en el orden en que se escriben: nombre, valor, nombre, valor… */
    private static Map<String, String> filtros(String... pares) {
        Map<String, String> filtros = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            filtros.put(pares[i], pares[i + 1]);
        }
        return filtros;
    }

    /** Los códigos de la primera página con esos filtros; falla si no responde 200. */
    private List<String> codigos(String... pares) {
        MvcTestResult resultado = lotes.listar(token, filtros(pares));
        assertThat(resultado).as("filtros %s", List.of(pares)).hasStatus(HttpStatus.OK);
        return codigos(resultado);
    }

    private static List<String> codigos(MvcTestResult resultado) {
        return JsonPath.read(cuerpo(resultado), "$.contenido[*].codigo");
    }

    private static String cuerpo(MvcTestResult resultado) {
        return new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("RF-14 · C2 · la fecha del resultado se filtra por días de Colombia con las dos fechas incluidas: del 20 al 20 trae los resultados de las 00:00 a las 23:59:59,999999 de Bogotá, y un lote pendiente no sale (prueba de límite)")
    void porFechaDelResultado() {
        analizado(token, "L-0101", "ESTANDAR", "2026-11-19T23:59:59.999999-05:00");
        analizado(token, "L-0102", "ESTANDAR", "2026-11-20T00:00:00-05:00");
        analizado(token, "L-0103", "ESTANDAR", "2026-11-20T23:59:59.999999-05:00");
        analizado(token, "L-0104", "ESTANDAR", "2026-11-21T00:00:00-05:00");
        lotes.registrarOk(token, "L-0105");

        assertThat(codigos("desde", "2026-11-20", "hasta", "2026-11-20")).containsExactly("L-0103", "L-0102");
        assertThat(codigos("desde", "2026-11-20")).containsExactly("L-0104", "L-0103", "L-0102");
        assertThat(codigos("hasta", "2026-11-20")).containsExactly("L-0103", "L-0102", "L-0101");
        assertThat(codigos("desde", "2026-11-19", "hasta", "2026-11-21"))
                .containsExactly("L-0104", "L-0103", "L-0102", "L-0101");
        assertThat(codigos()).as("sin filtros, también el pendiente").hasSize(5);
    }

    @Test
    @DisplayName("RF-14 · C2 · un lote pendiente no sale al filtrar por fecha, porque todavía no tiene fecha del resultado, aunque se haya registrado en esas fechas")
    void losPendientesNoTienenFecha() {
        analizado(token, "L-0101", "ESTANDAR", "2026-11-20T10:00:00-05:00");
        lotes.registrarOk(token, "L-0102");

        assertThat(codigos("desde", "2000-01-01")).containsExactly("L-0101");
        assertThat(codigos("hasta", "2999-12-31")).containsExactly("L-0101");
    }

    @Test
    @DisplayName("RF-14 · C2 · el filtro por estado trae solo los lotes pendientes de análisis o solo los analizados")
    void porEstado() {
        analizado(token, "L-0201", "ESTANDAR", "2026-11-20T10:00:00-05:00");
        lotes.registrarOk(token, "L-0202");

        assertThat(codigos("estado", "PENDIENTE")).containsExactly("L-0202");
        assertThat(codigos("estado", "ANALIZADO")).containsExactly("L-0201");
    }

    @Test
    @DisplayName("RF-14 · C2 · el filtro por categoría trae solo los lotes de esa categoría probable")
    void porCategoria() {
        analizado(token, "L-0301", "PREMIUM", "2026-11-20T10:00:00-05:00");
        analizado(token, "L-0302", "ESTANDAR", "2026-11-20T11:00:00-05:00");
        analizado(token, "L-0303", "CORRIENTE", "2026-11-20T12:00:00-05:00");
        lotes.registrarOk(token, "L-0304");

        assertThat(codigos("categoria", "ESTANDAR")).containsExactly("L-0302");
        assertThat(codigos("categoria", "PREMIUM")).containsExactly("L-0301");
        assertThat(codigos("categoria", "FUERA_DE_NORMA")).isEmpty();
    }

    @Test
    @DisplayName("RF-14 · C2 · el código se busca dentro del código del lote, sin distinguir mayúsculas ni los espacios del inicio y del final")
    void porCodigo() {
        lotes.registrarOk(token, "L-0142");
        lotes.registrarOk(token, "L-0143");
        lotes.registrarOk(token, "LX-142");
        lotes.registrarOk(token, "A-0145");

        assertThat(codigos("codigo", "  l-014 ")).containsExactly("L-0143", "L-0142");
        assertThat(codigos("codigo", "014")).containsExactly("A-0145", "L-0143", "L-0142");
        assertThat(codigos("codigo", "x-1")).containsExactly("LX-142");
        assertThat(codigos("codigo", "L-0142")).containsExactly("L-0142");
        assertThat(codigos("codigo", "Z")).isEmpty();
    }

    @Test
    @DisplayName("RF-14 · C2 · en el código, %, _ y \\ se buscan como caracteres y no como comodines")
    void elCodigoNoTieneComodines() {
        lotes.registrarOk(token, "L_01");
        lotes.registrarOk(token, "LA01");
        lotes.registrarOk(token, "L%02");
        lotes.registrarOk(token, "LB02");
        lotes.registrarOk(token, "L\\03");
        lotes.registrarOk(token, "LC03");

        assertThat(codigos("codigo", "_")).containsExactly("L_01");
        assertThat(codigos("codigo", "%")).containsExactly("L%02");
        assertThat(codigos("codigo", "\\")).containsExactly("L\\03");
        assertThat(codigos("codigo", "l_0")).containsExactly("L_01");
    }

    @Test
    @DisplayName("RF-14 · C2 · el filtro por usuario trae solo los lotes que registró esa persona, también para la administradora")
    void porUsuario() {
        lotes.registrarOk(token, "L-0401");
        lotes.registrarOk(tokenDePedro, "L-0402");
        lotes.registrarOk(token, "L-0403");

        assertThat(codigos("usuario", pedro.toString())).containsExactly("L-0402");
        assertThat(codigos("usuario", luis.toString())).containsExactly("L-0403", "L-0401");
        assertThat(codigos(lotes.listar(api.token(ANA), filtros("usuario", pedro.toString()))))
                .containsExactly("L-0402");
    }

    @Test
    @DisplayName("RF-14 · C2 · los filtros se combinan: fecha, estado, categoría, código y usuario a la vez traen solo los lotes que cumplen todos, del último movimiento al más antiguo, con el conteo y las páginas de esos lotes")
    void combinados() {
        analizado(token, "L-0501", "ESTANDAR", "2026-11-20T10:00:00-05:00");
        analizado(token, "L-0502", "PREMIUM", "2026-11-20T11:00:00-05:00");
        analizado(tokenDePedro, "L-0503", "ESTANDAR", "2026-11-20T12:00:00-05:00");
        analizado(token, "L-0504", "ESTANDAR", "2026-11-22T10:00:00-05:00");
        analizado(token, "L-0505", "ESTANDAR", "2026-11-18T10:00:00-05:00");
        analizado(token, "X-0506", "ESTANDAR", "2026-11-21T10:00:00-05:00");
        lotes.registrarOk(token, "L-0507");
        String[] todos = {"desde", "2026-11-19", "hasta", "2026-11-22", "estado", "ANALIZADO", "categoria", "ESTANDAR",
                "codigo", "l-05", "usuario", luis.toString()};

        MvcTestResult todosALaVez = lotes.listar(token, filtros(todos));
        Map<String, String> porPaginas = filtros(todos);
        porPaginas.put("tamano", "1");
        MvcTestResult primera = lotes.listar(token, porPaginas);
        porPaginas.put("pagina", "1");
        MvcTestResult segunda = lotes.listar(token, porPaginas);

        assertThat(codigos(todosALaVez)).containsExactly("L-0504", "L-0501");
        assertThat(json(todosALaVez)).containsEntry("totalElementos", 2).containsEntry("totalPaginas", 1);
        assertThat(codigos(primera)).containsExactly("L-0504");
        assertThat(json(primera)).containsEntry("totalElementos", 2).containsEntry("totalPaginas", 2);
        assertThat(codigos(segunda)).containsExactly("L-0501");
        assertThat(codigos("estado", "ANALIZADO", "categoria", "ESTANDAR"))
                .containsExactly("L-0504", "X-0506", "L-0503", "L-0501", "L-0505");
        assertThat(codigos("codigo", "l-05", "estado", "PENDIENTE")).containsExactly("L-0507");
    }

    @Test
    @DisplayName("RF-14 · los filtros en blanco no filtran: con las fechas, el estado, la categoría, el código y el usuario vacíos, la lista trae todos los lotes")
    void filtrosEnBlanco() {
        analizado(token, "L-0601", "ESTANDAR", "2026-11-20T10:00:00-05:00");
        lotes.registrarOk(tokenDePedro, "L-0602");

        assertThat(codigos("desde", "", "hasta", "", "estado", "", "categoria", "", "codigo", "", "usuario", ""))
                .containsExactlyInAnyOrder("L-0601", "L-0602");
    }

    @Test
    @DisplayName("RF-14 · si «Hasta» es anterior a «Resultado desde», la búsqueda responde 400 en «hasta» con el motivo; con las dos iguales, responde 200 (prueba de límite)")
    void hastaAntesDeDesde() {
        MvcTestResult alReves = lotes.listar(token, filtros("desde", "2026-11-20", "hasta", "2026-11-19"));
        MvcTestResult iguales = lotes.listar(token, filtros("desde", "2026-11-20", "hasta", "2026-11-20"));

        assertThat(alReves).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(alReves)).containsExactly(error("hasta", HASTA_ANTES_DE_DESDE));
        assertThat(iguales).hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("RF-14 · el código del filtro tiene hasta 30 caracteres: con 30 responde 200 y con 31, 400 en «codigo» (prueba de límite)")
    void codigoDeTreintaCaracteres() {
        MvcTestResult treinta = lotes.listar(token, filtros("codigo", "L".repeat(30)));
        MvcTestResult treintaYUno = lotes.listar(token, filtros("codigo", "L".repeat(31)));

        assertThat(treinta).hasStatus(HttpStatus.OK);
        assertThat(treintaYUno).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(treintaYUno)).containsExactly(error("codigo", LONGITUD_DEL_CODIGO));
    }

    @Test
    @DisplayName("RF-14 · un estado, una categoría, una fecha o un usuario que no son válidos responden 400 en su campo, y no un error del servidor")
    void valoresQueNoSonValidos() {
        Map<String, Map<String, String>> casos = Map.of(
                "estado", filtros("estado", "TERMINADO"),
                "categoria", filtros("categoria", "ORO"),
                "desde", filtros("desde", "2026-13-01"),
                "hasta", filtros("hasta", "20/11/2026"),
                "usuario", filtros("usuario", "luis"));

        casos.forEach((campo, consulta) -> {
            MvcTestResult resultado = lotes.listar(token, consulta);
            assertThat(resultado).as(campo).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(errores(resultado)).as(campo).containsExactly(error(campo, VALOR_INVALIDO));
        });
    }

    @Test
    @DisplayName("RF-14 · la lista del filtro «Usuario» trae el id y el nombre de quienes registraron lotes de la empresa, sin correos ni a quienes no registraron, ordenada por nombre sin distinguir tildes ni mayúsculas, para los dos roles")
    void quienesRegistraronLotes() {
        UUID angela = api.crearUsuario(empresa, "Ángela Mora", "angela@cacaos.test", "USUARIO_FINAL");
        UUID beatriz = api.crearUsuario(empresa, "beatriz Soto", "beatriz@cacaos.test", "USUARIO_FINAL");
        api.crearUsuario(empresa, "Carlos Ruiz", "carlos@cacaos.test", "USUARIO_FINAL");
        lotes.registrarOk(tokenDePedro, "L-0701");
        lotes.registrarOk(token, "L-0702");
        lotes.registrarOk(api.token("beatriz@cacaos.test"), "L-0703");
        lotes.registrarOk(api.token("angela@cacaos.test"), "L-0704");
        lotes.registrarOk(token, "L-0705");

        MvcTestResult deLuis = lotes.registradores(token);
        MvcTestResult deAna = lotes.registradores(api.token(ANA));

        assertThat(deLuis).hasStatus(HttpStatus.OK);
        assertThat(ApiSesion.lista(deLuis)).containsExactly(
                Map.<String, Object>of("id", angela.toString(), "nombre", "Ángela Mora"),
                Map.<String, Object>of("id", beatriz.toString(), "nombre", "beatriz Soto"),
                Map.<String, Object>of("id", luis.toString(), "nombre", "Luis Gómez"),
                Map.<String, Object>of("id", pedro.toString(), "nombre", "Pedro Díaz"));
        assertThat(cuerpo(deLuis)).doesNotContain("@").doesNotContain("Carlos").doesNotContain("Ana Pérez");
        assertThat(deAna).hasStatus(HttpStatus.OK);
        assertThat(ApiSesion.lista(deAna)).isEqualTo(ApiSesion.lista(deLuis));
    }

    @Test
    @DisplayName("RF-14 · sin lotes registrados, la lista del filtro «Usuario» va vacía")
    void sinLotesNoHayRegistradores() {
        MvcTestResult registradores = lotes.registradores(token);

        assertThat(registradores).hasStatus(HttpStatus.OK);
        assertThat(ApiSesion.lista(registradores)).isEmpty();
    }

    @Test
    @DisplayName("RF-03 · C6 · los lotes de un usuario desactivado siguen en el historial con su nombre, él sigue en la lista del filtro «Usuario» y se puede filtrar por él")
    void usuarioDesactivado() {
        lotes.registrarOk(tokenDePedro, "L-0801");
        lotes.registrarOk(token, "L-0802");
        jdbc.update("UPDATE usuario SET activo = false WHERE id = ?", pedro);

        MvcTestResult historial = lotes.listar(token, 0);
        MvcTestResult registradores = lotes.registradores(api.token(ANA));

        assertThat(codigos(historial)).containsExactly("L-0802", "L-0801");
        Map<String, Object> dePedro = JsonPath.read(cuerpo(historial), "$.contenido[1]");
        assertThat(dePedro).containsEntry("usuario", Map.of("id", pedro.toString(), "nombre", "Pedro Díaz"));
        assertThat(registradores).hasStatus(HttpStatus.OK);
        assertThat(ApiSesion.lista(registradores))
                .contains(Map.<String, Object>of("id", pedro.toString(), "nombre", "Pedro Díaz"));
        assertThat(codigos("usuario", pedro.toString())).containsExactly("L-0801");
        assertThat(api.iniciar(PEDRO, ApiSesion.CONTRASENA)).as("Pedro ya no inicia sesión")
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
