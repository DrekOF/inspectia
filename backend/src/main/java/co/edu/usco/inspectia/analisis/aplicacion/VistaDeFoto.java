/*
 * Nombre: VistaDeFoto.java
 * Descripción: Una foto de la muestra como la ve la página: su número, su formato, su tamaño, su estado, su enlace para
 *              verla y lo que dijo su revisión (RF-05, RF-06, RF-07, RF-09, RF-14 C5, D-46).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.FormatoDeFoto;
import co.edu.usco.inspectia.analisis.dominio.MotivoDeFoto;
import java.util.List;
import java.util.Objects;

/**
 * Una foto de la muestra como la ve la página (RF-05, RF-14 C5). El enlace de vista es nuevo en cada respuesta y
 * vence en minutos (RNF-03 C3).
 *
 * @param numero su número en la muestra, de 1 a 3
 * @param formato JPG o PNG
 * @param tamanoBytes cuánto pesa
 * @param estado cargada, aprobada o por repetir
 * @param vista el enlace para verla
 * @param motivo por qué hay que repetirla, o {@code null}
 * @param granosEncontrados los granos válidos que encontró su revisión, o {@code null} mientras está cargada
 * @param regionesPegadas dónde están los granos pegados; vacía salvo con {@code GRANOS_PEGADOS} (D-45)
 * @param granosDudosos los granos dudosos de la foto que RF-09 pidió repetir, sin clase ni confianza; vacía salvo con
 *     {@code GRANOS_DUDOSOS}
 */
public record VistaDeFoto(int numero, FormatoDeFoto formato, long tamanoBytes, EstadoDeFoto estado,
        EnlaceTemporal vista, MotivoDeFoto motivo, Integer granosEncontrados, List<Rectangulo> regionesPegadas,
        List<Rectangulo> granosDudosos) {

    /**
     * Copia las listas.
     */
    public VistaDeFoto {
        regionesPegadas = List.copyOf(Objects.requireNonNullElse(regionesPegadas, List.of()));
        granosDudosos = List.copyOf(Objects.requireNonNullElse(granosDudosos, List.of()));
    }

    /**
     * Una vista sin revisión: sin motivo, sin conteo y sin marcas.
     *
     * @param numero su número en la muestra
     * @param formato JPG o PNG
     * @param tamanoBytes cuánto pesa
     * @param estado su estado
     * @param vista el enlace para verla
     */
    public VistaDeFoto(int numero, FormatoDeFoto formato, long tamanoBytes, EstadoDeFoto estado,
            EnlaceTemporal vista) {
        this(numero, formato, tamanoBytes, estado, vista, null, null, List.of(), List.of());
    }
}
