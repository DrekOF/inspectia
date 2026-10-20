/*
 * Nombre: Decimales.java
 * Descripción: Deja un número con la escala de su columna, como lo guarda la base, para que la API lo
 *              responda igual al guardarlo y al listarlo (RF-13).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.numero;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ajusta los números que llegan a la API a la escala de su columna. La base guarda 90.50 como 90.5 en
 * una columna de un decimal; si el registro guardara 90.50, la respuesta de la edición diría 90.50 y la
 * del listado, 90.5. La usan los mapeadores generados al copiar los atributos {@code decimal} de una
 * solicitud (RP-23).
 */
public final class Decimales {

    private Decimales() {
    }

    /**
     * El número con la escala dada, sin redondear: 90.50 queda 90.5 y 100, 100.0.
     *
     * @param numero el número, ya validado con {@code @MaximoDecimales}, o {@code null}
     * @param escala los decimales de su columna
     * @return el número con esa escala, o {@code null}
     * @throws ArithmeticException si el número tiene más decimales de los de la escala: la validación
     *                             debió rechazarlo antes
     */
    public static BigDecimal conEscala(BigDecimal numero, int escala) {
        return numero == null ? null : numero.setScale(escala, RoundingMode.UNNECESSARY);
    }
}
