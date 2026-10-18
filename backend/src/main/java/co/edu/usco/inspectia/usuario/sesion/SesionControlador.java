/*
 * Nombre: SesionControlador.java
 * Descripción: API REST de la sesión en /api/v1/sesion: iniciar sesión y ver la sesión actual (RF-01,
 *              D-28). Intercambia JSON.
 * Fecha de creación: 14/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.usuario.sesion;

import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST de la sesión en {@value #RUTA} (RF-01, D-28). Intercambia JSON.
 *
 * <p>Para cerrar sesión no hay ruta: el frontend borra el token (D-28).
 */
@RestController
@RequestMapping(SesionControlador.RUTA)
@Tag(name = "Sesión", description = "Inicio de sesión con correo y contraseña y sesión actual (RF-01, D-28). "
        + "El token va en el encabezado Authorization: Bearer de cada petición y vence a las 8 horas; para "
        + "cerrar sesión, el frontend lo borra.")
public class SesionControlador {

    /** Ruta de la sesión. */
    public static final String RUTA = "/api/v1/sesion";

    private final SesionServicio servicio;

    /**
     * Crea el controlador; Spring le pasa el servicio.
     *
     * @param servicio el inicio de sesión
     */
    public SesionControlador(SesionServicio servicio) {
        this.servicio = servicio;
    }

    /**
     * Inicia sesión con el correo y la contraseña.
     *
     * @param solicitud el correo y la contraseña
     * @return el token, cuándo vence y el usuario con su rol y su empresa
     */
    @PostMapping
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión", description = "Revisa el correo y la contraseña y entrega el token, con "
            + "el usuario, su rol y su empresa para armar el menú de su rol. Todo intento fallido responde 401 con "
            + "«Correo o contraseña incorrectos»; después de 5 seguidos, la cuenta se bloquea 15 minutos. Pública: "
            + "no lee el encabezado Authorization, así que con la sesión vencida se puede volver a entrar.")
    public SesionRespuesta iniciar(@Valid @RequestBody InicioDeSesionSolicitud solicitud) {
        return servicio.iniciar(solicitud.correo(), solicitud.contrasena());
    }

    /**
     * La sesión actual: el usuario, leído de la base en esta petición, y cuándo vence.
     *
     * @param usuario el usuario de la sesión
     * @return la sesión
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Ver la sesión actual", description = "El usuario de la sesión, con su rol y su empresa "
            + "leídos de la base en esta petición, y cuándo vence la sesión. Todos los roles.")
    public SesionActualRespuesta actual(@AuthenticationPrincipal UsuarioEnSesion usuario) {
        return SesionActualRespuesta.de(usuario);
    }
}
