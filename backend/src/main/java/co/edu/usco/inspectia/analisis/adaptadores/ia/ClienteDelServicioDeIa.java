/*
 * Nombre: ClienteDelServicioDeIa.java
 * Descripción: Cliente HTTP del servicio de IA: la revisión rápida de una foto (POST /revisar), el análisis de una
 *              muestra (POST /analizar) y los modelos de la comparación (GET /modelos), con sus tiempos límite y la
 *              traducción de su JSON a los datos del módulo (RF-06, RF-07, RF-08, RF-17, D-46).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.ia;

import co.edu.usco.inspectia.analisis.aplicacion.Clasificacion;
import co.edu.usco.inspectia.analisis.aplicacion.FotoParaClasificar;
import co.edu.usco.inspectia.analisis.aplicacion.GranoClasificado;
import co.edu.usco.inspectia.analisis.aplicacion.MetricasDelModelo;
import co.edu.usco.inspectia.analisis.aplicacion.ModeloUsado;
import co.edu.usco.inspectia.analisis.aplicacion.Rectangulo;
import co.edu.usco.inspectia.analisis.aplicacion.RevisionDeFoto;
import co.edu.usco.inspectia.analisis.aplicacion.SinRespuestaDeLaIaExcepcion;
import co.edu.usco.inspectia.analisis.dominio.ClaseDeGrano;
import co.edu.usco.inspectia.analisis.dominio.EstadoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.MotivoDeFoto;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * El cliente HTTP del servicio de IA (contrato común del despliegue, sección 1). Manda las fotos como
 * {@code multipart/form-data}, con su contenido y no su enlace (specs/arquitectura.md), y traduce las respuestas:
 *
 * <ul>
 *   <li>Si se acaba el tiempo (10 s para {@code /revisar}, 60 s para {@code /analizar}), no se puede conectar o el
 *       servicio responde 5xx, lanza {@link SinRespuestaDeLaIaExcepcion} (RF-08 C6).</li>
 *   <li>Si responde 4xx o algo que no cumple el contrato, como una clase de grano que no existe o el motivo
 *       {@code GRANOS_DUDOSOS}, que solo pone el backend (RF-09), lanza {@link IllegalStateException}.</li>
 * </ul>
 */
public class ClienteDelServicioDeIa {

    /** Los motivos que puede dar el servicio; {@code GRANOS_DUDOSOS} lo pone el backend (RF-09). */
    private static final Set<MotivoDeFoto> MOTIVOS_DEL_SERVICIO = Set.of(MotivoDeFoto.BORROSA, MotivoDeFoto.POCA_LUZ,
            MotivoDeFoto.GRANOS_PEGADOS);

    private final RestClient paraRevisar;
    private final RestClient paraAnalizar;

    /**
     * Crea el cliente.
     *
     * @param url la dirección del servicio
     * @param tiempos los tiempos límite
     */
    public ClienteDelServicioDeIa(URI url, PropiedadesDelServicioDeIa.Tiempos tiempos) {
        Objects.requireNonNull(url, "url");
        HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(tiempos.conexion()).build();
        this.paraRevisar = RestClient.builder().baseUrl(url.toString()).requestFactory(fabrica(http, tiempos.revisar()))
                .build();
        this.paraAnalizar = RestClient.builder().baseUrl(url.toString())
                .requestFactory(fabrica(http, tiempos.analizar())).build();
    }

    /**
     * La revisión rápida de una foto ({@code POST /revisar}, D-46).
     *
     * @param foto la foto
     * @return su revisión
     * @throws SinRespuestaDeLaIaExcepcion si el servicio no responde
     * @throws IllegalStateException si responde algo que no sirve
     */
    public RevisionDeFoto revisar(FotoParaClasificar foto) {
        MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
        agregar(partes, "foto", foto);
        RespuestasDelServicio.Revision revision = llamar("/revisar", () -> paraRevisar.post().uri("/revisar")
                .contentType(MediaType.MULTIPART_FORM_DATA).body(partes).retrieve()
                .body(RespuestasDelServicio.Revision.class));
        return revision(revision);
    }

    /**
     * El análisis de las fotos de una muestra ({@code POST /analizar}, RF-07, RF-08): la revisión de cada foto y los
     * granos de las que la pasan.
     *
     * @param fotos las fotos de la muestra, de 1 a 3
     * @return el modelo, los granos de todas las fotos y la revisión de cada una
     * @throws SinRespuestaDeLaIaExcepcion si el servicio no responde
     * @throws IllegalStateException si responde algo que no sirve
     */
    public Clasificacion analizar(List<FotoParaClasificar> fotos) {
        MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
        fotos.forEach(foto -> agregar(partes, "foto" + foto.numero(), foto));
        RespuestasDelServicio.Analisis analisis = llamar("/analizar", () -> paraAnalizar.post().uri("/analizar")
                .contentType(MediaType.MULTIPART_FORM_DATA).body(partes).retrieve()
                .body(RespuestasDelServicio.Analisis.class));
        if (analisis == null || analisis.modelo() == null || analisis.fotos() == null) {
            throw new IllegalStateException("El servicio de IA respondió un análisis sin modelo o sin fotos");
        }
        List<GranoClasificado> granos = new ArrayList<>();
        Map<Integer, RevisionDeFoto> revisiones = new HashMap<>();
        for (RespuestasDelServicio.FotoAnalizada foto : analisis.fotos()) {
            RevisionDeFoto revision = revision(foto.revision());
            if (revisiones.put(foto.numero(), revision) != null) {
                throw new IllegalStateException("El servicio de IA respondió dos veces la foto " + foto.numero());
            }
            if (revision.pideRepetir()) {
                continue;
            }
            for (RespuestasDelServicio.Grano grano : Optional.ofNullable(foto.granos()).orElse(List.of())) {
                granos.add(new GranoClasificado(foto.numero(), grano.x(), grano.y(), grano.ancho(), grano.alto(),
                        clase(grano.clase()), Objects.requireNonNull(grano.confianza(), "confianza")));
            }
        }
        return new Clasificacion(new ModeloUsado(analisis.modelo().nombre(), analisis.modelo().version()), granos,
                revisiones);
    }

    /**
     * Los modelos de la comparación y el que carga el servicio ({@code GET /modelos}, D-18, D-48).
     *
     * @return los modelos y el que está en uso
     * @throws SinRespuestaDeLaIaExcepcion si el servicio no responde
     * @throws IllegalStateException si responde algo que no sirve, o si el modelo en uso no está en la lista
     */
    public ModelosDelServicio modelos() {
        RespuestasDelServicio.Modelos respuesta = llamar("/modelos", () -> paraRevisar.get().uri("/modelos")
                .retrieve().body(RespuestasDelServicio.Modelos.class));
        if (respuesta == null || respuesta.modelos() == null || respuesta.modelos().isEmpty()) {
            throw new IllegalStateException("El servicio de IA no respondió sus modelos");
        }
        List<MetricasDelModelo> modelos = respuesta.modelos().stream().map(ClienteDelServicioDeIa::metricas).toList();
        MetricasDelModelo enUso = modelos.stream().filter(modelo -> modelo.nombre().equals(respuesta.enUso()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("El modelo en uso del servicio de IA, " + respuesta.enUso()
                        + ", no está en su lista de modelos"));
        return new ModelosDelServicio(enUso, modelos);
    }

    /**
     * Los modelos que responde el servicio.
     *
     * @param enUso el que carga el servicio
     * @param modelos los de la comparación, con sus métricas
     */
    public record ModelosDelServicio(MetricasDelModelo enUso, List<MetricasDelModelo> modelos) {
    }

    private static JdkClientHttpRequestFactory fabrica(HttpClient http, Duration lectura) {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(lectura);
        return fabrica;
    }

    /** Una parte del formulario: la foto con su nombre de archivo y su tipo, como la sube un navegador. */
    private static void agregar(MultiValueMap<String, Object> partes, String nombre, FotoParaClasificar foto) {
        HttpHeaders encabezados = new HttpHeaders();
        encabezados.setContentType(MediaType.parseMediaType(foto.formato().getTipo()));
        encabezados.setContentDisposition(ContentDisposition.formData().name(nombre)
                .filename(nombre + "." + foto.formato().getExtension()).build());
        partes.add(nombre, new HttpEntity<>(new ByteArrayResource(foto.contenido()), encabezados));
    }

    /** Hace la llamada y traduce sus errores (ver la descripción de la clase). */
    private static <T> T llamar(String ruta, Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (ResourceAccessException excepcion) {
            throw new SinRespuestaDeLaIaExcepcion("El servicio de IA no respondió " + ruta + " a tiempo o no se pudo "
                    + "conectar", excepcion);
        } catch (RestClientResponseException excepcion) {
            if (excepcion.getStatusCode().is5xxServerError()) {
                throw new SinRespuestaDeLaIaExcepcion("El servicio de IA falló en " + ruta + " con "
                        + excepcion.getStatusCode().value(), excepcion);
            }
            throw new IllegalStateException("El servicio de IA rechazó " + ruta + " con "
                    + excepcion.getStatusCode().value() + ": " + excepcion.getResponseBodyAsString(), excepcion);
        } catch (RestClientException excepcion) {
            throw new IllegalStateException("La respuesta del servicio de IA en " + ruta + " no sirve", excepcion);
        }
    }

    /** La revisión del servicio, con las reglas de la foto: solo la de granos pegados lleva regiones. */
    private static RevisionDeFoto revision(RespuestasDelServicio.Revision revision) {
        if (revision == null || revision.estado() == null || revision.granosEncontrados() == null) {
            throw new IllegalStateException("El servicio de IA respondió una revisión incompleta");
        }
        try {
            EstadoDeFoto estado = EstadoDeFoto.valueOf(revision.estado());
            MotivoDeFoto motivo = revision.motivo() == null ? null : MotivoDeFoto.valueOf(revision.motivo());
            if (motivo != null && !MOTIVOS_DEL_SERVICIO.contains(motivo)) {
                throw new IllegalStateException("El servicio de IA no puede dar el motivo " + motivo);
            }
            List<Rectangulo> regiones = motivo != MotivoDeFoto.GRANOS_PEGADOS ? List.of()
                    : Optional.ofNullable(revision.regionesPegadas()).orElse(List.of()).stream()
                            .map(region -> new Rectangulo(region.x(), region.y(), region.ancho(), region.alto()))
                            .toList();
            return new RevisionDeFoto(estado, motivo, revision.granosEncontrados(), regiones);
        } catch (IllegalArgumentException excepcion) {
            throw new IllegalStateException("El servicio de IA respondió una revisión que no sirve: " + revision,
                    excepcion);
        }
    }

    private static ClaseDeGrano clase(String clase) {
        try {
            return ClaseDeGrano.valueOf(Objects.requireNonNull(clase, "clase"));
        } catch (IllegalArgumentException excepcion) {
            throw new IllegalStateException("El servicio de IA respondió una clase que no existe: " + clase,
                    excepcion);
        }
    }

    private static MetricasDelModelo metricas(RespuestasDelServicio.ModeloComparado modelo) {
        return new MetricasDelModelo(modelo.nombre(), modelo.version(), modelo.tipo(), modelo.f1Macro(),
                modelo.precisionMacro(), modelo.recallMacro(), modelo.recallSano(), modelo.recallNoSano(),
                modelo.precisionSano(), modelo.precisionNoSano(), entero(modelo.sanosComoSanos()),
                entero(modelo.sanosComoNoSanos()), entero(modelo.noSanosComoSanos()),
                entero(modelo.noSanosComoNoSanos()), modelo.tiempoPorGranoMs());
    }

    private static int entero(Integer valor) {
        if (valor == null) {
            throw new IllegalStateException("El servicio de IA respondió un modelo sin su matriz de confusión");
        }
        return valor;
    }
}
