/*
 * Nombre: PropiedadesJwt.java
 * Descripción: Propiedad inspectia.jwt.clave: la clave de los tokens del inicio de sesión (D-28).
 * Fecha de creación: 14/10/2026
 * Actualización: 14/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de los tokens del inicio de sesión (D-28).
 *
 * @param clave clave HS256 en Base64, de al menos 32 bytes. Sale de la variable de entorno
 *              {@code INSPECTIA_JWT_CLAVE} (en local, de {@code infra/.env}) y nunca se guarda en el
 *              repositorio
 */
@ConfigurationProperties("inspectia.jwt")
public record PropiedadesJwt(String clave) {

    /** No muestra la clave si la configuración termina en un registro del servidor. */
    @Override
    public String toString() {
        return "PropiedadesJwt[clave=****]";
    }
}
