/*
 * Nombre: SesionServicioTest.java
 * Descripción: Pruebas unitarias del inicio de sesión con Mockito: una sola comparación BCrypt en cada
 *              intento, bloqueo, contraseñas de más de 72 bytes y entrega del token (RF-01, D-28), y
 *              el corte de las sesiones abiertas antes de cambiar la contraseña (RNF-04, D-30).
 * Fecha de creación: 14/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.usuario.sesion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.usco.inspectia.comun.errores.SesionRechazadaExcepcion;
import co.edu.usco.inspectia.comun.seguridad.EmisorDeTokens;
import co.edu.usco.inspectia.comun.seguridad.SesionNoValidaExcepcion;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Pruebas del servicio del inicio de sesión, sin base de datos: el repositorio, BCrypt y el emisor de
 * tokens son simulados y el reloj es fijo. Revisan sobre todo lo que las pruebas de integración no
 * pueden medir bien: que cada intento haga una sola comparación BCrypt, para que todos tarden lo mismo
 * (D-28). {@code InicioSesionIT} prueba lo mismo con PostgreSQL.
 */
@ExtendWith(MockitoExtension.class)
class SesionServicioTest {

    private static final Instant AHORA = Instant.parse("2026-10-14T15:00:00Z");
    private static final UUID ID = UUID.fromString("0199e2a0-0000-7000-8000-000000000001");
    private static final UUID EMPRESA = UUID.fromString("0199e2a0-0000-7000-8000-000000000002");
    private static final String CORREO = "ana@cacaos.test";
    private static final String CONTRASENA = "Clave-Segura-2026";
    private static final String HASH = "hash-guardado";
    private static final String FICTICIO = "hash-ficticio";

    @Mock
    private SesionRepositorio repositorio;

    @Mock
    private PasswordEncoder codificador;

    @Mock
    private EmisorDeTokens emisor;

    private SesionServicio servicio;

    @BeforeEach
    void preparar() {
        when(codificador.encode(anyString())).thenReturn(FICTICIO);
        servicio = new SesionServicio(repositorio, codificador, emisor, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("RF-01 · C1 · con la contraseña correcta, los intentos vuelven a cero y entrega el token con el usuario, su rol y su empresa")
    void conLaContrasenaCorrectaEntra() {
        when(repositorio.buscarPorCorreo(CORREO)).thenReturn(Optional.of(usuario(null)));
        when(codificador.matches(CONTRASENA, HASH)).thenReturn(true);
        Instant vence = AHORA.plus(Duration.ofHours(8));
        when(emisor.emitir(ID)).thenReturn(new EmisorDeTokens.TokenEmitido("token-firmado", vence));

        SesionRespuesta sesion = servicio.iniciar(CORREO, CONTRASENA);

        assertThat(sesion.token()).isEqualTo("token-firmado");
        assertThat(sesion.tipo()).isEqualTo("Bearer");
        assertThat(sesion.vence()).isEqualTo(vence);
        assertThat(sesion.usuario()).isEqualTo(new UsuarioDeSesionRespuesta(ID, "Ana Pérez", CORREO,
                UsuarioRol.ADMIN_EMPRESA, new UsuarioDeSesionRespuesta.EmpresaDeSesion(EMPRESA, "Cacaos del Huila")));
        verify(repositorio).reiniciarIntentos(ID);
    }

    @Test
    @DisplayName("RF-01 · C2 · si el correo no existe, la contraseña igual se compara con BCrypt, para que la respuesta tarde lo mismo")
    void sinUsuarioIgualComparaConBcrypt() {
        when(repositorio.buscarPorCorreo("nadie@cacaos.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.iniciar("nadie@cacaos.test", CONTRASENA))
                .isInstanceOfSatisfying(SesionRechazadaExcepcion.class, SesionServicioTest::credencialesIncorrectas);

        verify(codificador).matches(CONTRASENA, FICTICIO);
        verify(repositorio, never()).registrarFallo(any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("RF-01 · C5 · un fallo se registra con la hora del servidor y, si es el quinto seguido, bloquea la cuenta 15 minutos desde ese momento")
    void unFalloSeRegistra() {
        when(repositorio.buscarPorCorreo(CORREO)).thenReturn(Optional.of(usuario(null)));

        assertThatThrownBy(() -> servicio.iniciar(CORREO, "Otra-Clave-2026"))
                .isInstanceOfSatisfying(SesionRechazadaExcepcion.class, SesionServicioTest::credencialesIncorrectas);

        verify(codificador).matches("Otra-Clave-2026", HASH);
        verify(repositorio).registrarFallo(ID, 5, enUtc(AHORA), enUtc(AHORA.plus(Duration.ofMinutes(15))));
        verify(emisor, never()).emitir(any());
    }

    @Test
    @DisplayName("RF-01 · C5 · durante el bloqueo, ni la contraseña correcta entra; el intento no cuenta y también se compara con BCrypt")
    void duranteElBloqueoNoEntra() {
        when(repositorio.buscarPorCorreo(CORREO)).thenReturn(Optional.of(usuario(AHORA.plusSeconds(60))));
        when(codificador.matches(CONTRASENA, HASH)).thenReturn(true);

        assertThatThrownBy(() -> servicio.iniciar(CORREO, CONTRASENA))
                .isInstanceOfSatisfying(SesionRechazadaExcepcion.class, SesionServicioTest::credencialesIncorrectas);

        verify(codificador).matches(CONTRASENA, HASH);
        verify(repositorio, never()).registrarFallo(any(), anyInt(), any(), any());
        verify(repositorio, never()).reiniciarIntentos(any());
        verify(emisor, never()).emitir(any());
    }

    @ParameterizedTest(name = "bloqueada hasta {0} segundos después de ahora: ¿entra? {1}")
    @CsvSource({"1, false", "0, true"})
    @DisplayName("RF-01 · C5 · el bloqueo dura hasta su hora exacta: un segundo antes no entra y en ese segundo ya entra (prueba de límite)")
    void elBloqueoTerminaASuHora(long segundos, boolean entra) {
        when(repositorio.buscarPorCorreo(CORREO)).thenReturn(Optional.of(usuario(AHORA.plusSeconds(segundos))));
        when(codificador.matches(CONTRASENA, HASH)).thenReturn(true);
        if (entra) {
            when(emisor.emitir(ID)).thenReturn(new EmisorDeTokens.TokenEmitido("token-firmado", AHORA));
            assertThat(servicio.iniciar(CORREO, CONTRASENA).token()).isEqualTo("token-firmado");
        } else {
            assertThatThrownBy(() -> servicio.iniciar(CORREO, CONTRASENA))
                    .isInstanceOf(SesionRechazadaExcepcion.class);
        }
    }

    @Test
    @DisplayName("RF-01 · C8 · una contraseña de más de 72 bytes no entra aunque BCrypt diga que coincide, cuenta como fallo y también se compara con BCrypt")
    void unaContrasenaDeMasDe72BytesNoEntra() {
        String larga = "ñ".repeat(8) + "a".repeat(56) + "b";
        when(repositorio.buscarPorCorreo(CORREO)).thenReturn(Optional.of(usuario(null)));
        when(codificador.matches(larga, HASH)).thenReturn(true);

        assertThatThrownBy(() -> servicio.iniciar(CORREO, larga))
                .isInstanceOfSatisfying(SesionRechazadaExcepcion.class, SesionServicioTest::credencialesIncorrectas);

        verify(codificador).matches(larga, HASH);
        verify(repositorio).registrarFallo(ID, 5, enUtc(AHORA), enUtc(AHORA.plus(Duration.ofMinutes(15))));
        verify(emisor, never()).emitir(any());
    }

    @ParameterizedTest(name = "emitido {0} segundos antes del cambio: ¿sirve? {1}")
    @CsvSource({"1, false", "0, true"})
    @DisplayName("RNF-04 · C6 · un token emitido antes del segundo en que cambió la contraseña ya no sirve; uno de ese mismo segundo, sí (prueba de límite)")
    void laSesionSirveDesdeElCambio(long segundosAntes, boolean sirve) {
        Instant cambio = AHORA.minusSeconds(60);
        when(repositorio.buscarPorId(ID)).thenReturn(Optional.of(usuario(null, cambio)));
        Instant emitido = cambio.minusSeconds(segundosAntes);
        Instant vence = emitido.plus(Duration.ofHours(8));

        if (sirve) {
            assertThat(servicio.cargar(ID, emitido, vence).id()).isEqualTo(ID);
        } else {
            assertThatThrownBy(() -> servicio.cargar(ID, emitido, vence))
                    .isInstanceOfSatisfying(SesionNoValidaExcepcion.class,
                            excepcion -> assertThat(excepcion.getClave()).isEqualTo("error.contrasena_cambiada"));
        }
    }

    @Test
    @DisplayName("RNF-04 · si la contraseña nunca cambió, cualquier token vigente del usuario sirve")
    void sinCambioDeContrasenaSirve() {
        when(repositorio.buscarPorId(ID)).thenReturn(Optional.of(usuario(null)));

        assertThat(servicio.cargar(ID, AHORA.minus(Duration.ofHours(7)), AHORA.plus(Duration.ofHours(1))).id())
                .isEqualTo(ID);
    }

    /** Un administrador de empresa activo, con la empresa activa y sin cambios de contraseña. */
    private static DatosDeSesion usuario(Instant bloqueadoHasta) {
        return usuario(bloqueadoHasta, null);
    }

    /** Un administrador de empresa activo, con la empresa activa, que cambió su contraseña en {@code sesionesDesde}. */
    private static DatosDeSesion usuario(Instant bloqueadoHasta, Instant sesionesDesde) {
        return new DatosDeSesion(ID, "Ana Pérez", CORREO, HASH, UsuarioRol.ADMIN_EMPRESA, true,
                bloqueadoHasta == null ? null : enUtc(bloqueadoHasta),
                sesionesDesde == null ? null : enUtc(sesionesDesde), EMPRESA, "Cacaos del Huila", true);
    }

    private static OffsetDateTime enUtc(Instant instante) {
        return instante.atOffset(ZoneOffset.UTC);
    }

    /** La respuesta de todo intento fallido: el mismo mensaje y la regla del bloqueo. */
    private static void credencialesIncorrectas(SesionRechazadaExcepcion excepcion) {
        assertThat(excepcion.getClave()).isEqualTo("error.credenciales_incorrectas");
        assertThat(excepcion.getAyuda()).isEqualTo("error.ayuda_bloqueo");
    }
}
