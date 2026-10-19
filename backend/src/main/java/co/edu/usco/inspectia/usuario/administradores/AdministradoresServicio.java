/*
 * Nombre: AdministradoresServicio.java
 * Descripción: El administrador de la plataforma lista los administradores de una empresa y les
 *              restablece la contraseña, cortando sus sesiones abiertas (RF-02 C12, RNF-04, D-30).
 * Fecha de creación: 18/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario.administradores;

import co.edu.usco.inspectia.comun.errores.NoEncontradoExcepcion;
import co.edu.usco.inspectia.comun.seguridad.UsuarioEnSesion;
import co.edu.usco.inspectia.empresa.EmpresaRepositorio;
import co.edu.usco.inspectia.usuario.ReglasUsuario;
import co.edu.usco.inspectia.usuario.Usuario;
import co.edu.usco.inspectia.usuario.UsuarioRol;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lo que hace el administrador de la plataforma con los administradores de una empresa (RF-02 C12).
 * Si el usuario no es administrador de esa empresa, responde que no existe (404), como si la ruta no
 * llevara a ningún registro. Sirve también con el administrador o la empresa desactivados, sin
 * reactivar nada.
 */
@Service
@Transactional(readOnly = true)
public class AdministradoresServicio {

    /** Orden del listado: por nombre, sin distinguir mayúsculas; los empates, por antigüedad. */
    private static final Sort ORDEN = Sort.by(Sort.Order.asc("nombre").ignoreCase(), Sort.Order.asc("id"));

    private final AdministradoresRepositorio repositorio;
    private final EmpresaRepositorio empresas;
    private final ReglasUsuario reglas;
    private final PasswordEncoder codificador;

    /**
     * Crea el servicio; Spring le pasa sus dependencias.
     *
     * @param repositorio los administradores de cada empresa
     * @param empresas las empresas, para saber si existe la de la ruta
     * @param reglas las reglas de usuario, con el efecto que corta las sesiones
     * @param codificador el codificador BCrypt de las contraseñas
     */
    public AdministradoresServicio(AdministradoresRepositorio repositorio, EmpresaRepositorio empresas,
            ReglasUsuario reglas, PasswordEncoder codificador) {
        this.repositorio = repositorio;
        this.empresas = empresas;
        this.reglas = reglas;
        this.codificador = codificador;
    }

    /**
     * Los administradores de una empresa, activos y desactivados, por nombre.
     *
     * @param empresaId la empresa
     * @return sus administradores
     * @throws NoEncontradoExcepcion si la empresa no existe (404)
     */
    public List<AdministradorRespuesta> listar(UUID empresaId) {
        if (!empresas.existsById(empresaId)) {
            throw new NoEncontradoExcepcion("empresa " + empresaId);
        }
        return repositorio.findAllByEmpresaIdAndRol(empresaId, UsuarioRol.ADMIN_EMPRESA, ORDEN).stream()
                .map(AdministradorRespuesta::de)
                .toList();
    }

    /**
     * Restablece la contraseña de un administrador de empresa: guarda su hash BCrypt y aplica el efecto
     * {@code cortar_sesiones_al_cambiar_contrasena}, que corta sus sesiones abiertas y desbloquea su
     * cuenta (RNF-04 C6, D-30).
     *
     * @param empresaId la empresa de la ruta
     * @param usuarioId el administrador
     * @param contrasena la contraseña nueva, ya validada
     * @param sesion el administrador de la plataforma
     * @throws NoEncontradoExcepcion si el usuario no existe, no es de esa empresa o no es su
     *                               administrador (404)
     */
    @Transactional
    public void restablecerContrasena(UUID empresaId, UUID usuarioId, String contrasena, UsuarioEnSesion sesion) {
        Usuario administrador = repositorio.findByIdAndEmpresaIdAndRol(usuarioId, empresaId, UsuarioRol.ADMIN_EMPRESA)
                .orElseThrow(() -> new NoEncontradoExcepcion("administrador " + usuarioId + " de la empresa " + empresaId));
        administrador.setContrasenaHash(codificador.encode(contrasena));
        reglas.cortarSesionesAlCambiarContrasena(administrador, sesion);
    }
}
