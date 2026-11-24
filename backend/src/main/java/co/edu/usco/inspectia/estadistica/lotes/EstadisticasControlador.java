/*
 * Nombre: EstadisticasControlador.java
 * Descripción: API REST de las estadísticas de la empresa (RF-16): lotes analizados, promedio de granos sanos, lotes
 *              por categoría, promedio por mes y por finca o proveedor en un periodo. Solo para el administrador de
 *              empresa (RF-16 C3, RNF-04).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.estadistica.lotes;

import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST de las estadísticas en {@value #RUTA} (RF-16). Intercambia JSON. La empresa sale de la sesión: las cifras
 * nunca mezclan empresas (RNF-03 C2). Los permisos los revisan la regla por ruta ({@link EstadisticasAcceso}) y
 * {@code @PreAuthorize} (RNF-04).
 */
@RestController
@RequestMapping(EstadisticasControlador.RUTA)
@Tag(name = "Estadísticas", description = "El administrador de empresa consulta los indicadores de los lotes "
        + "analizados en un periodo (RF-16).")
public class EstadisticasControlador {

    /** Ruta de las estadísticas. */
    public static final String RUTA = "/api/v1/estadisticas";

    private final EstadisticasServicio servicio;

    /**
     * Crea el controlador; Spring le pasa el servicio.
     *
     * @param servicio las estadísticas
     */
    public EstadisticasControlador(EstadisticasServicio servicio) {
        this.servicio = servicio;
    }

    /**
     * Las estadísticas de la empresa en el periodo.
     *
     * @param desde el primer día de la fecha del resultado, incluido, o vacío
     * @param hasta el último día de la fecha del resultado, incluido, o vacío
     * @param sesion el administrador de empresa
     * @return las estadísticas
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    @Operation(summary = "Consultar las estadísticas de la empresa", description = "Los lotes analizados de la empresa "
            + "con la fecha del resultado en el periodo: cuántos son, el promedio de su porcentaje de granos sanos, "
            + "cuántos hay de cada categoría probable, el promedio de cada mes y el de cada finca o proveedor. Usa el "
            + "resultado de cada lote; con 2 muestras, el de la suma (D-25). Las cifras coinciden con el historial "
            + "filtrado por «Analizado» y las mismas fechas (RF-16 C1). Las fechas son días de Colombia con las dos "
            + "incluidas; sin fechas, cuenta todos los lotes analizados. Si «hasta» es anterior a «desde», responde "
            + "400 en «hasta»; una fecha que no es un día, o fuera de los años 0001 a 9999, 400 en su campo. Sin lotes "
            + "en el periodo, «sinLotes» es verdadero (RF-16 C2). La misma finca escrita con otras mayúsculas, tildes "
            + "o espacios al inicio o al final cuenta una vez (RF-16 C4). Roles: ADMIN_EMPRESA.")
    public Estadisticas consultar(
            @Parameter(description = "«Desde»: el primer día de la fecha del resultado, en Colombia e incluido, de los "
                    + "años 0001 a 9999.", example = "2026-11-01")
            @RequestParam(name = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "«Hasta»: el último día de la fecha del resultado, en Colombia e incluido, de los "
                    + "años 0001 a 9999; no puede ser anterior a «desde».", example = "2026-11-30")
            @RequestParam(name = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        return servicio.calcular(sesion.empresaId(), desde, hasta);
    }
}
