/*
 * Nombre: ClienteDelServicioDeIaTest.java
 * Descripción: Pruebas del cliente HTTP del servicio de IA contra un servidor HTTP de prueba: lo que manda, cómo lee la
 *              revisión, el análisis y los modelos, y cómo traduce los tiempos agotados, las caídas y las respuestas
 *              que no sirven (RF-06, RF-07, RF-08, D-46).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.usco.inspectia.analisis.ServidorDeIaDePrueba;
import co.edu.usco.inspectia.analisis.ServidorDeIaDePrueba.FotoAnalizada;
import co.edu.usco.inspectia.analisis.aplicacion.Clasificacion;
import co.edu.usco.inspectia.analisis.aplicacion.FotoParaClasificar;
import co.edu.usco.inspectia.analisis.aplicacion.GranoClasificado;
import co.edu.usco.inspectia.analisis.aplicacion.ModeloUsado;
import co.edu.usco.inspectia.analisis.aplicacion.Rectangulo;
import co.edu.usco.inspectia.analisis.aplicacion.RevisionDeFoto;
import co.edu.usco.inspectia.analisis.aplicacion.SinRespuestaDeLaIaExcepcion;
import co.edu.usco.inspectia.analisis.dominio.ClaseDeGrano;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.FormatoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.MotivoDeFoto;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** El cliente HTTP del servicio de IA, con un servidor de prueba en un puerto libre y tiempos límite cortos. */
class ClienteDelServicioDeIaTest {

    private static final PropiedadesDelServicioDeIa.Tiempos TIEMPOS = new PropiedadesDelServicioDeIa.Tiempos(
            Duration.ofSeconds(2), Duration.ofMillis(700), Duration.ofMillis(700), Duration.ofSeconds(15));
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 4, 5};

    private ServidorDeIaDePrueba servidor;
    private ClienteDelServicioDeIa cliente;

    @BeforeEach
    void preparar() {
        servidor = ServidorDeIaDePrueba.iniciar();
        cliente = new ClienteDelServicioDeIa(servidor.url(), TIEMPOS);
    }

    @AfterEach
    void apagar() {
        servidor.close();
    }

    private static FotoParaClasificar foto(int numero) {
        return new FotoParaClasificar(numero, numero == 1 ? FormatoDeFoto.JPG : FormatoDeFoto.PNG,
                numero == 1 ? JPG : PNG);
    }

    @Test
    @DisplayName("RF-06 · C3 · una foto que cumple la guía pasa sin avisos: el cliente manda la foto en la parte «foto» y lee la revisión aprobada con sus granos")
    void revisaUnaFotoAprobada() {
        servidor.responder("/revisar", 200, ServidorDeIaDePrueba.revision("APROBADA", null, 52));

        RevisionDeFoto revision = cliente.revisar(foto(1));

        assertThat(revision).isEqualTo(RevisionDeFoto.aprobada(52));
        ServidorDeIaDePrueba.Peticion peticion = servidor.peticiones("/revisar").get(0);
        assertThat(peticion.tipo()).startsWith("multipart/form-data");
        assertThat(peticion.tieneParte("foto")).isTrue();
        assertThat(new String(peticion.cuerpo(), java.nio.charset.StandardCharsets.ISO_8859_1))
                .contains("filename=\"foto.jpg\"").contains("Content-Type: image/jpeg");
    }

    @Test
    @DisplayName("RF-06 · C1 · una foto borrosa se rechaza: la revisión queda por repetir con el motivo BORROSA")
    void fotoBorrosa() {
        servidor.responder("/revisar", 200, ServidorDeIaDePrueba.revision("POR_REPETIR", "BORROSA", 0));

        assertThat(cliente.revisar(foto(1))).isEqualTo(RevisionDeFoto.porRepetir(MotivoDeFoto.BORROSA, 0));
    }

    @Test
    @DisplayName("RF-06 · C2 · una foto oscura se rechaza: la revisión queda por repetir con el motivo POCA_LUZ")
    void fotoOscura() {
        servidor.responder("/revisar", 200, ServidorDeIaDePrueba.revision("POR_REPETIR", "POCA_LUZ", 31));

        assertThat(cliente.revisar(foto(2))).isEqualTo(RevisionDeFoto.porRepetir(MotivoDeFoto.POCA_LUZ, 31));
    }

    @Test
    @DisplayName("RF-07 · C2 · una foto con granos pegados se rechaza con dónde están: las regiones van en la revisión")
    void granosPegados() {
        servidor.responder("/revisar", 200, ServidorDeIaDePrueba.revision("POR_REPETIR", "GRANOS_PEGADOS", 47,
                new int[] {1210, 860, 240, 130}, new int[] {10, 20, 30, 40}));

        RevisionDeFoto revision = cliente.revisar(foto(1));

        assertThat(revision.estado()).isEqualTo(EstadoDeFoto.POR_REPETIR);
        assertThat(revision.motivo()).isEqualTo(MotivoDeFoto.GRANOS_PEGADOS);
        assertThat(revision.granosEncontrados()).isEqualTo(47);
        assertThat(revision.regionesPegadas()).containsExactly(new Rectangulo(1210, 860, 240, 130),
                new Rectangulo(10, 20, 30, 40));
    }

    @Test
    @DisplayName("RF-08 · C1 · el análisis manda las fotos en foto1, foto2 y foto3 y cada grano vuelve con una sola clase, su posición y su foto")
    void analizaLasFotos() {
        servidor.responder("/analizar", 200, ServidorDeIaDePrueba.analisis(List.of(
                FotoAnalizada.aprobada(1, 3, 1, 1), FotoAnalizada.aprobada(2, 2, 0, 0))));

        Clasificacion clasificacion = cliente.analizar(List.of(foto(1), foto(2)));

        ServidorDeIaDePrueba.Peticion peticion = servidor.peticiones("/analizar").get(0);
        assertThat(peticion.fotos()).containsExactly(1, 2);
        assertThat(peticion.tieneParte("foto3")).isFalse();
        assertThat(clasificacion.modelo()).isEqualTo(new ModeloUsado("EfficientNet-B0", "1.0"));
        assertThat(clasificacion.granos()).hasSize(5);
        assertThat(clasificacion.granos().get(0)).isEqualTo(new GranoClasificado(1, 0, 7, 10, 10, ClaseDeGrano.NO_SANO,
                new BigDecimal("55.00")));
        assertThat(clasificacion.granos().get(3)).isEqualTo(new GranoClasificado(2, 0, 7, 10, 10, ClaseDeGrano.SANO,
                new BigDecimal("90.00")));
        assertThat(clasificacion.revisiones()).containsEntry(1, RevisionDeFoto.aprobada(3))
                .containsEntry(2, RevisionDeFoto.aprobada(2));
    }

    @Test
    @DisplayName("RF-08 · C2 · cada clasificación trae su confianza, con 2 decimales, tal como la responde el servicio")
    void cadaGranoTraeSuConfianza() {
        servidor.responder("/analizar", 200, ServidorDeIaDePrueba.analisis(List.of(
                FotoAnalizada.aprobada(1, 4, 0, 2))));

        Clasificacion clasificacion = cliente.analizar(List.of(foto(1)));

        assertThat(clasificacion.granos()).extracting(GranoClasificado::confianza)
                .containsExactly(new BigDecimal("55.00"), new BigDecimal("55.00"), new BigDecimal("90.00"),
                        new BigDecimal("90.00"));
    }

    @Test
    @DisplayName("D-46 · en el análisis, una foto que no pasa la revisión de la cola viene por repetir y sin granos")
    void fotoPorRepetirEnLaCola() {
        servidor.responder("/analizar", 200, ServidorDeIaDePrueba.analisis(List.of(
                FotoAnalizada.aprobada(1, 50, 0, 0), FotoAnalizada.porRepetir(2, "BORROSA", 12))));

        Clasificacion clasificacion = cliente.analizar(List.of(foto(1), foto(2)));

        assertThat(clasificacion.granos()).hasSize(50).allSatisfy(grano -> assertThat(grano.numeroDeFoto()).isOne());
        assertThat(clasificacion.revisiones()).containsEntry(2, RevisionDeFoto.porRepetir(MotivoDeFoto.BORROSA, 12));
    }

    @Test
    @DisplayName("RF-08 · C6 · si el servicio no responde a tiempo, el cliente lo dice como «sin respuesta» (prueba de límite)")
    void tiempoAgotado() {
        servidor.demorar("/analizar", Duration.ofSeconds(3));
        servidor.demorar("/revisar", Duration.ofSeconds(3));

        assertThatThrownBy(() -> cliente.analizar(List.of(foto(1)))).isInstanceOf(SinRespuestaDeLaIaExcepcion.class);
        assertThatThrownBy(() -> cliente.revisar(foto(1))).isInstanceOf(SinRespuestaDeLaIaExcepcion.class);
    }

    @Test
    @DisplayName("RF-08 · C6 · un 503 del servicio y un servicio apagado también son «sin respuesta»")
    void servicioCaidoOApagado() throws Exception {
        servidor.responder("/analizar", 503, "{\"listo\": false}");
        assertThatThrownBy(() -> cliente.analizar(List.of(foto(1)))).isInstanceOf(SinRespuestaDeLaIaExcepcion.class);

        int puertoLibre;
        try (ServerSocket socket = new ServerSocket(0)) {
            puertoLibre = socket.getLocalPort();
        }
        ClienteDelServicioDeIa apagado = new ClienteDelServicioDeIa(URI.create("http://127.0.0.1:" + puertoLibre),
                TIEMPOS);
        assertThatThrownBy(() -> apagado.revisar(foto(1))).isInstanceOf(SinRespuestaDeLaIaExcepcion.class);
        assertThatThrownBy(apagado::modelos).isInstanceOf(SinRespuestaDeLaIaExcepcion.class);
    }

    @Test
    @DisplayName("RF-08 · un 400 del servicio no es «sin respuesta»: es otra falla, sin motivo para la página")
    void rechazoDelServicio() {
        servidor.responder("/revisar", 400, "{\"detail\": \"No es una imagen\"}");

        assertThatThrownBy(() -> cliente.revisar(foto(1))).isInstanceOf(IllegalStateException.class)
                .isNotInstanceOf(SinRespuestaDeLaIaExcepcion.class).hasMessageContaining("400");
    }

    @Test
    @DisplayName("RF-08 · C1 · una respuesta con una clase que no existe, o con el motivo GRANOS_DUDOSOS, que solo pone el backend, no sirve")
    void respuestasQueNoSirven() {
        servidor.responder("/analizar", 200, ServidorDeIaDePrueba.analisis(List.of(FotoAnalizada.aprobada(1, 1, 0, 0)))
                .replace("\"SANO\"", "\"REGULAR\""));
        assertThatThrownBy(() -> cliente.analizar(List.of(foto(1)))).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REGULAR");

        servidor.responder("/revisar", 200, ServidorDeIaDePrueba.revision("POR_REPETIR", "GRANOS_DUDOSOS", 50));
        assertThatThrownBy(() -> cliente.revisar(foto(1))).isInstanceOf(IllegalStateException.class);

        servidor.responder("/revisar", 200, ServidorDeIaDePrueba.revision("APROBADA", "BORROSA", 50));
        assertThatThrownBy(() -> cliente.revisar(foto(1))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("D-48 · los modelos traen sus 5 filas con sus métricas y el que carga el servicio")
    void leeLosModelos() {
        ClienteDelServicioDeIa.ModelosDelServicio modelos = cliente.modelos();

        assertThat(modelos.modelos()).hasSize(5);
        assertThat(modelos.enUso().nombre()).isEqualTo("EfficientNet-B0");
        assertThat(modelos.enUso().version()).isEqualTo("1.0");
        assertThat(modelos.enUso().f1Macro()).isEqualByComparingTo("73.50");
        assertThat(modelos.enUso().sanosComoSanos()).isEqualTo(238);
        assertThat(modelos.modelos()).extracting(modelo -> modelo.tipo()).containsOnly("CNN", "TRANSFORMER");

        servidor.responder("/modelos", 200, ServidorDeIaDePrueba.MODELOS.replace("\"enUso\": \"EfficientNet-B0\"",
                "\"enUso\": \"Otro\""));
        assertThatThrownBy(cliente::modelos).isInstanceOf(IllegalStateException.class).hasMessageContaining("Otro");
    }
}
