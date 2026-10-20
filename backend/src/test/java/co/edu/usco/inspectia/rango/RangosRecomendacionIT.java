/*
 * Nombre: RangosRecomendacionIT.java
 * Descripción: Pruebas de los rangos de recomendación de la empresa por la API, con PostgreSQL 18 y tokens
 *              reales: edición de los 4 en bloque, límites, valores por defecto, permisos y separación por
 *              empresa (RF-13, RNF-03, RNF-04).
 * Fecha de creación: 19/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia.rango;

import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.CONTRASENA;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.error;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.errores;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.lista;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Pruebas de los rangos de recomendación de la empresa (RF-13).
 *
 * <p>Contrato que fijan estas pruebas:
 * <ul>
 *   <li>{@code GET /api/v1/rangos-recomendacion} responde las 4 filas de la empresa de la sesión, sin páginas
 *       y en el orden de las categorías: {@code id}, {@code categoria}, {@code limiteInferior},
 *       {@code recomendacion} y las fechas.</li>
 *   <li>{@code PUT /api/v1/rangos-recomendacion} recibe {@code filas}: las 4 categorías, cada una con
 *       {@code categoria}, {@code limiteInferior} y {@code recomendacion}. Responde las 4 filas guardadas. Con
 *       cualquier error responde 400 y no guarda nada.</li>
 *   <li>{@code POST /api/v1/rangos-recomendacion/restaurar-por-defecto} vuelve a los valores de D-10 y responde
 *       las 4 filas.</li>
 *   <li>Solo las usa el administrador de empresa, y la empresa sale de la sesión (RNF-03, RNF-04).</li>
 *   <li>Editar y restaurar bloquean las filas de la empresa hasta terminar: si dos administradores las
 *       cambian a la vez, el segundo espera al primero.</li>
 * </ul>
 * Las empresas se crean por la API de RF-02, que les da sus rangos por defecto. C2 (un análisis hecho
 * después del cambio usa los rangos nuevos) se prueba con RF-12, cuando exista el análisis.
 */
@PruebaIntegracion
class RangosRecomendacionIT {

    private static final String RUTA = "/api/v1/rangos-recomendacion";
    private static final String RESTAURAR = RUTA + "/restaurar-por-defecto";
    private static final String PLATAFORMA = "plataforma@inspectia.test";
    private static final String ADMIN_A = "admin@cacaos.test";
    private static final String ADMIN_B = "admin@cacaosur.test";
    private static final String LUIS = "luis@cacaos.test";

    private static final String FUERA_DE_ORDEN =
            "Los límites deben bajar en orden: Premium o Especial > Estándar > Corriente > 0 %.";
    private static final String FILAS_COMPLETAS =
            "Envía una fila por cada categoría, sin repetir: Premium o Especial, Estándar, Corriente y Fuera de norma.";
    private static final String FUERA_DE_NORMA_FIJO = "El límite de Fuera de norma es fijo: 0 %.";
    private static final String OBLIGATORIO = "Este dato es obligatorio.";
    private static final String LARGA = "Puede tener como máximo 300 caracteres.";
    private static final String VALOR_INVALIDO = "El valor no es válido.";

    /** Los rangos de D-10 (anexo A.3), en el orden de las categorías. */
    private static final List<Rango> D10 = List.of(
            new Rango("PREMIUM", new BigDecimal("96.0"), "Apto para mercado especial o de exportación."),
            new Rango("ESTANDAR", new BigDecimal("90.0"),
                    "Apto para venta estándar. Retirar a mano los granos defectuosos podría subirlo de categoría."),
            new Rango("CORRIENTE", new BigDecimal("82.0"),
                    "Retirar los granos defectuosos antes de vender, y revisar la fermentación y el secado."),
            new Rango("FUERA_DE_NORMA", new BigDecimal("0.0"), "No venderlo como cacao de calidad: hacer una "
                    + "selección completa y revisar todo el proceso después de la cosecha."));

    /** Un juego de rangos válido, distinto del de D-10. */
    private static final List<Rango> OTROS = List.of(
            new Rango("PREMIUM", new BigDecimal("97.5"), "Exportación."),
            new Rango("ESTANDAR", new BigDecimal("91.0"), "Venta estándar."),
            new Rango("CORRIENTE", new BigDecimal("80.0"), "Seleccionar antes de vender."),
            new Rango("FUERA_DE_NORMA", new BigDecimal("0.0"), "No vender como cacao de calidad."));

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource fuente;

    private ApiSesion api;
    private String tokenPlataforma;
    private UUID empresaA;
    private UUID empresaB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void preparar() {
        api = new ApiSesion(mvc, jdbc);
        ApiSesion.borrarDatos(jdbc);
        api.crearUsuario(null, "Equipo InspectIA", PLATAFORMA, "ADMIN_PLATAFORMA");
        tokenPlataforma = api.token(PLATAFORMA);
        empresaA = crearEmpresa("Cacaos del Huila", "900123456-8", ADMIN_A);
        empresaB = crearEmpresa("Cacao del Sur", "890903938-8", ADMIN_B);
        tokenA = api.token(ADMIN_A);
        tokenB = api.token(ADMIN_B);
    }

    @AfterEach
    void limpiar() {
        ApiSesion.borrarDatos(jdbc);
    }

    @Test
    @DisplayName("RF-13 · el administrador ve los 4 rangos de su empresa, en el orden de las categorías, con los valores por defecto de una empresa nueva")
    void listaLosRangosDeLaEmpresa() {
        MvcTestResult resultado = listar(tokenA);

        assertThat(resultado).hasStatus(HttpStatus.OK);
        assertThat(deLaRespuesta(resultado)).isEqualTo(D10);
        assertThat(lista(resultado).getFirst())
                .containsOnlyKeys("id", "categoria", "limiteInferior", "recomendacion", "fechaCreacion", "fechaActualizacion");
    }

    @Test
    @DisplayName("RF-13 · el administrador cambia los límites y los textos de las 4 categorías: se guardan sin espacios al inicio ni al final, que son los que leerá el análisis (RF-12)")
    void guardaLosCambios() {
        List<Map<String, Object>> filas = filas(OTROS);
        filas.get(0).put("recomendacion", "  Exportación.  ");

        MvcTestResult resultado = editar(tokenA, filas);

        assertThat(resultado).hasStatus(HttpStatus.OK);
        assertThat(deLaRespuesta(resultado)).isEqualTo(OTROS);
        assertThat(deLaRespuesta(listar(tokenA))).isEqualTo(OTROS);
        assertThat(enLaBase(empresaA)).isEqualTo(OTROS);
    }

    @Test
    @DisplayName("RF-13 · editar responde los límites como quedan guardados, igual que el listado: 100 como 100.0, 90.50 como 90.5 y 1E+1 como 10.0")
    void respondeLosLimitesComoQuedanGuardados() {
        List<Map<String, Object>> filas = filas(D10);
        filas.get(0).put("limiteInferior", new BigDecimal("100"));
        filas.get(1).put("limiteInferior", new BigDecimal("90.50"));
        filas.get(2).put("limiteInferior", new BigDecimal("1E+1"));

        MvcTestResult resultado = editar(tokenA, filas);

        assertThat(resultado).hasStatus(HttpStatus.OK);
        assertThat(limitesComoVienen(resultado)).containsExactly("100.0", "90.5", "10.0", "0.0")
                .isEqualTo(limitesComoVienen(listar(tokenA)));
    }

    @Test
    @DisplayName("RF-13 · C1 · si los límites no bajan en orden (Estándar 97 % y Premium 96 %), el sistema no guarda y explica el error")
    void limitesFueraDeOrden() {
        List<Map<String, Object>> filas = filas(D10);
        filas.get(1).put("limiteInferior", new BigDecimal("97.0"));

        MvcTestResult resultado = editar(tokenA, filas);

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(error(null, FUERA_DE_ORDEN));
        assertThat(enLaBase(empresaA)).as("no guardó nada").isEqualTo(D10);
    }

    @ParameterizedTest(name = "Premium {0}, Estándar {1} y Corriente {2}")
    @DisplayName("RF-13 · C1 · los límites deben bajar estrictamente: un empate o Corriente en 0 % tampoco se guardan")
    @CsvSource({"96.0, 96.0, 82.0", "96.0, 90.0, 90.0", "96.0, 90.0, 0.0"})
    void empatesYCorrienteEnCero(String premium, String estandar, String corriente) {
        List<Map<String, Object>> filas = filas(D10);
        filas.get(0).put("limiteInferior", new BigDecimal(premium));
        filas.get(1).put("limiteInferior", new BigDecimal(estandar));
        filas.get(2).put("limiteInferior", new BigDecimal(corriente));

        MvcTestResult resultado = editar(tokenA, filas);

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(error(null, FUERA_DE_ORDEN));
        assertThat(enLaBase(empresaA)).as("no guardó nada").isEqualTo(D10);
    }

    @ParameterizedTest(name = "fila {0} en {1}: {2}")
    @DisplayName("RF-13 · los límites van de 0 a 100 % con un decimal como máximo, contado sin los ceros del final: 90,50 vale 90,5 (prueba de límite)")
    @CsvSource(delimiter = '|', textBlock = """
            0 | 100.0 | 200 |
            0 | 100.1 | 400 | Debe ser menor o igual a 100.
            2 | 0.1   | 200 |
            2 | -0.1  | 400 | Debe ser mayor o igual a 0.
            1 | 90.5  | 200 |
            1 | 90.50 | 200 |
            1 | 90.55 | 400 | Usa como máximo 1 decimal.
            """)
    void limitesDeCadaCategoria(int fila, String limite, int estado, String mensaje) {
        List<Map<String, Object>> filas = filas(D10);
        filas.get(fila).put("limiteInferior", new BigDecimal(limite));

        MvcTestResult resultado = editar(tokenA, filas);

        assertThat(resultado).hasStatus(estado);
        if (estado == 400) {
            assertThat(errores(resultado)).containsExactly(error("filas[" + fila + "].limiteInferior", mensaje));
            assertThat(enLaBase(empresaA)).as("no guardó nada").isEqualTo(D10);
        } else {
            assertThat(enLaBase(empresaA).get(fila).limiteInferior()).isEqualByComparingTo(limite);
        }
    }

    @ParameterizedTest(name = "Fuera de norma en {0}: {1}")
    @DisplayName("RF-13 · el límite de Fuera de norma es fijo en 0 %: con 0 se acepta y con otro valor no se guarda nada (prueba de límite)")
    @CsvSource({"0.0, 200", "0.1, 400"})
    void fueraDeNormaEsFijo(String limite, int estado) {
        List<Map<String, Object>> filas = filas(D10);
        filas.get(3).put("limiteInferior", new BigDecimal(limite));
        filas.get(3).put("recomendacion", "No vender.");

        MvcTestResult resultado = editar(tokenA, filas);

        assertThat(resultado).hasStatus(estado);
        if (estado == 400) {
            assertThat(errores(resultado)).containsExactly(error("filas[3].limiteInferior", FUERA_DE_NORMA_FIJO));
            assertThat(enLaBase(empresaA)).as("no guardó nada").isEqualTo(D10);
        } else {
            assertThat(enLaBase(empresaA).get(3).recomendacion()).as("sí cambia su texto").isEqualTo("No vender.");
        }
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("RF-13 · se editan las 4 categorías juntas: con menos filas, con más o con una categoría repetida no se guarda nada")
    @MethodSource("filasMalArmadas")
    void filasMalArmadas(String caso, List<Map<String, Object>> filas) {
        MvcTestResult resultado = editar(tokenA, filas);

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(error("filas", FILAS_COMPLETAS));
        assertThat(enLaBase(empresaA)).as("no guardó nada").isEqualTo(D10);
    }

    static Stream<Arguments> filasMalArmadas() {
        List<Map<String, Object>> tres = filas(D10);
        tres.remove(3);
        List<Map<String, Object>> cinco = filas(D10);
        cinco.add(fila(new Rango("ESTANDAR", new BigDecimal("91.0"), "Otra.")));
        List<Map<String, Object>> repetida = filas(D10);
        repetida.set(2, fila(new Rango("ESTANDAR", new BigDecimal("85.0"), "Repetida.")));
        return Stream.of(Arguments.of("3 filas", tres), Arguments.of("5 filas", cinco),
                Arguments.of("una categoría repetida", repetida));
    }

    @Test
    @DisplayName("RF-13 · sin filas responde 400 en filas y no guarda nada")
    void sinFilas() {
        MvcTestResult resultado = api.enviar(HttpMethod.PUT, RUTA, Map.of(), tokenA);

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(error("filas", OBLIGATORIO));
        assertThat(enLaBase(empresaA)).isEqualTo(D10);
    }

    @Test
    @DisplayName("RF-13 · una categoría que no existe responde 400 en el campo de su fila")
    void categoriaQueNoExiste() {
        List<Map<String, Object>> filas = filas(D10);
        filas.get(2).put("categoria", "SUPER");

        MvcTestResult resultado = editar(tokenA, filas);

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(resultado)).containsExactly(error("filas[2].categoria", VALOR_INVALIDO));
        assertThat(enLaBase(empresaA)).isEqualTo(D10);
    }

    @ParameterizedTest(name = "{0}: {2}")
    @DisplayName("RF-13 · la recomendación es obligatoria y tiene como máximo 300 caracteres: vacía, de solo espacios o de 301 se rechaza (prueba de límite)")
    @MethodSource("recomendaciones")
    void recomendacion(String caso, String recomendacion, int estado, String mensaje) {
        List<Map<String, Object>> filas = filas(D10);
        filas.get(0).put("recomendacion", recomendacion);

        MvcTestResult resultado = editar(tokenA, filas);

        assertThat(resultado).hasStatus(estado);
        if (estado == 400) {
            assertThat(errores(resultado)).containsExactly(error("filas[0].recomendacion", mensaje));
            assertThat(enLaBase(empresaA)).as("no guardó nada").isEqualTo(D10);
        } else {
            assertThat(enLaBase(empresaA).getFirst().recomendacion()).isEqualTo(recomendacion);
        }
    }

    static Stream<Arguments> recomendaciones() {
        return Stream.of(
                Arguments.of("vacía", "", 400, OBLIGATORIO),
                Arguments.of("de solo espacios", "   ", 400, OBLIGATORIO),
                Arguments.of("de 300 caracteres", "x".repeat(300), 200, null),
                Arguments.of("de 301 caracteres", "x".repeat(301), 400, LARGA));
    }

    /**
     * Mientras el cambio sale, la prueba hace de otro administrador: tiene tomadas las filas de la empresa
     * ({@code SELECT … FOR UPDATE}), baja Premium a 91 y Estándar a 85 y guarda cuando el cambio ya espera
     * en la base, sin depender de la suerte.
     *
     * <p>El cambio deja Premium en 96 y sube Estándar a 95. Sin el bloqueo, leería Premium 96 antes de que
     * el otro guardara, no la volvería a escribir y quedaría Premium 91 con Estándar 95, fuera de orden.
     * Con él, espera, lee lo que guardó el otro y deja sus 4 filas como las envió.
     */
    @Test
    @DisplayName("RF-13 · si dos administradores cambian los rangos a la vez, el segundo espera a que el primero guarde y deja sus 4 filas como las envió: los límites no quedan fuera de orden")
    void dosCambiosALaVez() throws Exception {
        List<Rango> cambio = new ArrayList<>(D10);
        cambio.set(1, new Rango("ESTANDAR", new BigDecimal("95.0"), D10.get(1).recomendacion()));
        ExecutorService hilos = Executors.newSingleThreadExecutor();
        try (Connection otro = fuente.getConnection()) {
            otro.setAutoCommit(false);
            try (PreparedStatement bloqueo = otro.prepareStatement(
                    "SELECT id FROM rango_recomendacion WHERE empresa_id = ? FOR UPDATE")) {
                bloqueo.setObject(1, empresaA);
                bloqueo.executeQuery().close();
            }
            Future<MvcTestResult> peticion = hilos.submit(() -> editar(tokenA, filas(cambio)));
            esperarABloquearse(peticion);
            try (PreparedStatement guardar = otro.prepareStatement(
                    "UPDATE rango_recomendacion SET limite_inferior = ? WHERE empresa_id = ? AND categoria = ?")) {
                for (Map.Entry<String, BigDecimal> limite : Map.of("PREMIUM", new BigDecimal("91.0"),
                        "ESTANDAR", new BigDecimal("85.0")).entrySet()) {
                    guardar.setBigDecimal(1, limite.getValue());
                    guardar.setObject(2, empresaA);
                    guardar.setString(3, limite.getKey());
                    guardar.executeUpdate();
                }
            }
            otro.commit();
            assertThat(peticion.get(30, TimeUnit.SECONDS)).hasStatus(HttpStatus.OK);
        } finally {
            hilos.shutdownNow();
        }

        assertThat(enLaBase(empresaA)).isEqualTo(cambio);
    }

    @Test
    @DisplayName("RF-13 · C3 · los análisis anteriores no cambian: un lote ya analizado conserva su categoría y su recomendación al editar y al restaurar los rangos")
    void losAnalisisAnterioresNoCambian() {
        UUID lote = loteAnalizado();
        Map<String, Object> antes = resultadoDelLote(lote);
        List<Map<String, Object>> filas = filas(D10);
        filas.get(1).put("limiteInferior", new BigDecimal("92.0"));
        filas.get(1).put("recomendacion", "Ahora con 91 % sería Corriente.");

        assertThat(editar(tokenA, filas)).hasStatus(HttpStatus.OK);
        assertThat(resultadoDelLote(lote)).as("después de editar").isEqualTo(antes);
        assertThat(restaurar(tokenA)).hasStatus(HttpStatus.OK);
        assertThat(resultadoDelLote(lote)).as("después de restaurar").isEqualTo(antes);
    }

    @Test
    @DisplayName("RF-13 · C4 · «Restaurar valores por defecto» vuelve a los rangos de D-10, también si se repite")
    void restaurarVuelveALosDeD10() {
        assertThat(editar(tokenA, filas(OTROS))).hasStatus(HttpStatus.OK);

        MvcTestResult resultado = restaurar(tokenA);

        assertThat(resultado).hasStatus(HttpStatus.OK);
        assertThat(deLaRespuesta(resultado)).isEqualTo(D10);
        assertThat(enLaBase(empresaA)).isEqualTo(D10);
        assertThat(restaurar(tokenA)).hasStatus(HttpStatus.OK);
        assertThat(enLaBase(empresaA)).as("repetirlo no cambia nada").isEqualTo(D10);
    }

    @Test
    @DisplayName("RF-13 · C4 · restaurar deja lo mismo que recibe una empresa nueva (RF-02): los valores por defecto salen de una sola fuente")
    void restaurarIgualAUnaEmpresaNueva() {
        assertThat(editar(tokenA, filas(OTROS))).hasStatus(HttpStatus.OK);
        assertThat(restaurar(tokenA)).hasStatus(HttpStatus.OK);

        UUID nueva = crearEmpresa("Cacaos de Pitalito", "899999068-1", "admin@pitalito.test");

        assertThat(enLaBase(empresaA)).isEqualTo(enLaBase(nueva));
    }

    @Test
    @DisplayName("RF-13 · restaurar repone una categoría que falte: la empresa siempre queda con las 4")
    void restaurarReponeLaQueFalte() {
        jdbc.update("DELETE FROM rango_recomendacion WHERE empresa_id = ? AND categoria = 'CORRIENTE'", empresaA);

        assertThat(restaurar(tokenA)).hasStatus(HttpStatus.OK);

        assertThat(enLaBase(empresaA)).isEqualTo(D10);
    }

    @Test
    @DisplayName("RF-13 · C5 · un usuario final no puede entrar a los rangos: listar, editar y restaurar responden 403 y no cambian nada (RNF-04)")
    void unUsuarioFinalNoEntra() {
        api.crearUsuario(empresaA, "Luis Gómez", LUIS, "USUARIO_FINAL");
        String tokenLuis = api.token(LUIS);
        assertThat(editar(tokenA, filas(OTROS))).hasStatus(HttpStatus.OK);

        for (MvcTestResult resultado : List.of(listar(tokenLuis), editar(tokenLuis, filas(D10)), restaurar(tokenLuis))) {
            assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN);
        }
        assertThat(enLaBase(empresaA)).isEqualTo(OTROS);
    }

    @Test
    @DisplayName("RNF-04 · C2 · el administrador de la plataforma tampoco gestiona los rangos de una empresa: responde 403")
    void laPlataformaTampoco() {
        for (MvcTestResult resultado : List.of(listar(tokenPlataforma), editar(tokenPlataforma, filas(OTROS)),
                restaurar(tokenPlataforma))) {
            assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN);
        }
        assertThat(enLaBase(empresaA)).isEqualTo(D10);
    }

    @Test
    @DisplayName("RNF-04 · C1 · sin sesión, las rutas de los rangos responden 401")
    void sinSesion() {
        for (MvcTestResult resultado : List.of(listar(null), editar(null, filas(OTROS)), restaurar(null))) {
            assertThat(resultado).hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    @DisplayName("RNF-03 · C2 · cada empresa lista solo sus 4 rangos")
    void cadaEmpresaListaLosSuyos() {
        assertThat(editar(tokenB, filas(OTROS))).hasStatus(HttpStatus.OK);

        MvcTestResult deA = listar(tokenA);

        assertThat(deLaRespuesta(deA)).isEqualTo(D10);
        assertThat(lista(deA)).extracting(fila -> UUID.fromString((String) fila.get("id")))
                .containsExactlyInAnyOrderElementsOf(ids(empresaA));
    }

    @Test
    @DisplayName("RNF-03 · C5 · editar y restaurar los rangos de una empresa no toca los de otra, aunque el cuerpo traiga sus identificadores y su empresa (prueba de seguridad)")
    void noTocaLosDeOtraEmpresa() {
        assertThat(editar(tokenB, filas(OTROS))).hasStatus(HttpStatus.OK);
        List<UUID> deB = ids(empresaB);
        List<Map<String, Object>> filas = filas(D10);
        for (int i = 0; i < filas.size(); i++) {
            filas.get(i).put("id", deB.get(i).toString());
            filas.get(i).put("empresaId", empresaB.toString());
        }
        filas.get(0).put("recomendacion", "Cambiada por la empresa A.");

        assertThat(editar(tokenA, filas)).hasStatus(HttpStatus.OK);
        assertThat(restaurar(tokenA)).hasStatus(HttpStatus.OK);

        assertThat(enLaBase(empresaB)).as("los de la otra empresa no cambiaron").isEqualTo(OTROS);
        assertThat(ids(empresaB)).isEqualTo(deB);
    }

    /** Crea una empresa por la API de RF-02, con su administrador, y devuelve su identificador. */
    private UUID crearEmpresa(String nombre, String nit, String correoAdmin) {
        Map<String, Object> administrador = Map.of("nombre", "Administrador de " + nombre, "correo", correoAdmin,
                "contrasena", CONTRASENA);
        Map<String, Object> datos = Map.of("nombre", nombre, "nit", nit, "ciudad", "Neiva", "administrador", administrador);
        MvcTestResult resultado = api.enviar(HttpMethod.POST, "/api/v1/empresas", datos, tokenPlataforma);
        assertThat(resultado).hasStatus(HttpStatus.CREATED);
        return UUID.fromString((String) json(resultado).get("id"));
    }

    private MvcTestResult listar(String token) {
        return api.enviar(HttpMethod.GET, RUTA, null, token);
    }

    private MvcTestResult editar(String token, List<Map<String, Object>> filas) {
        return api.enviar(HttpMethod.PUT, RUTA, Map.of("filas", filas), token);
    }

    private MvcTestResult restaurar(String token) {
        return api.enviar(HttpMethod.POST, RESTAURAR, null, token);
    }

    /** Las filas de la edición en bloque, en el orden dado, listas para cambiarles un dato. */
    private static List<Map<String, Object>> filas(List<Rango> rangos) {
        List<Map<String, Object>> filas = new ArrayList<>();
        rangos.forEach(rango -> filas.add(fila(rango)));
        return filas;
    }

    private static Map<String, Object> fila(Rango rango) {
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("categoria", rango.categoria());
        fila.put("limiteInferior", rango.limiteInferior());
        fila.put("recomendacion", rango.recomendacion());
        return fila;
    }

    /** Los rangos de una respuesta, en su orden. */
    private static List<Rango> deLaRespuesta(MvcTestResult resultado) {
        return lista(resultado).stream()
                .map(fila -> new Rango((String) fila.get("categoria"),
                        new BigDecimal(String.valueOf(fila.get("limiteInferior"))), (String) fila.get("recomendacion")))
                .toList();
    }

    /**
     * Los límites de una respuesta tal como vienen escritos en el JSON, como 90.5 o 1E+1: al leerlos como
     * números, 90.5 y 90.50 serían iguales.
     */
    private static List<String> limitesComoVienen(MvcTestResult resultado) {
        String cuerpo = new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        Matcher limite = Pattern.compile("\"limiteInferior\":([^,}]+)").matcher(cuerpo);
        List<String> limites = new ArrayList<>();
        while (limite.find()) {
            limites.add(limite.group(1));
        }
        return limites;
    }

    /** Los rangos de una empresa en la base, en el orden de las categorías. */
    private List<Rango> enLaBase(UUID empresa) {
        return jdbc.query("""
                SELECT categoria, limite_inferior, recomendacion FROM rango_recomendacion WHERE empresa_id = ?
                ORDER BY array_position(ARRAY['PREMIUM', 'ESTANDAR', 'CORRIENTE', 'FUERA_DE_NORMA'], categoria::text)""",
                (fila, numero) -> new Rango(fila.getString("categoria"), fila.getBigDecimal("limite_inferior"),
                        fila.getString("recomendacion")),
                empresa);
    }

    /** Los identificadores de los rangos de una empresa, en el orden de las categorías. */
    private List<UUID> ids(UUID empresa) {
        return jdbc.queryForList("""
                SELECT id FROM rango_recomendacion WHERE empresa_id = ?
                ORDER BY array_position(ARRAY['PREMIUM', 'ESTANDAR', 'CORRIENTE', 'FUERA_DE_NORMA'], categoria::text)""",
                UUID.class, empresa);
    }

    /** Un lote de la empresa A ya analizado, con su resultado guardado como lo guardará RF-12. */
    private UUID loteAnalizado() {
        UUID usuario = jdbc.queryForObject("SELECT id FROM usuario WHERE correo = ?", UUID.class, ADMIN_A);
        return jdbc.queryForObject("""
                INSERT INTO lote (empresa_id, codigo, fecha_recepcion, origen, estado, usuario_id, porcentaje_sanos,
                                  categoria, recomendacion, sugerir_otra_muestra, aviso_confianza_baja, fecha_resultado)
                VALUES (?, 'L-001', current_date, 'Finca La Esperanza', 'ANALIZADO', ?, 91.0, 'ESTANDAR', ?, false, false,
                        now())
                RETURNING id""", UUID.class, empresaA, usuario, D10.get(1).recomendacion());
    }

    /** Espera, hasta 10 segundos, a que la petición haya terminado o esté esperando un bloqueo de la base. */
    private void esperarABloquearse(Future<MvcTestResult> peticion) throws InterruptedException {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!peticion.isDone() && esperandoUnBloqueo() == 0) {
            assertThat(System.nanoTime()).as("la petición no llegó a la base").isLessThan(limite);
            Thread.sleep(20);
        }
    }

    /** Cuántas conexiones a la base esperan un bloqueo que tiene otra transacción. */
    private int esperandoUnBloqueo() {
        return jdbc.queryForObject("""
                SELECT count(*) FROM pg_stat_activity
                WHERE datname = current_database() AND wait_event_type = 'Lock'""", Integer.class);
    }

    private Map<String, Object> resultadoDelLote(UUID lote) {
        return jdbc.queryForMap("""
                SELECT estado, porcentaje_sanos, categoria, recomendacion, sugerir_otra_muestra, fecha_resultado
                FROM lote WHERE id = ?""", lote);
    }

    /**
     * Un rango: su categoría, su límite inferior y su recomendación. El límite queda con un decimal, para
     * comparar por valor: en JSON, 90.5 y 90.50 son el mismo número.
     */
    private record Rango(String categoria, BigDecimal limiteInferior, String recomendacion) {

        Rango {
            limiteInferior = limiteInferior.setScale(1, RoundingMode.UNNECESSARY);
        }
    }
}
