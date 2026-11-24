/*
 * Nombre: MetricasDelModeloControlador.java
 * Descripción: API REST de la pantalla «Modelo de IA»: las métricas de prueba de los modelos, cuál está en uso y la
 *              confianza promedio de los análisis de la empresa, solo para el administrador de empresa (RF-17, RNF-04).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.estadistica.modelo;

import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST del modelo de IA en {@value #RUTA} (RF-17). Intercambia JSON. Solo la usa el administrador de empresa: la
 * regla por ruta ({@link MetricasDelModeloAcceso}) y {@code @PreAuthorize} lo revisan (RNF-04); el usuario final no ve
 * métricas técnicas (RF-17 C3).
 */
@RestController
@RequestMapping(MetricasDelModeloControlador.RUTA)
@Tag(name = "Modelo de IA", description = "El administrador de empresa ve las métricas de prueba de los modelos de IA, "
        + "cuál está en uso y la confianza promedio de los análisis de su empresa (RF-17, D-14, D-18, D-48).")
public class MetricasDelModeloControlador {

    /** Ruta del modelo de IA. */
    public static final String RUTA = "/api/v1/modelo";

    private final MetricasDelModeloServicio servicio;

    /**
     * Crea el controlador; Spring le pasa el servicio.
     *
     * @param servicio lo que arma la respuesta
     */
    public MetricasDelModeloControlador(MetricasDelModeloServicio servicio) {
        this.servicio = servicio;
    }

    /**
     * Las métricas de los modelos y la confianza promedio de la empresa de la sesión.
     *
     * @param sesion el administrador de empresa
     * @return los modelos y la confianza promedio
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    @Operation(summary = "Ver las métricas del modelo de IA", description = "Los modelos registrados con sus métricas "
            + "del conjunto de prueba, las de la tabla comparativa de D-18: F1, precisión y recall macro, precisión y "
            + "recall de cada clase, la matriz de confusión y el tiempo por grano (D-48), del mayor F1 macro al menor; "
            + "el modelo en uso lleva enUso en true. Trae también la confianza promedio de los granos de los análisis "
            + "de la empresa de la sesión, con cuántos granos y muestras la dan; solo de su empresa (RNF-03). "
            + "Roles: ADMIN_EMPRESA.")
    public MetricasDelModeloRespuesta consultar(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        return servicio.consultar(sesion.empresaId());
    }
}
