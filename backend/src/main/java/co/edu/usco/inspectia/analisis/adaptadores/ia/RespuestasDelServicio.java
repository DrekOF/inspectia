/*
 * Nombre: RespuestasDelServicio.java
 * Descripción: El JSON que responde el servicio de IA en /revisar, /analizar y /modelos, como records de Java (RF-06,
 *              RF-07, RF-08, RF-17, D-46).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.ia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

/**
 * Las respuestas del servicio de IA (contrato común del despliegue, sección 1). Los nombres van en camelCase, como los
 * de la API del backend, y las posiciones, en píxeles de la foto ya girada según su EXIF (D-45). Los campos que el
 * backend no usa se ignoran.
 */
final class RespuestasDelServicio {

    private RespuestasDelServicio() {
    }

    /**
     * Un rectángulo de la foto.
     *
     * @param x posición horizontal
     * @param y posición vertical
     * @param ancho ancho
     * @param alto alto
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Region(int x, int y, int ancho, int alto) {
    }

    /**
     * La revisión de una foto ({@code POST /revisar}, y la de cada foto en {@code /analizar}).
     *
     * @param estado {@code APROBADA} o {@code POR_REPETIR}
     * @param motivo {@code BORROSA}, {@code POCA_LUZ}, {@code GRANOS_PEGADOS} o vacío
     * @param granosEncontrados los granos válidos
     * @param regionesPegadas dónde están los granos pegados
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Revision(String estado, String motivo, Integer granosEncontrados, List<Region> regionesPegadas) {
    }

    /**
     * Un grano clasificado.
     *
     * @param x posición horizontal
     * @param y posición vertical
     * @param ancho ancho
     * @param alto alto
     * @param clase {@code SANO} o {@code NO_SANO}
     * @param confianza de 0 a 100, con 2 decimales
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Grano(int x, int y, int ancho, int alto, String clase, BigDecimal confianza) {
    }

    /**
     * Una foto analizada.
     *
     * @param numero su número en la muestra
     * @param revision su revisión
     * @param granos sus granos; vacía si está por repetir
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record FotoAnalizada(int numero, Revision revision, List<Grano> granos) {
    }

    /**
     * El modelo que clasificó.
     *
     * @param nombre su nombre
     * @param version su versión
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Modelo(String nombre, String version) {
    }

    /**
     * El análisis de una muestra ({@code POST /analizar}).
     *
     * @param modelo el modelo que clasificó
     * @param fotos las fotos, por número
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Analisis(Modelo modelo, List<FotoAnalizada> fotos) {
    }

    /**
     * Un modelo de la comparación con sus métricas ({@code GET /modelos}).
     *
     * @param nombre nombre
     * @param version versión
     * @param tipo CNN o TRANSFORMER
     * @param f1Macro F1 macro
     * @param precisionMacro precisión macro
     * @param recallMacro recall macro
     * @param recallSano recall de sano
     * @param recallNoSano recall de no sano
     * @param precisionSano precisión de sano
     * @param precisionNoSano precisión de no sano
     * @param sanosComoSanos sanos como sanos
     * @param sanosComoNoSanos sanos como no sanos
     * @param noSanosComoSanos no sanos como sanos
     * @param noSanosComoNoSanos no sanos como no sanos
     * @param tiempoPorGranoMs tiempo por grano
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ModeloComparado(String nombre, String version, String tipo, BigDecimal f1Macro, BigDecimal precisionMacro,
            BigDecimal recallMacro, BigDecimal recallSano, BigDecimal recallNoSano, BigDecimal precisionSano,
            BigDecimal precisionNoSano, Integer sanosComoSanos, Integer sanosComoNoSanos, Integer noSanosComoSanos,
            Integer noSanosComoNoSanos, BigDecimal tiempoPorGranoMs) {
    }

    /**
     * Los modelos de la comparación y el que carga el servicio ({@code GET /modelos}).
     *
     * @param enUso el nombre del modelo que carga el servicio
     * @param modelos los 5 modelos
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Modelos(String enUso, List<ModeloComparado> modelos) {
    }
}
