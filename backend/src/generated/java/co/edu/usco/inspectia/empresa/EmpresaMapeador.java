/*
 * Nombre: EmpresaMapeador.java
 * Descripción: Conversión entre empresa y sus solicitudes y respuesta.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.8)
 * Versión: 1.8
 * Entidades: empresa 1.3, usuario 1.7, rango_recomendacion 1.2
 */
package co.edu.usco.inspectia.empresa;

import co.edu.usco.inspectia.usuario.Usuario;
import co.edu.usco.inspectia.usuario.UsuarioRol;
import java.util.UUID;

/** Conversión entre {@link Empresa} y sus solicitudes y respuesta. Solo copia datos. */
public final class EmpresaMapeador {

    private EmpresaMapeador() {
    }

    /**
     * Arma un registro nuevo con los datos de la solicitud; lo demás queda con su valor por defecto.
     *
     * @param solicitud los datos validados
     * @return el registro, sin guardar
     */
    public static Empresa deSolicitud(EmpresaCrearSolicitud solicitud) {
        Empresa empresa = new Empresa();
        empresa.setNombre(solicitud.nombre());
        empresa.setNit(solicitud.nit());
        empresa.setCiudad(solicitud.ciudad());
        return empresa;
    }

    /**
     * Copia en el registro los datos de la solicitud.
     *
     * @param solicitud los datos validados
     * @param empresa el registro que se edita
     */
    public static void aplicar(EmpresaEditarSolicitud solicitud, Empresa empresa) {
        empresa.setNombre(solicitud.nombre());
        empresa.setNit(solicitud.nit());
        empresa.setCiudad(solicitud.ciudad());
    }

    /**
     * Copia un registro sin ligarlo a la base: sirve para revisar los cambios antes de aplicarlos.
     *
     * @param original el registro guardado
     * @return la copia
     */
    public static Empresa copia(Empresa original) {
        Empresa copia = new Empresa();
        copia.setId(original.getId());
        copia.setNombre(original.getNombre());
        copia.setNit(original.getNit());
        copia.setCiudad(original.getCiudad());
        copia.setActiva(original.getActiva());
        copia.setFechaCreacion(original.getFechaCreacion());
        copia.setFechaActualizacion(original.getFechaActualizacion());
        return copia;
    }

    /**
     * La respuesta de la API para un registro.
     *
     * @param empresa el registro
     * @return la respuesta
     */
    public static EmpresaRespuesta respuesta(Empresa empresa) {
        return new EmpresaRespuesta(
                empresa.getId(),
                empresa.getNombre(),
                empresa.getNit(),
                empresa.getCiudad(),
                empresa.getActiva(),
                empresa.getFechaCreacion(),
                empresa.getFechaActualizacion());
    }

    /**
     * Arma el registro de {@code usuario} que se crea junto con el de {@code empresa}
     * (RF-02).
     *
     * @param solicitud los datos validados
     * @param empresaId la empresa recién creada
     * @param contrasenaHash la contraseña ya codificada con BCrypt
     * @return el registro, sin guardar
     */
    public static Usuario administradorDeSolicitud(AdministradorSolicitud solicitud, UUID empresaId, String contrasenaHash) {
        Usuario nuevo = new Usuario();
        nuevo.setEmpresaId(empresaId);
        nuevo.setNombre(solicitud.nombre());
        nuevo.setCorreo(solicitud.correo());
        nuevo.setContrasenaHash(contrasenaHash);
        nuevo.setRol(UsuarioRol.ADMIN_EMPRESA);
        return nuevo;
    }
}
