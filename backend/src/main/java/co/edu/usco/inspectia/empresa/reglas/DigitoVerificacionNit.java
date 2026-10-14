/*
 * Nombre: DigitoVerificacionNit.java
 * Descripción: Dígito de verificación del NIT con el algoritmo de la DIAN (RF-02, D-27).
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.empresa.reglas;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dígito de verificación del NIT, con el algoritmo de la DIAN descrito en
 * {@code specs/modelo-datos.md}. Lo usa la regla {@code nit_digito_verificacion} de
 * {@code specs/entidades/empresa.json} al crear y al editar una empresa (RF-02, D-27).
 */
public final class DigitoVerificacionNit {

    /** Pesos de cada dígito, empezando por la derecha. */
    private static final int[] PESOS = {3, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47, 53, 59, 67, 71};

    /** El NIT con su dígito, como lo pide {@code empresa.json}: {@code 900123456-8}. */
    private static final Pattern NIT_CON_DIGITO = Pattern.compile("^([0-9]{6,10})-([0-9])$");

    private static final Pattern SOLO_NUMEROS = Pattern.compile("^[0-9]{1,15}$");

    private DigitoVerificacionNit() {
    }

    /**
     * Calcula el dígito de verificación de un NIT: multiplica cada dígito, desde la derecha, por su
     * peso y suma; si el residuo de dividir la suma por 11 es 0 o 1, ese es el dígito; si no, es 11
     * menos el residuo.
     *
     * @param numero el NIT sin el dígito de verificación, solo con números; por ejemplo {@code 900123456}
     * @return el dígito, de 0 a 9
     * @throws IllegalArgumentException si {@code numero} no tiene de 1 a 15 números
     */
    public static int calcular(String numero) {
        if (numero == null || !SOLO_NUMEROS.matcher(numero).matches()) {
            throw new IllegalArgumentException("El NIT debe tener de 1 a 15 números: " + numero);
        }
        int suma = 0;
        for (int i = 0; i < numero.length(); i++) {
            int digito = numero.charAt(numero.length() - 1 - i) - '0';
            suma += digito * PESOS[i];
        }
        int residuo = suma % 11;
        return residuo <= 1 ? residuo : 11 - residuo;
    }

    /**
     * Dice si un NIT escrito con su dígito de verificación, como {@code 900123456-8}, tiene el dígito
     * correcto.
     *
     * @param nit el NIT con el formato {@code ^[0-9]{6,10}-[0-9]$}
     * @return {@code true} si el dígito corresponde; {@code false} si no corresponde, si falta o si el
     *         NIT no tiene ese formato
     */
    public static boolean esValido(String nit) {
        if (nit == null) {
            return false;
        }
        Matcher partes = NIT_CON_DIGITO.matcher(nit);
        return partes.matches() && calcular(partes.group(1)) == partes.group(2).charAt(0) - '0';
    }
}
