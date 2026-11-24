/*
 * Nombre: PropiedadesDelAnalisis.java
 * Descripción: Propiedades inspectia.analisis.*: los parámetros del análisis (P-15, P-19, D-24) y la cola (RNF-02 C3),
 *              validados al arrancar.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.configuracion;

import co.edu.usco.inspectia.analisis.dominio.ParametrosDelAnalisis;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Los parámetros del análisis, en {@code application.yml} bajo {@code inspectia.analisis}. Los fija el equipo, no la
 * empresa (RF-09, restricción 4): cuando Julián cierre P-15 y P-19, se cambia un número y no el código. Se validan al
 * arrancar: con uno que falta o que no cabe en su columna, el servidor no arranca.
 *
 * @param umbralConfianzaBaja el umbral de confianza baja, de 0 a 100 con hasta 2 decimales; hoy 60 (P-15)
 * @param porcentajeMaxDudosos el máximo de granos dudosos, de 0 a 100 con hasta 1 decimal; hoy 10 (RF-09)
 * @param factorDelMargen el factor {@code k} del margen, mayor que 0; hoy 1 (P-19)
 * @param puntosParaOtraMuestra a cuántos puntos de un límite se sugiere otra muestra, mayor que 0; hoy 1 (D-24)
 * @param cola cómo se atiende la cola de análisis
 */
@ConfigurationProperties("inspectia.analisis")
@Validated
public record PropiedadesDelAnalisis(
        @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2)
        BigDecimal umbralConfianzaBaja,
        @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 1)
        BigDecimal porcentajeMaxDudosos,
        @NotNull @DecimalMin(value = "0", inclusive = false)
        BigDecimal factorDelMargen,
        @NotNull @DecimalMin(value = "0", inclusive = false)
        BigDecimal puntosParaOtraMuestra,
        @Valid @DefaultValue Cola cola) {

    /**
     * Cómo se atiende la cola de análisis (RNF-02 C3).
     *
     * @param automatica si un hilo del servidor la atiende solo; las pruebas de integración la apagan y la atienden a
     *        mano, una muestra a la vez
     * @param esperaMaxima cada cuánto revisa la tabla el hilo si nadie lo despierta; mayor que 0
     */
    public record Cola(@DefaultValue("true") boolean automatica, @DefaultValue("5s") Duration esperaMaxima) {

        /**
         * Revisa la espera.
         *
         * @throws IllegalArgumentException si la espera no es mayor que 0
         */
        public Cola {
            if (esperaMaxima == null || esperaMaxima.isNegative() || esperaMaxima.isZero()) {
                throw new IllegalArgumentException("inspectia.analisis.cola.espera-maxima debe ser mayor que 0, y es "
                        + esperaMaxima);
            }
        }
    }

    /**
     * Los parámetros para el dominio, que los vuelve a revisar.
     *
     * @return los parámetros del análisis
     */
    public ParametrosDelAnalisis parametros() {
        return new ParametrosDelAnalisis(umbralConfianzaBaja, porcentajeMaxDudosos, factorDelMargen,
                puntosParaOtraMuestra);
    }
}
