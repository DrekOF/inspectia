/*
 * Nombre: UsuarioControlador.java
 * Descripción: API REST de usuario en /api/v1/usuarios (RF-03).
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.9)
 * Versión: 1.9
 * Entidades: usuario 1.8
 */
package co.edu.usco.inspectia.usuario;

import co.edu.usco.inspectia.comun.api.Pagina;
import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 * API REST de {@code usuario} en {@code /api/v1/usuarios} (RF-03, RF-01, RF-02, RNF-03, RNF-04). Intercambia JSON.
 *
 * <p>Los roles de cada operación salen de {@code operaciones} del JSON: la cadena de seguridad los
 * revisa por ruta ({@link UsuarioAcceso}) y {@code @PreAuthorize} es la segunda barrera (RNF-04).
 *
 * <p>La empresa sale de la sesión y nunca de la petición: cada operación solo ve y modifica los
 * registros de la empresa del usuario (RNF-03).
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios", description = "Persona que usa InspectIA, con su rol. Los usuarios no se borran: se desactivan, para no perder quién hizo cada análisis (RF-03). Solo el administrador de la plataforma no tiene empresa (RNF-03).")
public class UsuarioControlador {

    private final UsuarioServicio servicio;

    /**
     * Crea el controlador; Spring le pasa el servicio.
     *
     * @param servicio las operaciones de {@code usuario}
     */
    public UsuarioControlador(UsuarioServicio servicio) {
        this.servicio = servicio;
    }

    /**
     * Lista usuarios por páginas, por nombre, sin distinguir mayúsculas.
     *
     * @param pagina número de la página, desde 0
     * @param tamano filas por página
     * @param activo solo los activos ({@code true}) o solo los desactivados ({@code false}); sin él, todos
     * @param sesion el usuario de la sesión: solo ve los de su empresa
     * @return la página
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    @Operation(summary = "Listar usuarios", description = "Página de usuarios, por nombre, sin distinguir mayúsculas. Roles: ADMIN_EMPRESA.")
    public Pagina<UsuarioRespuesta> listar(
            @Parameter(description = "Número de la página, desde 0.")
            @RequestParam(name = "pagina", defaultValue = "0")
            @Min(value = 0, message = "{validacion.minimo}") int pagina,
            @Parameter(description = "Filas por página, de 1 a " + Pagina.TAMANO_MAXIMO + ".")
            @RequestParam(name = "tamano", defaultValue = "" + Pagina.TAMANO_POR_DEFECTO)
            @Min(value = 1, message = "{validacion.minimo}")
            @Max(value = Pagina.TAMANO_MAXIMO, message = "{validacion.maximo}") int tamano,
            @Parameter(description = "Solo los activos (true) o solo los desactivados (false); sin él, todos.")
            @RequestParam(name = "activo", required = false) Boolean activo,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        return servicio.listar(pagina, tamano, activo, sesion);
    }

    /**
     * Un registro por su identificador.
     *
     * @param id el identificador
     * @param sesion el usuario de la sesión: un registro de otra empresa no existe para él
     * @return el registro
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    @Operation(summary = "Ver usuario", description = "Roles: ADMIN_EMPRESA.")
    public UsuarioRespuesta ver(@PathVariable("id") UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        return servicio.ver(id, sesion);
    }

    /**
     * Registra un registro nuevo. Responde 201 con su ubicación.
     *
     * @param solicitud los datos
     * @param ubicacion la dirección de esta petición, para armar la del registro nuevo
     * @param sesion el usuario de la sesión: el registro queda en su empresa
     * @return lo que quedó guardado
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    @Operation(summary = "Crear usuario", description = "Roles: ADMIN_EMPRESA.")
    public ResponseEntity<UsuarioRespuesta> crear(@Valid @RequestBody UsuarioCrearSolicitud solicitud,
            UriComponentsBuilder ubicacion,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        UsuarioRespuesta respuesta = servicio.crear(solicitud, sesion);
        URI uri = ubicacion.path("/api/v1/usuarios/{id}").buildAndExpand(respuesta.id()).toUri();
        return ResponseEntity.created(uri).body(respuesta);
    }

    /**
     * Edita un registro.
     *
     * @param id el registro
     * @param solicitud los datos nuevos
     * @param sesion el usuario de la sesión: un registro de otra empresa no existe para él
     * @return lo que quedó guardado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    @Operation(summary = "Editar usuario", description = "Roles: ADMIN_EMPRESA.")
    public UsuarioRespuesta editar(@PathVariable("id") UUID id,
            @Valid @RequestBody UsuarioEditarSolicitud solicitud,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        return servicio.editar(id, solicitud, sesion);
    }

    /**
     * Desactiva un registro; no borra nada.
     *
     * @param id el registro
     * @param sesion el usuario de la sesión: un registro de otra empresa no existe para él
     * @return lo que quedó guardado
     */
    @PostMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    @Operation(summary = "Desactivar usuario", description = "No borra nada. Roles: ADMIN_EMPRESA.")
    public UsuarioRespuesta desactivar(@PathVariable("id") UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        return servicio.desactivar(id, sesion);
    }

    /**
     * Reactiva un registro; no borra nada.
     *
     * @param id el registro
     * @param sesion el usuario de la sesión: un registro de otra empresa no existe para él
     * @return lo que quedó guardado
     */
    @PostMapping("/{id}/reactivar")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    @Operation(summary = "Reactivar usuario", description = "No borra nada. Roles: ADMIN_EMPRESA.")
    public UsuarioRespuesta reactivar(@PathVariable("id") UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        return servicio.reactivar(id, sesion);
    }
}
