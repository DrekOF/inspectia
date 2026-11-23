/*
 * Nombre: PropiedadesDeArchivos.java
 * Descripción: Propiedad inspectia.archivos.carpeta: la carpeta de las fotos en el PC de desarrollo y en las pruebas
 *              (D-41). Vacía, la de siempre; relativa, no deja arrancar.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.carpeta;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * La carpeta de las fotos (D-41).
 *
 * @param carpeta la carpeta, de la variable de entorno {@code INSPECTIA_FOTOS_CARPETA} (en local, de
 *                {@code infra/.env}); vacía o sin la variable, {@code <carpeta del usuario>/.inspectia/fotos}
 */
@ConfigurationProperties("inspectia.archivos")
public record PropiedadesDeArchivos(String carpeta) {

    /** Dónde van las fotos si no se dice otra carpeta: fuera del repositorio, en la carpeta del usuario. */
    static Path porDefecto() {
        return Path.of(System.getProperty("user.home"), ".inspectia", "fotos");
    }

    /**
     * La carpeta de las fotos. Una línea vacía en {@code infra/.env} cuenta como si no estuviera: así las fotos nunca
     * quedan en la carpeta desde donde arranca el backend, que está dentro del repositorio.
     *
     * @return la carpeta, como ruta completa
     * @throws IllegalStateException si la carpeta no es una ruta completa
     */
    public Path carpetaDeFotos() {
        if (carpeta == null || carpeta.isBlank()) {
            return porDefecto();
        }
        Path ruta = Path.of(carpeta.strip());
        if (!ruta.isAbsolute()) {
            throw new IllegalStateException("La carpeta de las fotos tiene que ser una ruta completa, como "
                    + "C:/Users/tu-usuario/fotos-inspectia, y «" + carpeta + "» no lo es. Corrige "
                    + "INSPECTIA_FOTOS_CARPETA en infra/.env, como dice infra/.env.example, o quita la línea para usar "
                    + porDefecto() + ".");
        }
        return ruta.normalize();
    }
}
