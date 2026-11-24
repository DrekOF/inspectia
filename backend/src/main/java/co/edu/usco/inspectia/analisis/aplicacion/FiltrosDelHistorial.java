/*
 * Nombre: FiltrosDelHistorial.java
 * Descripción: Los filtros del historial de lotes, todos opcionales y combinables: la fecha del resultado desde y
 *              hasta, en días de Colombia y con las dos fechas incluidas, el estado, la categoría, el código y quien
 *              registró el lote (RF-14 C2, restricción 5).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis.aplicacion;

import co.edu.usco.inspectia.analisis.dominio.EstadoDeLote;
import co.edu.usco.inspectia.comun.texto.Textos;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Los filtros del historial (RF-14 C2). Cada uno es opcional: {@code null} no filtra. Se cumplen todos a la vez.
 *
 * <p>Las fechas son días de Colombia («Fechas» en {@code specs/modelo-datos.md}), con las dos incluidas: el filtro
 * va de las 00:00 de {@code desde} hasta antes de las 00:00 del día siguiente a {@code hasta}, en la hora de Colombia
 * ({@link #inicio()} y {@link #fin()}). Así el adaptador de la base recibe instantes y no sabe de zonas. Un lote
 * pendiente todavía no tiene fecha del resultado: no sale cuando se filtra por fecha.
 *
 * @param desde el primer día de la fecha del resultado, incluido, o {@code null}
 * @param hasta el último día de la fecha del resultado, incluido, o {@code null}
 * @param estado pendiente o analizado, o {@code null}
 * @param categoria el código de la categoría probable, como {@code ESTANDAR}, o {@code null}
 * @param codigo lo que se busca dentro del código del lote, sin distinguir mayúsculas: se guarda sin los espacios del
 *     inicio y del final y en minúsculas, y vacío queda en {@code null}
 * @param usuarioId quien registró el lote, o {@code null}; uno de otra empresa da la lista vacía (RNF-03)
 */
public record FiltrosDelHistorial(LocalDate desde, LocalDate hasta, EstadoDeLote estado, String categoria,
        String codigo, UUID usuarioId) {

    /** Deja el código como se busca, y un código o una categoría en blanco como «sin filtro». */
    public FiltrosDelHistorial {
        codigo = codigo == null || codigo.isBlank() ? null : Textos.minusculas(Textos.recortar(codigo));
        categoria = categoria == null || categoria.isBlank() ? null : categoria;
    }

    /**
     * El primer instante que entra con {@code desde}: las 00:00 de ese día en Colombia.
     *
     * @return el instante, o {@code null} si no se filtra desde una fecha
     */
    public Instant inicio() {
        return desde == null ? null : desde.atStartOfDay(RegistrarLote.COLOMBIA).toInstant();
    }

    /**
     * El primer instante que ya no entra con {@code hasta}: las 00:00 del día siguiente en Colombia, así el día
     * {@code hasta} entra completo.
     *
     * @return el instante, o {@code null} si no se filtra hasta una fecha
     */
    public Instant fin() {
        return hasta == null ? null : hasta.plusDays(1).atStartOfDay(RegistrarLote.COLOMBIA).toInstant();
    }

    /**
     * Si «Hasta» es anterior a «Resultado desde»: la búsqueda no tiene sentido y se rechaza (RF-14 restricción 5).
     * Las dos iguales sí sirven: traen los resultados de ese día.
     *
     * @return {@code true} si las dos fechas están y «Hasta» es anterior
     */
    public boolean fechasAlReves() {
        return desde != null && hasta != null && hasta.isBefore(desde);
    }
}
