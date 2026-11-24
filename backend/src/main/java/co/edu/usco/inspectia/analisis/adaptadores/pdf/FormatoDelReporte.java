/*
 * Nombre: FormatoDelReporte.java
 * Descripción: Los números, las fechas y los textos del reporte PDF en su idioma, con el mismo formato de la página: el
 *              porcentaje con un decimal, el rango probable recortado a 0–100 % (D-43), los puntos del aviso de la
 *              sugerencia (D-47) y las fechas en la hora de Colombia (RF-15 C2).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.adaptadores.pdf;

import co.edu.usco.inspectia.analisis.aplicacion.IdiomaDelReporte;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.context.MessageSource;

/**
 * Da formato a lo que escribe el reporte, en su idioma (RF-15 C2). Repite las reglas de la página
 * ({@code frontend/src/lote/resultado.js} y {@code fechas.js}) con lo guardado, sin recalcular nada:
 * <ul>
 *   <li>El porcentaje con un decimal, «90,6» en español y «90.6» en inglés, sin separador de miles.</li>
 *   <li>El rango probable es el porcentaje más y menos el margen, recortado a 0–100 %: un extremo recortado va sin
 *       decimales («100») y los demás con uno (D-43).</li>
 *   <li>Los puntos del aviso de la sugerencia van con un decimal, y el límite sin decimales si es entero (D-47).</li>
 *   <li>Las fechas y horas, en la hora de Colombia: «20/10/2026» y «11:42 a. m.» en español, «10/20/2026» y
 *       «11:42 AM» en inglés. La fecha de recepción es un día del calendario, sin zona.</li>
 * </ul>
 */
final class FormatoDelReporte {

    /** La zona de las fechas y horas, como en la página. */
    static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");

    private static final BigDecimal CERO = BigDecimal.ZERO;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final IdiomaDelReporte idioma;
    private final MessageSource mensajes;
    private final DateTimeFormatter dia;
    private final DateTimeFormatter hora;

    /**
     * Crea el formato de un idioma.
     *
     * @param idioma el idioma del reporte
     * @param mensajes los textos del backend, en {@code i18n/mensajes} e {@code i18n/generado}
     */
    FormatoDelReporte(IdiomaDelReporte idioma, MessageSource mensajes) {
        this.idioma = idioma;
        this.mensajes = mensajes;
        this.dia = DateTimeFormatter.ofPattern(idioma == IdiomaDelReporte.EN ? "MM/dd/yyyy" : "dd/MM/yyyy",
                idioma.region());
        this.hora = DateTimeFormatter.ofPattern("h:mm a", idioma.region());
    }

    /**
     * Un texto del reporte, con sus valores ya escritos. Los números enteros van tal cual, sin separador de miles.
     *
     * @param clave la clave en {@code i18n/mensajes}
     * @param valores los valores de sus marcadores, en orden
     * @return el texto
     */
    String texto(String clave, Object... valores) {
        Object[] escritos = new Object[valores.length];
        for (int i = 0; i < valores.length; i++) {
            escritos[i] = valores[i] instanceof Number ? String.valueOf(valores[i]) : valores[i];
        }
        return mensajes.getMessage(clave, escritos.length == 0 ? null : escritos, idioma.region());
    }

    /**
     * El nombre de una categoría de la recomendación, como «Estándar» o «Standard».
     *
     * @param codigo el código de la categoría, como {@code ESTANDAR}
     * @return su nombre
     */
    String categoria(String codigo) {
        return texto("rango_recomendacion.categoria." + codigo);
    }

    /**
     * Un número con los decimales dados, en el formato del idioma y sin separador de miles.
     *
     * @param valor el número
     * @param decimales cuántos decimales
     * @return el texto
     */
    String decimal(BigDecimal valor, int decimales) {
        DecimalFormat formato = new DecimalFormat(decimales == 0 ? "0" : "0." + "0".repeat(decimales),
                DecimalFormatSymbols.getInstance(idioma.region()));
        formato.setRoundingMode(java.math.RoundingMode.HALF_UP);
        return formato.format(valor);
    }

    /**
     * Un porcentaje guardado, con un decimal y el signo, como «90,6 %». Entre el número y el signo va un espacio que no
     * se parte al final de una línea, como en la página.
     *
     * @param porcentaje el porcentaje
     * @return el texto
     */
    String porcentaje(BigDecimal porcentaje) {
        return decimal(porcentaje, 1) + " %";
    }

    /**
     * Los dos extremos del rango probable, como en la página (D-43).
     *
     * @param porcentaje el porcentaje corregido guardado
     * @param margen el margen guardado
     * @return «desde» y «hasta»
     */
    String[] rango(BigDecimal porcentaje, BigDecimal margen) {
        BigDecimal desde = porcentaje.subtract(margen);
        BigDecimal hasta = porcentaje.add(margen);
        return new String[] {
            desde.compareTo(CERO) < 0 ? decimal(CERO, 0) : decimal(desde, 1),
            hasta.compareTo(CIEN) > 0 ? decimal(CIEN, 0) : decimal(hasta, 1),
        };
    }

    /**
     * A cuántos puntos quedó el porcentaje del límite de la sugerencia, con un decimal (D-47).
     *
     * @param porcentaje el porcentaje corregido guardado
     * @param limite el límite guardado
     * @return los puntos
     */
    String puntos(BigDecimal porcentaje, BigDecimal limite) {
        return decimal(porcentaje.subtract(limite).abs(), 1);
    }

    /**
     * El límite de la sugerencia, sin decimales si es entero, como «90», o con uno, como «90,5» (D-47).
     *
     * @param limite el límite guardado
     * @return el texto
     */
    String limite(BigDecimal limite) {
        return decimal(limite, limite.stripTrailingZeros().scale() <= 0 ? 0 : 1);
    }

    /**
     * El día de una fecha y hora, en Colombia.
     *
     * @param instante la fecha y hora
     * @return el día, como «23/11/2026»
     */
    String dia(Instant instante) {
        return dia.format(instante.atZone(COLOMBIA));
    }

    /**
     * La hora de una fecha y hora, en Colombia.
     *
     * @param instante la fecha y hora
     * @return la hora, como «10:00 a. m.»
     */
    String hora(Instant instante) {
        return hora.format(instante.atZone(COLOMBIA)).replace(' ', ' ').replace(' ', ' ');
    }

    /**
     * Un día del calendario, sin zona, como la fecha de recepción.
     *
     * @param fecha el día
     * @return el texto, como «20/10/2026»
     */
    String dia(LocalDate fecha) {
        return dia.format(fecha);
    }
}
