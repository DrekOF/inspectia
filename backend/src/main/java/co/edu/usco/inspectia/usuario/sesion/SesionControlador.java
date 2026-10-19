/*
 * Nombre: SesionControlador.java
 * Descripción: API REST de la sesión en /api/v1/sesion: iniciar sesión, ver la sesión actual y cambiar
 *              la propia contraseña (RF-01, RF-18, D-28). Intercambia JSON.
 * Fecha de creación: 14/10/2026
 * Actualización: 19/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia.usuario.sesion;

import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST de la sesión en {@value #RUTA} (RF-01, RF-18, D-28). Intercambia JSON.
 *
 * <p>Para cerrar sesión no hay ruta: el frontend borra el token (D-28).
 */
@RestController
@RequestMapping(SesionControlador.RUTA)
@Tag(name = "Sesión", description = "Inicio de sesión con correo y contraseña, sesión actual y cambio de la propia "
        + "contraseña (RF-01, RF-18, D-28). El token va en el encabezado Authorization: Bearer de cada petición y "
        + "vence a las 8 horas; para cerrar sesión, el frontend lo borra.")
public class SesionControlador {

    /** Ruta de la sesión. */
    public static final String RUTA = "/api/v1/sesion";

    /** Ruta del cambio de la propia contraseña (RF-18). */
    public static final String CONTRASENA = RUTA + "/contrasena";

    private final SesionServicio servicio;
    private final CambioDeContrasenaServicio cambio;

    /**
     * Crea el controlador; Spring le pasa los servicios.
     *
     * @param servicio el inicio de sesión
     * @param cambio el cambio de la propia contraseña
     */
    public SesionControlador(SesionServicio servicio, CambioDeContrasenaServicio cambio) {
        this.servicio = servicio;
        this.cambio = cambio;
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

    /**
     * Cambia la contraseña del usuario de la sesión. La sesión sigue con el token nuevo de la respuesta;
     * las demás dejan de servir (RF-18 C5).
     *
     * @param solicitud la contraseña actual y la nueva
     * @param usuario el usuario de la sesión: cada usuario solo cambia la suya
     * @return el token con el que sigue la sesión, cuándo vence y el usuario
     */
    @PutMapping("/contrasena")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cambiar la propia contraseña", description = "Pide la contraseña actual y valida la nueva "
            + "como la inicial. Si la actual no es correcta, responde 400 y el intento cuenta para el bloqueo del "
            + "inicio de sesión; desde el quinto fallo seguido y mientras dure el bloqueo responde 429, con "
            + "bloqueadoHasta y el encabezado Retry-After. Si cambia, las demás sesiones del usuario dejan de servir "
            + "y esta sigue con el token nuevo de la respuesta, que vence cuando vencía el anterior. Todos los roles.")
    public SesionRespuesta cambiarContrasena(@Valid @RequestBody CambioDeContrasenaSolicitud solicitud,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioEnSesion usuario) {
        return cambio.cambiar(usuario, solicitud.contrasenaActual(), solicitud.contrasenaNueva());
    }
}
