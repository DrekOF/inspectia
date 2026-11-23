/*
 * Nombre: ColaDeAnalisisIT.java
 * Descripción: Pruebas de integración de la cola de análisis: el usuario ve su análisis en espera, varios análisis de
 *              empresas distintas se atienden en el orden en que se enviaron y ninguno se pierde, una muestra que quedó
 *              en proceso vuelve a la cola, una bloqueada se salta, y un análisis que falla o tiene pocos granos vuelve
 *              a pendiente sin resultado (RNF-02 C3, RNF-01, RF-07, RF-08).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.ConfiguracionContenedores;
import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.analisis.aplicacion.AtenderCola;
import co.edu.usco.inspectia.analisis.aplicacion.ColaDeAnalisis;
import co.edu.usco.inspectia.analisis.aplicacion.MuestraTomada;
import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * La cola de análisis (RNF-02 C3): es de toda la plataforma y está en la base. Tres empresas analizan un lote cada una:
 * Cacaos del Huila (Luis Gómez), Cacao del Sur (Marta Ruiz) y Chocolates del Valle (Sara León). La cola va a mano: estas
 * pruebas la atienden con {@link AtenderCola#atenderUna()} y la toman con {@link ColaDeAnalisis}.
 */
@PruebaIntegracion
class ColaDeAnalisisIT {

    private static final int[] SANAS = {100, 100};

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AtenderCola atender;

    @Autowired
    private ColaDeAnalisis cola;

    @Autowired
    private ClasificadorProgramable clasificador;

    @Autowired
    private PlatformTransactionManager transacciones;

    private ApiAnalisis analisis;
    private String tokenDeLuis;
    private String tokenDeMarta;
    private String tokenDeSara;
    private UUID loteDeLuis;
    private UUID loteDeMarta;
    private UUID loteDeSara;

    @BeforeEach
    void preparar() {
        ApiSesion api = new ApiSesion(mvc, jdbc);
        analisis = new ApiAnalisis(api, new ApiFotos(api, mvc));
        ApiSesion.borrarDatos(jdbc);
        api.crearUsuario(api.crearEmpresa("Cacaos del Huila", "900123456-8"), "Luis Gómez", "luis@cacaos.test",
                "USUARIO_FINAL");
        api.crearUsuario(api.crearEmpresa("Cacao del Sur", "800765432-1"), "Marta Ruiz", "marta@cacaosur.test",
                "USUARIO_FINAL");
        api.crearUsuario(api.crearEmpresa("Chocolates del Valle", "890903938-8"), "Sara León", "sara@valle.test",
                "USUARIO_FINAL");
        tokenDeLuis = api.token("luis@cacaos.test");
        tokenDeMarta = api.token("marta@cacaosur.test");
        tokenDeSara = api.token("sara@valle.test");
        ApiLotes lotes = new ApiLotes(api);
        loteDeLuis = lotes.registrarOk(tokenDeLuis, "L-001");
        loteDeMarta = lotes.registrarOk(tokenDeMarta, "S-001");
        loteDeSara = lotes.registrarOk(tokenDeSara, "V-001");
        analisis.subirFotos(tokenDeLuis, loteDeLuis, SANAS, SANAS);
        analisis.subirFotos(tokenDeMarta, loteDeMarta, SANAS, SANAS);
        analisis.subirFotos(tokenDeSara, loteDeSara, SANAS, SANAS);
    }

    @AfterEach
    void limpiar() {
        clasificador.restablecer();
        ApiSesion.borrarDatos(jdbc);
        ConfiguracionContenedores.vaciarCarpetaDeFotos();
    }

    @Test
    @DisplayName("RNF-02 · C3 · mientras espera su turno, el usuario ve que su análisis está «en espera», con su fecha de envío")
    void elUsuarioVeLaEspera() {
        analisis.enviarOk(tokenDeSara, loteDeSara);
        analisis.enviarOk(tokenDeLuis, loteDeLuis);

        for (Map.Entry<String, UUID> lote : Map.of(tokenDeSara, loteDeSara, tokenDeLuis, loteDeLuis).entrySet()) {
            Map<String, Object> avance = analisis.avanceOk(lote.getKey(), lote.getValue());
            assertThat(avance).containsEntry("muestra", 1).containsEntry("estado", "EN_ESPERA")
                    .containsEntry("fechaInicio", null).containsEntry("fechaFin", null).containsEntry("fallo", false);
            assertThat(avance.get("fechaEnvio")).isNotNull();
        }
        assertThat(analisis.avanceOk(tokenDeMarta, loteDeMarta)).as("Marta todavía no envía su muestra")
                .containsEntry("estado", "PENDIENTE").containsEntry("fechaEnvio", null).containsEntry("fallo", false);
    }

    @Test
    @DisplayName("RNF-02 · C3 · tres análisis de empresas distintas se atienden en el orden en que se enviaron, no en el que se crearon, y ninguno se pierde")
    void enOrdenYNingunoSePierde() {
        analisis.enviarOk(tokenDeSara, loteDeSara);
        analisis.enviarOk(tokenDeLuis, loteDeLuis);
        analisis.enviarOk(tokenDeMarta, loteDeMarta);

        List<UUID> atendidos = new ArrayList<>();
        while (atender.atenderUna()) {
            atendidos.add(ultimoTerminado(atendidos));
        }

        assertThat(atendidos).containsExactly(loteDeSara, loteDeLuis, loteDeMarta);
        assertThat(jdbc.queryForList("SELECT DISTINCT estado FROM muestra", String.class)).containsExactly("TERMINADA");
        assertThat(jdbc.queryForList("SELECT DISTINCT estado FROM lote", String.class)).containsExactly("ANALIZADO");
        for (Map.Entry<String, UUID> lote : Map.of(tokenDeSara, loteDeSara, tokenDeLuis, loteDeLuis, tokenDeMarta,
                loteDeMarta).entrySet()) {
            assertThat(analisis.avanceOk(lote.getKey(), lote.getValue())).containsEntry("estado", "TERMINADA");
        }
    }

    @Test
    @DisplayName("RNF-02 · C3 · un análisis que quedó «en proceso» al apagar el servidor vuelve a la cola al arrancar, con su fecha de envío, y se atiende primero")
    void vuelveALaColaAlArrancar() {
        analisis.enviarOk(tokenDeLuis, loteDeLuis);
        analisis.enviarOk(tokenDeMarta, loteDeMarta);
        OffsetDateTime envio = fechaDeEnvio(loteDeLuis);
        MuestraTomada aMedias = cola.tomarLaSiguiente(Instant.now().truncatedTo(ChronoUnit.MICROS)).orElseThrow();
        assertThat(aMedias.loteId()).isEqualTo(loteDeLuis);
        assertThat(analisis.avanceOk(tokenDeLuis, loteDeLuis)).containsEntry("estado", "EN_PROCESO");

        int devueltas = cola.devolverLasInterrumpidas();

        assertThat(devueltas).isOne();
        assertThat(analisis.avanceOk(tokenDeLuis, loteDeLuis)).containsEntry("estado", "EN_ESPERA")
                .containsEntry("fechaInicio", null);
        assertThat(fechaDeEnvio(loteDeLuis)).isEqualTo(envio);
        assertThat(atender.atenderUna()).isTrue();
        assertThat(ultimoTerminado(List.of())).isEqualTo(loteDeLuis);
    }

    @Test
    @DisplayName("RNF-02 · C3 · si otra ejecución tiene bloqueada la primera muestra de la cola, la cola la salta y toma la siguiente (SKIP LOCKED)")
    void saltaLasBloqueadas() throws Exception {
        analisis.enviarOk(tokenDeLuis, loteDeLuis);
        analisis.enviarOk(tokenDeMarta, loteDeMarta);
        UUID muestraDeLuis = jdbc.queryForObject("SELECT id FROM muestra WHERE lote_id = ?", UUID.class, loteDeLuis);
        CountDownLatch bloqueada = new CountDownLatch(1);
        CountDownLatch soltar = new CountDownLatch(1);
        ExecutorService otra = Executors.newSingleThreadExecutor();
        try {
            Future<?> bloqueo = otra.submit(() -> new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
                jdbc.queryForObject("SELECT id FROM muestra WHERE id = ? FOR UPDATE", UUID.class, muestraDeLuis);
                bloqueada.countDown();
                esperar(soltar);
            }));
            assertThat(bloqueada.await(10, TimeUnit.SECONDS)).isTrue();

            Optional<MuestraTomada> tomada = cola.tomarLaSiguiente(Instant.now().truncatedTo(ChronoUnit.MICROS));

            assertThat(tomada).map(MuestraTomada::loteId).contains(loteDeMarta);
            soltar.countDown();
            bloqueo.get(10, TimeUnit.SECONDS);
        } finally {
            soltar.countDown();
            otra.shutdownNow();
        }
        assertThat(cola.tomarLaSiguiente(Instant.now().truncatedTo(ChronoUnit.MICROS)))
                .map(MuestraTomada::loteId).contains(loteDeLuis);
    }

    @Test
    @DisplayName("RNF-01 · mientras se analiza, el avance dice «EN_PROCESO» con su hora de inicio; al terminar, «TERMINADA» con su hora de fin")
    void avanceMientrasSeAnaliza() {
        analisis.enviarOk(tokenDeLuis, loteDeLuis);
        cola.tomarLaSiguiente(Instant.now().truncatedTo(ChronoUnit.MICROS)).orElseThrow();

        Map<String, Object> enProceso = analisis.avanceOk(tokenDeLuis, loteDeLuis);
        cola.devolverLasInterrumpidas();
        atender.atenderUna();
        Map<String, Object> terminada = analisis.avanceOk(tokenDeLuis, loteDeLuis);

        assertThat(enProceso).containsEntry("estado", "EN_PROCESO").containsEntry("fechaFin", null);
        assertThat(enProceso.get("fechaInicio")).isNotNull();
        assertThat(terminada).containsEntry("estado", "TERMINADA").containsEntry("fallo", false);
        assertThat(Instant.parse((String) terminada.get("fechaFin")))
                .isAfterOrEqualTo(Instant.parse((String) terminada.get("fechaInicio")));
    }

    @Test
    @DisplayName("RF-08 · si el clasificador falla, la muestra vuelve a pendiente sin resultado, el avance dice que falló, y se puede enviar de nuevo")
    void fallaYSeReenvia() {
        analisis.enviarOk(tokenDeLuis, loteDeLuis);
        clasificador.fallar(new IllegalStateException("El servicio de IA no responde"));

        assertThat(atender.atenderUna()).isTrue();

        Map<String, Object> avance = analisis.avanceOk(tokenDeLuis, loteDeLuis);
        assertThat(avance).containsEntry("estado", "PENDIENTE").containsEntry("fallo", true);
        assertThat(avance.get("fechaEnvio")).isNotNull();
        assertSinResultado(loteDeLuis);
        assertThat(jdbc.queryForList("SELECT DISTINCT f.estado FROM foto f JOIN muestra m ON m.id = f.muestra_id "
                + "WHERE m.lote_id = ?", String.class, loteDeLuis)).containsExactly("CARGADA");

        clasificador.restablecer();
        assertThat(analisis.enviarOk(tokenDeLuis, loteDeLuis)).containsEntry("estado", "EN_ESPERA")
                .containsEntry("fallo", false);
        assertThat(atender.atenderUna()).isTrue();
        assertThat(analisis.avanceOk(tokenDeLuis, loteDeLuis)).containsEntry("estado", "TERMINADA");
    }

    @Test
    @DisplayName("RF-07 · con menos de 100 granos entre todas las fotos no se calcula el resultado: la muestra vuelve a pendiente y el avance dice que falló (prueba de límite)")
    void menosDeCienGranos() {
        analisis.enviarOk(tokenDeLuis, loteDeLuis);
        clasificador.conGranos(99);

        assertThat(atender.atenderUna()).isTrue();

        assertThat(analisis.avanceOk(tokenDeLuis, loteDeLuis)).containsEntry("estado", "PENDIENTE")
                .containsEntry("fallo", true);
        assertSinResultado(loteDeLuis);
    }

    /** El lote que acaba de terminar: el único analizado que no estaba en la lista. */
    private UUID ultimoTerminado(List<UUID> yaAtendidos) {
        List<UUID> terminados = new ArrayList<>(jdbc.queryForList(
                "SELECT lote_id FROM muestra WHERE estado = 'TERMINADA'", UUID.class));
        terminados.removeAll(yaAtendidos);
        assertThat(terminados).hasSize(1);
        return terminados.get(0);
    }

    private OffsetDateTime fechaDeEnvio(UUID lote) {
        return jdbc.queryForObject("SELECT fecha_envio FROM muestra WHERE lote_id = ?", OffsetDateTime.class, lote);
    }

    private void assertSinResultado(UUID lote) {
        assertThat(jdbc.queryForMap("SELECT estado, porcentaje_sanos, margen, categoria FROM lote WHERE id = ?", lote))
                .containsEntry("estado", "PENDIENTE").containsEntry("porcentaje_sanos", null)
                .containsEntry("margen", null).containsEntry("categoria", null);
        assertThat(jdbc.queryForMap("SELECT estado, total_granos, modelo_id FROM muestra WHERE lote_id = ?", lote))
                .containsEntry("estado", "PENDIENTE").containsEntry("total_granos", null)
                .containsEntry("modelo_id", null);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM grano", Integer.class)).isZero();
    }

    private static void esperar(CountDownLatch soltar) {
        try {
            assertThat(soltar.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
        }
    }
}
