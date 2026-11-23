/*
 * Nombre: EnlaceNoValidoExcepcion.java
 * Descripción: Un enlace firmado no trae firma, su firma no coincide o venció (D-41). La API responde 403.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.errores;

/**
 * Un enlace firmado no sirve (D-41): no trae firma, su firma no coincide o venció. La API responde 403, como S3. No
 * es una {@code AccessDeniedException}: esas rutas no piden sesión, y la cadena de seguridad la convertiría en un
 * 401, con el que la página cerraría la sesión.
 */
public class EnlaceNoValidoExcepcion extends RuntimeException {

    /** Crea la excepción. */
    public EnlaceNoValidoExcepcion() {
        super("El enlace no tiene una firma válida o venció");
    }
}
