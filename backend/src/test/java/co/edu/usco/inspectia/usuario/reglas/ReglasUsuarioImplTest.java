/*
 * Nombre: ReglasUsuarioImplTest.java
 * Descripción: Pruebas unitarias de las reglas de usuario que se programan a mano: máximo de 25 activos,
 *              no desactivarse ni cambiarse el rol o la contraseña a sí mismo (RF-03).
 * Fecha de creación: 18/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario.reglas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import co.edu.usco.inspectia.usuario.Usuario;
import co.edu.usco.inspectia.usuario.UsuarioRol;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas de {@link ReglasUsuarioImpl} sin base de datos: el conteo de activos es simulado. La sesión es
 * la de Ana, administradora de la empresa; Luis es otro usuario de la misma empresa.
 */
@ExtendWith(MockitoExtension.class)
class ReglasUsuarioImplTest {

    private static final UUID EMPRESA = UUID.fromString("0199e2a0-0000-7000-8000-000000000001");
    private static final UUID ANA = UUID.fromString("0199e2a0-0000-7000-8000-000000000002");
    private static final UUID LUIS = UUID.fromString("0199e2a0-0000-7000-8000-000000000003");

    @Mock
    private UsuariosActivosRepositorio activos;

    @InjectMocks
    private ReglasUsuarioImpl reglas;

    @ParameterizedTest(name = "{0} activos: ¿se puede? {1}")
    @DisplayName("RF-03 · C5 · con 24 activos en la empresa se puede crear o reactivar otro; con 25, no (prueba de límite)")
    @CsvSource({"24, true", "25, false"})
    void maximoDeActivos(long cantidad, boolean cumple) {
        when(activos.countByEmpresaIdAndActivoTrue(EMPRESA)).thenReturn(cantidad);
        Usuario nuevo = new Usuario();
        nuevo.setEmpresaId(EMPRESA);

        assertThat(reglas.cumpleMaximoUsuariosActivos(nuevo, sesion())).isEqualTo(cumple);
    }

    @Test
    @DisplayName("RF-03 · reactivar a un usuario que ya está activo no suma otro: cumple sin contar")
    void reactivarAUnActivoNoSuma() {
        assertThat(reglas.cumpleMaximoUsuariosActivos(usuario(LUIS, UsuarioRol.USUARIO_FINAL), sesion())).isTrue();

        verifyNoInteractions(activos);
    }

    @Test
    @DisplayName("RF-03 · C7 · el administrador no puede desactivarse a sí mismo; a otro usuario sí")
    void noDesactivarseASiMismo() {
        assertThat(reglas.cumpleNoDesactivarseASiMismo(usuario(ANA, UsuarioRol.ADMIN_EMPRESA), sesion())).isFalse();
        assertThat(reglas.cumpleNoDesactivarseASiMismo(usuario(LUIS, UsuarioRol.USUARIO_FINAL), sesion())).isTrue();
    }

    @Test
    @DisplayName("RF-03 · C8 · el administrador no puede cambiar su propio rol; sí dejarlo igual o cambiar el de otro")
    void noCambiarSuPropioRol() {
        assertThat(reglas.cumpleNoCambiarSuPropioRol(usuario(ANA, UsuarioRol.USUARIO_FINAL), sesion())).isFalse();
        assertThat(reglas.cumpleNoCambiarSuPropioRol(usuario(ANA, UsuarioRol.ADMIN_EMPRESA), sesion())).isTrue();
        assertThat(reglas.cumpleNoCambiarSuPropioRol(usuario(LUIS, UsuarioRol.ADMIN_EMPRESA), sesion())).isTrue();
    }

    @Test
    @DisplayName("RF-03 · C11 · el administrador no puede cambiar su propia contraseña desde la gestión de usuarios; la de otro sí")
    void noCambiarSuPropiaContrasena() {
        assertThat(reglas.cumpleNoCambiarSuPropiaContrasena(usuario(ANA, UsuarioRol.ADMIN_EMPRESA), sesion())).isFalse();
        assertThat(reglas.cumpleNoCambiarSuPropiaContrasena(usuario(LUIS, UsuarioRol.USUARIO_FINAL), sesion())).isTrue();
    }

    /** La sesión de Ana, administradora de la empresa. */
    private static UsuarioEnSesion sesion() {
        return new UsuarioEnSesion(ANA, "Ana Pérez", "ana@cacaos.test", "ADMIN_EMPRESA", EMPRESA, "Cacaos del Huila",
                Instant.now());
    }

    /** Un usuario guardado y activo de la empresa, con los datos que se van a guardar. */
    private static Usuario usuario(UUID id, UsuarioRol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmpresaId(EMPRESA);
        usuario.setRol(rol);
        return usuario;
    }
}
