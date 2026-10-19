/*
 * Nombre: ContrasenaActualIncorrectaExcepcion.java
 * Descripción: Al cambiar la propia contraseña, la actual no es la del usuario; la API responde 400 con
 *              la regla del bloqueo (RF-18).
 * Fecha de creación: 19/10/2026
 * Actualización: 19/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.errores;

import java.util.List;

/**
 * La contraseña actual que escribió el usuario para cambiar la suya no es la correcta (RF-18).
 * {@link ManejadorDeErrores} responde 400 con el error en el campo y, en {@code ayuda}, la regla del
 * bloqueo, como en el inicio de sesión: el intento cuenta para el bloqueo de RF-01.
 */
public class ContrasenaActualIncorrectaExcepcion extends ExcepcionConCampos {

    /** Clave del mensaje del campo, en {@code i18n/mensajes}. */
    public static final String MENSAJE = "error.contrasena_actual_incorrecta";

    private final int intentosPermitidos;
    private final long minutosDeBloqueo;

    /**
     * Crea la excepción.
     *
     * @param campo el campo de la contraseña actual en la petición
     * @param intentosPermitidos los intentos fallidos seguidos que bloquean la cuenta
     * @param minutosDeBloqueo cuántos minutos dura el bloqueo
     */
    public ContrasenaActualIncorrectaExcepcion(String campo, int intentosPermitidos, long minutosDeBloqueo) {
        super(List.of(new CampoInvalido(campo, MENSAJE)));
        this.intentosPermitidos = intentosPermitidos;
        this.minutosDeBloqueo = minutosDeBloqueo;
    }

    /**
     * Los intentos fallidos seguidos que bloquean la cuenta.
     *
     * @return los intentos, para la regla del bloqueo
     */
    public int getIntentosPermitidos() {
        return intentosPermitidos;
    }

    /**
     * Cuántos minutos dura el bloqueo.
     *
     * @return los minutos, para la regla del bloqueo
     */
    public long getMinutosDeBloqueo() {
        return minutosDeBloqueo;
    }
}
