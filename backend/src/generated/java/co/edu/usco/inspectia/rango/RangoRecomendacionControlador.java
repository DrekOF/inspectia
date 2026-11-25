/*
 * Nombre: RangoRecomendacionControlador.java
 * Descripción: API REST de rango_recomendacion en /api/v1/rangos-recomendacion (RF-13).
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.11)
 * Versión: 1.11
 * Entidades: rango_recomendacion 1.3
 */
package co.edu.usco.inspectia.rango;

import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST de {@code rango_recomendacion} en {@code /api/v1/rangos-recomendacion} (RF-13, RF-02, RF-12, D-10). Intercambia JSON.
 *
 * <p>Se edita en bloque: cada empresa tiene una fila por cada categoría, y las filas se editan
 * todas juntas. No se crean ni se borran filas sueltas.
 *
 * <p>Los roles de cada operación salen de {@code operaciones} del JSON: la cadena de seguridad los
 * revisa por ruta ({@link RangoRecomendacionAcceso}) y {@code @PreAuthorize} es la segunda barrera (RNF-04).
 *
 * <p>La empresa sale de la sesión y nunca de la petición: cada operación solo ve y modifica las filas
 * de la empresa del usuario (RNF-03).
 */
@RestController
@RequestMapping("/api/v1/rangos-recomendacion")
@Tag(name = "Rangos de recomendación", description = "Rango de una categoría probable: desde qué porcentaje de granos sanos aplica y qué se recomienda (D-10, anexo A.3). Cada empresa tiene siempre las 4 categorías; solo cambian sus límites y sus textos (RF-13).")
public class RangoRecomendacionControlador {

    private final RangoRecomendacionServicio servicio;

    /**
     * Crea el controlador; Spring le pasa el servicio.
     *
     * @param servicio las operaciones de {@code rango_recomendacion}
     */
    public RangoRecomendacionControlador(RangoRecomendacionServicio servicio) {
        this.servicio = servicio;
    }

    /**
     * Las filas de la empresa, una por cada categoría, en el orden de la lista.
     *
     * @param sesion el usuario de la sesión: solo ve las de su empresa
     * @return las filas
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    @Operation(summary = "Listar rangos de recomendación", description = "Las filas de la empresa, una por cada categoría, en el orden de la lista. Roles: ADMIN_EMPRESA.")
    public List<RangoRecomendacionRespuesta> listar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        return servicio.listar(sesion);
    }

    /**
     * Edita todas las filas de la empresa a la vez: llega una por cada categoría, sin repetir.
     * Si alguna no cumple, no guarda ninguna.
     *
     * @param solicitud las filas
     * @param sesion el usuario de la sesión: solo cambia las de su empresa
     * @return las filas guardadas
     */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    @Operation(summary = "Editar rangos de recomendación", description = "Llegan todas las filas juntas, una por cada categoría; si alguna no cumple, no guarda ninguna. Roles: ADMIN_EMPRESA.")
    public List<RangoRecomendacionRespuesta> editar(
            @Valid @RequestBody RangoRecomendacionEdicionSolicitud solicitud,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        return servicio.editar(solicitud, sesion);
    }

    /**
     * Vuelve a los valores por defecto: los mismos que recibe cada empresa nueva.
     *
     * @param sesion el usuario de la sesión: solo cambia las de su empresa
     * @return las filas guardadas
     */
    @PostMapping("/restaurar-por-defecto")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    @Operation(summary = "Restaurar los valores por defecto de rangos de recomendación", description = "Deja las filas con los valores que recibe cada empresa nueva. Roles: ADMIN_EMPRESA.")
    public List<RangoRecomendacionRespuesta> restaurarPorDefecto(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        return servicio.restaurarPorDefecto(sesion);
    }
}
