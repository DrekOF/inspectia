/*
 * Nombre: SegundaMuestraIT.java
 * Descripción: Pruebas de integración de la segunda muestra de un lote (D-25): con otra muestra sugerida, el lote
 *              analizado recibe las fotos de la muestra 2 sin tocar la 1, la envía, la cola la atiende y el resultado
 *              se recalcula con la suma de las dos y los rangos vigentes, sin sugerencia ni límite; sin sugerencia, o
 *              con 2 muestras, no admite otra; una foto pedida para una muestra que ya no recibe fotos responde 409
 *              (RF-12 C8 y C12, RF-14 C3 y C6, RF-10 C4 y C11, RF-13 C2, RF-05 C2, RF-08, RF-15, D-47).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis;

import static co.edu.usco.inspectia.analisis.ApiAnalisis.FOTOS_MINIMO;
import static co.edu.usco.inspectia.analisis.ApiAnalisis.LOTE_ANALIZADO;
import static co.edu.usco.inspectia.analisis.ApiAnalisis.fotos;
import static co.edu.usco.inspectia.analisis.ApiAnalisis.granos;
import static co.edu.usco.inspectia.analisis.ApiAnalisis.lote;
import static co.edu.usco.inspectia.analisis.ApiAnalisis.muestras;
import static co.edu.usco.inspectia.analisis.ApiFotos.LOTE_NO_PENDIENTE;
import static co.edu.usco.inspectia.analisis.ApiFotos.MUESTRA_CAMBIO;
import static co.edu.usco.inspectia.analisis.ApiFotos.PNG;
import static co.edu.usco.inspectia.analisis.ApiFotos.SIN_OTRA_MUESTRA;
import static co.edu.usco.inspectia.analisis.ApiFotos.png;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.error;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.errores;
import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.ConfiguracionContenedores;
import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.analisis.aplicacion.AtenderCola;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import java.math.BigDecimal;
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
 * La segunda muestra de un lote (D-25, RF-14 C6). En Cacaos del Huila, Luis Gómez analiza el lote L-0143 de La
 * Esperanza; Pedro Díaz, su compañero, puede tomar la muestra 2, y Ana Pérez, la administradora, solo la consulta. La
 * cola va a mano: estas pruebas la atienden con {@link AtenderCola#atenderUna()}.
 *
 * <p>Con el clasificador falso (no sanos = ancho mod 50, 50 granos por foto), las fotos de 106 × 100 y 107 × 100 dan
 * 87 de 100: 90,6 %, Estándar, con sugerencia por el límite de Estándar (90 %). Una muestra 2 igual da 174 de 200:
 * 90,6 %, sin sugerencia (D-25); con 108 × 100 y 107 × 100, 172 de 200: 88,3 %, Corriente. Las consultas a la tabla
 * {@code muestra} van siempre con su número: el lote tiene dos.
 *
 * <p>Como la página, cada enlace de subida dice qué muestra espera: {@code fotos} y {@code analisis} piden los de la
 * muestra 1, y {@code fotosDeLaDos} y {@code analisisDeLaDos}, los de la 2.
 */
@PruebaIntegracion
class SegundaMuestraIT {

    private static final String LUIS = "luis@cacaos.test";
    private static final String PEDRO = "pedro@cacaos.test";
    private static final String ANA = "admin@cacaos.test";
    private static final int[] SEIS_NO_SANOS = {106, 100};
    private static final int[] SIETE_NO_SANOS = {107, 100};
    private static final int[] OCHO_NO_SANOS = {108, 100};
    private static final int[] SANAS = {100, 100};

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AtenderCola atender;

    @Autowired
    private ClasificadorProgramable clasificador;

    private ApiFotos fotos;
    private ApiAnalisis analisis;
    private ApiFotos fotosDeLaDos;
    private ApiAnalisis analisisDeLaDos;
    private UUID empresa;
    private UUID luis;
    private UUID pedro;
    private String tokenDeLuis;
    private String tokenDePedro;
    private String tokenDeAna;
    private UUID lote;

    @BeforeEach
    void preparar() {
        ApiSesion api = new ApiSesion(mvc, jdbc);
        fotos = new ApiFotos(api, mvc);
        analisis = new ApiAnalisis(api, fotos);
        fotosDeLaDos = fotos.deLaMuestra(2);
        analisisDeLaDos = new ApiAnalisis(api, fotosDeLaDos);
        ApiSesion.borrarDatos(jdbc);
        empresa = api.crearEmpresa("Cacaos del Huila", "900123456-8");
        luis = api.crearUsuario(empresa, "Luis Gómez", LUIS, "USUARIO_FINAL");
        pedro = api.crearUsuario(empresa, "Pedro Díaz", PEDRO, "USUARIO_FINAL");
        api.crearUsuario(empresa, "Ana Pérez", ANA, "ADMIN_EMPRESA");
        tokenDeLuis = api.token(LUIS);
        tokenDePedro = api.token(PEDRO);
        tokenDeAna = api.token(ANA);
        lote = new ApiLotes(api).registrarOk(tokenDeLuis, "L-0143", "La Esperanza");
    }

    @AfterEach
    void limpiar() {
        clasificador.restablecer();
        ApiSesion.borrarDatos(jdbc);
        ConfiguracionContenedores.vaciarCarpetaDeFotos();
    }

    /** Sube las fotos de la muestra 1, la envía y atiende la cola. */
    private void analizar(String token, int[]... medidas) {
        analizar(analisis, token, medidas);
    }

    /** Sube las fotos de la muestra 2, la envía y atiende la cola. */
    private void analizarLaDos(String token, int[]... medidas) {
        analizar(analisisDeLaDos, token, medidas);
    }

    /** Sube las fotos con las llamadas de su muestra, la envía y atiende la cola. */
    private void analizar(ApiAnalisis deSuMuestra, String token, int[]... medidas) {
        deSuMuestra.subirYEnviar(token, lote, medidas);
        assertThat(atender.atenderUna()).as("la cola atiende la muestra").isTrue();
    }

    /** La muestra 1 de 87 de 100: el lote queda en 90,6 %, Estándar, con sugerencia por el límite de 90. */
    private void primeraMuestraConSugerencia() {
        analizar(tokenDeLuis, SEIS_NO_SANOS, SIETE_NO_SANOS);
        assertThat(jdbc.queryForMap("SELECT estado, sugerir_otra_muestra FROM lote WHERE id = ?", lote))
                .as("la muestra 1 deja el lote analizado y con sugerencia")
                .containsEntry("estado", "ANALIZADO").containsEntry("sugerir_otra_muestra", true);
    }

    /** Las dos muestras: la 1 con sugerencia y la 2 con las fotos dadas, ya atendida por la cola. */
    private Map<String, Object> dosMuestras(int[]... segunda) {
        primeraMuestraConSugerencia();
        analizarLaDos(tokenDeLuis, segunda);
        return analisis.resultadoOk(tokenDeLuis, lote);
    }

    /**
     * Los ejemplos de RF-10 C4 y RF-12 C12, de punta a punta: la muestra 1 con 107 sanos de 120 (fotos de 105, 105 y
     * 103 de ancho, y la cola con los primeros 120 granos) y la 2 con 113 de 125 (fotos de 104 de ancho y 125 granos).
     */
    private Map<String, Object> dosMuestrasDeLaFicha() {
        clasificador.conGranos(120);
        analizar(tokenDeLuis, new int[] {105, 100}, new int[] {105, 100}, new int[] {103, 100});
        assertThat(resultadoGuardado()).as("95,4 %, a 0,6 de 96: la muestra 1 sugiere otra")
                .containsEntry("estado", "ANALIZADO").containsEntry("sugerir_otra_muestra", true);
        clasificador.conGranos(125);
        analizarLaDos(tokenDeLuis, new int[] {104, 100}, new int[] {104, 100}, new int[] {104, 100});
        return analisis.resultadoOk(tokenDeLuis, lote);
    }

    /** Una muestra del lote, por su número. */
    private Map<String, Object> muestra(int numero) {
        return jdbc.queryForMap("SELECT id, estado, usuario_id, total_granos, granos_sanos, fecha_envio FROM muestra "
                + "WHERE lote_id = ? AND numero = ?", lote, numero);
    }

    /** Las fotos de una muestra del lote, por número, con su identificador, su ruta y su estado. */
    private List<Map<String, Object>> fotosDeLaMuestra(int numero) {
        return jdbc.queryForList("SELECT f.id, f.numero, f.ruta, f.estado FROM foto f JOIN muestra m ON m.id = "
                + "f.muestra_id WHERE m.lote_id = ? AND m.numero = ? ORDER BY f.numero", lote, numero);
    }

    /** Cuántos granos guardó una muestra del lote. */
    private int granosDeLaMuestra(int numero) {
        return jdbc.queryForObject("SELECT count(*) FROM grano g JOIN foto f ON f.id = g.foto_id JOIN muestra m ON "
                + "m.id = f.muestra_id WHERE m.lote_id = ? AND m.numero = ?", Integer.class, lote, numero);
    }

    private List<Integer> numerosDeMuestra() {
        return jdbc.queryForList("SELECT numero FROM muestra WHERE lote_id = ? ORDER BY numero", Integer.class, lote);
    }

    /** El resultado guardado en el lote, para ver que no cambia. */
    private Map<String, Object> resultadoGuardado() {
        return jdbc.queryForMap("SELECT estado, porcentaje_sanos, margen, categoria, recomendacion, "
                + "sugerir_otra_muestra, limite_sugerencia, categoria_del_limite, aviso_confianza_baja, "
                + "fecha_resultado FROM lote WHERE id = ?", lote);
    }

    /**
     * Una llave de la muestra 2 con la forma que arma el backend para este lote, para confirmar una foto que no se
     * pidió.
     */
    private String llaveDelLote() {
        return ApiFotos.llave(empresa, lote, 2, "png");
    }

    @Test
    @DisplayName("RF-14 · C6 · con otra muestra sugerida, la lista de fotos del lote analizado va vacía y sin número, lista para la muestra 2")
    void lasFotosDeLaMuestraDosVanVacias() {
        primeraMuestraConSugerencia();

        MvcTestResult resultado = fotos.listar(tokenDeLuis, lote);

        assertThat(resultado).hasStatus(HttpStatus.OK);
        assertThat(json(resultado)).containsEntry("muestra", null).containsEntry("estado", null)
                .containsEntry("fotos", List.of());
    }

    @Test
    @DisplayName("RF-14 · C6 · un lote con otra muestra sugerida permite analizarla desde el detalle: las 2 fotos de la muestra 2 se suben y se envían, la cola la atiende y el resultado suma los granos de las dos, 174 de 200 con 90,6 %, sin sugerencia ni límite")
    void analizarLaMuestraDos() {
        primeraMuestraConSugerencia();
        assertThat(lote(analisis.resultadoOk(tokenDeLuis, lote))).containsEntry("sugerirOtraMuestra", true)
                .containsEntry("limiteSugerencia", 90.0).containsEntry("categoriaDelLimite", "ESTANDAR");

        analisisDeLaDos.subirFotos(tokenDePedro, lote, SEIS_NO_SANOS, SIETE_NO_SANOS);
        Map<String, Object> laLista = json(fotos.listar(tokenDePedro, lote));
        Map<String, Object> enviada = analisis.enviarOk(tokenDePedro, lote);
        Map<String, Object> enEspera = analisis.avanceOk(tokenDeLuis, lote);
        assertThat(atender.atenderUna()).isTrue();
        Map<String, Object> resultado = analisis.resultadoOk(tokenDeLuis, lote);

        assertThat(laLista).containsEntry("muestra", 2).containsEntry("estado", "PENDIENTE");
        assertThat(enviada).containsEntry("muestra", 2).containsEntry("estado", "EN_ESPERA");
        assertThat(enEspera).containsEntry("muestra", 2).containsEntry("estado", "EN_ESPERA");
        assertThat(lote(resultado)).containsEntry("estado", "ANALIZADO").containsEntry("porcentajeSanos", 90.6)
                .containsEntry("margen", 5.3).containsEntry("categoria", "ESTANDAR")
                .containsEntry("sugerirOtraMuestra", false).containsEntry("limiteSugerencia", null)
                .containsEntry("categoriaDelLimite", null).containsEntry("totalGranos", 200)
                .containsEntry("granosSanos", 174).containsEntry("porcentajeContado", 87.0);
        assertThat(muestras(resultado)).extracting(muestra -> muestra.get("numero")).containsExactly(1, 2);
        assertThat(muestras(resultado)).allSatisfy(muestra -> assertThat(muestra).containsEntry("estado", "TERMINADA")
                .containsEntry("totalGranos", 100).containsEntry("granosSanos", 87)
                .containsEntry("porcentajeContado", 87.0));
        assertThat(muestras(resultado).get(0))
                .containsEntry("analizadaPor", Map.of("id", luis.toString(), "nombre", "Luis Gómez"));
        assertThat(muestras(resultado).get(1))
                .containsEntry("analizadaPor", Map.of("id", pedro.toString(), "nombre", "Pedro Díaz"));
        assertThat(lote(resultado).get("fechaResultado")).as("la fecha del resultado es el fin de la muestra 2")
                .isEqualTo(muestras(resultado).get(1).get("fechaFin"));
        assertThat(fotos(muestras(resultado).get(1))).hasSize(2)
                .allSatisfy(foto -> assertThat(granos(foto)).hasSize(50));
        assertThat(jdbc.queryForMap("SELECT sugerir_otra_muestra, limite_sugerencia, categoria_del_limite FROM lote "
                + "WHERE id = ?", lote)).containsEntry("sugerir_otra_muestra", false)
                .containsEntry("limite_sugerencia", null).containsEntry("categoria_del_limite", null);
        assertThat(muestra(2)).containsEntry("estado", "TERMINADA").containsEntry("usuario_id", pedro);
        assertThat(granosDeLaMuestra(1)).isEqualTo(100);
        assertThat(granosDeLaMuestra(2)).isEqualTo(100);
    }

    @Test
    @DisplayName("RF-14 · C3 · mientras la muestra 2 espera, el detalle trae el resultado guardado de la muestra 1, con su sugerencia, y las dos muestras: la 2 en espera, con sus fotos sin granos")
    void mientrasLaMuestraDosEspera() {
        primeraMuestraConSugerencia();
        analisisDeLaDos.subirYEnviar(tokenDeLuis, lote, SEIS_NO_SANOS, SIETE_NO_SANOS);

        Map<String, Object> resultado = analisis.resultadoOk(tokenDeAna, lote);

        assertThat(lote(resultado)).containsEntry("estado", "ANALIZADO").containsEntry("porcentajeSanos", 90.6)
                .containsEntry("margen", 7.5).containsEntry("categoria", "ESTANDAR")
                .containsEntry("sugerirOtraMuestra", true).containsEntry("limiteSugerencia", 90.0)
                .containsEntry("categoriaDelLimite", "ESTANDAR").containsEntry("totalGranos", 100)
                .containsEntry("granosSanos", 87).containsEntry("porcentajeContado", 87.0);
        assertThat(muestras(resultado)).extracting(muestra -> muestra.get("numero")).containsExactly(1, 2);
        assertThat(fotos(muestras(resultado).get(0))).allSatisfy(foto -> assertThat(granos(foto)).hasSize(50));
        Map<String, Object> segunda = muestras(resultado).get(1);
        assertThat(segunda).containsEntry("estado", "EN_ESPERA").containsEntry("totalGranos", null)
                .containsEntry("granosSanos", null).containsEntry("fechaFin", null);
        assertThat(fotos(segunda)).hasSize(2).allSatisfy(foto -> assertThat(granos(foto)).isEmpty());
    }

    @Test
    @DisplayName("RF-12 · la primera foto de la muestra 2 crea la muestra 2 con quien la subió y no toca las fotos, los archivos ni los granos de la muestra 1 (D-25)")
    void laPrimeraFotoNoTocaLaMuestraUno() {
        primeraMuestraConSugerencia();
        List<Map<String, Object>> deLaMuestraUno = fotosDeLaMuestra(1);

        Map<String, Object> foto = fotosDeLaDos.subirFoto(tokenDePedro, lote, 1, PNG, png(106, 100));

        assertThat(foto).containsEntry("numero", 1).containsEntry("estado", "CARGADA");
        assertThat(numerosDeMuestra()).containsExactly(1, 2);
        assertThat(muestra(1)).containsEntry("estado", "TERMINADA").containsEntry("usuario_id", luis);
        assertThat(muestra(2)).containsEntry("estado", "PENDIENTE").containsEntry("usuario_id", pedro);
        assertThat(fotosDeLaMuestra(1)).as("las fotos de la muestra 1, sin reemplazar").isEqualTo(deLaMuestraUno);
        assertThat(deLaMuestraUno).extracting(fila -> fila.get("estado")).containsOnly("APROBADA");
        assertThat(deLaMuestraUno).allSatisfy(fila -> assertThat(ConfiguracionContenedores.CARPETA_DE_FOTOS
                .resolve((String) fila.get("ruta"))).as("su archivo sigue en la carpeta").exists());
        assertThat(granosDeLaMuestra(1)).isEqualTo(100);
        assertThat(fotosDeLaMuestra(2)).singleElement()
                .satisfies(fila -> assertThat(fila).containsEntry("numero", 1).containsEntry("estado", "CARGADA"));
    }

    @Test
    @DisplayName("D-25 · una foto pedida para la muestra 1 y confirmada después de su análisis responde 409 porque el lote cambió: no crea la muestra 2 y la muestra 1 conserva sus fotos y sus granos")
    void laFotoPedidaParaLaMuestraUnoNoCreaLaDos() {
        analisis.subirFotos(tokenDeLuis, lote, SEIS_NO_SANOS, SIETE_NO_SANOS);
        byte[] reemplazo = png(106, 100);
        Map<String, Object> subida = fotos.subida(tokenDePedro, lote, 1, PNG, reemplazo.length);
        assertThat(fotos.subir((String) subida.get("url"), PNG, reemplazo))
                .as("Pedro sube el reemplazo y no lo confirma").hasStatus(HttpStatus.OK);
        analisis.enviarOk(tokenDeLuis, lote);
        assertThat(atender.atenderUna()).as("la cola atiende la muestra 1").isTrue();
        Map<String, Object> antes = resultadoGuardado();
        assertThat(antes).containsEntry("estado", "ANALIZADO").containsEntry("sugerir_otra_muestra", true);
        List<Map<String, Object>> deLaMuestraUno = fotosDeLaMuestra(1);

        MvcTestResult confirmada = fotos.confirmar(tokenDePedro, lote, 1, (String) subida.get("llave"));

        assertThat(confirmada).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(confirmada)).containsEntry("detail", MUESTRA_CAMBIO);
        assertThat(numerosDeMuestra()).containsExactly(1);
        assertThat(fotosDeLaMuestra(1)).as("las fotos de la muestra 1, sin reemplazar").hasSize(2)
                .isEqualTo(deLaMuestraUno);
        assertThat(granosDeLaMuestra(1)).isEqualTo(100);
        assertThat(resultadoGuardado()).isEqualTo(antes);
    }

    @Test
    @DisplayName("D-25 · confirmar dos veces la llave de la primera foto de la muestra 2 es seguro: las dos responden la misma foto, que crea una sola muestra 2 con una sola foto")
    void confirmarDosVecesLaPrimeraDeLaDos() {
        primeraMuestraConSugerencia();
        byte[] contenido = png(106, 100);
        Map<String, Object> subida = fotosDeLaDos.subida(tokenDePedro, lote, 1, PNG, contenido.length);
        assertThat(fotos.subir((String) subida.get("url"), PNG, contenido)).hasStatus(HttpStatus.OK);

        MvcTestResult primera = fotos.confirmar(tokenDePedro, lote, 1, (String) subida.get("llave"));
        MvcTestResult segunda = fotos.confirmar(tokenDeLuis, lote, 1, (String) subida.get("llave"));

        assertThat(primera).hasStatus(HttpStatus.OK);
        assertThat(segunda).hasStatus(HttpStatus.OK);
        assertThat(json(segunda)).containsEntry("numero", 1)
                .containsEntry("tamanoBytes", json(primera).get("tamanoBytes"));
        assertThat(numerosDeMuestra()).containsExactly(1, 2);
        assertThat(fotosDeLaMuestra(2)).singleElement().extracting(foto -> foto.get("ruta"))
                .isEqualTo(subida.get("llave"));
    }

    @Test
    @DisplayName("D-25 · con la muestra 1 terminada y otra muestra sugerida, pedir el enlace de una foto para la muestra 1 responde 409 porque el lote cambió, y para la muestra 2 da el enlace, con la muestra en la llave")
    void elEnlaceSoloParaLaMuestraQueRecibe() {
        primeraMuestraConSugerencia();

        MvcTestResult paraLaUno = fotos.pedirSubida(tokenDeLuis, lote, 1, PNG, 1_000);
        MvcTestResult paraLaDos = fotosDeLaDos.pedirSubida(tokenDeLuis, lote, 1, PNG, 1_000);

        assertThat(paraLaUno).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(paraLaUno)).containsEntry("detail", MUESTRA_CAMBIO);
        assertThat(paraLaDos).hasStatus(HttpStatus.CREATED);
        assertThat((String) json(paraLaDos).get("llave"))
                .matches(empresa + "/" + lote + "/m2-[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}\\.png");
        assertThat(numerosDeMuestra()).containsExactly(1);
    }

    @Test
    @DisplayName("RF-12 · C8 · un lote analizado sin sugerencia no admite otra muestra: pedir el enlace y confirmar una foto responden 409 con el motivo, enviarlo responde 409, y no se crea otra muestra")
    void sinSugerenciaNoAdmiteOtra() {
        analizar(tokenDeLuis, SANAS, SANAS);
        Map<String, Object> antes = resultadoGuardado();
        assertThat(antes).as("100 %, a 4 puntos de 96").containsEntry("sugerir_otra_muestra", false);

        MvcTestResult pedir = fotosDeLaDos.pedirSubida(tokenDeLuis, lote, 1, PNG, 1_000);
        MvcTestResult confirmar = fotos.confirmar(tokenDeLuis, lote, 1, llaveDelLote());
        MvcTestResult enviar = analisis.enviar(tokenDeLuis, lote);

        assertThat(pedir).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(pedir)).containsEntry("detail", SIN_OTRA_MUESTRA);
        assertThat(confirmar).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(confirmar)).containsEntry("detail", SIN_OTRA_MUESTRA);
        assertThat(enviar).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(enviar)).containsEntry("detail", LOTE_ANALIZADO);
        assertThat(numerosDeMuestra()).containsExactly(1);
        assertThat(fotosDeLaMuestra(1)).hasSize(2);
        assertThat(resultadoGuardado()).isEqualTo(antes);
        assertThat(json(fotos.listar(tokenDeLuis, lote))).as("la lista sigue con la muestra 1")
                .containsEntry("muestra", 1).containsEntry("estado", "TERMINADA");
    }

    @Test
    @DisplayName("RF-12 · C8 · un lote que ya tiene 2 muestras no admite otra: pedir el enlace y confirmar una foto responden 409 con el motivo, enviarlo responde 409, y su resultado no cambia")
    void conDosMuestrasNoAdmiteOtra() {
        dosMuestras(SEIS_NO_SANOS, SIETE_NO_SANOS);
        Map<String, Object> antes = resultadoGuardado();

        MvcTestResult pedir = fotosDeLaDos.pedirSubida(tokenDeLuis, lote, 1, PNG, 1_000);
        MvcTestResult confirmar = fotos.confirmar(tokenDeLuis, lote, 1, llaveDelLote());
        MvcTestResult enviar = analisis.enviar(tokenDeLuis, lote);

        assertThat(pedir).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(pedir)).containsEntry("detail", SIN_OTRA_MUESTRA);
        assertThat(confirmar).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(confirmar)).containsEntry("detail", SIN_OTRA_MUESTRA);
        assertThat(enviar).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(enviar)).containsEntry("detail", LOTE_ANALIZADO);
        assertThat(numerosDeMuestra()).containsExactly(1, 2);
        assertThat(resultadoGuardado()).isEqualTo(antes);
        assertThat(atender.atenderUna()).as("la cola sigue vacía").isFalse();
    }

    @Test
    @DisplayName("RF-05 · C2 · la muestra 2 también necesita al menos 2 fotos: sin fotos o con 1, enviarla responde 400 en «fotos» y el lote conserva su resultado (prueba de límite)")
    void laMuestraDosNecesitaDosFotos() {
        primeraMuestraConSugerencia();
        Map<String, Object> antes = resultadoGuardado();

        MvcTestResult sinFotos = analisis.enviar(tokenDeLuis, lote);
        fotosDeLaDos.subirFoto(tokenDeLuis, lote, 1, PNG, png(106, 100));
        MvcTestResult conUna = analisis.enviar(tokenDeLuis, lote);

        assertThat(sinFotos).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(sinFotos)).containsExactly(error("fotos", FOTOS_MINIMO));
        assertThat(conUna).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(errores(conUna)).containsExactly(error("fotos", FOTOS_MINIMO));
        assertThat(muestra(2)).containsEntry("estado", "PENDIENTE").containsEntry("fecha_envio", null);
        assertThat(resultadoGuardado()).isEqualTo(antes);
        assertThat(atender.atenderUna()).as("la cola sigue vacía").isFalse();
    }

    @Test
    @DisplayName("RF-12 · la muestra 2 enviada ya no recibe fotos: responde 409 porque ya no está pendiente")
    void laMuestraDosEnviadaNoRecibeFotos() {
        primeraMuestraConSugerencia();
        analisisDeLaDos.subirYEnviar(tokenDeLuis, lote, SEIS_NO_SANOS, SIETE_NO_SANOS);

        MvcTestResult resultado = fotosDeLaDos.pedirSubida(tokenDeLuis, lote, 3, PNG, 1_000);

        assertThat(resultado).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(resultado)).containsEntry("detail", LOTE_NO_PENDIENTE);
    }

    @Test
    @DisplayName("RF-14 · la administradora de la empresa ve la sugerencia pero no toma la muestra 2: subir sus fotos y enviarla le responden 403")
    void laAdministradoraNoTomaLaMuestraDos() {
        primeraMuestraConSugerencia();

        MvcTestResult pedir = fotosDeLaDos.pedirSubida(tokenDeAna, lote, 1, PNG, 1_000);
        MvcTestResult enviar = analisis.enviar(tokenDeAna, lote);

        assertThat(lote(analisis.resultadoOk(tokenDeAna, lote))).containsEntry("sugerirOtraMuestra", true);
        assertThat(pedir).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(enviar).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(numerosDeMuestra()).containsExactly(1);
    }

    @Test
    @DisplayName("RF-08 · si el análisis de la muestra 2 falla, la muestra 2 vuelve a pendiente, el avance dice que falló, el lote conserva el resultado de la muestra 1 y la 2 se puede enviar de nuevo")
    void laMuestraDosFalla() {
        primeraMuestraConSugerencia();
        Map<String, Object> antes = resultadoGuardado();
        analisisDeLaDos.subirYEnviar(tokenDeLuis, lote, SEIS_NO_SANOS, SIETE_NO_SANOS);
        clasificador.fallar(new IllegalStateException("El servicio de IA no responde"));

        assertThat(atender.atenderUna()).isTrue();

        assertThat(analisis.avanceOk(tokenDeLuis, lote)).containsEntry("muestra", 2)
                .containsEntry("estado", "PENDIENTE").containsEntry("fallo", true);
        assertThat(resultadoGuardado()).isEqualTo(antes);
        assertThat(muestra(1)).containsEntry("estado", "TERMINADA");
        assertThat(granosDeLaMuestra(2)).isZero();
        clasificador.restablecer();
        assertThat(analisis.enviarOk(tokenDeLuis, lote)).containsEntry("muestra", 2)
                .containsEntry("estado", "EN_ESPERA");
        assertThat(atender.atenderUna()).isTrue();
        assertThat(lote(analisis.resultadoOk(tokenDeLuis, lote))).containsEntry("totalGranos", 200)
                .containsEntry("sugerirOtraMuestra", false);
    }

    @Test
    @DisplayName("RF-12 · al terminar la muestra 2, la categoría sale de la suma de las dos: 172 sanos de 200 dan 88,3 %, Corriente, con margen 5,5 y sin sugerencia (D-25)")
    void laCategoriaPuedeCambiar() {
        Map<String, Object> resultado = dosMuestras(OCHO_NO_SANOS, SIETE_NO_SANOS);

        assertThat(lote(resultado)).containsEntry("porcentajeSanos", 88.3).containsEntry("margen", 5.5)
                .containsEntry("categoria", "CORRIENTE").containsEntry("sugerirOtraMuestra", false)
                .containsEntry("limiteSugerencia", null).containsEntry("categoriaDelLimite", null)
                .containsEntry("totalGranos", 200).containsEntry("granosSanos", 172)
                .containsEntry("porcentajeContado", 86.0);
        assertThat(muestras(resultado).get(1)).containsEntry("granosSanos", 85)
                .containsEntry("porcentajeContado", 85.0);
    }

    @Test
    @DisplayName("RF-13 · C2 · si la empresa cambia sus rangos mientras se toma la muestra 2, el resultado de la suma usa los nuevos: con Estándar desde 91 %, 90,6 % pasa a Corriente con su texto")
    void laMuestraDosUsaLosRangosVigentes() {
        primeraMuestraConSugerencia();
        String insertar = "INSERT INTO rango_recomendacion (empresa_id, categoria, limite_inferior, recomendacion) "
                + "VALUES (?, ?, ?, ?)";
        jdbc.update(insertar, empresa, "PREMIUM", new BigDecimal("96.0"), "Exportación");
        jdbc.update(insertar, empresa, "ESTANDAR", new BigDecimal("91.0"), "Venta estándar de la casa");
        jdbc.update(insertar, empresa, "CORRIENTE", new BigDecimal("82.0"), "Selección a mano antes de vender");
        jdbc.update(insertar, empresa, "FUERA_DE_NORMA", new BigDecimal("0.0"), "Reproceso completo");

        analizarLaDos(tokenDeLuis, SEIS_NO_SANOS, SIETE_NO_SANOS);

        assertThat(lote(analisis.resultadoOk(tokenDeLuis, lote))).containsEntry("porcentajeSanos", 90.6)
                .containsEntry("categoria", "CORRIENTE")
                .containsEntry("recomendacion", "Selección a mano antes de vender")
                .containsEntry("sugerirOtraMuestra", false).containsEntry("limiteSugerencia", null);
    }

    @Test
    @DisplayName("RF-15 · al terminar la muestra 2, el lote olvida la ruta del reporte de la muestra 1, para rehacerlo con el resultado de las dos (D-25)")
    void olvidaElReporteAnterior() {
        primeraMuestraConSugerencia();
        jdbc.update("UPDATE lote SET reporte_ruta = ? WHERE id = ?", empresa + "/" + lote + "/reporte", lote);

        analizarLaDos(tokenDeLuis, SEIS_NO_SANOS, SIETE_NO_SANOS);

        assertThat(jdbc.queryForObject("SELECT reporte_ruta FROM lote WHERE id = ?", String.class, lote)).isNull();
    }

    @Test
    @DisplayName("D-47 · la muestra 1 con 107 sanos de 120 deja el lote en 95,4 %, Estándar, y guarda el límite de Premium (96 %), el más cercano aunque sea de otra categoría; el resultado lo responde")
    void elLimiteDeOtraCategoria() {
        clasificador.conGranos(120);

        analizar(tokenDeLuis, new int[] {105, 100}, new int[] {105, 100}, new int[] {103, 100});

        assertThat(jdbc.queryForMap("SELECT categoria, limite_sugerencia, categoria_del_limite FROM lote WHERE id = ?",
                lote)).containsEntry("categoria", "ESTANDAR").containsEntry("limite_sugerencia", new BigDecimal("96.0"))
                .containsEntry("categoria_del_limite", "PREMIUM");
        assertThat(lote(analisis.resultadoOk(tokenDeLuis, lote))).containsEntry("porcentajeSanos", 95.4)
                .containsEntry("categoria", "ESTANDAR").containsEntry("sugerirOtraMuestra", true)
                .containsEntry("limiteSugerencia", 96.0).containsEntry("categoriaDelLimite", "PREMIUM");
    }

    @Test
    @DisplayName("RF-10 · C4 · con 2 muestras, 107 sanos de 120 y 113 de 125 dan 220 de 245 = 89,8 % contado, de punta a punta con el clasificador falso, y cada muestra conserva sus cifras")
    void sumaDeLasDosMuestras() {
        Map<String, Object> resultado = dosMuestrasDeLaFicha();

        assertThat(lote(resultado)).containsEntry("totalGranos", 245).containsEntry("granosSanos", 220)
                .containsEntry("porcentajeContado", 89.8);
        assertThat(muestras(resultado).get(0)).containsEntry("numero", 1).containsEntry("totalGranos", 120)
                .containsEntry("granosSanos", 107).containsEntry("porcentajeContado", 89.2);
        assertThat(muestras(resultado).get(1)).containsEntry("numero", 2).containsEntry("totalGranos", 125)
                .containsEntry("granosSanos", 113).containsEntry("porcentajeContado", 90.4);
    }

    @Test
    @DisplayName("RF-10 · C11 · con EfficientNet-B0 y 2 muestras, 220 sanos de 245 (89,8 % contado) dan 96,9 % corregido, de punta a punta con el clasificador falso, que usa sus recall")
    void corregidoDeLasDosMuestras() {
        Map<String, Object> resultado = dosMuestrasDeLaFicha();

        assertThat(lote(resultado)).containsEntry("porcentajeSanos", 96.9).containsEntry("margen", 4.3)
                .containsEntry("porcentajeContado", 89.8);
    }

    @Test
    @DisplayName("RF-12 · C12 · con 2 muestras, la categoría sale de la suma de sus granos: 220 sanos de 245 (96,9 %) es Premium o Especial, y no se sugiere otra muestra aunque quede a 0,9 puntos de 96, de punta a punta")
    void categoriaDeLasDosMuestras() {
        Map<String, Object> resultado = dosMuestrasDeLaFicha();

        assertThat(lote(resultado)).containsEntry("porcentajeSanos", 96.9).containsEntry("categoria", "PREMIUM")
                .containsEntry("sugerirOtraMuestra", false).containsEntry("limiteSugerencia", null)
                .containsEntry("categoriaDelLimite", null);
    }
}
