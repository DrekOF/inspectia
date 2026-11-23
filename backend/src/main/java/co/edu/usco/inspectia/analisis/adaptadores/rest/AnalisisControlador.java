/*
 * Nombre: AnalisisControlador.java
 * Descripción: API REST del análisis de un lote: «Analizar la muestra», el avance del análisis y el resultado con las
 *              fotos marcadas (RF-05 C2, RF-10, RF-11, RF-12, RF-14 C3, RNF-01 C4, RNF-02 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.rest;

import co.edu.usco.inspectia.analisis.aplicacion.ConsultarAnalisis;
import co.edu.usco.inspectia.analisis.aplicacion.ConsultarResultado;
import co.edu.usco.inspectia.analisis.aplicacion.EnviarMuestra;
import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST del análisis de un lote, en {@value #RUTA} (RF-05 C2, RF-10 a RF-12, RF-14 C3). Intercambia JSON. La
 * empresa y el usuario salen de la sesión: un lote de otra empresa responde 404, como si no existiera (RNF-03). Solo
 * el usuario final envía a analizar; él y la administradora de empresa ven el avance y el resultado (RNF-04). Los
 * permisos los revisan la regla por ruta ({@link LotesAcceso}) y {@code @PreAuthorize}.
 */
@RestController
@RequestMapping(AnalisisControlador.RUTA)
@Tag(name = "Análisis", description = "Enviar a analizar la muestra de un lote, ver el avance y ver el resultado con "
        + "las fotos marcadas (RF-05, RF-10, RF-11, RF-12, RF-14).")
public class AnalisisControlador {

    /** Ruta de un lote, debajo de la cual están el análisis y el resultado. */
    public static final String RUTA = LotesControlador.RUTA + "/{id}";

    private final EnviarMuestra enviar;
    private final ConsultarAnalisis analisis;
    private final ConsultarResultado resultado;

    /**
     * Crea el controlador; Spring le pasa los casos de uso.
     *
     * @param enviar enviar la muestra a analizar
     * @param analisis consultar el avance del análisis
     * @param resultado consultar el resultado
     */
    public AnalisisControlador(EnviarMuestra enviar, ConsultarAnalisis analisis, ConsultarResultado resultado) {
        this.enviar = enviar;
        this.analisis = analisis;
        this.resultado = resultado;
    }

    /**
     * «Analizar la muestra»: envía la muestra en curso a la cola de análisis. Responde 202 con su avance.
     *
     * @param id el lote
     * @param sesion el usuario final, que queda como quien analizó la muestra
     * @return el avance de la muestra
     */
    @PostMapping("/analisis")
    @PreAuthorize("hasRole('USUARIO_FINAL')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Analizar la muestra", description = "Envía la muestra en curso del lote a la cola de "
            + "análisis: queda EN_ESPERA, con la fecha de envío y con quien la envía. Necesita al menos 2 fotos "
            + "confirmadas: sin muestra o con 1 foto responde 400 en «fotos», sin cambiar nada (RF-05 C2). Si la "
            + "muestra ya está EN_ESPERA o EN_PROCESO, responde lo mismo, así un reintento es seguro. Un lote ya "
            + "analizado responde 409. La cola atiende las muestras en el orden en que se enviaron (RNF-02 C3). Rol: "
            + "USUARIO_FINAL.")
    public AnalisisRespuesta enviar(@PathVariable("id") UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        return AnalisisRespuesta.de(enviar.enviar(sesion.empresaId(), sesion.id(), id));
    }

    /**
     * El avance del análisis de la muestra en curso, para el indicador de la página.
     *
     * @param id el lote
     * @param sesion el usuario de la sesión: solo ve los lotes de su empresa
     * @return el avance
     */
    @GetMapping("/analisis")
    @PreAuthorize("hasAnyRole('USUARIO_FINAL', 'ADMIN_EMPRESA')")
    @Operation(summary = "Ver el avance del análisis", description = "El estado y las fechas de la muestra en curso "
            + "del lote, para el indicador de avance (RNF-01 C4): EN_ESPERA mientras espera su turno (RNF-02 C3), "
            + "EN_PROCESO mientras se analiza y TERMINADA con el resultado. «fallo» es verdadero si el último envío "
            + "volvió sin resultado. Sin muestra, todo va vacío. No firma enlaces ni lee granos. Un lote de otra "
            + "empresa responde 404. Roles: USUARIO_FINAL y ADMIN_EMPRESA.")
    public AnalisisRespuesta avance(@PathVariable("id") UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        return AnalisisRespuesta.de(analisis.avance(sesion.empresaId(), id));
    }

    /**
     * El resultado guardado del lote y el detalle de sus muestras, con las fotos marcadas.
     *
     * @param id el lote
     * @param sesion el usuario de la sesión: solo ve los lotes de su empresa
     * @return el resultado
     */
    @GetMapping("/resultado")
    @PreAuthorize("hasAnyRole('USUARIO_FINAL', 'ADMIN_EMPRESA')")
    @Operation(summary = "Ver el resultado del lote", description = "El resultado guardado del lote, sin recalcularlo "
            + "(RF-13 C3): porcentaje corregido y su margen, categoría probable, recomendación, sugerencia de otra "
            + "muestra, aviso de confianza baja y lo que contó el modelo. Cada muestra trae quién la analizó, su "
            + "modelo, sus conteos y sus fotos, cada una con un enlace para verla que vence en 15 minutos y sus granos "
            + "con su clase y si son dudosos, sin la confianza (RF-11 C4). Mientras el lote está pendiente, las cifras "
            + "del resultado van vacías y las fotos, sin granos. Un lote de otra empresa responde 404. Roles: "
            + "USUARIO_FINAL y ADMIN_EMPRESA.")
    public ResultadoRespuesta resultado(@PathVariable("id") UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        return ResultadoRespuesta.de(resultado.detalle(sesion.empresaId(), id));
    }
}
