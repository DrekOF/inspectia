/*
 * Nombre: ApiAnalisis.java
 * Descripción: Llamadas a la API del análisis de un lote y sus mensajes, para las pruebas: enviar la muestra, ver el
 *              avance y ver el resultado, también de la muestra 2 (RF-05 C2, RF-10, RF-11, RF-12, RF-14 C6, RNF-02 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis;

import static co.edu.usco.inspectia.usuario.sesion.ApiSesion.json;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Llamadas a la API del análisis de un lote ({@code /api/v1/lotes/{id}/analisis} y {@code /resultado}).
 *
 * <p>Contrato que fijan estas pruebas (PR 3a y 3b):
 * <ul>
 *   <li>{@code POST …/analisis} («Analizar la muestra», solo el usuario final) responde 202 con el avance. Sin
 *       muestra o con menos de 2 fotos, 400 en {@code fotos}; con el lote analizado, 409, salvo la muestra 2 de un
 *       lote que sugiere otra muestra (D-25).</li>
 *   <li>{@code GET …/analisis} responde {@code muestra}, {@code estado}, {@code fechaEnvio}, {@code fechaInicio},
 *       {@code fechaFin} y {@code fallo}.</li>
 *   <li>{@code GET …/resultado} responde {@code lote}, con su resultado guardado y, si sugiere otra muestra, el
 *       {@code limiteSugerencia} y su {@code categoriaDelLimite} (D-47), y {@code muestras}, cada una con sus fotos
 *       y sus granos {@code x}, {@code y}, {@code ancho}, {@code alto}, {@code clase} y {@code dudoso}, sin la
 *       confianza.</li>
 * </ul>
 */
final class ApiAnalisis {

    static final String FOTOS_MINIMO = "Sube al menos 2 fotos para analizar la muestra.";
    static final String LOTE_ANALIZADO = "Este lote ya tiene su resultado: no se puede volver a analizar.";

    private final ApiSesion api;
    private final ApiFotos fotos;

    ApiAnalisis(ApiSesion api, ApiFotos fotos) {
        this.api = api;
        this.fotos = fotos;
    }

    /** La ruta del análisis de un lote. */
    static String ruta(UUID lote) {
        return ApiLotes.RUTA + "/" + lote + "/analisis";
    }

    /** La ruta del resultado de un lote. */
    static String rutaDelResultado(UUID lote) {
        return ApiLotes.RUTA + "/" + lote + "/resultado";
    }

    MvcTestResult enviar(String token, UUID lote) {
        return api.enviar(HttpMethod.POST, ruta(lote), null, token);
    }

    MvcTestResult avance(String token, UUID lote) {
        return api.enviar(HttpMethod.GET, ruta(lote), null, token);
    }

    MvcTestResult resultado(String token, UUID lote) {
        return api.enviar(HttpMethod.GET, rutaDelResultado(lote), null, token);
    }

    /** Envía la muestra y espera el 202. */
    Map<String, Object> enviarOk(String token, UUID lote) {
        MvcTestResult resultado = enviar(token, lote);
        assertThat(resultado).hasStatus(HttpStatus.ACCEPTED);
        return json(resultado);
    }

    /** El avance, con su 200. */
    Map<String, Object> avanceOk(String token, UUID lote) {
        MvcTestResult resultado = avance(token, lote);
        assertThat(resultado).hasStatus(HttpStatus.OK);
        return json(resultado);
    }

    /** El resultado, con su 200. */
    Map<String, Object> resultadoOk(String token, UUID lote) {
        MvcTestResult resultado = resultado(token, lote);
        assertThat(resultado).hasStatus(HttpStatus.OK);
        return json(resultado);
    }

    /**
     * Sube una foto PNG por cada medida, numeradas desde 1.
     *
     * @param medidas el ancho y el alto de cada foto
     */
    void subirFotos(String token, UUID lote, int[]... medidas) {
        for (int i = 0; i < medidas.length; i++) {
            fotos.subirFoto(token, lote, i + 1, ApiFotos.PNG, ApiFotos.png(medidas[i][0], medidas[i][1]));
        }
    }

    /** Sube las fotos y envía la muestra. */
    void subirYEnviar(String token, UUID lote, int[]... medidas) {
        subirFotos(token, lote, medidas);
        enviarOk(token, lote);
    }

    /** El lote del resultado. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> lote(Map<String, Object> resultado) {
        return (Map<String, Object>) resultado.get("lote");
    }

    /** Las muestras del resultado. */
    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> muestras(Map<String, Object> resultado) {
        return (List<Map<String, Object>>) resultado.get("muestras");
    }

    /** Las fotos de una muestra del resultado. */
    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> fotos(Map<String, Object> muestra) {
        return (List<Map<String, Object>>) muestra.get("fotos");
    }

    /** Los granos de una foto del resultado. */
    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> granos(Map<String, Object> foto) {
        return (List<Map<String, Object>>) foto.get("granos");
    }
}
