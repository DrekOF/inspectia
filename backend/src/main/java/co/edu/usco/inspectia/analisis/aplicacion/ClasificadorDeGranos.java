/*
 * Nombre: ClasificadorDeGranos.java
 * Descripción: Puerto del clasificador de granos (D-21): recibe las fotos de una muestra y responde sus granos
 *              clasificados y el modelo que los clasificó (RF-07, RF-08).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import java.util.List;

/**
 * Puerto del clasificador de granos (D-21). Lo implementa el clasificador falso de desarrollo
 * ({@code inspectia.ia.clasificador=falso}) y, con el PR 4, el cliente del servicio de IA. El caso de uso lo llama
 * fuera de toda transacción: puede tardar segundos (RNF-01).
 */
public interface ClasificadorDeGranos {

    /**
     * Encuentra y clasifica los granos de las fotos de una muestra.
     *
     * @param fotos las fotos de la muestra, por número, con su contenido
     * @return el modelo que los clasificó y los granos de todas las fotos
     * @throws RuntimeException si no puede clasificarlas: la muestra vuelve a pendiente, sin resultado
     */
    Clasificacion clasificar(List<FotoParaClasificar> fotos);
}
