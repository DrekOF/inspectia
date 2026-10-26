/*
 * Nombre: CambioDeContrasenaServicio.java
 * Descripción: Cambio de la propia contraseña: revisa la actual con el bloqueo de RF-01, guarda la nueva,
 *              corta las demás sesiones y entrega el token con el que sigue la sesión (RF-18, D-30).
 * Fecha de creación: 19/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.usuario.sesion;

import co.edu.usco.inspectia.comun.errores.ContrasenaActualIncorrectaExcepcion;
import co.edu.usco.inspectia.comun.errores.CuentaBloqueadaExcepcion;
import co.edu.usco.inspectia.comun.seguridad.EmisorDeTokens;
import co.edu.usco.inspectia.comun.seguridad.SesionNoValidaExcepcion;
import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import co.edu.usco.inspectia.usuario.ReglasUsuario;
import co.edu.usco.inspectia.usuario.Usuario;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Cambio de la propia contraseña (RF-18). Cada usuario solo cambia la suya: el usuario sale de la
 * sesión, nunca de la petición. Revisa, en este orden:
 * <ol>
 *   <li>Los datos, con las validaciones de la contraseña (las revisa Spring antes de llamar al servicio):
 *       un dato inválido no cuenta como intento.</li>
 *   <li>El bloqueo de RF-01: durante el bloqueo no se puede cambiar la contraseña (429).</li>
 *   <li>La contraseña actual, con una sola comparación BCrypt. Si no coincide, el intento cuenta para el
 *       bloqueo, igual que en el inicio de sesión, para que esta función no sirva para adivinar
 *       contraseñas (400; el quinto fallo seguido ya responde 429).</li>
 * </ol>
 *
 * <p>Si todo está bien, guarda la nueva y aplica la regla {@code cortar_sesiones_al_cambiar_contrasena},
 * la misma del restablecimiento: las sesiones abiertas dejan de servir y la cuenta queda sin intentos
 * fallidos (D-30). La sesión desde la que se cambió sigue con un token nuevo, de la versión de sesión que
 * dejó la regla y que vence cuando vencía el anterior (RF-18 C5).
 *
 * <p>No es transaccional en conjunto: un intento fallido se guarda en su propia transacción, para que
 * cuente aunque la respuesta sea un error, y el cambio se guarda en otra.
 */
@Service
public class CambioDeContrasenaServicio {

    /** Campo de la contraseña actual en la petición: ahí va el error si no coincide. */
    static final String CAMPO_ACTUAL = "contrasenaActual";

    private final SesionRepositorio repositorio;
    private final ReglasUsuario reglas;
    private final PasswordEncoder codificador;
    private final EmisorDeTokens emisor;
    private final Clock reloj;
    private final TransactionTemplate transacciones;

    /**
     * Crea el servicio; Spring le pasa sus dependencias.
     *
     * @param repositorio las consultas de la sesión
     * @param reglas las reglas de usuario, con el efecto que corta las sesiones
     * @param codificador el codificador BCrypt de las contraseñas
     * @param emisor emite el token con el que sigue la sesión
     * @param reloj la hora del servidor
     * @param transacciones el gestor de transacciones, para guardar el cambio en una sola
     */
    public CambioDeContrasenaServicio(SesionRepositorio repositorio, ReglasUsuario reglas, PasswordEncoder codificador,
            EmisorDeTokens emisor, Clock reloj, PlatformTransactionManager transacciones) {
        this.repositorio = repositorio;
        this.reglas = reglas;
        this.codificador = codificador;
        this.emisor = emisor;
        this.reloj = reloj;
        this.transacciones = new TransactionTemplate(transacciones);
    }

    /**
     * Cambia la contraseña del usuario de la sesión.
     *
     * @param sesion el usuario de la sesión, con el vencimiento de su token
     * @param actual la contraseña actual, tal cual
     * @param nueva la contraseña nueva, ya validada
     * @return el token con el que sigue la sesión, cuándo vence y el usuario
     * @throws CuentaBloqueadaExcepcion si la cuenta está bloqueada o este fallo la bloqueó (429)
     * @throws ContrasenaActualIncorrectaExcepcion si la contraseña actual no coincide (400)
     */
    public SesionRespuesta cambiar(UsuarioEnSesion sesion, String actual, String nueva) {
        DatosDeSesion usuario = buscar(sesion.id());
        Instant ahora = reloj.instant();
        if (usuario.bloqueadaEn(ahora)) {
            throw bloqueada(usuario, ahora);
        }
        if (!coincide(actual, usuario.contrasenaHash())) {
            repositorio.registrarFallo(usuario.id(), SesionServicio.INTENTOS_PERMITIDOS, enUtc(ahora),
                    enUtc(ahora.plus(SesionServicio.BLOQUEO)));
            DatosDeSesion despues = buscar(usuario.id());
            if (despues.bloqueadaEn(ahora)) {
                throw bloqueada(despues, ahora);
            }
            throw new ContrasenaActualIncorrectaExcepcion(CAMPO_ACTUAL, SesionServicio.INTENTOS_PERMITIDOS,
                    SesionServicio.BLOQUEO.toMinutes());
        }
        String hash = codificador.encode(nueva);
        int versionSesion = transacciones.execute(estado -> guardar(sesion, hash));
        EmisorDeTokens.TokenEmitido token = emisor.emitir(sesion.id(), versionSesion, ahora, sesion.vence());
        return SesionRespuesta.de(token, sesion);
    }

    /**
     * Guarda el hash nuevo y aplica la regla que corta las sesiones abiertas y quita el bloqueo (D-30).
     *
     * @return la versión de sesión nueva, que la regla subió en la base
     */
    private int guardar(UsuarioEnSesion sesion, String hash) {
        Usuario usuario = repositorio.findById(sesion.id())
                .orElseThrow(() -> new SesionNoValidaExcepcion("error.sesion_invalida"));
        usuario.setContrasenaHash(hash);
        reglas.cortarSesionesAlCambiarContrasena(usuario, sesion);
        return usuario.getVersionSesion();
    }

    /**
     * Si la contraseña coincide con la guardada. BCrypt va primero y siempre, como en el inicio de sesión;
     * una contraseña de más de 72 bytes nunca coincide, aunque sus primeros 72 sí (RF-01 C8).
     */
    private boolean coincide(String contrasena, String hash) {
        return codificador.matches(contrasena, hash)
                && contrasena.getBytes(StandardCharsets.UTF_8).length <= SesionServicio.MAXIMO_BYTES;
    }

    /** El usuario de la sesión; si ya no existe, la sesión no sirve. */
    private DatosDeSesion buscar(UUID id) {
        return repositorio.buscarPorId(id).orElseThrow(() -> new SesionNoValidaExcepcion("error.sesion_invalida"));
    }

    /** La respuesta de una cuenta bloqueada, con los segundos que faltan redondeados hacia arriba. */
    private static CuentaBloqueadaExcepcion bloqueada(DatosDeSesion usuario, Instant ahora) {
        Instant hasta = usuario.bloqueadoHasta().toInstant();
        Duration falta = Duration.between(ahora, hasta);
        long segundos = falta.toSeconds() + (falta.toNanosPart() > 0 ? 1 : 0);
        return new CuentaBloqueadaExcepcion(hasta, Math.max(1, segundos));
    }

    private static OffsetDateTime enUtc(Instant instante) {
        return instante.atOffset(ZoneOffset.UTC);
    }
}
