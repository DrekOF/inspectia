/*
 * Nombre: EmpresaServicioTest.java
 * Descripción: Pruebas unitarias del servicio generado de empresa con Mockito: cuándo revisa las reglas
 *              y en qué orden guarda la empresa, su administrador y sus rangos (RF-02).
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.empresa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.usco.inspectia.comun.errores.ReglaIncumplidaExcepcion;
import co.edu.usco.inspectia.rango.RangoRecomendacion;
import co.edu.usco.inspectia.rango.RangoRecomendacionRepositorio;
import co.edu.usco.inspectia.usuario.Usuario;
import co.edu.usco.inspectia.usuario.UsuarioRepositorio;
import co.edu.usco.inspectia.usuario.UsuarioRol;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas del servicio generado de empresa, sin base de datos: los repositorios, las reglas y el
 * codificador son simulados. Las de integración ({@code RegistroEmpresaIT} y {@code EdicionEmpresaIT})
 * prueban lo mismo con PostgreSQL.
 */
@ExtendWith(MockitoExtension.class)
class EmpresaServicioTest {

    private static final UUID ID = UUID.fromString("0199e2a0-0000-7000-8000-000000000001");

    @Mock
    private EmpresaRepositorio repositorio;

    @Mock
    private ReglasEmpresa reglas;

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @Mock
    private PasswordEncoder codificador;

    @Mock
    private RangoRecomendacionRepositorio rangoRecomendacionRepositorio;

    @InjectMocks
    private EmpresaServicio servicio;

    @Test
    @DisplayName("RF-02 · al crear, si la regla del NIT no se cumple, el servicio no guarda nada")
    void alCrearRevisaLaRegla() {
        when(reglas.cumpleNitDigitoVerificacion(any())).thenReturn(false);

        assertThatThrownBy(() -> servicio.crear(solicitud("900123456-7")))
                .isInstanceOf(ReglaIncumplidaExcepcion.class);

        verify(repositorio, never()).saveAndFlush(any());
        verify(usuarioRepositorio, never()).saveAndFlush(any());
        verify(rangoRecomendacionRepositorio, never()).saveAllAndFlush(any());
    }

    @Test
    @DisplayName("RF-02 · al crear, guarda la empresa, su administrador con la contraseña codificada y los rangos, en ese orden")
    void alCrearGuardaTodoEnOrden() {
        when(reglas.cumpleNitDigitoVerificacion(any())).thenReturn(true);
        when(codificador.encode("Clave-Segura-2026")).thenReturn("hash-de-prueba");
        when(repositorio.saveAndFlush(any())).thenAnswer(invocacion -> {
            Empresa guardada = invocacion.getArgument(0);
            guardada.setId(ID);
            return guardada;
        });

        EmpresaRespuesta respuesta = servicio.crear(solicitud("900123456-8"));

        assertThat(respuesta.id()).isEqualTo(ID);
        InOrder orden = inOrder(repositorio, usuarioRepositorio, rangoRecomendacionRepositorio);
        orden.verify(repositorio).saveAndFlush(any());
        ArgumentCaptor<Usuario> administrador = ArgumentCaptor.forClass(Usuario.class);
        orden.verify(usuarioRepositorio).saveAndFlush(administrador.capture());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RangoRecomendacion>> rangos = ArgumentCaptor.forClass(List.class);
        orden.verify(rangoRecomendacionRepositorio).saveAllAndFlush(rangos.capture());
        assertThat(administrador.getValue().getEmpresaId()).isEqualTo(ID);
        assertThat(administrador.getValue().getRol()).isEqualTo(UsuarioRol.ADMIN_EMPRESA);
        assertThat(administrador.getValue().getContrasenaHash()).isEqualTo("hash-de-prueba");
        assertThat(rangos.getValue()).hasSize(4).allSatisfy(rango -> assertThat(rango.getEmpresaId()).isEqualTo(ID));
    }

    @Test
    @DisplayName("RF-02 · crear guarda la empresa, su administrador y sus rangos en una sola transacción")
    void crearEsUnaSolaTransaccion() throws NoSuchMethodException {
        Transactional transaccion = EmpresaServicio.class.getMethod("crear", EmpresaCrearSolicitud.class)
                .getAnnotation(Transactional.class);

        assertThat(transaccion).isNotNull();
        assertThat(transaccion.readOnly()).isFalse();
    }

    @Test
    @DisplayName("RF-02 · al editar, revisa la regla con los datos nuevos y, si no se cumple, no cambia la empresa")
    void alEditarRevisaLaReglaEnUnaCopia() {
        Empresa guardada = new Empresa();
        guardada.setId(ID);
        guardada.setNombre("Cacaos del Huila");
        guardada.setNit("900123456-8");
        guardada.setCiudad("Neiva");
        when(repositorio.findById(ID)).thenReturn(Optional.of(guardada));
        when(reglas.cumpleNitDigitoVerificacion(any())).thenReturn(false);

        assertThatThrownBy(() -> servicio.editar(ID,
                new EmpresaEditarSolicitud("Cacaos del Sur", "890903938-9", "Pitalito")))
                .isInstanceOf(ReglaIncumplidaExcepcion.class);

        ArgumentCaptor<Empresa> revisada = ArgumentCaptor.forClass(Empresa.class);
        verify(reglas).cumpleNitDigitoVerificacion(revisada.capture());
        assertThat(revisada.getValue()).isNotSameAs(guardada);
        assertThat(revisada.getValue().getNit()).isEqualTo("890903938-9");
        assertThat(guardada.getNombre()).isEqualTo("Cacaos del Huila");
        assertThat(guardada.getNit()).isEqualTo("900123456-8");
        verify(repositorio, never()).flush();
    }

    private static EmpresaCrearSolicitud solicitud(String nit) {
        return new EmpresaCrearSolicitud("Cacaos del Huila", nit, "Neiva",
                new AdministradorSolicitud("Ana Pérez", "ana@cacaos.test", "Clave-Segura-2026"));
    }
}
