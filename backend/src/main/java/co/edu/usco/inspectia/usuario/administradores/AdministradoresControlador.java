/*
 * Nombre: AdministradoresControlador.java
 * Descripción: API REST del administrador de la plataforma sobre los administradores de una empresa:
 *              listarlos y restablecerles la contraseña (RF-02 C12, D-30).
 * Fecha de creación: 18/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario.administradores;

import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST de los administradores de una empresa en {@value #RUTA} (RF-02 C12). Intercambia JSON. Solo
 * la usa el administrador de la plataforma: la regla por ruta ({@link AdministradoresAcceso}) y
 * {@code @PreAuthorize} lo revisan (RNF-04).
 */
@RestController
@RequestMapping(AdministradoresControlador.RUTA)
@Tag(name = "Administradores de empresa", description = "El administrador de la plataforma ve los administradores "
        + "de una empresa y les restablece la contraseña (RF-02 C12, D-30).")
public class AdministradoresControlador {

    /** Ruta de los administradores de una empresa. */
    public static final String RUTA = "/api/v1/empresas/{empresaId}/administradores";

    /** Ruta de la contraseña de uno de ellos. */
    public static final String CONTRASENA = RUTA + "/{usuarioId}/contrasena";

    private final AdministradoresServicio servicio;

    /**
     * Crea el controlador; Spring le pasa el servicio.
     *
     * @param servicio lo que se hace con los administradores de una empresa
     */
    public AdministradoresControlador(AdministradoresServicio servicio) {
        this.servicio = servicio;
    }

    /**
     * Los administradores de una empresa, para elegir a cuál restablecerle la contraseña.
     *
     * @param empresaId la empresa
     * @return sus administradores, por nombre
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN_PLATAFORMA')")
    @Operation(summary = "Listar los administradores de una empresa", description = "Sus administradores, activos "
            + "y desactivados, por nombre y sin su contraseña. Si la empresa no existe, responde 404. "
            + "Roles: ADMIN_PLATAFORMA.")
    public List<AdministradorRespuesta> listar(@PathVariable("empresaId") UUID empresaId) {
        return servicio.listar(empresaId);
    }

    /**
     * Restablece la contraseña de un administrador de la empresa. Responde 204, sin cuerpo.
     *
     * @param empresaId la empresa
     * @param usuarioId el administrador
     * @param solicitud la contraseña nueva
     * @param sesion el administrador de la plataforma
     */
    @PutMapping("/{usuarioId}/contrasena")
    @PreAuthorize("hasRole('ADMIN_PLATAFORMA')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Restablecer la contraseña de un administrador de empresa", description = "Con las mismas "
            + "validaciones de la contraseña inicial. Corta las sesiones abiertas del administrador y desbloquea su "
            + "cuenta; no lo reactiva ni a él ni a su empresa. Si el usuario no es administrador de esa empresa, "
            + "responde 404. Roles: ADMIN_PLATAFORMA.")
    public void restablecerContrasena(@PathVariable("empresaId") UUID empresaId,
            @PathVariable("usuarioId") UUID usuarioId,
            @Valid @RequestBody RestablecerContrasenaSolicitud solicitud,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion sesion) {
        servicio.restablecerContrasena(empresaId, usuarioId, solicitud.contrasena(), sesion);
    }
}
