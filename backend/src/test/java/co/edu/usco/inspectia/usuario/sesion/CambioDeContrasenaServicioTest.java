/*
 * Nombre: CambioDeContrasenaServicioTest.java
 * Descripción: Pruebas unitarias del cambio de la propia contraseña con Mockito: orden de las
 *              revisiones, bloqueo, contraseñas de más de 72 bytes y token de la sesión que sigue (RF-18).
 * Fecha de creación: 19/10/2026
 * Actualización: 19/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario.sesion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.usco.inspectia.comun.errores.CampoInvalido;
import co.edu.usco.inspectia.comun.errores.ContrasenaActualIncorrectaExcepcion;
import co.edu.usco.inspectia.comun.errores.CuentaBloqueadaExcepcion;
import co.edu.usco.inspectia.comun.seguridad.EmisorDeTokens;
import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import co.edu.usco.inspectia.usuario.ReglasUsuario;
import co.edu.usco.inspectia.usuario.Usuario;
import co.edu.usco.inspectia.usuario.UsuarioRol;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Pruebas del servicio del cambio de la propia contraseña, sin base de datos: el repositorio, BCrypt,
 * las reglas, el emisor de tokens y las transacciones son simulados, y el reloj es fijo.
 * {@code CambioDeContrasenaIT} prueba lo mismo con PostgreSQL.
 */
@ExtendWith(MockitoExtension.class)
class CambioDeContrasenaServicioTest {

    private static final Instant AHORA = Instant.parse("2026-10-19T15:00:00.400Z");
    private static final Instant VENCE = Instant.parse("2026-10-19T21:30:00Z");
    private static final UUID ID = UUID.fromString("0199e2a0-0000-7000-8000-000000000001");
    private static final UUID EMPRESA = UUID.fromString("0199e2a0-0000-7000-8000-000000000002");
    private static final String ACTUAL = "Clave-Segura-2026";
    private static final String NUEVA = "Nueva-Clave-2026";
    private static final String HASH = "hash-guardado";

    @Mock
    private SesionRepositorio repositorio;

    @Mock
    private ReglasUsuario reglas;

    @Mock
    private PasswordEncoder codificador;

    @Mock
    private EmisorDeTokens emisor;

    @Mock
    private PlatformTransactionManager transacciones;

    private CambioDeContrasenaServicio servicio;

    @BeforeEach
    void preparar() {
        servicio = new CambioDeContrasenaServicio(repositorio, reglas, codificador, emisor,
                Clock.fixed(AHORA, ZoneOffset.UTC), transacciones);
    }

    @Test
    @DisplayName("RF-18 · C2 · durante el bloqueo no compara la contraseña ni cuenta el intento: responde que la cuenta está bloqueada y cuántos segundos faltan, redondeados hacia arriba")
    void duranteElBloqueoNoCompara() {
        Instant hasta = AHORA.plusMillis(90_100);
        when(repositorio.buscarPorId(ID)).thenReturn(Optional.of(usuario(hasta)));

        assertThatThrownBy(() -> servicio.cambiar(sesion(), ACTUAL, NUEVA))
                .isInstanceOfSatisfying(CuentaBloqueadaExcepcion.class, excepcion -> {
                    assertThat(excepcion.getBloqueadoHasta()).isEqualTo(hasta);
                    assertThat(excepcion.getSegundosRestantes()).isEqualTo(91);
                });

        verify(codificador, never()).matches(anyString(), anyString());
        verify(repositorio, never()).registrarFallo(any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("RF-18 · C2 · con la contraseña actual errada registra el fallo con la hora del servidor y responde en el campo, con la regla del bloqueo")
    void conLaActualErradaRegistraElFallo() {
        when(repositorio.buscarPorId(ID)).thenReturn(Optional.of(usuario(null)));

        assertThatThrownBy(() -> servicio.cambiar(sesion(), "Otra-Clave-2026", NUEVA))
                .isInstanceOfSatisfying(ContrasenaActualIncorrectaExcepcion.class, excepcion -> {
                    assertThat(excepcion.getCampos()).containsExactly(
                            new CampoInvalido("contrasenaActual", ContrasenaActualIncorrectaExcepcion.MENSAJE));
                    assertThat(excepcion.getIntentosPermitidos()).isEqualTo(5);
                    assertThat(excepcion.getMinutosDeBloqueo()).isEqualTo(15);
                });

        verify(codificador).matches("Otra-Clave-2026", HASH);
        verify(repositorio).registrarFallo(ID, 5, enUtc(AHORA), enUtc(AHORA.plus(Duration.ofMinutes(15))));
        verify(codificador, never()).encode(anyString());
    }

    @Test
    @DisplayName("RF-18 · C2 · si ese fallo es el quinto seguido y bloquea la cuenta, ya responde que está bloqueada por 15 minutos")
    void elQuintoFalloYaRespondeBloqueada() {
        when(repositorio.buscarPorId(ID)).thenReturn(Optional.of(usuario(null)),
                Optional.of(usuario(AHORA.plus(Duration.ofMinutes(15)))));

        assertThatThrownBy(() -> servicio.cambiar(sesion(), "Otra-Clave-2026", NUEVA))
                .isInstanceOfSatisfying(CuentaBloqueadaExcepcion.class,
                        excepcion -> assertThat(excepcion.getSegundosRestantes()).isEqualTo(900));

        verify(repositorio).registrarFallo(ID, 5, enUtc(AHORA), enUtc(AHORA.plus(Duration.ofMinutes(15))));
    }

    @Test
    @DisplayName("RF-18 · una contraseña actual de más de 72 bytes no coincide aunque BCrypt diga que sí, y cuenta como fallo (RF-01 C8)")
    void unaActualDeMasDe72BytesNoCoincide() {
        String larga = "ñ".repeat(8) + "a".repeat(56) + "b";
        when(repositorio.buscarPorId(ID)).thenReturn(Optional.of(usuario(null)));
        when(codificador.matches(larga, HASH)).thenReturn(true);

        assertThatThrownBy(() -> servicio.cambiar(sesion(), larga, NUEVA))
                .isInstanceOf(ContrasenaActualIncorrectaExcepcion.class);

        verify(repositorio).registrarFallo(ID, 5, enUtc(AHORA), enUtc(AHORA.plus(Duration.ofMinutes(15))));
    }

    @Test
    @DisplayName("RF-18 · C5 · al cambiarla guarda el hash nuevo, aplica la regla que corta las sesiones y entrega un token del segundo del cambio que vence cuando vencía el anterior")
    void alCambiarlaSigueLaSesion() {
        Instant cambio = Instant.parse("2026-10-19T15:00:00Z");
        Usuario guardado = new Usuario();
        guardado.setId(ID);
        guardado.setContrasenaHash(HASH);
        when(repositorio.buscarPorId(ID)).thenReturn(Optional.of(usuario(null)));
        when(codificador.matches(ACTUAL, HASH)).thenReturn(true);
        when(codificador.encode(NUEVA)).thenReturn("hash-nuevo");
        when(repositorio.findById(ID)).thenReturn(Optional.of(guardado));
        UsuarioEnSesion sesion = sesion();
        doAnswer(invocacion -> {
            guardado.setSesionesDesde(cambio.atOffset(ZoneOffset.UTC));
            return null;
        }).when(reglas).cortarSesionesAlCambiarContrasena(guardado, sesion);
        when(emisor.emitir(ID, cambio, VENCE)).thenReturn(new EmisorDeTokens.TokenEmitido("token-nuevo", VENCE));

        SesionRespuesta respuesta = servicio.cambiar(sesion, ACTUAL, NUEVA);

        assertThat(respuesta.token()).isEqualTo("token-nuevo");
        assertThat(respuesta.vence()).isEqualTo(VENCE);
        assertThat(respuesta.usuario()).isEqualTo(UsuarioDeSesionRespuesta.de(sesion));
        assertThat(guardado.getContrasenaHash()).isEqualTo("hash-nuevo");
        InOrder orden = inOrder(codificador, reglas, emisor);
        orden.verify(codificador).matches(ACTUAL, HASH);
        orden.verify(reglas).cortarSesionesAlCambiarContrasena(guardado, sesion);
        orden.verify(emisor).emitir(ID, cambio, VENCE);
        verify(repositorio, never()).registrarFallo(any(), anyInt(), any(), any());
    }

    /** La sesión de Ana, administradora de empresa, que vence en {@link #VENCE}. */
    private static UsuarioEnSesion sesion() {
        return new UsuarioEnSesion(ID, "Ana Pérez", "ana@cacaos.test", "ADMIN_EMPRESA", EMPRESA, "Cacaos del Huila",
                VENCE);
    }

    /** Ana, activa y con su empresa activa, bloqueada hasta {@code bloqueadoHasta} o sin bloqueo. */
    private static DatosDeSesion usuario(Instant bloqueadoHasta) {
        return new DatosDeSesion(ID, "Ana Pérez", "ana@cacaos.test", HASH, UsuarioRol.ADMIN_EMPRESA, true,
                bloqueadoHasta == null ? null : enUtc(bloqueadoHasta), null, EMPRESA, "Cacaos del Huila", true);
    }

    private static OffsetDateTime enUtc(Instant instante) {
        return instante.atOffset(ZoneOffset.UTC);
    }
}
