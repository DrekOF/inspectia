/*
 * Nombre: UsuarioServicio.java
 * Descripción: Operaciones de usuario: validan, guardan y responden (RF-03).
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.8)
 * Versión: 1.8
 * Entidades: usuario 1.7
 */
package co.edu.usco.inspectia.usuario;

import co.edu.usco.inspectia.comun.api.Pagina;
import co.edu.usco.inspectia.comun.errores.CampoInvalido;
import co.edu.usco.inspectia.comun.errores.DatoRepetidoExcepcion;
import co.edu.usco.inspectia.comun.errores.NoEncontradoExcepcion;
import co.edu.usco.inspectia.comun.errores.ReglaIncumplidaExcepcion;
import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operaciones de {@code usuario} (RF-03, RF-01, RF-02, RNF-03, RNF-04). Cada una revisa, en este orden:
 * <ol>
 *   <li>Los datos, con las validaciones del JSON (las revisa Spring antes de llamar al servicio).</li>
 *   <li>Las reglas que se programan a mano: si alguna no se cumple, responde 400.</li>
 *   <li>Los datos únicos: si alguno ya existe, responde 409.</li>
 * </ol>
 * Lo que se guarda en una operación queda en una sola transacción: o se guarda todo o nada.
 *
 * <p>Cada operación recibe el usuario de la sesión y solo ve y modifica los registros de su empresa:
 * un registro de otra empresa responde 404, como si no existiera (RNF-03).
 *
 * <p>La contraseña nunca se guarda tal cual: se guarda su hash BCrypt.
 */
@Service
@Transactional(readOnly = true)
public class UsuarioServicio {

    /** Orden del listado: por nombre, sin distinguir mayúsculas; los empates, por antigüedad. */
    private static final Sort ORDEN = Sort.by(Sort.Order.asc("nombre").ignoreCase(), Sort.Order.asc("id"));

    private final UsuarioRepositorio repositorio;
    private final ReglasUsuario reglas;
    private final PasswordEncoder codificador;

    /**
     * Crea el servicio; Spring le pasa sus dependencias.
     *
     * @param repositorio el repositorio de {@code usuario}
     * @param reglas las reglas que se programan a mano
     * @param codificador el codificador BCrypt de las contraseñas
     */
    public UsuarioServicio(
            UsuarioRepositorio repositorio,
            ReglasUsuario reglas,
            PasswordEncoder codificador) {
        this.repositorio = repositorio;
        this.reglas = reglas;
        this.codificador = codificador;
    }

    /**
     * Una página de usuarios, por nombre, sin distinguir mayúsculas.
     *
     * @param pagina número de la página, desde 0
     * @param tamano filas por página
     * @param activo solo los activos ({@code true}) o los desactivados ({@code false}); con {@code null}, todos
     * @param sesion el usuario de la sesión: solo se listan los de su empresa
     * @return la página
     */
    public Pagina<UsuarioRespuesta> listar(int pagina, int tamano, Boolean activo, UsuarioEnSesion sesion) {
        PageRequest pedido = PageRequest.of(pagina, tamano, ORDEN);
        Page<Usuario> registros = activo == null
                ? repositorio.findAllByEmpresaId(sesion.empresaId(), pedido)
                : repositorio.findAllByEmpresaIdAndActivo(sesion.empresaId(), activo, pedido);
        return Pagina.de(registros, UsuarioMapeador::respuesta);
    }

    /**
     * Un registro por su identificador.
     *
     * @param id el identificador
     * @param sesion el usuario de la sesión
     * @return el registro
     * @throws NoEncontradoExcepcion si no existe o es de otra empresa (404)
     */
    public UsuarioRespuesta ver(UUID id, UsuarioEnSesion sesion) {
        return UsuarioMapeador.respuesta(buscar(id, sesion));
    }

    /**
     * Guarda el registro nuevo.
     *
     * @param solicitud los datos, ya validados con el JSON
     * @param sesion el usuario de la sesión: el registro queda en su empresa
     * @return lo que quedó guardado
     * @throws ReglaIncumplidaExcepcion si no se cumple una regla (400)
     * @throws DatoRepetidoExcepcion si un dato único ya existe (409)
     */
    @Transactional
    public UsuarioRespuesta crear(UsuarioCrearSolicitud solicitud, UsuarioEnSesion sesion) {
        Usuario usuario = UsuarioMapeador.deSolicitud(solicitud);
        usuario.setEmpresaId(sesion.empresaId());
        revisarReglasAlCrear(usuario, sesion);
        List<CampoInvalido> repetidos = new ArrayList<>();
        if (repositorio.existeCorreo(usuario.getCorreo())) {
            repetidos.add(new CampoInvalido("correo", "usuario.correo.unico"));
        }
        if (!repetidos.isEmpty()) {
            throw new DatoRepetidoExcepcion(repetidos);
        }
        usuario.setContrasenaHash(codificador.encode(solicitud.contrasena()));
        repositorio.saveAndFlush(usuario);
        return UsuarioMapeador.respuesta(usuario);
    }

    /**
     * Edita un registro con las mismas validaciones del registro. Los cambios se revisan en una copia
     * y solo se aplican si todo está bien.
     * Si llega una contraseña, la restablece; si no, la deja como estaba.
     * Después aplica el efecto {@code cortar_sesiones_al_cambiar_contrasena}, si llega una contraseña.
     *
     * @param id el registro que se edita
     * @param solicitud los datos, ya validados con el JSON
     * @param sesion el usuario de la sesión
     * @return lo que quedó guardado
     * @throws NoEncontradoExcepcion si no existe o es de otra empresa (404)
     * @throws ReglaIncumplidaExcepcion si no se cumple una regla (400)
     * @throws DatoRepetidoExcepcion si un dato único ya lo tiene otro registro (409)
     */
    @Transactional
    public UsuarioRespuesta editar(UUID id, UsuarioEditarSolicitud solicitud, UsuarioEnSesion sesion) {
        Usuario usuario = buscar(id, sesion);
        Usuario propuesta = UsuarioMapeador.copia(usuario);
        UsuarioMapeador.aplicar(solicitud, propuesta);
        revisarReglasAlEditar(propuesta, sesion, solicitud.contrasena() != null);
        List<CampoInvalido> repetidos = new ArrayList<>();
        if (repositorio.existeCorreoEnOtro(propuesta.getCorreo(), id)) {
            repetidos.add(new CampoInvalido("correo", "usuario.correo.unico"));
        }
        if (!repetidos.isEmpty()) {
            throw new DatoRepetidoExcepcion(repetidos);
        }
        UsuarioMapeador.aplicar(solicitud, usuario);
        if (solicitud.contrasena() != null) {
            usuario.setContrasenaHash(codificador.encode(solicitud.contrasena()));
            reglas.cortarSesionesAlCambiarContrasena(usuario, sesion);
        }
        repositorio.flush();
        return UsuarioMapeador.respuesta(usuario);
    }

    /**
     * Desactiva un registro: cambia {@code activo} y no borra nada.
     *
     * @param id el registro
     * @param sesion el usuario de la sesión
     * @return lo que quedó guardado
     * @throws NoEncontradoExcepcion si no existe o es de otra empresa (404)
     */
    @Transactional
    public UsuarioRespuesta desactivar(UUID id, UsuarioEnSesion sesion) {
        Usuario usuario = buscar(id, sesion);
        revisarReglasAlDesactivar(usuario, sesion);
        usuario.setActivo(false);
        repositorio.flush();
        return UsuarioMapeador.respuesta(usuario);
    }

    /**
     * Reactiva un registro: cambia {@code activo} y no borra nada.
     *
     * @param id el registro
     * @param sesion el usuario de la sesión
     * @return lo que quedó guardado
     * @throws NoEncontradoExcepcion si no existe o es de otra empresa (404)
     */
    @Transactional
    public UsuarioRespuesta reactivar(UUID id, UsuarioEnSesion sesion) {
        Usuario usuario = buscar(id, sesion);
        revisarReglasAlReactivar(usuario, sesion);
        usuario.setActivo(true);
        repositorio.flush();
        return UsuarioMapeador.respuesta(usuario);
    }

    /** El registro, solo si es de la empresa de la sesión: si es de otra, responde que no existe (RNF-03). */
    private Usuario buscar(UUID id, UsuarioEnSesion sesion) {
        return repositorio.findByIdAndEmpresaId(id, sesion.empresaId())
                .orElseThrow(() -> new NoEncontradoExcepcion("usuario " + id));
    }

    private void revisarReglasAlCrear(Usuario usuario, UsuarioEnSesion sesion) {
        List<CampoInvalido> incumplidas = new ArrayList<>();
        if (!reglas.cumpleMaximoUsuariosActivos(usuario, sesion)) {
            incumplidas.add(new CampoInvalido(null, "usuario.reglas.maximo_usuarios_activos"));
        }
        if (!incumplidas.isEmpty()) {
            throw new ReglaIncumplidaExcepcion(incumplidas);
        }
    }

    private void revisarReglasAlEditar(Usuario usuario, UsuarioEnSesion sesion, boolean cambiaContrasena) {
        List<CampoInvalido> incumplidas = new ArrayList<>();
        if (!reglas.cumpleNoCambiarSuPropioRol(usuario, sesion)) {
            incumplidas.add(new CampoInvalido("rol", "usuario.reglas.no_cambiar_su_propio_rol"));
        }
        if (cambiaContrasena && !reglas.cumpleNoCambiarSuPropiaContrasena(usuario, sesion)) {
            incumplidas.add(new CampoInvalido("contrasena", "usuario.reglas.no_cambiar_su_propia_contrasena"));
        }
        if (!incumplidas.isEmpty()) {
            throw new ReglaIncumplidaExcepcion(incumplidas);
        }
    }

    private void revisarReglasAlDesactivar(Usuario usuario, UsuarioEnSesion sesion) {
        List<CampoInvalido> incumplidas = new ArrayList<>();
        if (!reglas.cumpleNoDesactivarseASiMismo(usuario, sesion)) {
            incumplidas.add(new CampoInvalido(null, "usuario.reglas.no_desactivarse_a_si_mismo"));
        }
        if (!incumplidas.isEmpty()) {
            throw new ReglaIncumplidaExcepcion(incumplidas);
        }
    }

    private void revisarReglasAlReactivar(Usuario usuario, UsuarioEnSesion sesion) {
        List<CampoInvalido> incumplidas = new ArrayList<>();
        if (!reglas.cumpleMaximoUsuariosActivos(usuario, sesion)) {
            incumplidas.add(new CampoInvalido(null, "usuario.reglas.maximo_usuarios_activos"));
        }
        if (!incumplidas.isEmpty()) {
            throw new ReglaIncumplidaExcepcion(incumplidas);
        }
    }
}
