/*
 * Nombre: ApiUsuarios.java
 * Descripción: Llamadas a la API de usuarios de la empresa y sus mensajes, para las pruebas (RF-03,
 *              RNF-03).
 * Fecha de creación: 18/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.usuario;

import co.edu.usco.inspectia.usuario.sesion.ApiSesion;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Llamadas a la API de usuarios de la empresa ({@code /api/v1/usuarios}), con el token de quien las
 * hace, y los mensajes que responde.
 *
 * <p>Contrato que fijan estas pruebas (RF-03):
 * <ul>
 *   <li>Crear y editar reciben {@code nombre}, {@code correo}, {@code rol} y {@code contrasena}; al
 *       editar, la contraseña es opcional y, si llega, se restablece.</li>
 *   <li>Responden el usuario con {@code id}, {@code nombre}, {@code correo}, {@code rol},
 *       {@code activo} y las fechas: nunca la contraseña ni su hash, los intentos fallidos o el
 *       bloqueo.</li>
 *   <li>La empresa sale de la sesión: un usuario de otra empresa responde 404, como si no existiera.</li>
 * </ul>
 */
final class ApiUsuarios {

    static final String RUTA = "/api/v1/usuarios";

    static final String NO_EXISTE = "No existe un registro con ese identificador.";
    static final String CORREO_REPETIDO = "Ya existe un usuario con este correo.";
    static final String ROL_NO_PERMITIDO = "Solo puedes asignar los roles Usuario final o Administrador de empresa.";
    static final String VALOR_INVALIDO = "El valor no es válido.";
    static final String CONTRASENA_CORTA = "La contraseña debe tener al menos 8 caracteres.";
    static final String CONTRASENA_LARGA = "La contraseña puede tener como máximo 64 caracteres.";
    static final String CONTRASENA_BYTES =
            "La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes";
    static final String REGLA_MAXIMO =
            "La empresa ya tiene 25 usuarios activos: desactiva uno antes de crear o reactivar otro.";
    static final String REGLA_DESACTIVARSE = "No puedes desactivar tu propio usuario.";
    static final String REGLA_ROL = "No puedes cambiar tu propio rol.";
    static final String REGLA_CONTRASENA = "Para cambiar tu propia contraseña, usa «Cambiar contraseña».";

    private final ApiSesion api;

    ApiUsuarios(ApiSesion api) {
        this.api = api;
    }

    MvcTestResult crear(String token, Map<String, Object> datos) {
        return api.enviar(HttpMethod.POST, RUTA, datos, token);
    }

    MvcTestResult ver(String token, UUID id) {
        return api.enviar(HttpMethod.GET, RUTA + "/" + id, null, token);
    }

    MvcTestResult editar(String token, UUID id, Map<String, Object> datos) {
        return api.enviar(HttpMethod.PUT, RUTA + "/" + id, datos, token);
    }

    MvcTestResult desactivar(String token, UUID id) {
        return api.enviar(HttpMethod.POST, RUTA + "/" + id + "/desactivar", null, token);
    }

    MvcTestResult reactivar(String token, UUID id) {
        return api.enviar(HttpMethod.POST, RUTA + "/" + id + "/reactivar", null, token);
    }

    /** La primera página del listado, con espacio para todos los usuarios de una empresa. */
    MvcTestResult listar(String token) {
        return api.enviar(HttpMethod.GET, RUTA + "?tamano=100", null, token);
    }

    /**
     * Los datos de un usuario para crear o editar.
     *
     * @param contrasena la contraseña; {@code null} para no enviarla
     */
    static Map<String, Object> datos(String nombre, String correo, String rol, String contrasena) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", nombre);
        datos.put("correo", correo);
        datos.put("rol", rol);
        if (contrasena != null) {
            datos.put("contrasena", contrasena);
        }
        return datos;
    }
}
