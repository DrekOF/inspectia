/*
 * Nombre: SesionRechazadaExcepcion.java
 * Descripción: Inicio de sesión rechazado: responde 401 con su mensaje y, si lleva, una ayuda (RF-01,
 *              D-28).
 * Fecha de creación: 14/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.errores;

/**
 * Inicio de sesión rechazado (RF-01). {@link ManejadorDeErrores} responde 401 con el mensaje en
 * {@code detail} y, si lleva ayuda, la agrega en {@code ayuda}; por ejemplo, la regla del bloqueo.
 */
public class SesionRechazadaExcepcion extends RuntimeException {

    private final String clave;
    private final String ayuda;
    private final transient Object[] argumentosAyuda;

    /**
     * Crea la excepción con un mensaje y sin ayuda.
     *
     * @param clave la clave del mensaje, en {@code i18n/mensajes}
     */
    public SesionRechazadaExcepcion(String clave) {
        this(clave, null);
    }

    /**
     * Crea la excepción con un mensaje y una ayuda.
     *
     * @param clave la clave del mensaje, en {@code i18n/mensajes}
     * @param ayuda la clave de la ayuda; {@code null} si no lleva
     * @param argumentosAyuda los valores que completan la ayuda ({0}, {1}...)
     */
    public SesionRechazadaExcepcion(String clave, String ayuda, Object... argumentosAyuda) {
        super(clave);
        this.clave = clave;
        this.ayuda = ayuda;
        this.argumentosAyuda = argumentosAyuda.clone();
    }

    /**
     * La clave del mensaje.
     *
     * @return la clave, en {@code i18n/mensajes}
     */
    public String getClave() {
        return clave;
    }

    /**
     * La clave de la ayuda.
     *
     * @return la clave, o {@code null} si no lleva
     */
    public String getAyuda() {
        return ayuda;
    }

    /**
     * Los valores que completan la ayuda.
     *
     * @return una copia de los valores
     */
    public Object[] getArgumentosAyuda() {
        return argumentosAyuda.clone();
    }
}
