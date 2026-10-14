/*
 * Nombre: SesionServicio.java
 * Descripción: Inicio de sesión con correo y contraseña, bloqueo por intentos fallidos y usuario de
 *              cada token (RF-01, RNF-04, D-28).
 * Fecha de creación: 14/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario.sesion;

import co.edu.usco.inspectia.comun.errores.SesionRechazadaExcepcion;
import co.edu.usco.inspectia.comun.seguridad.CargadorDeSesion;
import co.edu.usco.inspectia.comun.seguridad.EmisorDeTokens;
import co.edu.usco.inspectia.comun.seguridad.SesionNoValidaExcepcion;
import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Inicio de sesión (RF-01, RNF-04, D-28) y usuario de cada token.
 *
 * <p>Todo intento fallido recibe la misma respuesta, «Correo o contraseña incorrectos», con la regla
 * del bloqueo: no dice si el correo existe, si la cuenta está bloqueada ni cuál de los dos datos
 * falló. Que el usuario o su empresa están desactivados solo se informa con la contraseña correcta.
 *
 * <p>Cada intento hace una sola comparación BCrypt, también si el correo no existe (contra un hash
 * de relleno) o si la cuenta está bloqueada. Así todos los intentos tardan lo mismo y el tiempo de
 * respuesta tampoco revela nada.
 *
 * <p>No es transaccional: cada cambio en la base se guarda por su cuenta, para que un intento
 * fallido quede contado aunque la respuesta sea un error.
 */
@Service
public class SesionServicio implements CargadorDeSesion {

    /** Intentos fallidos seguidos que bloquean la cuenta (RF-01 C5, RNF-04). */
    static final int INTENTOS_PERMITIDOS = 5;

    /** Duración del bloqueo (RF-01 C5, RNF-04). */
    static final Duration BLOQUEO = Duration.ofMinutes(15);

    /** Bytes que compara BCrypt. El sistema no guarda contraseñas más largas (RF-01 C8). */
    static final int MAXIMO_BYTES = 72;

    private final SesionRepositorio repositorio;
    private final PasswordEncoder codificador;
    private final EmisorDeTokens emisor;
    private final Clock reloj;

    /** Hash de una contraseña al azar, del mismo costo que los guardados: se usa si el correo no existe. */
    private final String hashDeRelleno;

    /**
     * Crea el servicio; Spring le pasa sus dependencias.
     *
     * @param repositorio las consultas de la sesión
     * @param codificador el codificador BCrypt de las contraseñas
     * @param emisor emite los tokens
     * @param reloj la hora del servidor
     */
    public SesionServicio(SesionRepositorio repositorio, PasswordEncoder codificador, EmisorDeTokens emisor,
            Clock reloj) {
        this.repositorio = repositorio;
        this.codificador = codificador;
        this.emisor = emisor;
        this.reloj = reloj;
        this.hashDeRelleno = codificador.encode(UUID.randomUUID().toString());
    }

    /**
     * Inicia sesión con un correo y una contraseña.
     *
     * @param correo el correo, sin distinguir mayúsculas ni espacios al inicio o al final
     * @param contrasena la contraseña
     * @return el token, cuándo vence y el usuario con su rol y su empresa
     * @throws SesionRechazadaExcepcion si no puede iniciar sesión (401)
     */
    public SesionRespuesta iniciar(String correo, String contrasena) {
        Optional<DatosDeSesion> encontrado = repositorio.buscarPorCorreo(correo);
        // BCrypt va primero y siempre: si el largo se revisara antes, una contraseña de más de 72
        // bytes respondería más rápido que las demás (RF-01 C8).
        boolean coincide = codificador.matches(contrasena,
                encontrado.map(DatosDeSesion::contrasenaHash).orElse(hashDeRelleno))
                && contrasena.getBytes(StandardCharsets.UTF_8).length <= MAXIMO_BYTES;
        Instant ahora = reloj.instant();
        DatosDeSesion usuario = encontrado.filter(datos -> !datos.bloqueadaEn(ahora))
                .orElseThrow(SesionServicio::credencialesIncorrectas);
        if (!coincide) {
            repositorio.registrarFallo(usuario.id(), INTENTOS_PERMITIDOS, enUtc(ahora), enUtc(ahora.plus(BLOQUEO)));
            throw credencialesIncorrectas();
        }
        repositorio.reiniciarIntentos(usuario.id());
        if (usuario.desactivado()) {
            throw new SesionRechazadaExcepcion("error.usuario_desactivado");
        }
        if (usuario.empresaDesactivada()) {
            throw new SesionRechazadaExcepcion("error.empresa_desactivada");
        }
        EmisorDeTokens.TokenEmitido token = emisor.emitir(usuario.id());
        return SesionRespuesta.de(token, usuario.enSesion(token.vence()));
    }

    /**
     * El usuario de un token válido, leído de la base en cada petición (D-28).
     *
     * @param id el identificador que trae el token
     * @param vence cuándo vence el token
     * @return el usuario, con su rol y su empresa de este momento
     * @throws SesionNoValidaExcepcion si el usuario ya no existe
     */
    @Override
    public UsuarioEnSesion cargar(UUID id, Instant vence) {
        return repositorio.buscarPorId(id)
                .map(usuario -> usuario.enSesion(vence))
                .orElseThrow(() -> new SesionNoValidaExcepcion("error.sesion_invalida"));
    }

    /** La respuesta de todo intento fallido: el mismo mensaje y la regla del bloqueo. */
    private static SesionRechazadaExcepcion credencialesIncorrectas() {
        return new SesionRechazadaExcepcion("error.credenciales_incorrectas", "error.ayuda_bloqueo",
                INTENTOS_PERMITIDOS, BLOQUEO.toMinutes());
    }

    private static OffsetDateTime enUtc(Instant instante) {
        return instante.atOffset(ZoneOffset.UTC);
    }
}
