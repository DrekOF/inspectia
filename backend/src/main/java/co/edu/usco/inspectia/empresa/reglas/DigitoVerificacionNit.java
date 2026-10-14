/*
 * Nombre: DigitoVerificacionNit.java
 * Descripción: Dígito de verificación del NIT con el algoritmo de la DIAN (RF-02, D-27). Por ahora
 *              solo tiene la firma: las pruebas van primero y el cálculo llega con las reglas de empresa.
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.empresa.reglas;

/**
 * Dígito de verificación del NIT, con el algoritmo de la DIAN descrito en
 * {@code specs/modelo-datos.md}. Lo usa la regla {@code nit_digito_verificacion} de
 * {@code specs/entidades/empresa.json} al crear y al editar una empresa (RF-02, D-27).
 */
public final class DigitoVerificacionNit {

    private DigitoVerificacionNit() {
    }

    /**
     * Calcula el dígito de verificación de un NIT.
     *
     * @param numero el NIT sin el dígito de verificación, solo con números; por ejemplo {@code 900123456}
     * @return el dígito, de 0 a 9
     */
    public static int calcular(String numero) {
        throw new UnsupportedOperationException("Aún no implementado");
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
        throw new UnsupportedOperationException("Aún no implementado");
    }
}
