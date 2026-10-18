/*
 * Nombre: SesionNoValidaExcepcion.java
 * Descripción: Un token con firma válida cuyo usuario no puede usar la sesión: responde 401 (D-28).
 * Fecha de creación: 14/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.seguridad;

import org.springframework.security.core.AuthenticationException;

/**
 * Un token con firma válida cuyo usuario no puede usar la sesión; por ejemplo, porque ya no existe.
 * La cadena de seguridad responde 401 ({@link RespuestasDeSeguridad}).
 */
public class SesionNoValidaExcepcion extends AuthenticationException {

    private final String clave;

    /**
     * Crea la excepción.
     *
     * @param clave la clave del mensaje que explica el motivo, en {@code i18n/mensajes}
     */
    public SesionNoValidaExcepcion(String clave) {
        super(clave);
        this.clave = clave;
    }

    /**
     * La clave del mensaje que explica el motivo.
     *
     * @return la clave, en {@code i18n/mensajes}
     */
    public String getClave() {
        return clave;
    }
}
