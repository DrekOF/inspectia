/*
 * Nombre: EmpresaControlador.java
 * Descripción: API REST de empresa en /api/v1/empresas (RF-02).
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.6)
 * Versión: 1.6
 * Entidades: empresa 1.2, usuario 1.5, rango_recomendacion 1.2
 */
package co.edu.usco.inspectia.empresa;

import co.edu.usco.inspectia.comun.api.Pagina;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * API REST de {@code empresa} en {@code /api/v1/empresas} (RF-02, RNF-03, D-13, D-27). Intercambia JSON.
 *
 * <p>Los roles de cada operación salen de {@code operaciones} del JSON: la cadena de seguridad los
 * revisa por ruta ({@link EmpresaAcceso}) y {@code @PreAuthorize} es la segunda barrera (RNF-04).
 */
@RestController
@RequestMapping("/api/v1/empresas")
@Tag(name = "Empresas", description = "Empresa cliente de InspectIA. Sus usuarios, rangos, lotes y resultados están separados de los de las demás empresas (RNF-03). No se borra: se desactiva (D-27).")
public class EmpresaControlador {

    private final EmpresaServicio servicio;

    /**
     * Crea el controlador; Spring le pasa el servicio.
     *
     * @param servicio las operaciones de {@code empresa}
     */
    public EmpresaControlador(EmpresaServicio servicio) {
        this.servicio = servicio;
    }

    /**
     * Lista empresas por páginas, por nombre, sin distinguir mayúsculas.
     *
     * @param pagina número de la página, desde 0
     * @param tamano filas por página
     * @return la página
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN_PLATAFORMA')")
    @Operation(summary = "Listar empresas", description = "Página de empresas, por nombre, sin distinguir mayúsculas. Roles: ADMIN_PLATAFORMA.")
    public Pagina<EmpresaRespuesta> listar(
            @Parameter(description = "Número de la página, desde 0.")
            @RequestParam(name = "pagina", defaultValue = "0")
            @Min(value = 0, message = "{validacion.minimo}") int pagina,
            @Parameter(description = "Filas por página, de 1 a " + Pagina.TAMANO_MAXIMO + ".")
            @RequestParam(name = "tamano", defaultValue = "" + Pagina.TAMANO_POR_DEFECTO)
            @Min(value = 1, message = "{validacion.minimo}")
            @Max(value = Pagina.TAMANO_MAXIMO, message = "{validacion.maximo}") int tamano) {
        return servicio.listar(pagina, tamano);
    }

    /**
     * Un registro por su identificador.
     *
     * @param id el identificador
     * @return el registro
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_PLATAFORMA')")
    @Operation(summary = "Ver empresa", description = "Roles: ADMIN_PLATAFORMA.")
    public EmpresaRespuesta ver(@PathVariable("id") UUID id) {
        return servicio.ver(id);
    }

    /**
     * Registra un registro nuevo. Responde 201 con su ubicación.
     *
     * @param solicitud los datos
     * @param ubicacion la dirección de esta petición, para armar la del registro nuevo
     * @return lo que quedó guardado
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN_PLATAFORMA')")
    @Operation(summary = "Crear empresa", description = "Roles: ADMIN_PLATAFORMA.")
    public ResponseEntity<EmpresaRespuesta> crear(@Valid @RequestBody EmpresaCrearSolicitud solicitud,
            UriComponentsBuilder ubicacion) {
        EmpresaRespuesta respuesta = servicio.crear(solicitud);
        URI uri = ubicacion.path("/api/v1/empresas/{id}").buildAndExpand(respuesta.id()).toUri();
        return ResponseEntity.created(uri).body(respuesta);
    }

    /**
     * Edita un registro.
     *
     * @param id el registro
     * @param solicitud los datos nuevos
     * @return lo que quedó guardado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_PLATAFORMA')")
    @Operation(summary = "Editar empresa", description = "Roles: ADMIN_PLATAFORMA.")
    public EmpresaRespuesta editar(@PathVariable("id") UUID id,
            @Valid @RequestBody EmpresaEditarSolicitud solicitud) {
        return servicio.editar(id, solicitud);
    }

    /**
     * Desactiva un registro; no borra nada.
     *
     * @param id el registro
     * @return lo que quedó guardado
     */
    @PostMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN_PLATAFORMA')")
    @Operation(summary = "Desactivar empresa", description = "No borra nada. Roles: ADMIN_PLATAFORMA.")
    public EmpresaRespuesta desactivar(@PathVariable("id") UUID id) {
        return servicio.desactivar(id);
    }

    /**
     * Reactiva un registro; no borra nada.
     *
     * @param id el registro
     * @return lo que quedó guardado
     */
    @PostMapping("/{id}/reactivar")
    @PreAuthorize("hasRole('ADMIN_PLATAFORMA')")
    @Operation(summary = "Reactivar empresa", description = "No borra nada. Roles: ADMIN_PLATAFORMA.")
    public EmpresaRespuesta reactivar(@PathVariable("id") UUID id) {
        return servicio.reactivar(id);
    }
}
