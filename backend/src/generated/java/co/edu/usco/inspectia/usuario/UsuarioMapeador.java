/*
 * Nombre: UsuarioMapeador.java
 * Descripción: Conversión entre usuario y sus solicitudes y respuesta.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.7)
 * Versión: 1.7
 * Entidades: usuario 1.6
 */
package co.edu.usco.inspectia.usuario;

/** Conversión entre {@link Usuario} y sus solicitudes y respuesta. Solo copia datos: la contraseña no, el servicio guarda su hash. */
public final class UsuarioMapeador {

    private UsuarioMapeador() {
    }

    /**
     * Arma un registro nuevo con los datos de la solicitud; lo demás queda con su valor por defecto.
     *
     * @param solicitud los datos validados
     * @return el registro, sin guardar
     */
    public static Usuario deSolicitud(UsuarioCrearSolicitud solicitud) {
        Usuario usuario = new Usuario();
        usuario.setNombre(solicitud.nombre());
        usuario.setCorreo(solicitud.correo());
        usuario.setRol(solicitud.rol());
        return usuario;
    }

    /**
     * Copia en el registro los datos de la solicitud.
     *
     * @param solicitud los datos validados
     * @param usuario el registro que se edita
     */
    public static void aplicar(UsuarioEditarSolicitud solicitud, Usuario usuario) {
        usuario.setNombre(solicitud.nombre());
        usuario.setCorreo(solicitud.correo());
        usuario.setRol(solicitud.rol());
    }

    /**
     * Copia un registro sin ligarlo a la base: sirve para revisar los cambios antes de aplicarlos.
     *
     * @param original el registro guardado
     * @return la copia
     */
    public static Usuario copia(Usuario original) {
        Usuario copia = new Usuario();
        copia.setId(original.getId());
        copia.setEmpresaId(original.getEmpresaId());
        copia.setNombre(original.getNombre());
        copia.setCorreo(original.getCorreo());
        copia.setContrasenaHash(original.getContrasenaHash());
        copia.setRol(original.getRol());
        copia.setActivo(original.getActivo());
        copia.setIntentosFallidos(original.getIntentosFallidos());
        copia.setBloqueadoHasta(original.getBloqueadoHasta());
        copia.setSesionesDesde(original.getSesionesDesde());
        copia.setFechaCreacion(original.getFechaCreacion());
        copia.setFechaActualizacion(original.getFechaActualizacion());
        return copia;
    }

    /**
     * La respuesta de la API para un registro.
     *
     * @param usuario el registro
     * @return la respuesta
     */
    public static UsuarioRespuesta respuesta(Usuario usuario) {
        return new UsuarioRespuesta(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol(),
                usuario.getActivo(),
                usuario.getFechaCreacion(),
                usuario.getFechaActualizacion());
    }
}
