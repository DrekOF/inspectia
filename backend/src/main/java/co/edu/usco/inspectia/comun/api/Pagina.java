/*
 * Nombre: Pagina.java
 * Descripción: Página de un listado, con el mismo formato en todos los CRUD y en el frontend.
 * Fecha de creación: 13/10/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.comun.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Una página de un listado. Todos los CRUD responden su listado con este formato, y el frontend lo
 * usa para paginar (RF-02, RF-14).
 *
 * @param contenido las filas de esta página
 * @param pagina número de la página, desde 0
 * @param tamano cuántas filas trae cada página
 * @param totalElementos cuántas filas hay en total
 * @param totalPaginas cuántas páginas hay en total
 * @param <T> tipo de cada fila
 */
@Schema(description = "Una página de un listado.")
public record Pagina<T>(
        @Schema(description = "Las filas de esta página.") List<T> contenido,
        @Schema(description = "Número de la página, desde 0.", example = "0") int pagina,
        @Schema(description = "Cuántas filas trae cada página.", example = "20") int tamano,
        @Schema(description = "Cuántas filas hay en total.", example = "35") long totalElementos,
        @Schema(description = "Cuántas páginas hay en total.", example = "2") int totalPaginas) {

    /** Filas por página si no se pide otro tamaño: 20, como el historial de lotes (RF-14). */
    public static final int TAMANO_POR_DEFECTO = 20;

    /** Tamaño máximo que se puede pedir. */
    public static final int TAMANO_MAXIMO = 100;

    /**
     * Página más alta que se puede pedir: con una mayor, la fila donde empieza la página no cabe en un
     * {@code int} y la consulta falla. Así responde 400 y no un error del servidor.
     */
    public static final int PAGINA_MAXIMA = Integer.MAX_VALUE / TAMANO_MAXIMO;

    /**
     * Arma la página a partir de una de Spring Data.
     *
     * @param pagina la página que devolvió el repositorio
     * @param conversion cómo pasar cada fila a lo que responde la API
     * @param <E> tipo de las filas en el repositorio
     * @param <T> tipo de las filas en la respuesta
     * @return la página para la API
     */
    public static <E, T> Pagina<T> de(Page<E> pagina, Function<? super E, ? extends T> conversion) {
        List<T> contenido = pagina.getContent().stream().<T>map(conversion).toList();
        return new Pagina<>(contenido, pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
